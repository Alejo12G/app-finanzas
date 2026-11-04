package com.finanzas.app.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.finanzas.app.MainActivity;
import com.finanzas.app.R;
import com.finanzas.app.api.ApiCallback;
import com.finanzas.app.api.ApiClient;
import com.finanzas.app.api.ApiError;
import com.finanzas.app.api.AppConfig;
import com.finanzas.app.model.HealthInfo;
import com.finanzas.app.model.PaginacionMeta;
import com.finanzas.app.util.CurrencyUtils;
import com.finanzas.app.util.FaIcon;
import com.finanzas.app.util.UiHelper;

/**
 * Vista de Configuracion y Estado de Conexion con el VPS (UI_DESIGN.md Seccion 6 y API_CONTRACT.md 7.1).
 */
public class ConfiguracionView extends ScrollView {

    private final MainActivity host;
    private TextView tvStatusIcon;
    private TextView tvStatusText;
    private TextView tvVersionApi;
    private TextView tvMonedaServidor;
    private TextView tvHoraServidor;

    public ConfiguracionView(Context context, MainActivity host) {
        super(context);
        this.host = host;
        setFillViewport(true);
        initUi(context);
    }

    private void initUi(Context context) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = UiHelper.dp(context, 16);
        root.setPadding(pad, pad, pad, pad);
        addView(root);

        // Tarjeta de estado de conexion (GET /health)
        LinearLayout statusCard = UiHelper.createCard(context);
        statusCard.addView(UiHelper.createSectionTitle(context, "Estado de conexion con el servidor"));

        LinearLayout statusRow = new LinearLayout(context);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        sLp.bottomMargin = UiHelper.dp(context, 12);
        statusRow.setLayoutParams(sLp);

        tvStatusIcon = new TextView(context);
        FaIcon.apply(tvStatusIcon, "fa-solid fa-server");
        tvStatusIcon.setTextColor(UiHelper.COLOR_SECUNDARIO);
        tvStatusIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        LinearLayout.LayoutParams icLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        icLp.rightMargin = UiHelper.dp(context, 10);
        tvStatusIcon.setLayoutParams(icLp);
        statusRow.addView(tvStatusIcon);

        tvStatusText = new TextView(context);
        tvStatusText.setText("Comprobando estado...");
        tvStatusText.setTextColor(UiHelper.COLOR_TEXTO);
        tvStatusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvStatusText.setTypeface(Typeface.DEFAULT_BOLD);
        statusRow.addView(tvStatusText);
        statusCard.addView(statusRow);

        tvVersionApi = createInfoRow(context, statusCard, "Version del contrato API:");
        tvMonedaServidor = createInfoRow(context, statusCard, "Moneda activa (ISO 4217):");
        tvHoraServidor = createInfoRow(context, statusCard, "Hora UTC del servidor:");

        LinearLayout btnCheck = UiHelper.createActionButton(
                context,
                "fa-solid fa-arrows-rotate",
                "Comprobar conexion ahora",
                R.drawable.bg_button_primary,
                UiHelper.COLOR_TEXTO_INVERTIDO
        );
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UiHelper.dp(context, 48)
        );
        bLp.topMargin = UiHelper.dp(context, 12);
        btnCheck.setLayoutParams(bLp);
        btnCheck.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                loadData();
            }
        });
        statusCard.addView(btnCheck);
        root.addView(statusCard);

        // Tarjeta de parametros del cliente (solo lectura segun UI_DESIGN.md Seccion 6)
        LinearLayout configCard = UiHelper.createCard(context);
        configCard.addView(UiHelper.createSectionTitle(context, "Parametros de red y seguridad (solo lectura)"));

        TextView tvUrl = createInfoRow(context, configCard, "URL base del servidor:");
        tvUrl.setText(AppConfig.BASE_URL);

        TextView tvHost = createInfoRow(context, configCard, "Host autorizado (Cleartext HTTP):");
        tvHost.setText(AppConfig.SERVER_HOST + ":" + AppConfig.SERVER_PORT);

        TextView tvHeader = createInfoRow(context, configCard, "Header de autenticacion:");
        tvHeader.setText(AppConfig.HEADER_TOKEN_NAME + " (Configurado, 32 caracteres)");

        TextView tvTimeouts = createInfoRow(context, configCard, "Timeouts HTTP:");
        tvTimeouts.setText("General: 15 s | Modulo IA Gemini: 90 s");

        TextView tvVersionApp = createInfoRow(context, configCard, "Version de la aplicacion Android:");
        tvVersionApp.setText("v" + AppConfig.APP_VERSION + " (Java nativo puro)");

        root.addView(configCard);
    }

    private TextView createInfoRow(Context context, LinearLayout parent, String label) {
        TextView lbl = UiHelper.createLabel(context, label);
        parent.addView(lbl);

        TextView val = new TextView(context);
        val.setText("-");
        val.setTextColor(UiHelper.COLOR_TEXTO);
        val.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        UiHelper.applyTabularNums(val);
        parent.addView(val);
        return val;
    }

    public void loadData() {
        host.setGlobalLoading(true);
        ApiClient.getInstance().getHealth(new ApiCallback<HealthInfo>() {
            @Override
            public void onSuccess(HealthInfo data, PaginacionMeta meta) {
                host.setGlobalLoading(false);
                FaIcon.apply(tvStatusIcon, "fa-solid fa-server");
                tvStatusIcon.setTextColor(UiHelper.COLOR_EXITO);
                tvStatusText.setText("CONECTADO (" + data.estado + ")");
                tvStatusText.setTextColor(UiHelper.COLOR_EXITO);
                tvVersionApi.setText(data.versionApi);
                tvMonedaServidor.setText(data.moneda);
                tvHoraServidor.setText(data.horaServidor);
                CurrencyUtils.setActiveCurrency(data.moneda);
                host.updateCurrencySubtitle();
            }

            @Override
            public void onError(ApiError error) {
                host.setGlobalLoading(false);
                FaIcon.apply(tvStatusIcon, "fa-solid fa-plug-circle-xmark");
                tvStatusIcon.setTextColor(UiHelper.COLOR_PELIGRO);
                tvStatusText.setText("SIN CONEXION (" + error.mensaje + ")");
                tvStatusText.setTextColor(UiHelper.COLOR_PELIGRO);
                tvVersionApi.setText("-");
                tvMonedaServidor.setText(CurrencyUtils.getActiveCurrency());
                tvHoraServidor.setText("-");
            }
        });
    }
}
