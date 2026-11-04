package com.finanzas.app.util;

/**
 * Validaciones del lado del cliente segun API_CONTRACT.md Secciones 2.2, 2.3 y 2.4.
 */
public final class ValidationUtils {

    private ValidationUtils() {
    }

    /**
     * Politica anti-emojis (API_CONTRACT.md Seccion 2.2):
     * Rangos prohibidos: U+1F000-U+1FAFF, U+2600-U+27BF, U+FE0F, U+200D, U+1F1E6-U+1F1FF.
     */
    public static boolean containsEmoji(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        final int length = text.length();
        for (int offset = 0; offset < length; ) {
            final int cp = text.codePointAt(offset);
            if ((cp >= 0x1F000 && cp <= 0x1FAFF)
                    || (cp >= 0x2600 && cp <= 0x27BF)
                    || cp == 0xFE0F
                    || cp == 0x200D
                    || (cp >= 0x1F1E6 && cp <= 0x1F1FF)) {
                return true;
            }
            offset += Character.charCount(cp);
        }
        return false;
    }

    public static boolean isValidIsoDate(String date) {
        if (date == null || !date.trim().matches("\\d{4}-\\d{2}-\\d{2}")) {
            return false;
        }
        try {
            String[] parts = date.trim().split("-");
            int y = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            int d = Integer.parseInt(parts[2]);
            return y >= 2000 && y <= 2100 && m >= 1 && m <= 12 && d >= 1 && d <= 31;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isValidMontoCentavos(long centavos) {
        return centavos >= 1L && centavos <= 99999999999L;
    }

    public static boolean isValidPrecioUnitarioCentavos(long centavos) {
        return centavos >= 0L && centavos <= 9999999999L;
    }

    public static boolean isValidCantidad(double cantidad) {
        return cantidad > 0.0 && cantidad <= 99999.0;
    }
}
