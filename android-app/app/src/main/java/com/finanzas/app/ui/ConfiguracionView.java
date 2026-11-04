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
import com.finanzas.app.util.ThemeManager;
import com.finanzas.app.util.UiHelper;

/**
 * Vista de Configuracion, Selector de Tema (Modo Oscuro Clasico por defecto / Modo Claro)
 * y Estado de Conexion con el VPS (UI_DESIGN.md Seccion 6 y API_CONTRACT.md 7.1).
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

    private void initUi(final Context context) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = UiHelper.dp(context, 16);
        root.setPadding(pad, pad, pad, pad);
        addView(root);

        // 1. Tarjeta de Apariencia y Tema Visual (Modo Oscuro por defecto y Modo Claro)
        LinearLayout themeCard = UiHelper.createCard(context);
        themeCard.addView(UiHelper.createSectionTitle(context, "Apariencia y tema visual"));

        final boolean isDark = ThemeManager.isDarkMode(context);

        LinearLayout currentThemeRow = new LinearLayout(context);
        currentThemeRow.setOrientation(LinearLayout.HORIZONTAL);
        currentThemeRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams ctLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        ctLp.bottomMargin = UiHelper.dp(context, 12);
        currentThemeRow.setLayoutParams(ctLp);

        TextView tvThemeIcon = new TextView(context);
        FaIcon.apply(tvThemeIcon, isDark ? "fa-solid fa-moon" : "fa-solid fa-sun");
        tvThemeIcon.setTextColor(UiHelper.COLOR_PRIMARIO);
        tvThemeIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        LinearLayout.LayoutParams ticLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        ticLp.rightMargin = UiHelper.dp(context, 10);
        tvThemeIcon.setLayoutParams(ticLp);
        currentThemeRow.addView(tvThemeIcon);

        LinearLayout themeInfoCol = new LinearLayout(context);
        themeInfoCol.setOrientation(LinearLayout.VERTICAL);
        themeInfoCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvThemeTitle = new TextView(context);
        tvThemeTitle.setText(isDark ? "Modo Oscuro Clasico (Activo)" : "Modo Claro Bancario (Activo)");
        tvThemeTitle.setTextColor(UiHelper.COLOR_TEXTO);
        tvThemeTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvThemeTitle.setTypeface(Typeface.DEFAULT_BOLD);

        TextView tvThemeSub = new TextView(context);
        tvThemeSub.setText("La preferencia se guarda en SharedPreferences. Modo Oscuro Clasico es el tema por defecto.");
        tvThemeSub.setTextColor(UiHelper.COLOR_TEXTO_SECUNDARIO);
        tvThemeSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);

        themeInfoCol.addView(tvThemeTitle);
        themeInfoCol.addView(tvThemeSub);
        currentThemeRow.addView(themeInfoCol);
        themeCard.addView(currentThemeRow);

        // Selector clasico con botones de Modo Oscuro y Modo Claro (FontAwesome)
        LinearLayout selectorRow = new LinearLayout(context);
        selectorRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams selLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        selLp.bottomMargin = UiHelper.dp(context, 8);
        selectorRow.setLayoutParams(selLp);

        LinearLayout btnDarkMode = UiHelper.createActionButton(
                context,
                "fa-solid fa-moon",
                context.getString(R.string.tema_modo_oscuro),
                isDark ? R.drawable.bg_button_primary : R.drawable.bg_button_secondary,
                isDark ? UiHelper.COLOR_TEXTO_INVERTIDO : UiHelper.COLOR_TEXTO_SECUNDARIO
        );
        LinearLayout.LayoutParams darkLp = new LinearLayout.LayoutParams(0, UiHelper.dp(context, 48), 1f);
        darkLp.rightMargin = UiHelper.dp(context, 8);
        btnDarkMode.setLayoutParams(darkLp);
        btnDarkMode.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ThemeManager.selectTheme(host, true);
            }
        });

        LinearLayout btnLightMode = UiHelper.createActionButton(
                context,
                "fa-solid fa-sun",
                context.getString(R.string.tema_modo_claro),
                !isDark ? R.drawable.bg_button_primary : R.drawable.bg_button_secondary,
                !isDark ? UiHelper.COLOR_TEXTO_INVERTIDO : UiHelper.COLOR_TEXTO_SECUNDARIO
        );
        LinearLayout.LayoutParams lightLp = new LinearLayout.LayoutParams(0, UiHelper.dp(context, 48), 1f);
        btnLightMode.setLayoutParams(lightLp);
        btnLightMode.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ThemeManager.selectTheme(host, false);
            }
        });

        selectorRow.addView(btnDarkMode);
        selectorRow.addView(btnLightMode);
        themeCard.addView(selectorRow);

        LinearLayout btnToggleTheme = UiHelper.createActionButton(
                context,
                "fa-solid fa-circle-half-stroke",
                isDark ? "Cambiar a Modo Claro" : "Cambiar a Modo Oscuro",
                R.drawable.bg_button_secondary,
                UiHelper.COLOR_PRIMARIO
        );
        LinearLayout.LayoutParams togLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, UiHelper.dp(context, 44)
        );
        btnToggleTheme.setLayoutParams(togLp);
        btnToggleTheme.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ThemeManager.toggleTheme(host);
            }
        });
        themeCard.addView(btnToggleTheme);
        root.addView(themeCard);

        // 2. Tarjeta de estado de conexion (GET /health)
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

        // 3. Tarjeta de parametros del cliente (solo lectura segun UI_DESIGN.md Seccion 6)
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
