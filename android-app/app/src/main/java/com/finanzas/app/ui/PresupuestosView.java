package com.finanzas.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.finanzas.app.MainActivity;
import com.finanzas.app.R;
import com.finanzas.app.api.ApiCallback;
import com.finanzas.app.api.ApiClient;
import com.finanzas.app.api.ApiError;
import com.finanzas.app.model.PaginacionMeta;
import com.finanzas.app.model.Presupuesto;
import com.finanzas.app.model.PresupuestosResumen;
import com.finanzas.app.ui.dialogs.PresupuestoFormDialog;
import com.finanzas.app.util.CurrencyUtils;
import com.finanzas.app.util.DateUtils;
import com.finanzas.app.util.FaIcon;
import com.finanzas.app.util.UiHelper;

/**
 * Vista de Control de Presupuestos Mensuales (UI_DESIGN.md Secciones 2.4 y 6, API_CONTRACT.md 7.5).
 */
public class PresupuestosView extends ScrollView {

    private final MainActivity host;
    private int anioActual;
    private int mesActual;

    private TextView tvPeriodo;
    private Spinner spEstado;
    private TextView tvGlobalLimites;
    private TextView tvGlobalPct;
    private ProgressBar pbGlobal;
    private LinearLayout listContainer;

    private static final String[] ESTADOS = {"TODOS", "NORMAL", "ADVERTENCIA", "EXCEDIDO"};

