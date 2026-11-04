package com.finanzas.app.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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
import com.finanzas.app.model.AnalisisIA;
import com.finanzas.app.model.PaginacionMeta;
import com.finanzas.app.model.Transaccion;
import com.finanzas.app.ui.dialogs.TransaccionFormDialog;
import com.finanzas.app.util.CurrencyUtils;
import com.finanzas.app.util.DateUtils;
import com.finanzas.app.util.FaIcon;
import com.finanzas.app.util.QuickExpenseParser;
import com.finanzas.app.util.UiHelper;
import com.finanzas.app.util.ValidationUtils;

import java.util.Map;

/**
 * Vista del Asesor Financiero IA Gemini (API_CONTRACT.md Seccion 8 y UI_DESIGN.md Seccion 6).
 * Incluye:
 * 1) Creacion rapida de gasto/ingreso por texto en lenguaje natural con IA.
 * 2) Analisis financiero estructurado via POST /ai/analyze (RESUMEN_MENSUAL, AHORRO, PRESUPUESTO, PREGUNTA_LIBRE).
 */
public class AsesorIaView extends ScrollView {

    private final MainActivity host;
    private int anioActual;
    private int mesActual;

    private EditText etGastoRapidoTexto;
    private TextView errGastoRapido;

    private TextView tvPeriodo;
    private Spinner spTipoAnalisis;
    private LinearLayout preguntaContainer;
    private EditText etPreguntaLibre;
    private TextView errPreguntaLibre;
    private TextView errGlobalIa;
    private LinearLayout btnAnalizar;

    private LinearLayout resultadoContainer;

    private static final String[] TIPOS_ANALISIS = {
            "RESUMEN_MENSUAL", "AHORRO", "PRESUPUESTO", "PREGUNTA_LIBRE"
    };

    private static final String[] ETIQUETAS_ANALISIS = {
            "Resumen mensual ejecutivo (RESUMEN_MENSUAL)",
            "Oportunidades de ahorro (AHORRO)",
            "Control de presupuestos (PRESUPUESTO)",
            "Pregunta especifica al asesor (PREGUNTA_LIBRE)"
    };

