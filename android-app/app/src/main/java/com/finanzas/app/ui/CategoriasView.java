package com.finanzas.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.finanzas.app.MainActivity;
import com.finanzas.app.R;
import com.finanzas.app.api.ApiCallback;
import com.finanzas.app.api.ApiClient;
import com.finanzas.app.api.ApiError;
import com.finanzas.app.model.Categoria;
import com.finanzas.app.model.PaginacionMeta;
import com.finanzas.app.ui.dialogs.CategoriaFormDialog;
import com.finanzas.app.util.FaIcon;
import com.finanzas.app.util.UiHelper;

import java.util.List;

/**
 * Vista de Gestion de Categorias agrupadas por tipo (UI_DESIGN.md Seccion 6 y API_CONTRACT.md 7.2).
 */
public class CategoriasView extends ScrollView {

    private final MainActivity host;
    private CheckBox cbIncluirInactivas;
    private LinearLayout ingresosContainer;
    private LinearLayout gastosContainer;

    public CategoriasView(Context context, MainActivity host) {
        super(context);
        this.host = host;
        setFillViewport(true);
        initUi(context);
    }

    private void initUi(final Context context) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = UiHelper.dp(context, 16);
        root.setPadding(pad, pad, pad, pad);
        addView(root);

        // Barra superior: Incluir inactivas + Nueva categoria
        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams topLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        topLp.bottomMargin = UiHelper.dp(context, 12);
        topRow.setLayoutParams(topLp);