    public PresupuestosView(Context context, MainActivity host) {
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

        // Selector de mes
        LinearLayout periodCard = UiHelper.createCard(context);
        periodCard.setOrientation(LinearLayout.HORIZONTAL);
        periodCard.setGravity(Gravity.CENTER_VERTICAL);
        int pad10 = UiHelper.dp(context, 10);
        periodCard.setPadding(pad10, pad10, pad10, pad10);

        LinearLayout btnPrev = UiHelper.createActionButton(
                context, "fa-solid fa-chevron-left", "", R.drawable.bg_button_secondary, UiHelper.COLOR_PRIMARIO
        );
        tvPeriodo = new TextView(context);
        tvPeriodo.setGravity(Gravity.CENTER);
        tvPeriodo.setTextColor(UiHelper.COLOR_PRIMARIO);
        tvPeriodo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        tvPeriodo.setTypeface(Typeface.DEFAULT_BOLD);
        tvPeriodo.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout btnNext = UiHelper.createActionButton(
                context, "fa-solid fa-chevron-right", "", R.drawable.bg_button_secondary, UiHelper.COLOR_PRIMARIO
        );

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
        periodCard.addView(tvPeriodo);
        periodCard.addView(btnNext);
        root.addView(periodCard);

        // Barra de acciones: Filtro estado + Nuevo presupuesto
        LinearLayout actionRow = new LinearLayout(context);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams actLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        actLp.bottomMargin = UiHelper.dp(context, 12);
        actionRow.setLayoutParams(actLp);

        spEstado = new Spinner(context);
        spEstado.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, ESTADOS));
        LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        spLp.rightMargin = UiHelper.dp(context, 8);
        spEstado.setLayoutParams(spLp);
        spEstado.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        LinearLayout btnNuevo = UiHelper.createActionButton(
                context, "fa-solid fa-plus", "Nuevo presupuesto", R.drawable.bg_button_primary, UiHelper.COLOR_TEXTO_INVERTIDO
        );
        btnNuevo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                PresupuestoFormDialog.show(context, null, anioActual, mesActual, host.getCachedCategorias(), new PresupuestoFormDialog.OnPresupuestoSavedListener() {
                    @Override
                    public void onSaved(Presupuesto presupuesto) {
                        loadData();
                    }
                });
            }
        });

        actionRow.addView(spEstado);
        actionRow.addView(btnNuevo);
        root.addView(actionRow);

        // Tarjeta de resumen global del mes
        LinearLayout globalCard = UiHelper.createCard(context);
        globalCard.addView(UiHelper.createSectionTitle(context, "Totales globales del periodo"));

        tvGlobalLimites = new TextView(context);
        tvGlobalLimites.setTextColor(UiHelper.COLOR_TEXTO);
        tvGlobalLimites.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        UiHelper.applyTabularNums(tvGlobalLimites);
        globalCard.addView(tvGlobalLimites);

        tvGlobalPct = new TextView(context);
        tvGlobalPct.setTextColor(UiHelper.COLOR_TEXTO_SECUNDARIO);
        tvGlobalPct.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvGlobalPct.setTypeface(Typeface.DEFAULT_BOLD);
        UiHelper.applyTabularNums(tvGlobalPct);
        globalCard.addView(tvGlobalPct);

        pbGlobal = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        pbGlobal.setMax(100);
        LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UiHelper.dp(context, 10)
        );
        pbLp.topMargin = UiHelper.dp(context, 8);
        pbGlobal.setLayoutParams(pbLp);
        globalCard.addView(pbGlobal);
        root.addView(globalCard);

        // Lista de presupuestos por categoria
        listContainer = new LinearLayout(context);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(listContainer);
    }

    public void loadData() {
        tvPeriodo.setText(DateUtils.getMonthYearLabel(anioActual, mesActual));
        int pos = spEstado.getSelectedItemPosition();
        String estadoFiltro = (pos > 0) ? ESTADOS[pos] : null;

        host.setGlobalLoading(true);
        ApiClient.getInstance().getPresupuestos(anioActual, mesActual, estadoFiltro, new ApiCallback<PresupuestosResumen>() {
            @Override
            public void onSuccess(PresupuestosResumen data, PaginacionMeta meta) {
                host.setGlobalLoading(false);
                renderPresupuestos(data);
            }

            @Override
            public void onError(ApiError error) {
                host.setGlobalLoading(false);
                host.handleGlobalApiError(error);
            }
        });
    }

    private void renderPresupuestos(PresupuestosResumen res) {
        listContainer.removeAllViews();
        if (res == null) return;

        tvGlobalLimites.setText(
                "Gastado: " + CurrencyUtils.formatConMoneda(res.totalGastadoCentavos)
                        + " | Limite: " + CurrencyUtils.formatConMoneda(res.totalLimiteCentavos)
                        + "\nDisponible: " + CurrencyUtils.formatBalance(res.totalDisponibleCentavos, true)
        );
        tvGlobalPct.setText("Ejecucion global: " + CurrencyUtils.formatPorcentaje(res.porcentajeUsadoGlobal));
        int visualGlobalPct = (int) Math.max(0, Math.min(100, Math.round(res.porcentajeUsadoGlobal)));
        pbGlobal.setProgress(visualGlobalPct);

        if (res.presupuestos == null || res.presupuestos.isEmpty()) {
            LinearLayout empty = UiHelper.createCard(getContext());
            empty.setGravity(Gravity.CENTER);
            TextView tv = new TextView(getContext());
            tv.setText("No hay presupuestos configurados para este periodo.");
            tv.setTextColor(UiHelper.COLOR_TEXTO_SECUNDARIO);
            empty.addView(tv);
            listContainer.addView(empty);
            return;
        }

        for (final Presupuesto p : res.presupuestos) {
            LinearLayout card = UiHelper.createCard(getContext());

            // Encabezado: Icono categoria + Nombre + Estado (Icono + Porcentaje real)
            LinearLayout header = new LinearLayout(getContext());
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);

            TextView catIcon = new TextView(getContext());
            String icClass = (p.categoria != null && p.categoria.icono != null) ? p.categoria.icono : "fa-solid fa-tag";
            FaIcon.apply(catIcon, icClass);
            catIcon.setTextColor(UiHelper.COLOR_PRIMARIO);
            catIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            LinearLayout.LayoutParams icLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            icLp.rightMargin = UiHelper.dp(getContext(), 8);
            catIcon.setLayoutParams(icLp);
            header.addView(catIcon);

            TextView tvCatName = new TextView(getContext());
            tvCatName.setText(p.categoria != null ? p.categoria.nombre : "Categoria");
            tvCatName.setTextColor(UiHelper.COLOR_TEXTO);
            tvCatName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            tvCatName.setTypeface(Typeface.DEFAULT_BOLD);
            tvCatName.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            header.addView(tvCatName);

            // Color e icono por estado (UI_DESIGN.md Seccion 2.4)
            int statusColor = UiHelper.COLOR_EXITO;
            String statusIcon = "fa-solid fa-circle-check";
            if ("ADVERTENCIA".equals(p.estado)) {
                statusColor = UiHelper.COLOR_ADVERTENCIA;
                statusIcon = "fa-solid fa-triangle-exclamation";
            } else if ("EXCEDIDO".equals(p.estado)) {
                statusColor = UiHelper.COLOR_PELIGRO;
                statusIcon = "fa-solid fa-circle-xmark";
            }

            TextView stIconTv = new TextView(getContext());
            FaIcon.apply(stIconTv, statusIcon);
            stIconTv.setTextColor(statusColor);
            stIconTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            stLp.rightMargin = UiHelper.dp(getContext(), 6);
            stIconTv.setLayoutParams(stLp);
            header.addView(stIconTv);

            TextView tvPct = new TextView(getContext());
            tvPct.setText(p.estado + " (" + CurrencyUtils.formatPorcentaje(p.porcentajeUsado) + ")");
            tvPct.setTextColor(statusColor);
            tvPct.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tvPct.setTypeface(Typeface.DEFAULT_BOLD);
            UiHelper.applyTabularNums(tvPct);
            header.addView(tvPct);

            card.addView(header);

            // Barra de progreso limitada visualmente al 100% (UI_DESIGN.md 2.4)
            ProgressBar pb = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
            pb.setMax(100);
            int visualPct = (int) Math.max(0, Math.min(100, Math.round(p.porcentajeUsado)));
            pb.setProgress(visualPct);
            pb.setProgressTintList(ColorStateList.valueOf(statusColor));
            LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, UiHelper.dp(getContext(), 10)
            );
            pLp.topMargin = UiHelper.dp(getContext(), 8);
            pLp.bottomMargin = UiHelper.dp(getContext(), 8);
            pb.setLayoutParams(pLp);
            card.addView(pb);

            // Cifras: Gastado | Limite | Disponible
            TextView tvNums = new TextView(getContext());
            tvNums.setText(
                    "Gastado: " + CurrencyUtils.formatNeutro(p.gastadoCentavos)
                            + " / Limite: " + CurrencyUtils.formatNeutro(p.montoLimiteCentavos)
                            + " | Disponible: " + CurrencyUtils.formatBalance(p.disponibleCentavos, false)
            );
            tvNums.setTextColor(UiHelper.COLOR_TEXTO_SECUNDARIO);
            tvNums.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            UiHelper.applyTabularNums(tvNums);
            card.addView(tvNums);

            if (p.notas != null && !p.notas.isEmpty()) {
                TextView tvN = new TextView(getContext());
                tvN.setText("Notas: " + p.notas);
                tvN.setTextColor(UiHelper.COLOR_TEXTO_SECUNDARIO);
                tvN.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                card.addView(tvN);
            }

            // Botones Editar | Eliminar
            LinearLayout btnRow = new LinearLayout(getContext());
            btnRow.setOrientation(LinearLayout.HORIZONTAL);
            btnRow.setGravity(Gravity.END);
            LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            bLp.topMargin = UiHelper.dp(getContext(), 8);
            btnRow.setLayoutParams(bLp);

            LinearLayout btnEdit = UiHelper.createActionButton(
                    getContext(), "fa-solid fa-pen", "Editar limite", R.drawable.bg_button_secondary, UiHelper.COLOR_SECUNDARIO
            );
            LinearLayout.LayoutParams edLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, UiHelper.dp(getContext(), 36)
            );
            edLp.rightMargin = UiHelper.dp(getContext(), 8);
            btnEdit.setLayoutParams(edLp);
            btnEdit.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    PresupuestoFormDialog.show(getContext(), p, anioActual, mesActual, host.getCachedCategorias(), new PresupuestoFormDialog.OnPresupuestoSavedListener() {
                        @Override
                        public void onSaved(Presupuesto presupuesto) {
                            loadData();
                        }
                    });
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
                    confirmarEliminar(p);
                }
            });

            btnRow.addView(btnEdit);
            btnRow.addView(btnDel);
            card.addView(btnRow);

            listContainer.addView(card);
        }
    }

    private void confirmarEliminar(final Presupuesto p) {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar presupuesto")
                .setMessage(getContext().getString(R.string.confirmar_eliminar_presupuesto))
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        host.setGlobalLoading(true);
                        ApiClient.getInstance().eliminarPresupuesto(p.id, new ApiCallback<Boolean>() {
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
