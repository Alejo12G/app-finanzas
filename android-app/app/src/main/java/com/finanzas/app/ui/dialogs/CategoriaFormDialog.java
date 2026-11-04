package com.finanzas.app.ui.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.finanzas.app.R;
import com.finanzas.app.api.ApiCallback;
import com.finanzas.app.api.ApiClient;
import com.finanzas.app.api.ApiError;
import com.finanzas.app.model.Categoria;
import com.finanzas.app.model.PaginacionMeta;
import com.finanzas.app.util.FaIcon;
import com.finanzas.app.util.UiHelper;
import com.finanzas.app.util.ValidationUtils;

import java.util.List;
import java.util.Map;

/**
 * Dialogo de creacion (POST /categorias) y edicion (PUT /categorias/{id})
 * con selector de iconos FontAwesome oficiales (UI_DESIGN.md 5.6 y 5.7) y paleta aprobada.
 */
public class CategoriaFormDialog {

    public interface OnCategoriaSavedListener {
        void onSaved(Categoria categoria);
    }

    private static final String[] TIPOS = {"GASTO", "INGRESO"};
    private static final String[] COLORES_PALETA = {"#4A5568", "#1B2A4A", "#2E7D32", "#C62828", "#B26A00"};

    public static void show(
            final Context context,
            final Categoria existente,
            final OnCategoriaSavedListener listener) {

        final boolean esEdicion = (existente != null && existente.id > 0);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        ScrollView scroll = new ScrollView(context);
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UiHelper.COLOR_SUPERFICIE);
        int pad = UiHelper.dp(context, 16);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        root.addView(UiHelper.createSectionTitle(context, esEdicion ? "Editar categoria" : "Nueva categoria"));

        final TextView globalErrorTv = UiHelper.createFieldErrorText(context);
        root.addView(globalErrorTv);

        // Nombre
        root.addView(UiHelper.createLabel(context, "Nombre de la categoria *"));
        final EditText etNombre = UiHelper.createInput(context, "Ej. Gimnasio");
        final TextView errNombre = UiHelper.createFieldErrorText(context);
        root.addView(etNombre);
        root.addView(errNombre);

