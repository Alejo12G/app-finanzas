package com.finanzas.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.finanzas.app.MainActivity;
import com.finanzas.app.R;
import com.finanzas.app.api.ApiCallback;
import com.finanzas.app.api.ApiClient;
import com.finanzas.app.api.ApiError;
import com.finanzas.app.model.Categoria;
import com.finanzas.app.model.Compra;
import com.finanzas.app.model.CreditCard;
import com.finanzas.app.model.PaginacionMeta;
import com.finanzas.app.model.Transaccion;
import com.finanzas.app.ui.dialogs.CompraDetalleDialog;
import com.finanzas.app.ui.dialogs.CompraFormDialog;
import com.finanzas.app.ui.dialogs.GastoRapidoIaDialog;
import com.finanzas.app.ui.dialogs.TransaccionFormDialog;
import com.finanzas.app.util.CurrencyUtils;
import com.finanzas.app.util.DateUtils;
import com.finanzas.app.util.FaIcon;
import com.finanzas.app.util.UiHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vista de listado filtrable y paginado de Transacciones (UI_DESIGN.md Seccion 6).
 * Consume GET/POST/PUT/DELETE /transacciones.
 */
public class TransaccionesView extends ScrollView {

    private final MainActivity host;
    private int paginaActual = 1;
    private int totalPaginas = 1;
    private int totalRegistros = 0;

    private EditText etBuscar;
    private EditText etDesde;
    private EditText etHasta;
    private Spinner spTipo;
    private Spinner spNaturaleza;
    private Spinner spMetodo;
    private Spinner spCategoria;
    private LinearLayout filterPanel;

    private LinearLayout listContainer;
    private TextView tvPaginacionInfo;
    private LinearLayout btnPrevPage;
    private LinearLayout btnNextPage;

    private static final String[] FILTRO_TIPOS = {"TODOS", "GASTO", "INGRESO"};
    private static final String[] FILTRO_NATURALEZA = {"TODAS", "VARIABLE", "FIJO"};
    private static final String[] FILTRO_METODOS = {"TODOS", "EFECTIVO", "DEBITO", "CREDIT_CARD", "TRANSFERENCIA", "OTRO"};

