package com.finanzas.app.util;

import android.content.Context;
import android.graphics.Typeface;
import android.widget.TextView;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Componente unico FaIcon (UI_DESIGN.md Seccion 1.1 y Seccion 5).
 * Carga las fuentes locales fa-solid-900.ttf, fa-regular-400.ttf y fa-brands-400.ttf
 * desde assets/fonts/ y traduce cualquier clase FontAwesome 6 a su glifo Unicode.
 */
public final class FaIcon {

    private static Typeface solidFont;
    private static Typeface regularFont;
    private static Typeface brandsFont;

    private static final Map<String, String> GLYPH_MAP = new HashMap<>();
    private static final String DEFAULT_ICON_CLASS = "fa-solid fa-tag";
    private static final String DEFAULT_GLYPH = "\uf02b";

    static {
        // 5.1 Navegacion principal
        GLYPH_MAP.put("fa-chart-pie", "\uf200");
        GLYPH_MAP.put("fa-money-bill-transfer", "\ue528");
        GLYPH_MAP.put("fa-cart-shopping", "\uf07a");
        GLYPH_MAP.put("fa-wallet", "\uf555");
        GLYPH_MAP.put("fa-tags", "\uf02c");
        GLYPH_MAP.put("fa-robot", "\uf544");
        GLYPH_MAP.put("fa-gear", "\uf013");
        GLYPH_MAP.put("fa-building-columns", "\uf19c");

        // 5.2 Botones y acciones
        GLYPH_MAP.put("fa-plus", "\u002b");
        GLYPH_MAP.put("fa-pen", "\uf304");
        GLYPH_MAP.put("fa-trash-can", "\uf2ed");
        GLYPH_MAP.put("fa-floppy-disk", "\uf0c7");
        GLYPH_MAP.put("fa-xmark", "\uf00d");
        GLYPH_MAP.put("fa-check", "\uf00c");
        GLYPH_MAP.put("fa-magnifying-glass", "\uf002");
        GLYPH_MAP.put("fa-filter", "\uf0b0");
        GLYPH_MAP.put("fa-filter-circle-xmark", "\ue17b");
        GLYPH_MAP.put("fa-arrows-rotate", "\uf021");
        GLYPH_MAP.put("fa-rotate-right", "\uf2f9");
        GLYPH_MAP.put("fa-file-export", "\uf56e");
        GLYPH_MAP.put("fa-file-csv", "\uf6dd");
        GLYPH_MAP.put("fa-print", "\uf02f");
        GLYPH_MAP.put("fa-eye", "\uf06e");
        GLYPH_MAP.put("fa-eye-slash", "\uf070");
        GLYPH_MAP.put("fa-square-plus", "\uf0fe");
        GLYPH_MAP.put("fa-square-minus", "\uf146");
        GLYPH_MAP.put("fa-receipt", "\uf543");
        GLYPH_MAP.put("fa-calendar-days", "\uf073");
        GLYPH_MAP.put("fa-chevron-left", "\uf053");
        GLYPH_MAP.put("fa-chevron-right", "\uf054");
        GLYPH_MAP.put("fa-bars", "\uf0c9");
        GLYPH_MAP.put("fa-ellipsis-vertical", "\uf142");
        GLYPH_MAP.put("fa-sort", "\uf0dc");
        GLYPH_MAP.put("fa-sort-up", "\uf0de");
        GLYPH_MAP.put("fa-sort-down", "\uf0dd");
        GLYPH_MAP.put("fa-wand-magic-sparkles", "\ue2ca");
        GLYPH_MAP.put("fa-paper-plane", "\uf1d8");
        GLYPH_MAP.put("fa-copy", "\uf0c5");
        GLYPH_MAP.put("fa-moon", "\uf186");
        GLYPH_MAP.put("fa-sun", "\uf185");

        // 5.3 Indicadores y estados
        GLYPH_MAP.put("fa-arrow-trend-up", "\ue098");
        GLYPH_MAP.put("fa-arrow-trend-down", "\ue097");
        GLYPH_MAP.put("fa-scale-balanced", "\uf24e");
        GLYPH_MAP.put("fa-piggy-bank", "\uf4d3");
        GLYPH_MAP.put("fa-thumbtack", "\uf08d");
        GLYPH_MAP.put("fa-shuffle", "\uf074");
        GLYPH_MAP.put("fa-circle-check", "\uf058");
        GLYPH_MAP.put("fa-triangle-exclamation", "\uf071");
        GLYPH_MAP.put("fa-circle-xmark", "\uf057");
        GLYPH_MAP.put("fa-circle-info", "\uf05a");
        GLYPH_MAP.put("fa-spinner", "\uf110");
        GLYPH_MAP.put("fa-server", "\uf233");
        GLYPH_MAP.put("fa-plug-circle-xmark", "\ue560");
        GLYPH_MAP.put("fa-key", "\uf084");
        GLYPH_MAP.put("fa-lock", "\uf023");
        GLYPH_MAP.put("fa-lightbulb", "\uf0eb");
        GLYPH_MAP.put("fa-circle-exclamation", "\uf06a");
        GLYPH_MAP.put("fa-circle-half-stroke", "\uf042");
        GLYPH_MAP.put("fa-circle", "\uf111");
        GLYPH_MAP.put("fa-gauge-high", "\uf625");
        GLYPH_MAP.put("fa-folder-open", "\uf07c");

        // 5.4 Metodos de pago
        GLYPH_MAP.put("fa-money-bill", "\uf0d6");
        GLYPH_MAP.put("fa-money-check", "\uf53c");
        GLYPH_MAP.put("fa-credit-card", "\uf09d");
        GLYPH_MAP.put("fa-circle-question", "\uf059");

        // 5.6 Categorias semilla
        GLYPH_MAP.put("fa-briefcase", "\uf0b1");
        GLYPH_MAP.put("fa-laptop-code", "\uf5fc");
        GLYPH_MAP.put("fa-chart-line", "\uf201");
        GLYPH_MAP.put("fa-hand-holding-dollar", "\uf4c0");
        GLYPH_MAP.put("fa-rotate-left", "\uf2ea");
        GLYPH_MAP.put("fa-coins", "\uf51e");
        GLYPH_MAP.put("fa-house-chimney", "\ue3af");
        GLYPH_MAP.put("fa-bolt", "\uf0e7");
        GLYPH_MAP.put("fa-droplet", "\uf043");
        GLYPH_MAP.put("fa-wifi", "\uf1eb");
        GLYPH_MAP.put("fa-basket-shopping", "\uf291");
        GLYPH_MAP.put("fa-utensils", "\uf2e7");
        GLYPH_MAP.put("fa-bus", "\uf207");
        GLYPH_MAP.put("fa-gas-pump", "\uf52f");
        GLYPH_MAP.put("fa-car", "\uf1b9");
        GLYPH_MAP.put("fa-heart-pulse", "\uf21e");
        GLYPH_MAP.put("fa-prescription-bottle-medical", "\uf486");
        GLYPH_MAP.put("fa-graduation-cap", "\uf19d");
        GLYPH_MAP.put("fa-film", "\uf008");
        GLYPH_MAP.put("fa-repeat", "\uf363");
        GLYPH_MAP.put("fa-shirt", "\uf553");
        GLYPH_MAP.put("fa-couch", "\uf4b8");
        GLYPH_MAP.put("fa-shield-halved", "\uf3ed");
        GLYPH_MAP.put("fa-landmark", "\uf66f");
        GLYPH_MAP.put("fa-paw", "\uf1b0");
        GLYPH_MAP.put("fa-gift", "\uf06b");
        GLYPH_MAP.put("fa-plane", "\uf072");
        GLYPH_MAP.put("fa-spa", "\uf5bb");
        GLYPH_MAP.put("fa-ellipsis", "\uf141");

        // 5.7 Iconos sugeridos para categorias nuevas
        GLYPH_MAP.put("fa-dumbbell", "\uf44b");
        GLYPH_MAP.put("fa-baby", "\uf77c");
        GLYPH_MAP.put("fa-book", "\uf02d");
        GLYPH_MAP.put("fa-tooth", "\uf5c9");
        GLYPH_MAP.put("fa-wrench", "\uf0ad");
        GLYPH_MAP.put("fa-mobile-screen", "\uf3cf");
        GLYPH_MAP.put("fa-tv", "\uf26c");
        GLYPH_MAP.put("fa-gamepad", "\uf11b");
        GLYPH_MAP.put("fa-mug-hot", "\uf7b6");
        GLYPH_MAP.put("fa-scissors", "\uf0c4");
        GLYPH_MAP.put("fa-hand-holding-heart", "\uf4be");
        GLYPH_MAP.put("fa-building", "\uf1ad");
        GLYPH_MAP.put("fa-truck", "\uf0d1");
        GLYPH_MAP.put("fa-children", "\ue4e1");
        GLYPH_MAP.put("fa-tag", "\uf02b");
    }

