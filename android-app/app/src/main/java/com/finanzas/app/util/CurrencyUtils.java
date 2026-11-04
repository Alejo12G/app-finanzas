package com.finanzas.app.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Formateador y conversor monetario en centavos enteros (API_CONTRACT.md Seccion 2 y UI_DESIGN.md 2.3)
 * y unidades monetarias para el modulo de tarjetas de credito (API_CONTRACT.md Seccion 11).
 */
public final class CurrencyUtils {

    private static String activeCurrency = "COP";

    private CurrencyUtils() {
    }

    public static void setActiveCurrency(String currency) {
        if (currency != null && !currency.trim().isEmpty()) {
            activeCurrency = currency.trim().toUpperCase(Locale.US);
        }
    }

    public static String getActiveCurrency() {
        return activeCurrency;
    }

    private static String formatAbsCentavos(long absCentavos) {
        long entero = absCentavos / 100L;
        long dec = absCentavos % 100L;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        return df.format(entero) + "." + (dec < 10 ? "0" + dec : String.valueOf(dec));
    }

    /**
     * Formato neutro: 1,250.00
     */
    public static String formatNeutro(long centavos) {
        if (centavos < 0) {
            return "- " + formatAbsCentavos(Math.abs(centavos));
        }
        return formatAbsCentavos(centavos);
    }

    /**
     * Formato neutro con codigo de moneda: USD 1,250.00
     */
    public static String formatConMoneda(long centavos) {
        if (centavos < 0) {
            return "- " + activeCurrency + " " + formatAbsCentavos(Math.abs(centavos));
        }
        return activeCurrency + " " + formatAbsCentavos(centavos);
    }

    /**
     * Formato segun tipo de transaccion (UI_DESIGN.md 2.3):
     * INGRESO -> "+ 1,250.00"
     * GASTO   -> "- 1,250.00"
     */
    public static String formatPorTipo(long centavos, String tipo) {
        String base = formatAbsCentavos(Math.abs(centavos));
        if ("INGRESO".equalsIgnoreCase(tipo)) {
            return "+ " + base;
        }
        return "- " + base;
    }

    /**
     * Formato de balance:
     * Positivo -> "1,250.00" (o con moneda)
     * Negativo -> "- 1,250.00"
     */
    public static String formatBalance(long centavos, boolean incluirMoneda) {
        String prefix = incluirMoneda ? (activeCurrency + " ") : "";
        if (centavos < 0) {
            return "- " + prefix + formatAbsCentavos(Math.abs(centavos));
        }
        return prefix + formatAbsCentavos(centavos);
    }

    /**
     * Convierte centavos a texto editable para inputs (ej. 1250 -> "12.50").
     */
    public static String centavosToInputString(long centavos) {
        long abs = Math.abs(centavos);
        long entero = abs / 100L;
        long dec = abs % 100L;
        return entero + "." + (dec < 10 ? "0" + dec : String.valueOf(dec));
    }

    /**
     * Convierte texto ingresado por el usuario (ej. "12.50" o "12,50") a centavos enteros.
     * Retorna -1 si el formato es invalido.
     */
    public static long parseMontoToCentavos(String input) {
        if (input == null || input.trim().isEmpty()) {
            return -1L;
        }
        try {
            String normalized = input.trim().replace(",", ".");
            BigDecimal bd = new BigDecimal(normalized);
            BigDecimal cents = bd.multiply(new BigDecimal(100)).setScale(0, RoundingMode.HALF_UP);
            return cents.longValueExact();
        } catch (Exception e) {
            return -1L;
        }
    }

    /**
     * Convierte unidades de moneda (double) a centavos enteros (long).
     */
    public static long unitsToCentavos(double units) {
        if (Double.isNaN(units) || Double.isInfinite(units)) {
            return 0L;
        }
        return BigDecimal.valueOf(units)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    /**
     * Convierte centavos enteros (long) a unidades de moneda (double con 2 decimales).
     */
    public static double centavosToUnits(long centavos) {
        return BigDecimal.valueOf(centavos)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Formatea unidades de moneda con codigo ISO activo (ej. "COP 94,559.60").
     */
    public static String formatUnitsConMoneda(double units) {
        return formatConMoneda(unitsToCentavos(units));
    }

    /**
     * Formatea unidades de moneda de forma neutra (ej. "94,559.60").
     */
    public static String formatUnitsNeutro(double units) {
        return formatNeutro(unitsToCentavos(units));
    }

    /**
     * Convierte unidades de moneda a texto editable sin separador de miles (ej. "5000.00").
     */
    public static String unitsToInputString(double units) {
        return centavosToInputString(unitsToCentavos(units));
    }

    public static String formatPorcentaje(double pct) {
        return String.format(Locale.US, "%.2f %%", pct);
    }

    public static String formatTasaInput(double rate) {
        if (rate == (long) rate) {
            return String.format(Locale.US, "%.1f", rate);
        }
        return BigDecimal.valueOf(rate).stripTrailingZeros().toPlainString();
    }
}
