package com.finanzas.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.finanzas.app.api.ApiCallback;
import com.finanzas.app.api.ApiClient;
import com.finanzas.app.api.ApiError;
import com.finanzas.app.model.Categoria;
import com.finanzas.app.model.CreditCard;
import com.finanzas.app.model.HealthInfo;
import com.finanzas.app.model.PaginacionMeta;
import com.finanzas.app.model.Transaccion;
import com.finanzas.app.ui.AsesorIaView;
import com.finanzas.app.ui.CategoriasView;
import com.finanzas.app.ui.ComprasView;
import com.finanzas.app.ui.ConfiguracionView;
import com.finanzas.app.ui.DashboardView;
import com.finanzas.app.ui.PresupuestosView;
import com.finanzas.app.ui.TarjetasView;
import com.finanzas.app.ui.TransaccionesView;
import com.finanzas.app.ui.dialogs.GastoRapidoIaDialog;
import com.finanzas.app.ui.dialogs.TransaccionFormDialog;
import com.finanzas.app.util.CurrencyUtils;
import com.finanzas.app.util.FaIcon;
import com.finanzas.app.util.ThemeManager;
import com.finanzas.app.util.UiHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Actividad principal de la aplicacion Android nativa en Java puro.
 * Implementa las 7 secciones obligatorias de UI_DESIGN.md Seccion 5.1 y Seccion 6,
 * e inyecta el sistema de Temas (Modo Oscuro Clasico por defecto y Modo Claro).
 */
public class MainActivity extends Activity {

    private static final int TAB_DASHBOARD = 0;
    private static final int TAB_TRANSACCIONES = 1;
    private static final int TAB_COMPRAS = 2;
    private static final int TAB_TARJETAS = 3;
    private static final int TAB_PRESUPUESTOS = 4;
    private static final int TAB_CATEGORIAS = 5;
    private static final int TAB_ASESOR_IA = 6;
    private static final int TAB_CONFIGURACION = 7;

    private static final String STATE_ACTIVE_TAB = "state_active_tab";
    private static int lastActiveTab = TAB_DASHBOARD;

    private TextView tvScreenTitle;
    private TextView tvSubtitleCurrency;
    private TextView btnThemeToggleTop;
    private ProgressBar progressBarGlobal;
    private LinearLayout bannerGlobalStatus;
    private TextView tvBannerGlobalIcon;
    private TextView tvBannerGlobalText;
    private Button btnBannerRetry;
    private FrameLayout contentContainer;
    private LinearLayout bottomNavContainer;

    private DashboardView dashboardView;
    private TransaccionesView transaccionesView;
    private ComprasView comprasView;
    private TarjetasView tarjetasView;
    private PresupuestosView presupuestosView;
    private CategoriasView categoriasView;
    private AsesorIaView asesorIaView;
    private ConfiguracionView configuracionView;

    private int activeTab = TAB_DASHBOARD;
    private final List<LinearLayout> navTabViews = new ArrayList<>();
    private List<Categoria> cachedCategorias = new ArrayList<>();
    private List<CreditCard> cachedCreditCards = new ArrayList<>();

