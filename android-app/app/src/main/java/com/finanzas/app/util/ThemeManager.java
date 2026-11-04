package com.finanzas.app.util;

import android.app.Activity;
import android.app.UiModeManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.view.Window;

import androidx.appcompat.app.AppCompatDelegate;

import com.finanzas.app.R;

/**
 * Gestor centralizado de Temas (Modo Oscuro Clasico por defecto y Modo Claro Bancario).
 * Persiste la eleccion en SharedPreferences y sincroniza la configuracion de recursos
 * values / values-night sin requerir librerias externas pesadas.
 */
public final class ThemeManager {

    public static final String PREFS_NAME = "finanzas_theme_prefs";
    public static final String KEY_NIGHT_MODE = "night_mode";

    public static final int MODE_NIGHT_NO = AppCompatDelegate.MODE_NIGHT_NO;
    public static final int MODE_NIGHT_YES = AppCompatDelegate.MODE_NIGHT_YES;

    private ThemeManager() {
    }

    /**
     * Lee el modo guardado en SharedPreferences.
     * Si es la primera vez que se abre la aplicacion (sin preferencia previa),
     * fuerza y guarda por defecto AppCompatDelegate.MODE_NIGHT_YES (Modo Oscuro Clasico).
     */
    public static int getSavedNightMode(Context context) {
        if (context == null) {
            return AppCompatDelegate.MODE_NIGHT_YES;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.contains(KEY_NIGHT_MODE)) {
            prefs.edit().putInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_YES).apply();
            return AppCompatDelegate.MODE_NIGHT_YES;
        }
        int mode = prefs.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_YES);
        if (mode != AppCompatDelegate.MODE_NIGHT_NO && mode != AppCompatDelegate.MODE_NIGHT_YES) {
            mode = AppCompatDelegate.MODE_NIGHT_YES;
        }
        return mode;
    }

    public static boolean isDarkMode(Context context) {
        return getSavedNightMode(context) == AppCompatDelegate.MODE_NIGHT_YES;
    }

    /**
     * Guarda el modo elegido y actualiza el estado global del tema.
     */
    public static void setNightMode(Context context, int nightMode) {
        int resolvedMode = (nightMode == AppCompatDelegate.MODE_NIGHT_NO)
                ? AppCompatDelegate.MODE_NIGHT_NO
                : AppCompatDelegate.MODE_NIGHT_YES;
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putInt(KEY_NIGHT_MODE, resolvedMode).apply();
        }
        AppCompatDelegate.setDefaultNightMode(resolvedMode);
        applySystemNightModeIfSupported(context, resolvedMode);
        if (context != null) {
            updateResourcesConfiguration(context, resolvedMode == AppCompatDelegate.MODE_NIGHT_YES);
            UiHelper.syncThemeColors(context);
        }
    }

    /**
     * Alterna entre Modo Oscuro y Modo Claro, guarda la preferencia y recrea la actividad.
     */
    public static boolean toggleTheme(Activity activity) {
        boolean nextDark = !isDarkMode(activity);
        int nextMode = nextDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        setNightMode(activity, nextMode);
        if (activity != null) {
            activity.recreate();
        }
        return nextDark;
    }

    /**
     * Establece explicitamente Modo Oscuro o Modo Claro desde un selector de UI.
     */
    public static void selectTheme(Activity activity, boolean darkMode) {
        boolean currentDark = isDarkMode(activity);
        int targetMode = darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        setNightMode(activity, targetMode);
        if (activity != null && currentDark != darkMode) {
            activity.recreate();
        }
    }

    /**
     * Envuelve el Context base con la configuracion uiMode correspondiente (values-night vs values).
     */
    public static Context wrapContext(Context base) {
        if (base == null) {
            return null;
        }
        boolean dark = isDarkMode(base);
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                | (dark ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO);
        return base.createConfigurationContext(config);
    }

    /**
     * Inicializa el tema en Application.onCreate o antes de setContentView en MainActivity.
     */
    public static void applyTheme(Context context) {
        int mode = getSavedNightMode(context);
        boolean dark = (mode == AppCompatDelegate.MODE_NIGHT_YES);
        AppCompatDelegate.setDefaultNightMode(mode);
        applySystemNightModeIfSupported(context, mode);

        if (context != null) {
            updateResourcesConfiguration(context, dark);
            Context appCtx = context.getApplicationContext();
            if (appCtx != null && appCtx != context) {
                updateResourcesConfiguration(appCtx, dark);
            }
            if (context instanceof Activity) {
                Activity activity = (Activity) context;
                activity.setTheme(dark ? R.style.AppTheme_Dark : R.style.AppTheme_Light);
                UiHelper.syncThemeColors(activity);
                Window window = activity.getWindow();
                if (window != null) {
                    int topBarColor = UiHelper.getColor(activity, R.color.color_barra_superior, UiHelper.COLOR_SUPERFICIE);
                    window.setStatusBarColor(topBarColor);
                    window.setNavigationBarColor(UiHelper.COLOR_SUPERFICIE);
                }
            } else {
                UiHelper.syncThemeColors(context);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static void updateResourcesConfiguration(Context context, boolean dark) {
        Resources res = context.getResources();
        if (res == null) {
            return;
        }
        Configuration config = new Configuration(res.getConfiguration());
        config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                | (dark ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO);
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    private static void applySystemNightModeIfSupported(Context context, int mode) {
        if (context == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
                if (uiModeManager != null) {
                    int sysMode = (mode == AppCompatDelegate.MODE_NIGHT_NO)
                            ? UiModeManager.MODE_NIGHT_NO
                            : UiModeManager.MODE_NIGHT_YES;
                    uiModeManager.setApplicationNightMode(sysMode);
                }
            } catch (Exception ignored) {
            }
        }
    }
}