    public TransaccionesView(Context context, MainActivity host) {
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

        // Barra de acciones superior
        LinearLayout topActions = new LinearLayout(context);
        topActions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams topLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        topLp.bottomMargin = UiHelper.dp(context, 12);
        topActions.setLayoutParams(topLp);

        LinearLayout btnNueva = UiHelper.createActionButton(
                context, "fa-solid fa-plus", "Nueva transaccion", R.drawable.bg_button_primary, UiHelper.COLOR_TEXTO_INVERTIDO
        );
        LinearLayout.LayoutParams b1Lp = new LinearLayout.LayoutParams(0, UiHelper.dp(context, 48), 1.1f);
        b1Lp.rightMargin = UiHelper.dp(context, 8);
        btnNueva.setLayoutParams(b1Lp);
        btnNueva.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TransaccionFormDialog.show(context, null, null, host.getCachedCategorias(), new TransaccionFormDialog.OnSavedListener() {
                    @Override
                    public void onSaved(Transaccion transaccion) {
                        paginaActual = 1;
                        loadData();
                    }
                });
            }
        });

        LinearLayout btnIa = UiHelper.createActionButton(
                context, "fa-solid fa-wand-magic-sparkles", "Rapido IA", R.drawable.bg_button_secondary, UiHelper.COLOR_PRIMARIO
        );
        LinearLayout.LayoutParams b2Lp = new LinearLayout.LayoutParams(0, UiHelper.dp(context, 48), 0.9f);
        b2Lp.rightMargin = UiHelper.dp(context, 8);
        btnIa.setLayoutParams(b2Lp);
        btnIa.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                GastoRapidoIaDialog.show(context, host.getCachedCategorias(), new TransaccionFormDialog.OnSavedListener() {
                    @Override
                    public void onSaved(Transaccion transaccion) {
                        paginaActual = 1;
                        loadData();
                    }
                });
            }
        });

        LinearLayout btnToggleFilters = UiHelper.createActionButton(
                context, "fa-solid fa-filter", "", R.drawable.bg_button_secondary, UiHelper.COLOR_SECUNDARIO
        );
        btnToggleFilters.setContentDescription("Mostrar u ocultar filtros");
        btnToggleFilters.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                filterPanel.setVisibility(filterPanel.getVisibility() == VISIBLE ? GONE : VISIBLE);
            }
        });

        topActions.addView(btnNueva);
        topActions.addView(btnIa);
        topActions.addView(btnToggleFilters);
        root.addView(topActions);

        // Panel de filtros colapsable
        filterPanel = UiHelper.createCard(context);
        filterPanel.setVisibility(VISIBLE);
        filterPanel.addView(UiHelper.createSectionTitle(context, "Filtros de busqueda"));

        etBuscar = UiHelper.createInput(context, "Buscar en descripcion, comercio o notas...");
        filterPanel.addView(etBuscar);

        // Rango de fechas
        LinearLayout dateRow = new LinearLayout(context);
        dateRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams dRowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        dRowLp.topMargin = UiHelper.dp(context, 8);
        dateRow.setLayoutParams(dRowLp);

        etDesde = UiHelper.createInput(context, "Desde YYYY-MM-DD");
        etDesde.setFocusable(false);
        etDesde.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                DateUtils.showDatePicker(context, etDesde);
            }
        });
        LinearLayout.LayoutParams d1Lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        d1Lp.rightMargin = UiHelper.dp(context, 8);
        etDesde.setLayoutParams(d1Lp);

        etHasta = UiHelper.createInput(context, "Hasta YYYY-MM-DD");
        etHasta.setFocusable(false);
        etHasta.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                DateUtils.showDatePicker(context, etHasta);
            }
        });
        LinearLayout.LayoutParams d2Lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        etHasta.setLayoutParams(d2Lp);

        dateRow.addView(etDesde);
        dateRow.addView(etHasta);
        filterPanel.addView(dateRow);

        // Selectores Tipo | Naturaleza | Metodo
        LinearLayout spinRow = new LinearLayout(context);
        spinRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams sRowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        sRowLp.topMargin = UiHelper.dp(context, 8);
        spinRow.setLayoutParams(sRowLp);

        spTipo = new Spinner(context);
        spTipo.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, FILTRO_TIPOS));
        spTipo.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        spNaturaleza = new Spinner(context);
        spNaturaleza.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, FILTRO_NATURALEZA));
        spNaturaleza.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        spMetodo = new Spinner(context);
        spMetodo.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, FILTRO_METODOS));
        spMetodo.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        spinRow.addView(spTipo);
        spinRow.addView(spNaturaleza);
        spinRow.addView(spMetodo);
        filterPanel.addView(spinRow);

        filterPanel.addView(UiHelper.createLabel(context, "Categoria"));
        spCategoria = new Spinner(context);
        filterPanel.addView(spCategoria);

        // Botones Aplicar / Limpiar filtros
        LinearLayout filterBtnRow = new LinearLayout(context);
        filterBtnRow.setOrientation(LinearLayout.HORIZONTAL);
        filterBtnRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams fBtnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        fBtnLp.topMargin = UiHelper.dp(context, 10);
        filterBtnRow.setLayoutParams(fBtnLp);

        LinearLayout btnLimpiar = UiHelper.createActionButton(
                context, "fa-solid fa-filter-circle-xmark", "Limpiar", R.drawable.bg_button_secondary, UiHelper.COLOR_SECUNDARIO
        );
        LinearLayout.LayoutParams clLp = (LinearLayout.LayoutParams) btnLimpiar.getLayoutParams();
        clLp.rightMargin = UiHelper.dp(context, 8);
        btnLimpiar.setLayoutParams(clLp);
        btnLimpiar.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                etBuscar.setText("");
                etDesde.setText("");
                etHasta.setText("");
                spTipo.setSelection(0);
                spNaturaleza.setSelection(0);
                spMetodo.setSelection(0);
                spCategoria.setSelection(0);
                paginaActual = 1;
                loadData();
            }
        });

        LinearLayout btnBuscar = UiHelper.createActionButton(
                context, "fa-solid fa-magnifying-glass", "Aplicar filtros", R.drawable.bg_button_primary, UiHelper.COLOR_TEXTO_INVERTIDO
        );
        btnBuscar.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                paginaActual = 1;
                loadData();
            }
        });

        filterBtnRow.addView(btnLimpiar);
        filterBtnRow.addView(btnBuscar);
        filterPanel.addView(filterBtnRow);
        root.addView(filterPanel);

        // Contenedor de lista
        listContainer = new LinearLayout(context);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(listContainer);

        // Controles de paginacion
        LinearLayout pagCard = UiHelper.createCard(context);
        pagCard.setOrientation(LinearLayout.HORIZONTAL);
        pagCard.setGravity(Gravity.CENTER_VERTICAL);
        int pad8 = UiHelper.dp(context, 8);
        pagCard.setPadding(pad8, pad8, pad8, pad8);

        btnPrevPage = UiHelper.createActionButton(
                context, "fa-solid fa-chevron-left", "Anterior", R.drawable.bg_button_secondary, UiHelper.COLOR_PRIMARIO
        );
        btnPrevPage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (paginaActual > 1) {
                    paginaActual--;
                    loadData();
                }
            }
        });

        tvPaginacionInfo = new TextView(context);
        tvPaginacionInfo.setGravity(Gravity.CENTER);
        tvPaginacionInfo.setTextColor(UiHelper.COLOR_TEXTO_SECUNDARIO);
        tvPaginacionInfo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvPaginacionInfo.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        btnNextPage = UiHelper.createActionButton(
                context, "fa-solid fa-chevron-right", "Siguiente", R.drawable.bg_button_secondary, UiHelper.COLOR_PRIMARIO
        );
        btnNextPage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (paginaActual < totalPaginas) {
                    paginaActual++;
                    loadData();
                }
            }
        });

        pagCard.addView(btnPrevPage);
        pagCard.addView(tvPaginacionInfo);
        pagCard.addView(btnNextPage);
        root.addView(pagCard);
    }

    private void refreshCategoriaFilterSpinner() {
        List<String> labels = new ArrayList<>();
        labels.add("Todas las categorias");
        List<Categoria> cats = host.getCachedCategorias();
        if (cats != null) {
            for (Categoria c : cats) {
                labels.add(c.nombre + " (" + c.tipo + ")");
            }
        }
        int prevSel = spCategoria.getSelectedItemPosition();
        spCategoria.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, labels));
        if (prevSel >= 0 && prevSel < labels.size()) {
            spCategoria.setSelection(prevSel);
        }
    }

    public void loadData() {
        refreshCategoriaFilterSpinner();
        Map<String, String> params = new HashMap<>();
        params.put("pagina", String.valueOf(paginaActual));
        params.put("por_pagina", "50");

        String q = etBuscar.getText().toString().trim();
        if (!q.isEmpty()) params.put("buscar", q);
        String d1 = etDesde.getText().toString().trim();
        if (!d1.isEmpty()) params.put("desde", d1);
        String d2 = etHasta.getText().toString().trim();
        if (!d2.isEmpty()) params.put("hasta", d2);

        int tPos = spTipo.getSelectedItemPosition();
        if (tPos > 0) params.put("tipo", FILTRO_TIPOS[tPos]);
        int nPos = spNaturaleza.getSelectedItemPosition();
        if (nPos > 0) params.put("naturaleza", FILTRO_NATURALEZA[nPos]);
        int mPos = spMetodo.getSelectedItemPosition();
        if (mPos > 0) params.put("metodo_pago", FILTRO_METODOS[mPos]);

        int cPos = spCategoria.getSelectedItemPosition();
        List<Categoria> cats = host.getCachedCategorias();
        if (cPos > 0 && cats != null && (cPos - 1) < cats.size()) {
            params.put("categoria_id", String.valueOf(cats.get(cPos - 1).id));
        }

        host.setGlobalLoading(true);
        ApiClient.getInstance().getTransacciones(params, new ApiCallback<List<Transaccion>>() {
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
                renderList(data);
            }

            @Override
            public void onError(ApiError error) {
                host.setGlobalLoading(false);
                host.handleGlobalApiError(error);
            }
        });
    }

    private void renderList(List<Transaccion> items) {
        listContainer.removeAllViews();
        tvPaginacionInfo.setText("Pag. " + paginaActual + " de " + totalPaginas + " (" + totalRegistros + " reg.)");
        btnPrevPage.setEnabled(paginaActual > 1);
        btnNextPage.setEnabled(paginaActual < totalPaginas);

        if (items == null || items.isEmpty()) {
            LinearLayout emptyCard = UiHelper.createCard(getContext());
            emptyCard.setGravity(Gravity.CENTER);
            TextView ic = new TextView(getContext());
            FaIcon.apply(ic, "fa-regular fa-folder-open");
            ic.setTextColor(UiHelper.COLOR_TEXTO_DESHABILITADO);
            ic.setTextSize(TypedValue.COMPLEX_UNIT_SP, 42);
            ic.setGravity(Gravity.CENTER);
            emptyCard.addView(ic);

            TextView msg = new TextView(getContext());
            msg.setText("No hay registros para este periodo.");
            msg.setTextColor(UiHelper.COLOR_TEXTO_SECUNDARIO);
            msg.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams mLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            mLp.topMargin = UiHelper.dp(getContext(), 8);
            msg.setLayoutParams(mLp);
            emptyCard.addView(msg);
            listContainer.addView(emptyCard);
            return;
        }

        for (final Transaccion t : items) {
            LinearLayout card = UiHelper.createCard(getContext());
            int pad12 = UiHelper.dp(getContext(), 12);
            card.setPadding(pad12, pad12, pad12, pad12);
            LinearLayout.LayoutParams cLp = (LinearLayout.LayoutParams) card.getLayoutParams();
            cLp.bottomMargin = UiHelper.dp(getContext(), 8);
            card.setLayoutParams(cLp);

            LinearLayout topRow = new LinearLayout(getContext());
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            // Icono de categoria
            TextView catIcon = new TextView(getContext());
            String icClass = (t.categoria != null && t.categoria.icono != null) ? t.categoria.icono : "fa-solid fa-tag";
            FaIcon.apply(catIcon, icClass);
            int catColor = (t.categoria != null) ? UiHelper.parseColorSafe(t.categoria.color, UiHelper.COLOR_PRIMARIO) : UiHelper.COLOR_PRIMARIO;
            catIcon.setTextColor(UiHelper.COLOR_TEXTO_INVERTIDO);
            catIcon.setBackground(UiHelper.createRoundedBox(getContext(), catColor, catColor, 0, 4));
            catIcon.setGravity(Gravity.CENTER);
            catIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            LinearLayout.LayoutParams icLp = new LinearLayout.LayoutParams(
                    UiHelper.dp(getContext(), 36), UiHelper.dp(getContext(), 36)
            );
            icLp.rightMargin = UiHelper.dp(getContext(), 10);
            catIcon.setLayoutParams(icLp);
            topRow.addView(catIcon);

            // Columna central
            LinearLayout midCol = new LinearLayout(getContext());
            midCol.setOrientation(LinearLayout.VERTICAL);
            midCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvDesc = new TextView(getContext());
            tvDesc.setText(t.descripcion);
            tvDesc.setTextColor(UiHelper.COLOR_TEXTO);
            tvDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tvDesc.setTypeface(Typeface.DEFAULT_BOLD);

            String catName = t.categoria != null ? t.categoria.nombre : "Sin categoria";
            StringBuilder subSb = new StringBuilder();
            subSb.append(t.fecha).append(" | ").append(catName).append(" | ").append(t.naturaleza);
            if (t.comercio != null && !t.comercio.isEmpty()) {
                subSb.append(" | ").append(t.comercio);
            }
            if (t.esCompra) {
                subSb.append(" | Ticket (").append(t.cantidadArticulos).append(" art.)");
            }
            TextView tvSub = new TextView(getContext());
            tvSub.setText(subSb.toString());
            tvSub.setTextColor(UiHelper.COLOR_TEXTO_SECUNDARIO);
            tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);

            midCol.addView(tvDesc);
            midCol.addView(tvSub);
            topRow.addView(midCol);

            // Monto alineado a la derecha
            TextView tvMonto = new TextView(getContext());
            tvMonto.setText(CurrencyUtils.formatPorTipo(t.montoCentavos, t.tipo));
            tvMonto.setTextColor("INGRESO".equals(t.tipo) ? UiHelper.COLOR_EXITO : UiHelper.COLOR_PELIGRO);
            tvMonto.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            tvMonto.setTypeface(Typeface.DEFAULT_BOLD);
            UiHelper.applyTabularNums(tvMonto);
            topRow.addView(tvMonto);

            card.addView(topRow);

            if (t.isCreditCardTransaction() && t.installments != null && t.installments >= 1) {
                String cardLabel = "Tarjeta #" + (t.cardId != null ? t.cardId : "");
                List<CreditCard> cards = host.getCachedCreditCards();
                if (cards != null && t.cardId != null) {
                    for (CreditCard cc : cards) {
                        if (cc.id == t.cardId) {
                            cardLabel = cc.name + " (" + cc.bank + ")";
                            break;
                        }
                    }
                }
                boolean exenta = Boolean.TRUE.equals(t.isInterestExempt)
                        || (t.appliedInterestRate != null && t.appliedInterestRate <= 0.0);
                StringBuilder finSb = new StringBuilder();
                finSb.append(cardLabel)
                        .append(" | ")
                        .append(t.installments)
                        .append(t.installments == 1 ? " cuota" : " cuotas")
                        .append(" | ")
                        .append(exenta ? "0% Exenta" : (t.appliedInterestRate + "% M.V."));
                if (t.monthlyInstallmentAmount != null) {
                    finSb.append(" | Cuota: ").append(CurrencyUtils.formatUnitsConMoneda(t.monthlyInstallmentAmount));
                }
                if (t.totalWithInterest != null && !exenta) {
                    finSb.append(" | Total c/int: ").append(CurrencyUtils.formatUnitsConMoneda(t.totalWithInterest));
                }
                TextView tvFinInfo = new TextView(getContext());
                tvFinInfo.setText(finSb.toString());
                tvFinInfo.setTextColor(UiHelper.COLOR_PRIMARIO);
                tvFinInfo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                tvFinInfo.setTypeface(Typeface.DEFAULT_BOLD);
                UiHelper.applyTabularNums(tvFinInfo);
                LinearLayout.LayoutParams finLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                );
                finLp.topMargin = UiHelper.dp(getContext(), 6);
                tvFinInfo.setLayoutParams(finLp);
                card.addView(tvFinInfo);
            }

            // Fila inferior de acciones (Metodo de pago + Ver/Editar/Eliminar)
            LinearLayout actRow = new LinearLayout(getContext());
            actRow.setOrientation(LinearLayout.HORIZONTAL);
            actRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams actLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            actLp.topMargin = UiHelper.dp(getContext(), 8);
            actRow.setLayoutParams(actLp);

            TextView tvMetodo = new TextView(getContext());
            tvMetodo.setText(FaIcon.getMetodoPagoLabel(t.metodoPago));
            tvMetodo.setTextColor(UiHelper.COLOR_TEXTO_SECUNDARIO);
            tvMetodo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            tvMetodo.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            actRow.addView(tvMetodo);

            if (t.esCompra) {
                TextView btnTicket = createIconLink(getContext(), "fa-solid fa-receipt", "Ver ticket", UiHelper.COLOR_PRIMARIO);
                btnTicket.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        abrirDetalleCompra(t.id);
                    }
                });
                actRow.addView(btnTicket);
            }

            TextView btnEdit = createIconLink(getContext(), "fa-solid fa-pen", "Editar", UiHelper.COLOR_SECUNDARIO);
            btnEdit.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (t.esCompra) {
                        // API_CONTRACT.md 7.3.4: si es_compra = true se debe usar /compras/{id}
                        editarCompraPorId(t.id);
                    } else {
                        TransaccionFormDialog.show(getContext(), t, null, host.getCachedCategorias(), new TransaccionFormDialog.OnSavedListener() {
                            @Override
                            public void onSaved(Transaccion transaccion) {
                                loadData();
                            }
                        });
                    }
                }
            });
            actRow.addView(btnEdit);

            TextView btnDel = createIconLink(getContext(), "fa-solid fa-trash-can", "Eliminar", UiHelper.COLOR_PELIGRO);
            btnDel.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmarEliminar(t);
                }
            });
            actRow.addView(btnDel);

            card.addView(actRow);
            listContainer.addView(card);
        }
    }

    private TextView createIconLink(Context context, String iconClass, String text, int color) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        int padH = UiHelper.dp(context, 10);
        int padV = UiHelper.dp(context, 4);
        tv.setPadding(padH, padV, padH, padV);
        return tv;
    }

    private void abrirDetalleCompra(int compraId) {
        host.setGlobalLoading(true);
        ApiClient.getInstance().getCompraDetalle(compraId, new ApiCallback<Compra>() {
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
                        confirmarEliminar(compra);
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

    private void editarCompraPorId(int compraId) {
        host.setGlobalLoading(true);
        ApiClient.getInstance().getCompraDetalle(compraId, new ApiCallback<Compra>() {
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

    private void confirmarEliminar(final Transaccion t) {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar transaccion")
                .setMessage(getContext().getString(R.string.confirmar_eliminar_transaccion))
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        host.setGlobalLoading(true);
                        ApiClient.getInstance().eliminarTransaccion(t.id, new ApiCallback<Boolean>() {
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