    private static final String[][] NAV_ITEMS = {
            {"Panel", "fa-solid fa-chart-pie"},
            {"Transacciones", "fa-solid fa-money-bill-transfer"},
            {"Compras", "fa-solid fa-cart-shopping"},
            {"Tarjetas", "fa-solid fa-credit-card"},
            {"Presupuestos", "fa-solid fa-wallet"},
            {"Categorias", "fa-solid fa-tags"},
            {"Asesor IA", "fa-solid fa-robot"},
            {"Configuracion", "fa-solid fa-gear"}
    };

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ThemeManager.wrapContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Inyeccion obligatoria del Tema (Modo Oscuro Clasico por defecto) antes de setContentView
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            activeTab = savedInstanceState.getInt(STATE_ACTIVE_TAB, lastActiveTab);
        } else {
            activeTab = lastActiveTab;
        }

        TextView tvAppLogoIcon = findViewById(R.id.tvAppLogoIcon);
        FaIcon.apply(tvAppLogoIcon, "fa-solid fa-building-columns");

        tvScreenTitle = findViewById(R.id.tvScreenTitle);
        tvSubtitleCurrency = findViewById(R.id.tvSubtitleCurrency);
        btnThemeToggleTop = findViewById(R.id.btnThemeToggleTop);
        progressBarGlobal = findViewById(R.id.progressBarGlobal);
        bannerGlobalStatus = findViewById(R.id.bannerGlobalStatus);
        tvBannerGlobalIcon = findViewById(R.id.tvBannerGlobalIcon);
        tvBannerGlobalText = findViewById(R.id.tvBannerGlobalText);
        btnBannerRetry = findViewById(R.id.btnBannerRetry);
        contentContainer = findViewById(R.id.contentContainer);
        bottomNavContainer = findViewById(R.id.bottomNavContainer);

        updateToolbarThemeButton();
        btnThemeToggleTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lastActiveTab = activeTab;
                ThemeManager.toggleTheme(MainActivity.this);
            }
        });

        TextView btnQuickAiTop = findViewById(R.id.btnQuickAiTop);
        FaIcon.apply(btnQuickAiTop, "fa-solid fa-wand-magic-sparkles");
        btnQuickAiTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GastoRapidoIaDialog.show(MainActivity.this, cachedCategorias, new TransaccionFormDialog.OnSavedListener() {
                    @Override
                    public void onSaved(Transaccion transaccion) {
                        reloadActiveScreen();
                    }
                });
            }
        });

        TextView btnRefreshTop = findViewById(R.id.btnRefreshTop);
        FaIcon.apply(btnRefreshTop, "fa-solid fa-arrows-rotate");
        btnRefreshTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bannerGlobalStatus.setVisibility(View.GONE);
                refreshCategoriasCache();
                refreshCreditCardsCache();
                reloadActiveScreen();
            }
        });

        btnBannerRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bannerGlobalStatus.setVisibility(View.GONE);
                refreshCategoriasCache();
                refreshCreditCardsCache();
                reloadActiveScreen();
            }
        });

        initViews();
        buildBottomNavigation();
        cargarCategoriasSemillaLocales();
        checkHealthAndInit();
        selectTab(activeTab);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_ACTIVE_TAB, activeTab);
        lastActiveTab = activeTab;
    }

    public void updateToolbarThemeButton() {
        if (btnThemeToggleTop == null) {
            return;
        }
        boolean isDark = ThemeManager.isDarkMode(this);
        FaIcon.apply(btnThemeToggleTop, isDark ? "fa-solid fa-sun" : "fa-solid fa-moon");
        btnThemeToggleTop.setContentDescription(isDark ? "Cambiar a Modo Claro" : "Cambiar a Modo Oscuro");
    }

    private void initViews() {
        dashboardView = new DashboardView(this, this);
        transaccionesView = new TransaccionesView(this, this);
        comprasView = new ComprasView(this, this);
        tarjetasView = new TarjetasView(this, this);
        presupuestosView = new PresupuestosView(this, this);
        categoriasView = new CategoriasView(this, this);
        asesorIaView = new AsesorIaView(this, this);
        configuracionView = new ConfiguracionView(this, this);
    }

    private void buildBottomNavigation() {
        bottomNavContainer.removeAllViews();
        navTabViews.clear();

        for (int i = 0; i < NAV_ITEMS.length; i++) {
            final int index = i;
            LinearLayout tab = new LinearLayout(this);
            tab.setOrientation(LinearLayout.VERTICAL);
            tab.setGravity(Gravity.CENTER);
            int padH = UiHelper.dp(this, 12);
            tab.setPadding(padH, UiHelper.dp(this, 6), padH, UiHelper.dp(this, 6));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            tab.setLayoutParams(lp);

            TextView iconTv = new TextView(this);
            FaIcon.apply(iconTv, NAV_ITEMS[i][1]);
            iconTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            iconTv.setGravity(Gravity.CENTER);
            iconTv.setContentDescription(null);

            TextView labelTv = new TextView(this);
            labelTv.setText(NAV_ITEMS[i][0]);
            labelTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            labelTv.setGravity(Gravity.CENTER);

            tab.addView(iconTv);
            tab.addView(labelTv);

            tab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectTab(index);
                }
            });

            navTabViews.add(tab);
            bottomNavContainer.addView(tab);
        }
    }

    public void selectTab(int tabIndex) {
        if (tabIndex < 0 || tabIndex >= NAV_ITEMS.length) {
            tabIndex = TAB_DASHBOARD;
        }
        activeTab = tabIndex;
        lastActiveTab = tabIndex;
        tvScreenTitle.setText(NAV_ITEMS[tabIndex][0]);

        for (int i = 0; i < navTabViews.size(); i++) {
            LinearLayout tab = navTabViews.get(i);
            boolean selected = (i == tabIndex);
            tab.setBackgroundColor(selected ? UiHelper.COLOR_PRIMARIO_SUAVE : UiHelper.COLOR_SUPERFICIE);
            TextView ic = (TextView) tab.getChildAt(0);
            TextView lbl = (TextView) tab.getChildAt(1);
            ic.setTextColor(selected ? UiHelper.COLOR_PRIMARIO : UiHelper.COLOR_SECUNDARIO);
            lbl.setTextColor(selected ? UiHelper.COLOR_PRIMARIO : UiHelper.COLOR_SECUNDARIO);
            lbl.setTypeface(selected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        }

        contentContainer.removeAllViews();
        switch (tabIndex) {
            case TAB_DASHBOARD:
                contentContainer.addView(dashboardView);
                dashboardView.loadData();
                break;
            case TAB_TRANSACCIONES:
                contentContainer.addView(transaccionesView);
                transaccionesView.loadData();
                break;
            case TAB_COMPRAS:
                contentContainer.addView(comprasView);
                comprasView.loadData();
                break;
            case TAB_TARJETAS:
                contentContainer.addView(tarjetasView);
                tarjetasView.loadData();
                break;
            case TAB_PRESUPUESTOS:
                contentContainer.addView(presupuestosView);
                presupuestosView.loadData();
                break;
            case TAB_CATEGORIAS:
                contentContainer.addView(categoriasView);
                categoriasView.loadData();
                break;
            case TAB_ASESOR_IA:
                contentContainer.addView(asesorIaView);
                break;
            case TAB_CONFIGURACION:
                contentContainer.addView(configuracionView);
                configuracionView.loadData();
                break;
        }
    }

    public void reloadActiveScreen() {
        selectTab(activeTab);
    }

    private void checkHealthAndInit() {
        ApiClient.getInstance().getHealth(new ApiCallback<HealthInfo>() {
            @Override
            public void onSuccess(HealthInfo data, PaginacionMeta meta) {
                if (data != null && data.moneda != null) {
                    CurrencyUtils.setActiveCurrency(data.moneda);
                    updateCurrencySubtitle();
                }
            }

            @Override
            public void onError(ApiError error) {
                handleGlobalApiError(error);
            }
        });
        refreshCategoriasCache();
        refreshCreditCardsCache();
    }

    public void refreshCategoriasCache() {
        ApiClient.getInstance().getCategorias(null, true, new ApiCallback<List<Categoria>>() {
            @Override
            public void onSuccess(List<Categoria> data, PaginacionMeta meta) {
                if (data != null && !data.isEmpty()) {
                    cachedCategorias = data;
                }
            }

            @Override
            public void onError(ApiError error) {
                // Mantiene respaldo semilla hasta que haya conexion
            }
        });
    }

    public void refreshCreditCardsCache() {
        ApiClient.getInstance().getCreditCards(new ApiCallback<List<CreditCard>>() {
            @Override
            public void onSuccess(List<CreditCard> data, PaginacionMeta meta) {
                if (data != null) {
                    cachedCreditCards = data;
                }
            }

            @Override
            public void onError(ApiError error) {
            }
        });
    }

    public List<Categoria> getCachedCategorias() {
        return cachedCategorias;
    }

    public List<CreditCard> getCachedCreditCards() {
        return cachedCreditCards;
    }

    public void updateCurrencySubtitle() {
        tvSubtitleCurrency.setText("Moneda: " + CurrencyUtils.getActiveCurrency());
    }

    public void setGlobalLoading(boolean loading) {
        progressBarGlobal.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    /**
     * Comportamiento comun ante errores segun API_CONTRACT.md Seccion 9.1.
     */
    public void handleGlobalApiError(ApiError error) {
        if (error == null) return;
        if (error.httpStatus == 401 || "TOKEN_INVALIDO".equals(error.codigo)) {
            bannerGlobalStatus.setVisibility(View.VISIBLE);
            FaIcon.apply(tvBannerGlobalIcon, "fa-solid fa-lock");
            tvBannerGlobalText.setText(getString(R.string.err_token_invalido));
            btnBannerRetry.setVisibility(View.GONE);
            contentContainer.setEnabled(false);
        } else if (error.esErrorRed) {
            bannerGlobalStatus.setVisibility(View.VISIBLE);
            FaIcon.apply(tvBannerGlobalIcon, "fa-solid fa-plug-circle-xmark");
            tvBannerGlobalText.setText(getString(R.string.err_sin_conexion));
            btnBannerRetry.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Respaldo de las 31 categorias semilla (API_CONTRACT.md Seccion 5) mientras se consulta GET /categorias.
     */
    private void cargarCategoriasSemillaLocales() {
        Object[][] seeds = {
                {1, "Salario", "INGRESO", "fa-solid fa-briefcase", "#2E7D32"},
                {2, "Honorarios", "INGRESO", "fa-solid fa-laptop-code", "#2E7D32"},
                {3, "Inversiones", "INGRESO", "fa-solid fa-chart-line", "#2E7D32"},
                {4, "Ventas", "INGRESO", "fa-solid fa-hand-holding-dollar", "#2E7D32"},
                {5, "Reembolsos", "INGRESO", "fa-solid fa-rotate-left", "#2E7D32"},
                {6, "Otros ingresos", "INGRESO", "fa-solid fa-coins", "#2E7D32"},
                {7, "Vivienda", "GASTO", "fa-solid fa-house-chimney", "#1B2A4A"},
                {8, "Electricidad", "GASTO", "fa-solid fa-bolt", "#1B2A4A"},
                {9, "Agua", "GASTO", "fa-solid fa-droplet", "#1B2A4A"},
                {10, "Internet y telefonia", "GASTO", "fa-solid fa-wifi", "#1B2A4A"},
                {11, "Supermercado", "GASTO", "fa-solid fa-basket-shopping", "#4A5568"},
                {12, "Restaurantes", "GASTO", "fa-solid fa-utensils", "#4A5568"},
                {13, "Transporte", "GASTO", "fa-solid fa-bus", "#4A5568"},
                {14, "Combustible", "GASTO", "fa-solid fa-gas-pump", "#4A5568"},
                {15, "Vehiculo", "GASTO", "fa-solid fa-car", "#4A5568"},
                {16, "Salud", "GASTO", "fa-solid fa-heart-pulse", "#4A5568"},
                {17, "Farmacia", "GASTO", "fa-solid fa-prescription-bottle-medical", "#4A5568"},
                {18, "Educacion", "GASTO", "fa-solid fa-graduation-cap", "#4A5568"},
                {19, "Entretenimiento", "GASTO", "fa-solid fa-film", "#4A5568"},
                {20, "Suscripciones", "GASTO", "fa-solid fa-repeat", "#1B2A4A"},
                {21, "Ropa", "GASTO", "fa-solid fa-shirt", "#4A5568"},
                {22, "Hogar", "GASTO", "fa-solid fa-couch", "#4A5568"},
                {23, "Seguros", "GASTO", "fa-solid fa-shield-halved", "#1B2A4A"},
                {24, "Deudas y prestamos", "GASTO", "fa-solid fa-credit-card", "#C62828"},
                {25, "Impuestos", "GASTO", "fa-solid fa-landmark", "#1B2A4A"},
                {26, "Mascotas", "GASTO", "fa-solid fa-paw", "#4A5568"},
                {27, "Regalos", "GASTO", "fa-solid fa-gift", "#4A5568"},
                {28, "Viajes", "GASTO", "fa-solid fa-plane", "#4A5568"},
                {29, "Cuidado personal", "GASTO", "fa-solid fa-spa", "#4A5568"},
                {30, "Ahorro", "GASTO", "fa-solid fa-piggy-bank", "#2E7D32"},
                {31, "Otros gastos", "GASTO", "fa-solid fa-ellipsis", "#4A5568"}
        };
        cachedCategorias.clear();
        for (Object[] row : seeds) {
            Categoria c = new Categoria();
            c.id = (Integer) row[0];
            c.nombre = (String) row[1];
            c.tipo = (String) row[2];
            c.icono = (String) row[3];
            c.color = (String) row[4];
            c.activa = true;
            cachedCategorias.add(c);
        }
    }
}