    public AsesorIaView(Context context, MainActivity host) {
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

        // SECCION 1: Creacion rapida de gasto por texto con IA
        LinearLayout quickCard = UiHelper.createCard(context);
        quickCard.addView(UiHelper.createSectionTitle(context, "Creacion rapida de gasto por texto con IA"));

        TextView tvQuickHelp = new TextView(context);
        tvQuickHelp.setText("Describa su gasto o ingreso en una frase. El interprete extraera el monto en centavos, categoria, fecha, comercio y metodo de pago.");
        tvQuickHelp.setTextColor(UiHelper.COLOR_TEXTO_SECUNDARIO);
        tvQuickHelp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        quickCard.addView(tvQuickHelp);

        etGastoRapidoTexto = UiHelper.createInput(context, "Ej. Pague 34.80 en Supermaxi hoy con tarjeta de debito");
        LinearLayout.LayoutParams qInLp = (LinearLayout.LayoutParams) etGastoRapidoTexto.getLayoutParams();
        qInLp.topMargin = UiHelper.dp(context, 8);
        etGastoRapidoTexto.setLayoutParams(qInLp);
        errGastoRapido = UiHelper.createFieldErrorText(context);
        quickCard.addView(etGastoRapidoTexto);
        quickCard.addView(errGastoRapido);

        LinearLayout btnQuickParse = UiHelper.createActionButton(
                context,
                "fa-solid fa-wand-magic-sparkles",
                "Analizar texto y crear movimiento",
                R.drawable.bg_button_primary,
                UiHelper.COLOR_TEXTO_INVERTIDO
        );
        LinearLayout.LayoutParams qpLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UiHelper.dp(context, 48)
        );
        qpLp.topMargin = UiHelper.dp(context, 10);
        btnQuickParse.setLayoutParams(qpLp);
        btnQuickParse.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                UiHelper.setFieldError(etGastoRapidoTexto, errGastoRapido, null);
                String txt = etGastoRapidoTexto.getText().toString().trim();
                if (txt.length() < 3) {
                    UiHelper.setFieldError(etGastoRapidoTexto, errGastoRapido, "Ingrese una descripcion valida del gasto.");
                    return;
                }
                if (ValidationUtils.containsEmoji(txt)) {
                    UiHelper.setFieldError(etGastoRapidoTexto, errGastoRapido, "No se permiten emojis en el texto.");
                    return;
                }
                QuickExpenseParser.ParsedExpense parsed = QuickExpenseParser.parse(txt, host.getCachedCategorias());
                TransaccionFormDialog.show(context, null, parsed, host.getCachedCategorias(), new TransaccionFormDialog.OnSavedListener() {
                    @Override
                    public void onSaved(Transaccion transaccion) {
                        etGastoRapidoTexto.setText("");
                        Toast.makeText(context, "Movimiento registrado correctamente.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        quickCard.addView(btnQuickParse);
        root.addView(quickCard);

        // SECCION 2: Analisis Financiero Gemini (POST /ai/analyze)
        LinearLayout iaCard = UiHelper.createCard(context);
        iaCard.addView(UiHelper.createSectionTitle(context, "Asesor Financiero IA (Gemini)"));

        // Selector de mes a analizar
        LinearLayout periodRow = new LinearLayout(context);
        periodRow.setOrientation(LinearLayout.HORIZONTAL);
        periodRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        pLp.bottomMargin = UiHelper.dp(context, 8);
        periodRow.setLayoutParams(pLp);

        LinearLayout btnPrev = UiHelper.createActionButton(
                context, "fa-solid fa-chevron-left", "", R.drawable.bg_button_secondary, UiHelper.COLOR_PRIMARIO
        );
        tvPeriodo = new TextView(context);
        tvPeriodo.setGravity(Gravity.CENTER);
        tvPeriodo.setTextColor(UiHelper.COLOR_PRIMARIO);
        tvPeriodo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tvPeriodo.setTypeface(Typeface.DEFAULT_BOLD);
        tvPeriodo.setText(DateUtils.getMonthYearLabel(anioActual, mesActual));
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
                tvPeriodo.setText(DateUtils.getMonthYearLabel(anioActual, mesActual));
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
                tvPeriodo.setText(DateUtils.getMonthYearLabel(anioActual, mesActual));
            }
        });

        periodRow.addView(btnPrev);
        periodRow.addView(tvPeriodo);
        periodRow.addView(btnNext);
        iaCard.addView(periodRow);

        iaCard.addView(UiHelper.createLabel(context, "Tipo de analisis *"));
        spTipoAnalisis = new Spinner(context);
        spTipoAnalisis.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, ETIQUETAS_ANALISIS));
        iaCard.addView(spTipoAnalisis);

        // Campo condicional para PREGUNTA_LIBRE
        preguntaContainer = new LinearLayout(context);
        preguntaContainer.setOrientation(LinearLayout.VERTICAL);
        preguntaContainer.setVisibility(GONE);
        preguntaContainer.addView(UiHelper.createLabel(context, "Su pregunta financiera (5 a 500 caracteres) *"));
        etPreguntaLibre = UiHelper.createInput(context, "Ej. En que categoria puedo reducir un 15 por ciento mis gastos este mes?");
        etPreguntaLibre.setMinLines(2);
        errPreguntaLibre = UiHelper.createFieldErrorText(context);
        preguntaContainer.addView(etPreguntaLibre);
        preguntaContainer.addView(errPreguntaLibre);
        iaCard.addView(preguntaContainer);

        spTipoAnalisis.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String tipo = TIPOS_ANALISIS[position];
                preguntaContainer.setVisibility("PREGUNTA_LIBRE".equals(tipo) ? VISIBLE : GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        errGlobalIa = UiHelper.createFieldErrorText(context);
        iaCard.addView(errGlobalIa);

        btnAnalizar = UiHelper.createActionButton(
                context,
                "fa-solid fa-wand-magic-sparkles",
                "Generar analisis IA",
                R.drawable.bg_button_primary,
                UiHelper.COLOR_TEXTO_INVERTIDO
        );
        LinearLayout.LayoutParams anLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UiHelper.dp(context, 48)
        );
        anLp.topMargin = UiHelper.dp(context, 12);
        btnAnalizar.setLayoutParams(anLp);
        btnAnalizar.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ejecutarAnalisisIa();
            }
        });
        iaCard.addView(btnAnalizar);
        root.addView(iaCard);

        // Contenedor de resultados del analisis IA
        resultadoContainer = new LinearLayout(context);
        resultadoContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(resultadoContainer);
    }

    private void ejecutarAnalisisIa() {
        UiHelper.setFieldError(null, errGlobalIa, null);
        UiHelper.setFieldError(etPreguntaLibre, errPreguntaLibre, null);

        String tipo = TIPOS_ANALISIS[spTipoAnalisis.getSelectedItemPosition()];
        String pregunta = null;
        if ("PREGUNTA_LIBRE".equals(tipo)) {
            pregunta = etPreguntaLibre.getText().toString().trim();
            if (pregunta.length() < 5 || pregunta.length() > 500) {
                UiHelper.setFieldError(etPreguntaLibre, errPreguntaLibre, "La pregunta debe tener entre 5 y 500 caracteres.");
                return;
            }
            if (ValidationUtils.containsEmoji(pregunta)) {
                UiHelper.setFieldError(etPreguntaLibre, errPreguntaLibre, "No se permiten emojis en la pregunta.");
                return;
            }
        }

        // Evitar doble envio (API_CONTRACT.md Seccion 9.1)
        btnAnalizar.setEnabled(false);
        host.setGlobalLoading(true);

        ApiClient.getInstance().analizarConIa(tipo, anioActual, mesActual, pregunta, new ApiCallback<AnalisisIA>() {
            @Override
            public void onSuccess(AnalisisIA data, PaginacionMeta meta) {
                btnAnalizar.setEnabled(true);
                host.setGlobalLoading(false);
                renderResultado(data);
            }

            @Override
            public void onError(ApiError error) {
                btnAnalizar.setEnabled(true);
                host.setGlobalLoading(false);
                host.handleGlobalApiError(error);
                UiHelper.setFieldError(null, errGlobalIa, error.mensaje);
                Map<String, String> map = error.getMapaErroresPorCampo();
                if (map.containsKey("pregunta")) {
                    UiHelper.setFieldError(etPreguntaLibre, errPreguntaLibre, map.get("pregunta"));
                }
            }
        });
    }

    private void renderResultado(final AnalisisIA res) {
        resultadoContainer.removeAllViews();
        if (res == null) return;
        final Context context = getContext();

        // 1. Puntuacion de salud financiera (0-100) + Resumen
        LinearLayout scoreCard = UiHelper.createCard(context);

        LinearLayout scoreHeader = new LinearLayout(context);
        scoreHeader.setOrientation(LinearLayout.HORIZONTAL);
        scoreHeader.setGravity(Gravity.CENTER_VERTICAL);

        TextView gaugeIcon = new TextView(context);
        FaIcon.apply(gaugeIcon, "fa-solid fa-gauge-high");
        int scoreColor = res.puntuacionSaludFinanciera >= 70
                ? UiHelper.COLOR_EXITO
                : (res.puntuacionSaludFinanciera >= 45 ? UiHelper.COLOR_ADVERTENCIA : UiHelper.COLOR_PELIGRO);
        gaugeIcon.setTextColor(scoreColor);
        gaugeIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        LinearLayout.LayoutParams gLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        gLp.rightMargin = UiHelper.dp(context, 10);
        gaugeIcon.setLayoutParams(gLp);
        scoreHeader.addView(gaugeIcon);

        TextView tvScoreLabel = new TextView(context);
        tvScoreLabel.setText("Salud Financiera: " + res.puntuacionSaludFinanciera + " / 100");
        tvScoreLabel.setTextColor(scoreColor);
        tvScoreLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tvScoreLabel.setTypeface(Typeface.DEFAULT_BOLD);
        UiHelper.applyTabularNums(tvScoreLabel);
        tvScoreLabel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        scoreHeader.addView(tvScoreLabel);

        LinearLayout btnCopy = UiHelper.createActionButton(
                context, "fa-solid fa-copy", "Copiar", R.drawable.bg_button_secondary, UiHelper.COLOR_SECUNDARIO
        );
        btnCopy.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, UiHelper.dp(context, 36)
        ));
        btnCopy.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("Analisis IA", res.resumen));
                    Toast.makeText(context, "Resumen copiado al portapapeles.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        scoreHeader.addView(btnCopy);
        scoreCard.addView(scoreHeader);

        ProgressBar pbScore = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        pbScore.setMax(100);
        pbScore.setProgress(Math.max(0, Math.min(100, res.puntuacionSaludFinanciera)));
        pbScore.setProgressTintList(ColorStateList.valueOf(scoreColor));
        LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UiHelper.dp(context, 10)
        );
        pbLp.topMargin = UiHelper.dp(context, 8);
        pbLp.bottomMargin = UiHelper.dp(context, 12);
        pbScore.setLayoutParams(pbLp);
        scoreCard.addView(pbScore);

        TextView tvResumen = new TextView(context);
        tvResumen.setText(res.resumen);
        tvResumen.setTextColor(UiHelper.COLOR_TEXTO);
        tvResumen.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        scoreCard.addView(tvResumen);

        TextView tvMeta = new TextView(context);
        tvMeta.setText("Modelo: " + res.modelo + " | Generado: " + res.generadoEn);
        tvMeta.setTextColor(UiHelper.COLOR_TEXTO_SECUNDARIO);
        tvMeta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        LinearLayout.LayoutParams mLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        mLp.topMargin = UiHelper.dp(context, 8);
        tvMeta.setLayoutParams(mLp);
        scoreCard.addView(tvMeta);

        resultadoContainer.addView(scoreCard);

        // 2. Alertas detectadas
        if (res.alertas != null && !res.alertas.isEmpty()) {
            LinearLayout alertCard = UiHelper.createCard(context);
            alertCard.addView(UiHelper.createSectionTitle(context, "Alertas detectadas (" + res.alertas.size() + ")"));

            for (AnalisisIA.Alerta al : res.alertas) {
                LinearLayout box = new LinearLayout(context);
                box.setOrientation(LinearLayout.VERTICAL);
                box.setBackgroundResource(R.drawable.bg_badge_warning);
                int pad10 = UiHelper.dp(context, 10);
                box.setPadding(pad10, pad10, pad10, pad10);
                LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                );
                bLp.bottomMargin = UiHelper.dp(context, 8);
                box.setLayoutParams(bLp);

                TextView tvCod = new TextView(context);
                tvCod.setText(al.codigo);
                tvCod.setTextColor(UiHelper.COLOR_ADVERTENCIA);
                tvCod.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                tvCod.setTypeface(Typeface.DEFAULT_BOLD);

                TextView tvMsg = new TextView(context);
                tvMsg.setText(al.mensaje);
                tvMsg.setTextColor(UiHelper.COLOR_TEXTO);
                tvMsg.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);

                box.addView(tvCod);
                box.addView(tvMsg);
                alertCard.addView(box);
            }
            resultadoContainer.addView(alertCard);
        }

        // 3. Recomendaciones con prioridad (ALTA, MEDIA, BAJA) y ahorro estimado
        if (res.recomendaciones != null && !res.recomendaciones.isEmpty()) {
            LinearLayout recCard = UiHelper.createCard(context);
            recCard.addView(UiHelper.createSectionTitle(context, "Recomendaciones del asesor (" + res.recomendaciones.size() + ")"));

            for (AnalisisIA.Recomendacion rec : res.recomendaciones) {
                LinearLayout item = new LinearLayout(context);
                item.setOrientation(LinearLayout.VERTICAL);
                item.setBackgroundResource(R.drawable.bg_card);
                int pad12 = UiHelper.dp(context, 12);
                item.setPadding(pad12, pad12, pad12, pad12);
                LinearLayout.LayoutParams iLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                );
                iLp.bottomMargin = UiHelper.dp(context, 8);
                item.setLayoutParams(iLp);

                LinearLayout top = new LinearLayout(context);
                top.setOrientation(LinearLayout.HORIZONTAL);
                top.setGravity(Gravity.CENTER_VERTICAL);

                // Icono de prioridad segun UI_DESIGN.md Seccion 5.3
                String prioIcon = "fa-solid fa-circle-half-stroke";
                int prioColor = UiHelper.COLOR_ADVERTENCIA;
                if ("ALTA".equals(rec.prioridad)) {
                    prioIcon = "fa-solid fa-circle-exclamation";
                    prioColor = UiHelper.COLOR_PELIGRO;
                } else if ("BAJA".equals(rec.prioridad)) {
                    prioIcon = "fa-regular fa-circle";
                    prioColor = UiHelper.COLOR_SECUNDARIO;
                }

                TextView icTv = new TextView(context);
                FaIcon.apply(icTv, prioIcon);
                icTv.setTextColor(prioColor);
                icTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                LinearLayout.LayoutParams icLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                );
                icLp.rightMargin = UiHelper.dp(context, 8);
                icTv.setLayoutParams(icLp);
                top.addView(icTv);

                TextView tvTit = new TextView(context);
                tvTit.setText(rec.titulo);
                tvTit.setTextColor(UiHelper.COLOR_PRIMARIO);
                tvTit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                tvTit.setTypeface(Typeface.DEFAULT_BOLD);
                tvTit.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                top.addView(tvTit);

                TextView tvPrio = new TextView(context);
                tvPrio.setText("Prioridad " + rec.prioridad);
                tvPrio.setTextColor(prioColor);
                tvPrio.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                tvPrio.setTypeface(Typeface.DEFAULT_BOLD);
                top.addView(tvPrio);

                item.addView(top);

                TextView tvDet = new TextView(context);
                tvDet.setText(rec.detalle);
                tvDet.setTextColor(UiHelper.COLOR_TEXTO);
                tvDet.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                );
                dLp.topMargin = UiHelper.dp(context, 6);
                tvDet.setLayoutParams(dLp);
                item.addView(tvDet);

                if (rec.ahorroEstimadoCentavos != null && rec.ahorroEstimadoCentavos > 0) {
                    TextView tvAh = new TextView(context);
                    tvAh.setText("Ahorro mensual estimado: " + CurrencyUtils.formatConMoneda(rec.ahorroEstimadoCentavos));
                    tvAh.setTextColor(UiHelper.COLOR_EXITO);
                    tvAh.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                    tvAh.setTypeface(Typeface.DEFAULT_BOLD);
                    UiHelper.applyTabularNums(tvAh);
                    LinearLayout.LayoutParams ahLp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    ahLp.topMargin = UiHelper.dp(context, 4);
                    tvAh.setLayoutParams(ahLp);
                    item.addView(tvAh);
                }

                recCard.addView(item);
            }
            resultadoContainer.addView(recCard);
        }
    }
}
