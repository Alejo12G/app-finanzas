package com.finanzas.app.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.finanzas.app.MainActivity;
import com.finanzas.app.R;
import com.finanzas.app.api.ApiCallback;
import com.finanzas.app.api.ApiClient;
import com.finanzas.app.api.ApiError;
import com.finanzas.app.model.Compra;
import com.finanzas.app.model.DashboardData;
import com.finanzas.app.model.PaginacionMeta;
import com.finanzas.app.model.Transaccion;
import com.finanzas.app.ui.chart.CategoriaBarChartView;
import com.finanzas.app.ui.chart.EvolucionBarChartView;
import com.finanzas.app.ui.dialogs.CompraFormDialog;
import com.finanzas.app.ui.dialogs.GastoRapidoIaDialog;
import com.finanzas.app.ui.dialogs.TransaccionFormDialog;
import com.finanzas.app.util.CurrencyUtils;
import com.finanzas.app.util.DateUtils;
import com.finanzas.app.util.FaIcon;
import com.finanzas.app.util.UiHelper;

import java.util.Locale;

/**
 * Vista principal del Panel / Dashboard (UI_DESIGN.md Seccion 6).
 * Consume GET /dashboard?anio=YYYY&mes=MM.
 */
public class DashboardView extends ScrollView {

    private final MainActivity host;
    private int anioActual;
    private int mesActual;

    private TextView tvMesAnio;
    private LinearLayout errorContainer;
    private TextView tvErrorMsg;

    private TextView tvKpiIngresos;
    private TextView tvKpiIngresosVar;
    private TextView tvKpiGastos;
    private TextView tvKpiGastosDetalle;
    private TextView tvKpiBalance;
    private TextView tvKpiAhorro;
    private TextView tvKpiTransaccionesCount;

    private CategoriaBarChartView chartCategorias;
    private EvolucionBarChartView chartEvolucion;

    private TextView tvPresupuestoResumen;
    private ProgressBar pbPresupuestoGlobal;
    private TextView tvPresupuestoEstados;

    private LinearLayout ultimasTransaccionesContainer;

    public DashboardView(Context context, MainActivity host) {
        super(context);
        this.host = host;
        this.anioActual = DateUtils.getCurrentYear();
        this.mesActual = DateUtils.getCurrentMonth();
        setFillViewport(true);
        initUi(context);
    }

    private void initUi(final Context context) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = UiHelper.dp(context, 16);
        root.setPadding(pad, pad, pad, pad);
        addView(root);

        // 1. Selector de periodo (Mes anterior | Mes Anio | Mes siguiente)
        LinearLayout periodCard = UiHelper.createCard(context);
        periodCard.setOrientation(LinearLayout.HORIZONTAL);
        periodCard.setGravity(Gravity.CENTER_VERTICAL);
        int pad10 = UiHelper.dp(context, 10);
        periodCard.setPadding(pad10, pad10, pad10, pad10);

        LinearLayout btnPrev = UiHelper.createActionButton(
                context, "fa-solid fa-chevron-left", "", R.drawable.bg_button_secondary, UiHelper.COLOR_PRIMARIO
        );
        btnPrev.setContentDescription("Mes anterior");

        tvMesAnio = new TextView(context);
        tvMesAnio.setGravity(Gravity.CENTER);
        tvMesAnio.setTextColor(UiHelper.COLOR_PRIMARIO);
        tvMesAnio.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tvMesAnio.setTypeface(Typeface.DEFAULT_BOLD);
        tvMesAnio.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout btnNext = UiHelper.createActionButton(
                context, "fa-solid fa-chevron-right", "", R.drawable.bg_button_secondary, UiHelper.COLOR_PRIMARIO
        );
        btnNext.setContentDescription("Mes siguiente");

