package com.finanzas.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.finanzas.app.MainActivity;
import com.finanzas.app.R;
import com.finanzas.app.api.ApiCallback;
import com.finanzas.app.api.ApiClient;
import com.finanzas.app.api.ApiError;
import com.finanzas.app.model.Compra;
import com.finanzas.app.model.PaginacionMeta;
import com.finanzas.app.model.Transaccion;
import com.finanzas.app.ui.dialogs.CompraDetalleDialog;
import com.finanzas.app.ui.dialogs.CompraFormDialog;
import com.finanzas.app.util.CurrencyUtils;
import com.finanzas.app.util.FaIcon;
import com.finanzas.app.util.UiHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vista de Compras con desglose de articulos (UI_DESIGN.md Seccion 6 y API_CONTRACT.md 7.4).
 */
public class ComprasView extends ScrollView {

    private final MainActivity host;
    private int paginaActual = 1;
    private int totalPaginas = 1;
    private int totalRegistros = 0;

    private EditText etBuscarComercio;
    private EditText etBuscarArticulo;
    private LinearLayout listContainer;
    private TextView tvPaginacion;
    private LinearLayout btnPrev;
    private LinearLayout btnNext;

    public ComprasView(Context context, MainActivity host) {
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

        // Boton principal: Nueva compra con desglose
        LinearLayout btnNueva = UiHelper.createActionButton(
                context,
                "fa-solid fa-plus",
                "Registrar compra con desglose de articulos",
                R.drawable.bg_button_primary,
                UiHelper.COLOR_TEXTO_INVERTIDO
        );
        LinearLayout.LayoutParams newLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UiHelper.dp(context, 48)
        );
        newLp.bottomMargin = UiHelper.dp(context, 12);
        btnNueva.setLayoutParams(newLp);
        btnNueva.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                CompraFormDialog.show(context, null, host.getCachedCategorias(), new CompraFormDialog.OnCompraSavedListener() {
                    @Override
                    public void onSaved(Compra compra) {
                        paginaActual = 1;
                        loadData();
                    }
                });
            }
        });
        root.addView(btnNueva);

        // Filtros especificos de compras (buscar comercio/descripcion + buscar por nombre de articulo)
        LinearLayout filterCard = UiHelper.createCard(context);
        filterCard.addView(UiHelper.createSectionTitle(context, "Buscar en tickets y articulos"));

        etBuscarComercio = UiHelper.createInput(context, "Comercio o descripcion (ej. Supermaxi)");
        filterCard.addView(etBuscarComercio);

        etBuscarArticulo = UiHelper.createInput(context, "Contiene el articulo (ej. Leche, Arroz)");
        LinearLayout.LayoutParams artInLp = (LinearLayout.LayoutParams) etBuscarArticulo.getLayoutParams();
        artInLp.topMargin = UiHelper.dp(context, 8);
        etBuscarArticulo.setLayoutParams(artInLp);
        filterCard.addView(etBuscarArticulo);

        LinearLayout btnRow = new LinearLayout(context);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams bRowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        bRowLp.topMargin = UiHelper.dp(context, 10);
        btnRow.setLayoutParams(bRowLp);

        LinearLayout btnLimpiar = UiHelper.createActionButton(
                context, "fa-solid fa-filter-circle-xmark", "Limpiar", R.drawable.bg_button_secondary, UiHelper.COLOR_SECUNDARIO
        );
        LinearLayout.LayoutParams clLp = (LinearLayout.LayoutParams) btnLimpiar.getLayoutParams();
        clLp.rightMargin = UiHelper.dp(context, 8);
        btnLimpiar.setLayoutParams(clLp);
        btnLimpiar.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                etBuscarComercio.setText("");
                etBuscarArticulo.setText("");
                paginaActual = 1;
                loadData();
            }
        });

        LinearLayout btnBuscar = UiHelper.createActionButton(
                context, "fa-solid fa-magnifying-glass", "Filtrar compras", R.drawable.bg_button_primary, UiHelper.COLOR_TEXTO_INVERTIDO
        );
        btnBuscar.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                paginaActual = 1;
                loadData();
            }
        });

        btnRow.addView(btnLimpiar);
        btnRow.addView(btnBuscar);
        filterCard.addView(btnRow);
        root.addView(filterCard);

        // Lista de compras
        listContainer = new LinearLayout(context);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(listContainer);

        // Paginacion
        LinearLayout pagCard = UiHelper.createCard(context);
        pagCard.setOrientation(LinearLayout.HORIZONTAL);
        pagCard.setGravity(Gravity.CENTER_VERTICAL);
        int pad8 = UiHelper.dp(context, 8);
        pagCard.setPadding(pad8, pad8, pad8, pad8);

        btnPrev = UiHelper.createActionButton(
                context, "fa-solid fa-chevron-left", "Anterior", R.drawable.bg_button_secondary, UiHelper.COLOR_PRIMARIO
        );
        btnPrev.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (paginaActual > 1) {
                    paginaActual--;
                    loadData();
                }
            }
        });

        tvPaginacion = new TextView(context);
        tvPaginacion.setGravity(Gravity.CENTER);
        tvPaginacion.setTextColor(UiHelper.COLOR_TEXTO_SECUNDARIO);
        tvPaginacion.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvPaginacion.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        btnNext = UiHelper.createActionButton(
                context, "fa-solid fa-chevron-right", "Siguiente", R.drawable.bg_button_secondary, UiHelper.COLOR_PRIMARIO
        );
        btnNext.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (paginaActual < totalPaginas) {
                    paginaActual++;
                    loadData();
                }
            }
        });

        pagCard.addView(btnPrev);
        pagCard.addView(tvPaginacion);
        pagCard.addView(btnNext);
        root.addView(pagCard);
    }

    public void loadData() {
        Map<String, String> params = new HashMap<>();
        params.put("pagina", String.valueOf(paginaActual));
        params.put("por_pagina", "50");

        String q = etBuscarComercio.getText().toString().trim();
        if (!q.isEmpty()) params.put("buscar", q);
        String art = etBuscarArticulo.getText().toString().trim();
        if (!art.isEmpty()) params.put("articulo", art);

        host.setGlobalLoading(true);
        ApiClient.getInstance().getCompras(params, new ApiCallback<List<Transaccion>>() {
            @Override
            public void onSuccess(List<Transaccion> data, PaginacionMeta meta) {
                host.setGlobalLoading(false);
                if (meta != null) {
                    paginaActual = meta.pagina;
                    totalPaginas = Math.max(1, meta.totalPaginas);
                    totalRegistros = meta.totalRegistros;
                } else {
                    totalPaginas = 1;
                    totalRegistros = data != null ? data.size() : 0;
                }
                renderCompras(data);
            }

            @Override
            public void onError(ApiError error) {
                host.setGlobalLoading(false);
                host.handleGlobalApiError(error);
            }
        });
    }

    private void renderCompras(List<Transaccion> compras) {
        listContainer.removeAllViews();
        tvPaginacion.setText("Pag. " + paginaActual + " de " + totalPaginas + " (" + totalRegistros + " compras)");
        btnPrev.setEnabled(paginaActual > 1);
        btnNext.setEnabled(paginaActual < totalPaginas);

        if (compras == null || compras.isEmpty()) {
            LinearLayout empty = UiHelper.createCard(getContext());
            empty.setGravity(Gravity.CENTER);
            TextView tv = new TextView(getContext());
            tv.setText("No hay compras registradas con estos criterios.");
            tv.setTextColor(UiHelper.COLOR_TEXTO_SECUNDARIO);
            empty.addView(tv);
            listContainer.addView(empty);
            return;
        }

        for (final Transaccion c : compras) {
            LinearLayout card = UiHelper.createCard(getContext());
            int pad12 = UiHelper.dp(getContext(), 12);
            card.setPadding(pad12, pad12, pad12, pad12);
            LinearLayout.LayoutParams cLp = (LinearLayout.LayoutParams) card.getLayoutParams();
            cLp.bottomMargin = UiHelper.dp(getContext(), 8);
            card.setLayoutParams(cLp);

            LinearLayout header = new LinearLayout(getContext());
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);

            TextView iconTv = new TextView(getContext());
            FaIcon.apply(iconTv, "fa-solid fa-receipt");
            iconTv.setTextColor(UiHelper.COLOR_PRIMARIO);
            iconTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            LinearLayout.LayoutParams icLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            icLp.rightMargin = UiHelper.dp(getContext(), 10);
            iconTv.setLayoutParams(icLp);
            header.addView(iconTv);

            LinearLayout infoCol = new LinearLayout(getContext());
            infoCol.setOrientation(LinearLayout.VERTICAL);
            infoCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvComercio = new TextView(getContext());
            tvComercio.setText((c.comercio != null && !c.comercio.isEmpty()) ? c.comercio : c.descripcion);
            tvComercio.setTextColor(UiHelper.COLOR_TEXTO);
            tvComercio.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            tvComercio.setTypeface(Typeface.DEFAULT_BOLD);

            String catName = c.categoria != null ? c.categoria.nombre : "Gasto";
            TextView tvSub = new TextView(getContext());
            tvSub.setText(c.fecha + " | " + catName + " | " + c.cantidadArticulos + " articulos | " + FaIcon.getMetodoPagoLabel(c.metodoPago));
            tvSub.setTextColor(UiHelper.COLOR_TEXTO_SECUNDARIO);
            tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);

            infoCol.addView(tvComercio);
            infoCol.addView(tvSub);
            header.addView(infoCol);

            TextView tvTotal = new TextView(getContext());
            tvTotal.setText(CurrencyUtils.formatPorTipo(c.montoCentavos, "GASTO"));
            tvTotal.setTextColor(UiHelper.COLOR_PELIGRO);
            tvTotal.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            tvTotal.setTypeface(Typeface.DEFAULT_BOLD);
            UiHelper.applyTabularNums(tvTotal);
            header.addView(tvTotal);

            card.addView(header);

            // Botones Ver desglose | Editar | Eliminar
            LinearLayout actions = new LinearLayout(getContext());
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setGravity(Gravity.END);
            LinearLayout.LayoutParams aLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            aLp.topMargin = UiHelper.dp(getContext(), 8);
            actions.setLayoutParams(aLp);

            LinearLayout btnVer = UiHelper.createActionButton(
                    getContext(), "fa-solid fa-eye", "Ver ticket", R.drawable.bg_button_secondary, UiHelper.COLOR_PRIMARIO
            );
            LinearLayout.LayoutParams vLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, UiHelper.dp(getContext(), 36)
            );
            vLp.rightMargin = UiHelper.dp(getContext(), 8);
            btnVer.setLayoutParams(vLp);
            btnVer.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    verTicketCompra(c.id);
                }
            });

            LinearLayout btnEdit = UiHelper.createActionButton(
                    getContext(), "fa-solid fa-pen", "Editar", R.drawable.bg_button_secondary, UiHelper.COLOR_SECUNDARIO
            );
            LinearLayout.LayoutParams eLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, UiHelper.dp(getContext(), 36)
            );
            eLp.rightMargin = UiHelper.dp(getContext(), 8);
            btnEdit.setLayoutParams(eLp);
            btnEdit.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    editarCompra(c.id);
                }
            });

            LinearLayout btnDel = UiHelper.createActionButton(
                    getContext(), "fa-solid fa-trash-can", "Eliminar", R.drawable.bg_button_danger, UiHelper.COLOR_TEXTO_INVERTIDO
            );
            btnDel.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, UiHelper.dp(getContext(), 36)
            ));
            btnDel.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmarEliminarCompra(c.id);
                }
            });

            actions.addView(btnVer);
            actions.addView(btnEdit);
            actions.addView(btnDel);
            card.addView(actions);

            listContainer.addView(card);
        }
    }

    private void verTicketCompra(int id) {
        host.setGlobalLoading(true);
        ApiClient.getInstance().getCompraDetalle(id, new ApiCallback<Compra>() {
            @Override
            public void onSuccess(Compra data, PaginacionMeta meta) {
                host.setGlobalLoading(false);
                CompraDetalleDialog.show(getContext(), data, new CompraDetalleDialog.OnTicketActionListener() {
                    @Override
                    public void onEdit(Compra compra) {
                        CompraFormDialog.show(getContext(), compra, host.getCachedCategorias(), new CompraFormDialog.OnCompraSavedListener() {
                            @Override
                            public void onSaved(Compra c) {
                                loadData();
                            }
                        });
                    }

                    @Override
                    public void onDelete(Compra compra) {
                        confirmarEliminarCompra(compra.id);
                    }
                });
            }

            @Override
            public void onError(ApiError error) {
                host.setGlobalLoading(false);
                Toast.makeText(getContext(), error.mensaje, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void editarCompra(int id) {
        host.setGlobalLoading(true);
        ApiClient.getInstance().getCompraDetalle(id, new ApiCallback<Compra>() {
            @Override
            public void onSuccess(Compra data, PaginacionMeta meta) {
                host.setGlobalLoading(false);
                CompraFormDialog.show(getContext(), data, host.getCachedCategorias(), new CompraFormDialog.OnCompraSavedListener() {
                    @Override
                    public void onSaved(Compra compra) {
                        loadData();
                    }
                });
            }

            @Override
            public void onError(ApiError error) {
                host.setGlobalLoading(false);
                Toast.makeText(getContext(), error.mensaje, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmarEliminarCompra(final int id) {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar compra")
                .setMessage(getContext().getString(R.string.confirmar_eliminar_compra))
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        host.setGlobalLoading(true);
                        ApiClient.getInstance().eliminarCompra(id, new ApiCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean data, PaginacionMeta meta) {
                                host.setGlobalLoading(false);
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
