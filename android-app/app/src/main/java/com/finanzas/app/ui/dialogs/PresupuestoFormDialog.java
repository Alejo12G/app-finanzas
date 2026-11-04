package com.finanzas.app.ui.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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
import com.finanzas.app.model.Presupuesto;
import com.finanzas.app.util.CurrencyUtils;
import com.finanzas.app.util.DateUtils;
import com.finanzas.app.util.UiHelper;
import com.finanzas.app.util.ValidationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dialogo de creacion (POST /presupuestos) y edicion (PUT /presupuestos/{id}).
 */
public class PresupuestoFormDialog {

    public interface OnPresupuestoSavedListener {
        void onSaved(Presupuesto presupuesto);
    }

    public static void show(
            final Context context,
            final Presupuesto existente,
            final int anioActual,
            final int mesActual,
            final List<Categoria> todasCategorias,
            final OnPresupuestoSavedListener listener) {

        final boolean esEdicion = (existente != null && existente.id > 0);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        ScrollView scroll = new ScrollView(context);
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UiHelper.COLOR_SUPERFICIE);
        int pad = UiHelper.dp(context, 16);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        root.addView(UiHelper.createSectionTitle(
                context,
                esEdicion ? "Editar presupuesto mensual" : "Nuevo presupuesto (" + DateUtils.getMonthYearLabel(anioActual, mesActual) + ")"
        ));

        final TextView globalErrorTv = UiHelper.createFieldErrorText(context);
        root.addView(globalErrorTv);

        // Categoria de gasto (inmutable en PUT segun API_CONTRACT.md 7.5.4)
        root.addView(UiHelper.createLabel(context, "Categoria de gasto *"));
        final Spinner spCategoria = new Spinner(context);
        final List<Categoria> categoriasGasto = new ArrayList<>();
        if (todasCategorias != null) {
            for (Categoria c : todasCategorias) {
                if ("GASTO".equals(c.tipo) && (c.activa || (esEdicion && existente.categoriaId == c.id))) {
                    categoriasGasto.add(c);
                }
            }
        }
        spCategoria.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, categoriasGasto));
        final TextView errCategoria = UiHelper.createFieldErrorText(context);
        root.addView(spCategoria);
        root.addView(errCategoria);

        // Monto limite
        root.addView(UiHelper.createLabel(context, "Monto limite mensual (" + CurrencyUtils.getActiveCurrency() + ") *"));
        final EditText etLimite = UiHelper.createInput(context, "0.00");
        etLimite.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        final TextView errLimite = UiHelper.createFieldErrorText(context);
        root.addView(etLimite);
        root.addView(errLimite);

        // Notas
        root.addView(UiHelper.createLabel(context, "Notas (opcional)"));
        final EditText etNotas = UiHelper.createInput(context, "Observaciones del presupuesto");
        final TextView errNotas = UiHelper.createFieldErrorText(context);
        root.addView(etNotas);
        root.addView(errNotas);

        if (esEdicion) {
            for (int i = 0; i < categoriasGasto.size(); i++) {
                if (categoriasGasto.get(i).id == existente.categoriaId) {
                    spCategoria.setSelection(i);
                    break;
                }
            }
            spCategoria.setEnabled(false);
            etLimite.setText(CurrencyUtils.centavosToInputString(existente.montoLimiteCentavos));
            etNotas.setText(existente.notas != null ? existente.notas : "");
        }

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
                UiHelper.setFieldError(etLimite, errLimite, null);
                UiHelper.setFieldError(etNotas, errNotas, null);
                UiHelper.setFieldError(null, errCategoria, null);

                long limiteCentavos = CurrencyUtils.parseMontoToCentavos(etLimite.getText().toString());
                String notas = etNotas.getText().toString().trim();

                boolean valid = true;
                if (!ValidationUtils.isValidMontoCentavos(limiteCentavos)) {
                    UiHelper.setFieldError(etLimite, errLimite, "El monto limite debe ser mayor a 0.00.");
                    valid = false;
                }
                if (notas.length() > 1000) {
                    UiHelper.setFieldError(etNotas, errNotas, "Maximo 1000 caracteres.");
                    valid = false;
                } else if (ValidationUtils.containsEmoji(notas)) {
                    UiHelper.setFieldError(etNotas, errNotas, "No se permiten emojis.");
                    valid = false;
                }
                if (categoriasGasto.isEmpty() || spCategoria.getSelectedItemPosition() < 0) {
                    UiHelper.setFieldError(null, errCategoria, "Seleccione una categoria.");
                    valid = false;
                }

                if (!valid) return;

                btnGuardar.setEnabled(false);
                ApiCallback<Presupuesto> cb = new ApiCallback<Presupuesto>() {
                    @Override
                    public void onSuccess(Presupuesto data, PaginacionMeta meta) {
                        btnGuardar.setEnabled(true);
                        dialog.dismiss();
                        if (listener != null) listener.onSaved(data);
                    }

                    @Override
                    public void onError(ApiError error) {
                        btnGuardar.setEnabled(true);
                        UiHelper.setFieldError(null, globalErrorTv, error.mensaje);
                        Map<String, String> map = error.getMapaErroresPorCampo();
                        if (map.containsKey("monto_limite_centavos")) {
                            UiHelper.setFieldError(etLimite, errLimite, map.get("monto_limite_centavos"));
                        }
                        if (map.containsKey("categoria_id")) {
                            UiHelper.setFieldError(null, errCategoria, map.get("categoria_id"));
                        }
                        if (map.containsKey("notas")) {
                            UiHelper.setFieldError(etNotas, errNotas, map.get("notas"));
                        }
                    }
                };

                if (esEdicion) {
                    ApiClient.getInstance().actualizarPresupuesto(existente.id, limiteCentavos, notas, cb);
                } else {
                    int catId = categoriasGasto.get(spCategoria.getSelectedItemPosition()).id;
                    ApiClient.getInstance().crearPresupuesto(catId, anioActual, mesActual, limiteCentavos, notas, cb);
                }
            }
        });

        dialog.show();
    }
}