        btnPrev.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mesActual--;
                if (mesActual < 1) {
                    mesActual = 12;
                    anioActual--;
                }
                loadData();
            }
        });

        btnNext.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mesActual++;
                if (mesActual > 12) {
                    mesActual = 1;
                    anioActual++;
                }
                loadData();
            }
        });

        periodCard.addView(btnPrev);
        periodCard.addView(tvMesAnio);
        periodCard.addView(btnNext);
        root.addView(periodCard);

        // 2. Botones de accion rapida (Nueva transaccion | Nueva compra | Gasto IA)
        LinearLayout quickRow = new LinearLayout(context);
        quickRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams qLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        qLp.bottomMargin = UiHelper.dp(context, 16);
        quickRow.setLayoutParams(qLp);

        LinearLayout btnNewTx = UiHelper.createActionButton(
                context, "fa-solid fa-plus", "Movimiento", R.drawable.bg_button_primary, UiHelper.COLOR_TEXTO_INVERTIDO
        );
        LinearLayout.LayoutParams b1Lp = new LinearLayout.LayoutParams(0, UiHelper.dp(context, 48), 1f);
        b1Lp.rightMargin = UiHelper.dp(context, 6);
        btnNewTx.setLayoutParams(b1Lp);
        btnNewTx.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                TransaccionFormDialog.show(context, null, null, host.getCachedCategorias(), new TransaccionFormDialog.OnSavedListener() {
                    @Override
                    public void onSaved(Transaccion transaccion) {
                        loadData();
                    }
                });
            }
        });

        LinearLayout btnNewCompra = UiHelper.createActionButton(
                context, "fa-solid fa-cart-shopping", "Compra", R.drawable.bg_button_secondary, UiHelper.COLOR_PRIMARIO
        );
        LinearLayout.LayoutParams b2Lp = new LinearLayout.LayoutParams(0, UiHelper.dp(context, 48), 1f);
        b2Lp.rightMargin = UiHelper.dp(context, 6);
        btnNewCompra.setLayoutParams(b2Lp);
        btnNewCompra.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                CompraFormDialog.show(context, null, host.getCachedCategorias(), new CompraFormDialog.OnCompraSavedListener() {
                    @Override
                    public void onSaved(Compra compra) {
                        loadData();
                    }
                });
            }
        });

        LinearLayout btnQuickIa = UiHelper.createActionButton(
                context, "fa-solid fa-wand-magic-sparkles", "Texto IA", R.drawable.bg_button_secondary, UiHelper.COLOR_PRIMARIO
        );
        LinearLayout.LayoutParams b3Lp = new LinearLayout.LayoutParams(0, UiHelper.dp(context, 48), 1f);
        btnQuickIa.setLayoutParams(b3Lp);
        btnQuickIa.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                GastoRapidoIaDialog.show(context, host.getCachedCategorias(), new TransaccionFormDialog.OnSavedListener() {
                    @Override
                    public void onSaved(Transaccion transaccion) {
                        loadData();
                    }
                });
            }
        });

        quickRow.addView(btnNewTx);
        quickRow.addView(btnNewCompra);
        quickRow.addView(btnQuickIa);
        root.addView(quickRow);

        // Banner de error local
        errorContainer = UiHelper.createCard(context);
        errorContainer.setBackgroundResource(R.drawable.bg_badge_danger);
        errorContainer.setVisibility(GONE);
        tvErrorMsg = new TextView(context);
        tvErrorMsg.setTextColor(UiHelper.COLOR_PELIGRO);
        tvErrorMsg.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        errorContainer.addView(tvErrorMsg);

        LinearLayout btnRetry = UiHelper.createActionButton(
                context, "fa-solid fa-rotate-right", "Reintentar", R.drawable.bg_button_secondary, UiHelper.COLOR_SECUNDARIO
        );
        LinearLayout.LayoutParams retryLp = (LinearLayout.LayoutParams) btnRetry.getLayoutParams();
        retryLp.topMargin = UiHelper.dp(context, 8);
        btnRetry.setLayoutParams(retryLp);
        btnRetry.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                loadData();
            }
        });
        errorContainer.addView(btnRetry);
        root.addView(errorContainer);

        // 3. Los 4 KPIs (Ingresos, Gastos, Balance, Tasa de ahorro)
        // Fila 1 de KPIs: Ingresos | Gastos
        LinearLayout kpiRow1 = new LinearLayout(context);
        kpiRow1.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout cardIngresos = UiHelper.createCard(context);
        LinearLayout.LayoutParams k1Lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        k1Lp.rightMargin = UiHelper.dp(context, 8);
        k1Lp.bottomMargin = UiHelper.dp(context, 12);
        cardIngresos.setLayoutParams(k1Lp);
        cardIngresos.addView(createKpiHeader(context, "fa-solid fa-arrow-trend-up", "INGRESOS", UiHelper.COLOR_EXITO));
        tvKpiIngresos = createKpiValueText(context, UiHelper.COLOR_EXITO);
        tvKpiIngresosVar = createKpiSubText(context);
        cardIngresos.addView(tvKpiIngresos);
        cardIngresos.addView(tvKpiIngresosVar);

        LinearLayout cardGastos = UiHelper.createCard(context);
        LinearLayout.LayoutParams k2Lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        k2Lp.bottomMargin = UiHelper.dp(context, 12);
        cardGastos.setLayoutParams(k2Lp);
        cardGastos.addView(createKpiHeader(context, "fa-solid fa-arrow-trend-down", "GASTOS", UiHelper.COLOR_PELIGRO));
        tvKpiGastos = createKpiValueText(context, UiHelper.COLOR_PELIGRO);
        tvKpiGastosDetalle = createKpiSubText(context);
        cardGastos.addView(tvKpiGastos);
        cardGastos.addView(tvKpiGastosDetalle);

        kpiRow1.addView(cardIngresos);
        kpiRow1.addView(cardGastos);
        root.addView(kpiRow1);

        // Fila 2 de KPIs: Balance | Tasa de ahorro
        LinearLayout kpiRow2 = new LinearLayout(context);
        kpiRow2.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout cardBalance = UiHelper.createCard(context);
        LinearLayout.LayoutParams k3Lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        k3Lp.rightMargin = UiHelper.dp(context, 8);
        k3Lp.bottomMargin = UiHelper.dp(context, 16);
        cardBalance.setLayoutParams(k3Lp);
        cardBalance.addView(createKpiHeader(context, "fa-solid fa-scale-balanced", "BALANCE NETO", UiHelper.COLOR_PRIMARIO));
        tvKpiBalance = createKpiValueText(context, UiHelper.COLOR_EXITO);
        TextView subBal = createKpiSubText(context);
        subBal.setText("Ingresos menos gastos");
        cardBalance.addView(tvKpiBalance);
        cardBalance.addView(subBal);

        LinearLayout cardAhorro = UiHelper.createCard(context);
        LinearLayout.LayoutParams k4Lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        k4Lp.bottomMargin = UiHelper.dp(context, 16);
        cardAhorro.setLayoutParams(k4Lp);
        cardAhorro.addView(createKpiHeader(context, "fa-solid fa-piggy-bank", "TASA DE AHORRO", UiHelper.COLOR_EXITO));
        tvKpiAhorro = createKpiValueText(context, UiHelper.COLOR_PRIMARIO);
        tvKpiTransaccionesCount = createKpiSubText(context);
        cardAhorro.addView(tvKpiAhorro);
        cardAhorro.addView(tvKpiTransaccionesCount);

        kpiRow2.addView(cardBalance);
        kpiRow2.addView(cardAhorro);
        root.addView(kpiRow2);

        // 4. Grafico de gastos por categoria
        LinearLayout cardCatChart = UiHelper.createCard(context);
        cardCatChart.addView(UiHelper.createSectionTitle(context, "Gastos por categoria"));
        chartCategorias = new CategoriaBarChartView(context);
        cardCatChart.addView(chartCategorias);
        root.addView(cardCatChart);

        // 5. Grafico de evolucion 6 meses
        LinearLayout cardEvoChart = UiHelper.createCard(context);
        cardEvoChart.addView(UiHelper.createSectionTitle(context, "Evolucion ultimos 6 meses"));
        chartEvolucion = new EvolucionBarChartView(context);
        cardEvoChart.addView(chartEvolucion);
        root.addView(cardEvoChart);

        // 6. Resumen de presupuestos del mes
        LinearLayout cardPresupuestos = UiHelper.createCard(context);
        cardPresupuestos.addView(UiHelper.createSectionTitle(context, "Estado de presupuestos del mes"));
        tvPresupuestoResumen = new TextView(context);
        tvPresupuestoResumen.setTextColor(UiHelper.COLOR_TEXTO);
        tvPresupuestoResumen.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        UiHelper.applyTabularNums(tvPresupuestoResumen);
        cardPresupuestos.addView(tvPresupuestoResumen);

        pbPresupuestoGlobal = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        pbPresupuestoGlobal.setMax(100);
        LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UiHelper.dp(context, 10)
        );
        pbLp.topMargin = UiHelper.dp(context, 8);
        pbLp.bottomMargin = UiHelper.dp(context, 8);
        pbPresupuestoGlobal.setLayoutParams(pbLp);
        cardPresupuestos.addView(pbPresupuestoGlobal);

        tvPresupuestoEstados = new TextView(context);
        tvPresupuestoEstados.setTextColor(UiHelper.COLOR_TEXTO_SECUNDARIO);
        tvPresupuestoEstados.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        cardPresupuestos.addView(tvPresupuestoEstados);
        root.addView(cardPresupuestos);

        // 7. Ultimas 5 transacciones
        LinearLayout cardUltimas = UiHelper.createCard(context);
        cardUltimas.addView(UiHelper.createSectionTitle(context, "Ultimos 5 movimientos del periodo"));
        ultimasTransaccionesContainer = new LinearLayout(context);
        ultimasTransaccionesContainer.setOrientation(LinearLayout.VERTICAL);
        cardUltimas.addView(ultimasTransaccionesContainer);
        root.addView(cardUltimas);
    }

    private LinearLayout createKpiHeader(Context context, String iconClass, String title, int iconColor) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView icon = new TextView(context);
        FaIcon.apply(icon, iconClass);
        icon.setTextColor(iconColor);
        icon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        LinearLayout.LayoutParams iLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        iLp.rightMargin = UiHelper.dp(context, 6);
        icon.setLayoutParams(iLp);

        TextView lbl = new TextView(context);
        lbl.setText(title);
        lbl.setTextColor(UiHelper.COLOR_TEXTO_SECUNDARIO);
        lbl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        lbl.setTypeface(Typeface.DEFAULT_BOLD);

        row.addView(icon);
        row.addView(lbl);
        return row;
    }

    private TextView createKpiValueText(Context context, int color) {
        TextView tv = new TextView(context);
        tv.setTextColor(color);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        UiHelper.applyTabularNums(tv);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = UiHelper.dp(context, 6);
        tv.setLayoutParams(lp);
        return tv;
    }

    private TextView createKpiSubText(Context context) {
        TextView tv = new TextView(context);
        tv.setTextColor(UiHelper.COLOR_TEXTO_SECUNDARIO);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        UiHelper.applyTabularNums(tv);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = UiHelper.dp(context, 4);
        tv.setLayoutParams(lp);
        return tv;
    }

    public void loadData() {
        tvMesAnio.setText(DateUtils.getMonthYearLabel(anioActual, mesActual));
        errorContainer.setVisibility(GONE);
        host.setGlobalLoading(true);

        ApiClient.getInstance().getDashboard(anioActual, mesActual, new ApiCallback<DashboardData>() {
            @Override
            public void onSuccess(DashboardData data, PaginacionMeta meta) {
                host.setGlobalLoading(false);
                renderDashboard(data);
            }

            @Override
            public void onError(ApiError error) {
                host.setGlobalLoading(false);
                host.handleGlobalApiError(error);
                errorContainer.setVisibility(VISIBLE);
                tvErrorMsg.setText(error.mensaje);
            }
        });
    }

    private void renderDashboard(DashboardData d) {
        if (d == null) return;

        // KPI Ingresos
        tvKpiIngresos.setText(CurrencyUtils.formatPorTipo(d.ingresosCentavos, "INGRESO"));
        if (d.ingresosVariacionPorcentaje != null) {
            String sign = d.ingresosVariacionPorcentaje >= 0 ? "+" : "";
            tvKpiIngresosVar.setText(String.format(Locale.US, "Vs mes ant: %s%.2f %%", sign, d.ingresosVariacionPorcentaje));
        } else {
            tvKpiIngresosVar.setText("Vs mes ant: N/D");
        }

        // KPI Gastos
        tvKpiGastos.setText(CurrencyUtils.formatPorTipo(d.gastosCentavos, "GASTO"));
        String fijosStr = CurrencyUtils.formatNeutro(d.gastosFijosCentavos);
        String varStr = CurrencyUtils.formatNeutro(d.gastosVariablesCentavos);
        tvKpiGastosDetalle.setText("Fijos: " + fijosStr + " | Var: " + varStr);

        // KPI Balance
        tvKpiBalance.setText(CurrencyUtils.formatBalance(d.balanceCentavos, true));
        tvKpiBalance.setTextColor(d.balanceCentavos >= 0 ? UiHelper.COLOR_EXITO : UiHelper.COLOR_PELIGRO);

        // KPI Tasa de ahorro
        tvKpiAhorro.setText(CurrencyUtils.formatPorcentaje(d.tasaAhorroPorcentaje));
        tvKpiAhorro.setTextColor(d.tasaAhorroPorcentaje >= 0 ? UiHelper.COLOR_EXITO : UiHelper.COLOR_PELIGRO);
        tvKpiTransaccionesCount.setText(d.cantidadTransacciones + " movimientos en el mes");

        // Graficos
        chartCategorias.setItems(d.gastosPorCategoria);
        chartEvolucion.setItems(d.evolucionMensual);

        // Presupuestos
        tvPresupuestoResumen.setText(
                "Gastado: " + CurrencyUtils.formatConMoneda(d.presupuestosTotalGastadoCentavos)
                        + " de " + CurrencyUtils.formatConMoneda(d.presupuestosTotalLimiteCentavos)
        );
        int pctPres = 0;
        if (d.presupuestosTotalLimiteCentavos > 0) {
            pctPres = (int) Math.min(100L, (d.presupuestosTotalGastadoCentavos * 100L) / d.presupuestosTotalLimiteCentavos);
        }
        pbPresupuestoGlobal.setProgress(pctPres);
        tvPresupuestoEstados.setText(
                "Normal: " + d.presupuestosCantidadNormal
                        + " | Advertencia: " + d.presupuestosCantidadAdvertencia
                        + " | Excedido: " + d.presupuestosCantidadExcedido
        );

        // Ultimas 5 transacciones
        ultimasTransaccionesContainer.removeAllViews();
        if (d.ultimasTransacciones == null || d.ultimasTransacciones.isEmpty()) {
            TextView emptyTv = new TextView(getContext());
            emptyTv.setText("No hay registros para este periodo.");
            emptyTv.setTextColor(UiHelper.COLOR_TEXTO_SECUNDARIO);
            emptyTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            ultimasTransaccionesContainer.addView(emptyTv);
        } else {
            for (int i = 0; i < d.ultimasTransacciones.size(); i++) {
                Transaccion t = d.ultimasTransacciones.get(i);
                LinearLayout row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                int pad8 = UiHelper.dp(getContext(), 8);
                row.setPadding(pad8, pad8, pad8, pad8);
                row.setBackgroundColor((i % 2 == 0) ? UiHelper.COLOR_SUPERFICIE : UiHelper.COLOR_FONDO);

                TextView iconTv = new TextView(getContext());
                String ic = (t.categoria != null && t.categoria.icono != null) ? t.categoria.icono : "fa-solid fa-tag";
                FaIcon.apply(iconTv, ic);
                int cColor = (t.categoria != null) ? UiHelper.parseColorSafe(t.categoria.color, UiHelper.COLOR_PRIMARIO) : UiHelper.COLOR_PRIMARIO;
                iconTv.setTextColor(cColor);
                iconTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                iconTv.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams icLp = new LinearLayout.LayoutParams(
                        UiHelper.dp(getContext(), 32), UiHelper.dp(getContext(), 32)
                );
                icLp.rightMargin = UiHelper.dp(getContext(), 8);
                iconTv.setLayoutParams(icLp);
                row.addView(iconTv);

                LinearLayout infoCol = new LinearLayout(getContext());
                infoCol.setOrientation(LinearLayout.VERTICAL);
                infoCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                TextView tvDesc = new TextView(getContext());
                tvDesc.setText(t.descripcion);
                tvDesc.setTextColor(UiHelper.COLOR_TEXTO);
                tvDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                tvDesc.setTypeface(Typeface.DEFAULT_BOLD);

                String catNombre = t.categoria != null ? t.categoria.nombre : "";
                TextView tvSub = new TextView(getContext());
                tvSub.setText(t.fecha + " | " + catNombre + (t.esCompra ? " (Compra)" : ""));
                tvSub.setTextColor(UiHelper.COLOR_TEXTO_SECUNDARIO);
                tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);

                infoCol.addView(tvDesc);
                infoCol.addView(tvSub);
                row.addView(infoCol);

                TextView tvMonto = new TextView(getContext());
                tvMonto.setText(CurrencyUtils.formatPorTipo(t.montoCentavos, t.tipo));
                tvMonto.setTextColor("INGRESO".equals(t.tipo) ? UiHelper.COLOR_EXITO : UiHelper.COLOR_PELIGRO);
                tvMonto.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                tvMonto.setTypeface(Typeface.DEFAULT_BOLD);
                UiHelper.applyTabularNums(tvMonto);
                row.addView(tvMonto);

                ultimasTransaccionesContainer.addView(row);
            }
        }
    }
}