        // Tipo (inmutable en PUT segun API_CONTRACT.md 7.2.3)
        root.addView(UiHelper.createLabel(context, "Tipo *"));
        final Spinner spTipo = new Spinner(context);
        spTipo.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, TIPOS));
        if (esEdicion) {
            spTipo.setSelection("INGRESO".equals(existente.tipo) ? 1 : 0);
            spTipo.setEnabled(false);
        }
        root.addView(spTipo);

        // Selector de Color de la paleta
        root.addView(UiHelper.createLabel(context, "Color de la categoria *"));
        final String[] selectedColor = {esEdicion && existente.color != null ? existente.color : "#4A5568"};
        final LinearLayout colorRow = new LinearLayout(context);
        colorRow.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(colorRow);

        final Runnable renderColorChips = new Runnable() {
            @Override
            public void run() {
                colorRow.removeAllViews();
                for (final String hex : COLORES_PALETA) {
                    TextView chip = new TextView(context);
                    boolean active = hex.equalsIgnoreCase(selectedColor[0]);
                    int fill = UiHelper.parseColorSafe(hex, UiHelper.COLOR_SECUNDARIO);
                    chip.setBackground(UiHelper.createRoundedBox(
                            context,
                            fill,
                            active ? UiHelper.COLOR_TEXTO : fill,
                            active ? 3 : 0,
                            4
                    ));
                    chip.setGravity(Gravity.CENTER);
                    chip.setTextColor(UiHelper.COLOR_TEXTO_INVERTIDO);
                    if (active) {
                        FaIcon.apply(chip, "fa-solid fa-check");
                    }
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            UiHelper.dp(context, 44),
                            UiHelper.dp(context, 36)
                    );
                    lp.rightMargin = UiHelper.dp(context, 8);
                    chip.setLayoutParams(lp);
                    chip.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            selectedColor[0] = hex;
                            run();
                        }
                    });
                    colorRow.addView(chip);
                }
            }
        };
        renderColorChips.run();

        // Selector de Icono FontAwesome (UI_DESIGN.md 5.6 y 5.7)
        root.addView(UiHelper.createLabel(context, "Icono FontAwesome 6 *"));
        final String[] selectedIcon = {esEdicion && existente.icono != null ? existente.icono : "fa-solid fa-tag"};
        final TextView tvSelectedIconName = new TextView(context);
        tvSelectedIconName.setText("Seleccionado: " + selectedIcon[0]);
        tvSelectedIconName.setTextColor(UiHelper.COLOR_TEXTO_SECUNDARIO);
        tvSelectedIconName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        root.addView(tvSelectedIconName);

        HorizontalScrollView iconScroll = new HorizontalScrollView(context);
        iconScroll.setHorizontalScrollBarEnabled(false);
        final LinearLayout iconContainer = new LinearLayout(context);
        iconContainer.setOrientation(LinearLayout.HORIZONTAL);
        iconContainer.setPadding(0, UiHelper.dp(context, 6), 0, UiHelper.dp(context, 6));
        iconScroll.addView(iconContainer);
        root.addView(iconScroll);

        final List<String> iconChoices = FaIcon.SELECTOR_CATEGORY_ICONS;
        final Runnable renderIconChoices = new Runnable() {
            @Override
            public void run() {
                iconContainer.removeAllViews();
                for (final String ic : iconChoices) {
                    TextView btn = new TextView(context);
                    FaIcon.apply(btn, ic);
                    boolean active = ic.equals(selectedIcon[0]);
                    btn.setGravity(Gravity.CENTER);
                    btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                    btn.setTextColor(active ? UiHelper.COLOR_TEXTO_INVERTIDO : UiHelper.COLOR_PRIMARIO);
                    btn.setBackground(UiHelper.createRoundedBox(
                            context,
                            active ? UiHelper.COLOR_PRIMARIO : UiHelper.COLOR_FONDO,
                            UiHelper.COLOR_BORDE,
                            1,
                            4
                    ));
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            UiHelper.dp(context, 44),
                            UiHelper.dp(context, 44)
                    );
                    lp.rightMargin = UiHelper.dp(context, 6);
                    btn.setLayoutParams(lp);
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            selectedIcon[0] = ic;
                            tvSelectedIconName.setText("Seleccionado: " + ic);
                            run();
                        }
                    });
                    iconContainer.addView(btn);
                }
            }
        };
        renderIconChoices.run();

        // Checkbox activa (en edicion)
        final CheckBox cbActiva = new CheckBox(context);
        cbActiva.setText("Categoria activa");
        cbActiva.setTextColor(UiHelper.COLOR_TEXTO);
        cbActiva.setChecked(!esEdicion || existente.activa);
        if (esEdicion) {
            root.addView(cbActiva);
            etNombre.setText(existente.nombre);
        }

        // Botones
        LinearLayout btnRow = new LinearLayout(context);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams btnRowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        btnRowLp.topMargin = UiHelper.dp(context, 16);
        btnRow.setLayoutParams(btnRowLp);

        LinearLayout btnCancelar = UiHelper.createActionButton(
                context,
                "fa-solid fa-xmark",
                "Cancelar",
                R.drawable.bg_button_secondary,
                UiHelper.COLOR_SECUNDARIO
        );
        LinearLayout.LayoutParams cancelLp = (LinearLayout.LayoutParams) btnCancelar.getLayoutParams();
        cancelLp.rightMargin = UiHelper.dp(context, 8);
        btnCancelar.setLayoutParams(cancelLp);

        final LinearLayout btnGuardar = UiHelper.createActionButton(
                context,
                "fa-solid fa-floppy-disk",
                "Guardar cambios",
                R.drawable.bg_button_primary,
                UiHelper.COLOR_TEXTO_INVERTIDO
        );

        btnRow.addView(btnCancelar);
        btnRow.addView(btnGuardar);
        root.addView(btnRow);

        builder.setView(scroll);
        final AlertDialog dialog = builder.create();

        btnCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UiHelper.setFieldError(null, globalErrorTv, null);
                UiHelper.setFieldError(etNombre, errNombre, null);

                String nombre = etNombre.getText().toString().trim();
                if (nombre.isEmpty() || nombre.length() > 60) {
                    UiHelper.setFieldError(etNombre, errNombre, "El nombre debe tener entre 1 y 60 caracteres.");
                    return;
                }
                if (ValidationUtils.containsEmoji(nombre)) {
                    UiHelper.setFieldError(etNombre, errNombre, "No se permiten emojis.");
                    return;
                }

                btnGuardar.setEnabled(false);
                ApiCallback<Categoria> cb = new ApiCallback<Categoria>() {
                    @Override
                    public void onSuccess(Categoria data, PaginacionMeta meta) {
                        btnGuardar.setEnabled(true);
                        dialog.dismiss();
                        if (listener != null) listener.onSaved(data);
                    }

                    @Override
                    public void onError(ApiError error) {
                        btnGuardar.setEnabled(true);
                        UiHelper.setFieldError(null, globalErrorTv, error.mensaje);
                        Map<String, String> map = error.getMapaErroresPorCampo();
                        if (map.containsKey("nombre")) {
                            UiHelper.setFieldError(etNombre, errNombre, map.get("nombre"));
                        }
                    }
                };

                if (esEdicion) {
                    ApiClient.getInstance().actualizarCategoria(
                            existente.id, nombre, selectedIcon[0], selectedColor[0], cbActiva.isChecked(), cb
                    );
                } else {
                    String tipo = TIPOS[spTipo.getSelectedItemPosition()];
                    ApiClient.getInstance().crearCategoria(
                            nombre, tipo, selectedIcon[0], selectedColor[0], cb
                    );
                }
            }
        });

        dialog.show();
    }
}