    /**
     * Lista oficial de iconos para el selector de categorias (UI_DESIGN.md 5.6 y 5.7).
     */
    public static final List<String> SELECTOR_CATEGORY_ICONS = Arrays.asList(
            "fa-solid fa-tag",
            "fa-solid fa-briefcase",
            "fa-solid fa-laptop-code",
            "fa-solid fa-chart-line",
            "fa-solid fa-hand-holding-dollar",
            "fa-solid fa-rotate-left",
            "fa-solid fa-coins",
            "fa-solid fa-house-chimney",
            "fa-solid fa-bolt",
            "fa-solid fa-droplet",
            "fa-solid fa-wifi",
            "fa-solid fa-basket-shopping",
            "fa-solid fa-utensils",
            "fa-solid fa-bus",
            "fa-solid fa-gas-pump",
            "fa-solid fa-car",
            "fa-solid fa-heart-pulse",
            "fa-solid fa-prescription-bottle-medical",
            "fa-solid fa-graduation-cap",
            "fa-solid fa-film",
            "fa-solid fa-repeat",
            "fa-solid fa-shirt",
            "fa-solid fa-couch",
            "fa-solid fa-shield-halved",
            "fa-solid fa-credit-card",
            "fa-solid fa-landmark",
            "fa-solid fa-paw",
            "fa-solid fa-gift",
            "fa-solid fa-plane",
            "fa-solid fa-spa",
            "fa-solid fa-piggy-bank",
            "fa-solid fa-ellipsis",
            "fa-solid fa-dumbbell",
            "fa-solid fa-baby",
            "fa-solid fa-book",
            "fa-solid fa-tooth",
            "fa-solid fa-wrench",
            "fa-solid fa-mobile-screen",
            "fa-solid fa-tv",
            "fa-solid fa-gamepad",
            "fa-solid fa-mug-hot",
            "fa-solid fa-scissors",
            "fa-solid fa-hand-holding-heart",
            "fa-solid fa-building",
            "fa-solid fa-truck",
            "fa-solid fa-children"
    );

