package androidx.appcompat.app;

/**
 * Delegado ligero en Java puro para control de modo nocturno sin dependencias externas pesadas.
 * Mantiene compatibilidad exacta de constantes y API con AppCompatDelegate del SDK.
 */
public final class AppCompatDelegate {

    public static final int MODE_NIGHT_FOLLOW_SYSTEM = -1;
    public static final int MODE_NIGHT_NO = 1;
    public static final int MODE_NIGHT_YES = 2;

    private static int defaultNightMode = MODE_NIGHT_YES;

    private AppCompatDelegate() {
    }

    public static void setDefaultNightMode(int mode) {
        if (mode == MODE_NIGHT_NO || mode == MODE_NIGHT_YES || mode == MODE_NIGHT_FOLLOW_SYSTEM) {
            defaultNightMode = mode;
        }
    }

    public static int getDefaultNightMode() {
        return defaultNightMode;
    }
}