        cbIncluirInactivas = new CheckBox(context);
        cbIncluirInactivas.setText("Mostrar inactivas");
        cbIncluirInactivas.setTextColor(UiHelper.COLOR_TEXTO);
        cbIncluirInactivas.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        cbIncluirInactivas.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                loadData();
            }
        });

        LinearLayout btnNueva = UiHelper.createActionButton(
                context, "fa-solid fa-plus", "Nueva categoria", R.drawable.bg_button_primary, UiHelper.COLOR_TEXTO_INVERTIDO
        );
        btnNueva.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                CategoriaFormDialog.show(context, null, new CategoriaFormDialog.OnCategoriaSavedListener() {
                    @Override
                    public void onSaved(Categoria categoria) {
                        host.refreshCategoriasCache();
                        loadData();
                    }
                });
            }
        });

        topRow.addView(cbIncluirInactivas);
        topRow.addView(btnNueva);
        root.addView(topRow);

        // Grupo INGRESO
        LinearLayout cardIngresos = UiHelper.createCard(context);
        cardIngresos.addView(UiHelper.createSectionTitle(context, "Categorias de Ingreso (INGRESO)"));
        ingresosContainer = new LinearLayout(context);
        ingresosContainer.setOrientation(LinearLayout.VERTICAL);
        cardIngresos.addView(ingresosContainer);
        root.addView(cardIngresos);

        // Grupo GASTO
        LinearLayout cardGastos = UiHelper.createCard(context);
        cardGastos.addView(UiHelper.createSectionTitle(context, "Categorias de Gasto (GASTO)"));
        gastosContainer = new LinearLayout(context);
        gastosContainer.setOrientation(LinearLayout.VERTICAL);
        cardGastos.addView(gastosContainer);
        root.addView(cardGastos);
    }

    public void loadData() {
        host.setGlobalLoading(true);
        ApiClient.getInstance().getCategorias(null, cbIncluirInactivas.isChecked(), new ApiCallback<List<Categoria>>() {
            @Override
            public void onSuccess(List<Categoria> data, PaginacionMeta meta) {
                host.setGlobalLoading(false);
                renderCategorias(data);
            }

            @Override
            public void onError(ApiError error) {
                host.setGlobalLoading(false);
                host.handleGlobalApiError(error);
            }
        });
    }

    private void renderCategorias(List<Categoria> categorias) {
        ingresosContainer.removeAllViews();
        gastosContainer.removeAllViews();
        if (categorias == null) return;

        for (final Categoria c : categorias) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int pad8 = UiHelper.dp(getContext(), 8);
            row.setPadding(pad8, pad8, pad8, pad8);

            TextView iconTv = new TextView(getContext());
            FaIcon.apply(iconTv, c.icono);
            int catColor = UiHelper.parseColorSafe(c.color, UiHelper.COLOR_SECUNDARIO);
            iconTv.setTextColor(UiHelper.COLOR_TEXTO_INVERTIDO);
            iconTv.setBackground(UiHelper.createRoundedBox(getContext(), catColor, catColor, 0, 4));
            iconTv.setGravity(Gravity.CENTER);
            iconTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            LinearLayout.LayoutParams icLp = new LinearLayout.LayoutParams(
                    UiHelper.dp(getContext(), 34), UiHelper.dp(getContext(), 34)
            );
            icLp.rightMargin = UiHelper.dp(getContext(), 10);
            iconTv.setLayoutParams(icLp);
            row.addView(iconTv);

            LinearLayout nameCol = new LinearLayout(getContext());
            nameCol.setOrientation(LinearLayout.VERTICAL);
            nameCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvNombre = new TextView(getContext());
            tvNombre.setText(c.nombre + (c.activa ? "" : " (Inactiva)"));
            tvNombre.setTextColor(c.activa ? UiHelper.COLOR_TEXTO : UiHelper.COLOR_TEXTO_DESHABILITADO);
            tvNombre.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tvNombre.setTypeface(Typeface.DEFAULT_BOLD);

            TextView tvSub = new TextView(getContext());
            tvSub.setText(c.icono + " | " + c.color);
            tvSub.setTextColor(UiHelper.COLOR_TEXTO_SECUNDARIO);
            tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);

            nameCol.addView(tvNombre);
            nameCol.addView(tvSub);
            row.addView(nameCol);

            // Boton Editar
            TextView btnEdit = new TextView(getContext());
            FaIcon.apply(btnEdit, "fa-solid fa-pen");
            btnEdit.setTextColor(UiHelper.COLOR_SECUNDARIO);
            btnEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            btnEdit.setPadding(pad8, pad8, pad8, pad8);
            btnEdit.setContentDescription("Editar categoria");
            btnEdit.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    CategoriaFormDialog.show(getContext(), c, new CategoriaFormDialog.OnCategoriaSavedListener() {
                        @Override
                        public void onSaved(Categoria categoria) {
                            host.refreshCategoriasCache();
                            loadData();
                        }
                    });
                }
            });
            row.addView(btnEdit);

            // Boton Eliminar
            TextView btnDel = new TextView(getContext());
            FaIcon.apply(btnDel, "fa-solid fa-trash-can");
            btnDel.setTextColor(UiHelper.COLOR_PELIGRO);
            btnDel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            btnDel.setPadding(pad8, pad8, pad8, pad8);
            btnDel.setContentDescription("Eliminar categoria");
            btnDel.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmarEliminarCategoria(c);
                }
            });
            row.addView(btnDel);

            if ("INGRESO".equals(c.tipo)) {
                ingresosContainer.addView(row);
            } else {
                gastosContainer.addView(row);
            }
        }
    }

    private void confirmarEliminarCategoria(final Categoria c) {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar categoria")
                .setMessage(getContext().getString(R.string.confirmar_eliminar_categoria))
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        host.setGlobalLoading(true);
                        ApiClient.getInstance().eliminarCategoria(c.id, new ApiCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean data, PaginacionMeta meta) {
                                host.setGlobalLoading(false);
                                host.refreshCategoriasCache();
                                loadData();
                            }

                            @Override
                            public void onError(ApiError error) {
                                host.setGlobalLoading(false);
                                // API_CONTRACT.md 7.2.4: Si devuelve 409 CATEGORIA_EN_USO, sugerir desactivarla con PUT activa: false
                                if (error.httpStatus == 409 || "CATEGORIA_EN_USO".equals(error.codigo)) {
                                    ofrecerDesactivarCategoria(c, error.mensaje);
                                } else {
                                    Toast.makeText(getContext(), error.mensaje, Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    }
                })
                .show();
    }

    private void ofrecerDesactivarCategoria(final Categoria c, String mensajeError) {
        new AlertDialog.Builder(getContext())
                .setTitle("Categoria en uso")
                .setMessage(mensajeError + "\n\nDesea desactivar esta categoria para que no aparezca en nuevos registros?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Desactivar categoria", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        host.setGlobalLoading(true);
                        ApiClient.getInstance().actualizarCategoria(c.id, c.nombre, c.icono, c.color, false, new ApiCallback<Categoria>() {
                            @Override
                            public void onSuccess(Categoria data, PaginacionMeta meta) {
                                host.setGlobalLoading(false);
                                host.refreshCategoriasCache();
                                loadData();
                            }

                            @Override
                            public void onError(ApiError error) {
                                host.setGlobalLoading(false);
                                Toast.makeText(getContext(), error.mensaje, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                })
                .show();
    }
}