    private FaIcon() {
    }

    public static synchronized Typeface getTypeface(Context context, String iconClass) {
        if (solidFont == null) {
            solidFont = Typeface.createFromAsset(context.getAssets(), "fonts/fa-solid-900.ttf");
            regularFont = Typeface.createFromAsset(context.getAssets(), "fonts/fa-regular-400.ttf");
            brandsFont = Typeface.createFromAsset(context.getAssets(), "fonts/fa-brands-400.ttf");
        }
        if (iconClass != null) {
            if (iconClass.startsWith("fa-regular")) {
                return regularFont;
            }
            if (iconClass.startsWith("fa-brands")) {
                return brandsFont;
            }
        }
        return solidFont;
    }

    public static String getGlyph(String iconClass) {
        if (iconClass == null || iconClass.trim().isEmpty()) {
            return DEFAULT_GLYPH;
        }
        String[] parts = iconClass.trim().split("\\s+");
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i];
            if (GLYPH_MAP.containsKey(part)) {
                return GLYPH_MAP.get(part);
            }
        }
        return DEFAULT_GLYPH;
    }

    public static void apply(TextView textView, String iconClass) {
        if (textView == null) {
            return;
        }
        String resolvedClass = (iconClass != null && !iconClass.isEmpty()) ? iconClass : DEFAULT_ICON_CLASS;
        textView.setTypeface(getTypeface(textView.getContext(), resolvedClass));
        textView.setText(getGlyph(resolvedClass));
    }

    public static String getMetodoPagoIcon(String metodoPago) {
        if ("EFECTIVO".equals(metodoPago)) return "fa-solid fa-money-bill";
        if ("DEBITO".equals(metodoPago)) return "fa-solid fa-money-check";
        if ("CREDITO".equals(metodoPago)) return "fa-solid fa-credit-card";
        if ("TRANSFERENCIA".equals(metodoPago)) return "fa-solid fa-building-columns";
        return "fa-solid fa-circle-question";
    }

    public static String getMetodoPagoLabel(String metodoPago) {
        if ("EFECTIVO".equals(metodoPago)) return "Efectivo";
        if ("DEBITO".equals(metodoPago)) return "Tarjeta de debito";
        if ("CREDITO".equals(metodoPago)) return "Tarjeta de credito";
        if ("TRANSFERENCIA".equals(metodoPago)) return "Transferencia";
        return "Otro";
    }

    public static String getUnidadLabel(String unidad) {
        if ("UNIDAD".equals(unidad)) return "und";
        if ("KG".equals(unidad)) return "kg";
        if ("G".equals(unidad)) return "g";
        if ("L".equals(unidad)) return "L";
        if ("ML".equals(unidad)) return "ml";
        if ("PAQUETE".equals(unidad)) return "paq";
        return "otro";
    }
}
