package com.finanzas.app.util;

import com.finanzas.app.model.Categoria;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interprete de lenguaje natural para la funcionalidad de "Creacion rapida de gasto por texto con IA".
 * Extrae monto en centavos, tipo (GASTO/INGRESO), naturaleza (FIJO/VARIABLE), fecha,
 * metodo de pago, comercio, categoria sugerida y descripcion a partir de una frase del usuario.
 */
public final class QuickExpenseParser {

    public static class ParsedExpense {
        public String tipo = "GASTO";
        public String naturaleza = "VARIABLE";
        public long montoCentavos = 0L;
        public String fecha;
        public String descripcion = "";
        public int categoriaId = 0;
        public String metodoPago = "EFECTIVO";
        public String comercio = "";
        public String notas = "";
    }

    private QuickExpenseParser() {
    }

    public static ParsedExpense parse(String rawText, List<Categoria> categorias) {
        ParsedExpense result = new ParsedExpense();
        result.fecha = DateUtils.getTodayIso();
        if (rawText == null || rawText.trim().isEmpty()) {
            return result;
        }

        String text = rawText.trim();
        String lower = text.toLowerCase(Locale.ROOT);

        // 1. Detectar Tipo (INGRESO vs GASTO)
        if (lower.contains("ingreso") || lower.contains("cobre") || lower.contains("salario")
                || lower.contains("sueldo") || lower.contains("reembolso") || lower.contains("venta")
                || lower.contains("honorario") || lower.contains("me pagaron")) {
            result.tipo = "INGRESO";
        } else {
            result.tipo = "GASTO";
        }

        // 2. Detectar Naturaleza (FIJO vs VARIABLE)
        if (lower.contains("alquiler") || lower.contains("arriendo") || lower.contains("mensualidad")
                || lower.contains("suscripcion") || lower.contains("internet") || lower.contains("luz")
                || lower.contains("electricidad") || lower.contains("agua") || lower.contains("seguro")
                || lower.contains("fijo") || lower.contains("salario") || lower.contains("sueldo")) {
            result.naturaleza = "FIJO";
        } else {
            result.naturaleza = "VARIABLE";
        }

        // 3. Detectar Fecha ("ayer", "hoy", o YYYY-MM-DD)
        Matcher isoMatcher = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})").matcher(text);
        if (isoMatcher.find()) {
            result.fecha = isoMatcher.group(1);
        } else if (lower.contains("ayer")) {
            result.fecha = DateUtils.getYesterdayIso();
        } else {
            result.fecha = DateUtils.getTodayIso();
        }

        // 4. Detectar Metodo de pago
        if (lower.contains("credito") || lower.contains("tc") || lower.contains("visa") || lower.contains("mastercard")) {
            result.metodoPago = "CREDITO";
        } else if (lower.contains("debito") || lower.contains("td")) {
            result.metodoPago = "DEBITO";
        } else if (lower.contains("transferencia") || lower.contains("banco") || lower.contains("deposito")) {
            result.metodoPago = "TRANSFERENCIA";
        } else if (lower.contains("efectivo") || lower.contains("cash") || lower.contains("contado")) {
            result.metodoPago = "EFECTIVO";
        } else {
            result.metodoPago = "DEBITO";
        }

        // 5. Extraer Monto (ej. $45.50, 45,50, 120, etc., ignorando fechas)
        String textWithoutDates = text.replaceAll("\\d{4}-\\d{2}-\\d{2}", " ");
        Matcher amountMatcher = Pattern.compile("(?:\\$|usd|eur|s/|\\b)\\s*(\\d{1,8}(?:[.,]\\d{1,2})?)\\b", Pattern.CASE_INSENSITIVE).matcher(textWithoutDates);
        if (amountMatcher.find()) {
            String numStr = amountMatcher.group(1).replace(",", ".");
            long parsed = CurrencyUtils.parseMontoToCentavos(numStr);
            if (parsed > 0) {
                result.montoCentavos = parsed;
            }
        }

        // 6. Extraer Comercio si dice "en <Comercio>"
        Matcher comercioMatcher = Pattern.compile("\\ben\\s+([A-Z0-9][A-Za-z0-9\\s]{1,30}?)(?:\\s+por|\\s+con|\\s+ayer|\\s+hoy|\\s+\\$|\\s+\\d|$)").matcher(text);
        if (comercioMatcher.find()) {
            result.comercio = comercioMatcher.group(1).trim();
        }

        // 7. Emparejar Categoria segun palabras clave y lista real de categorias
        result.categoriaId = matchCategoria(lower, result.tipo, categorias);

        // 8. Construir descripcion limpia
        String cleanDesc = text
                .replaceAll("(?i)\\b(gaste|pague|compre|ingreso|cobre|con tarjeta de credito|con tarjeta de debito|en efectivo|por transferencia|ayer|hoy)\\b", "")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleanDesc.isEmpty()) {
            cleanDesc = text;
        }
        if (cleanDesc.length() > 200) {
            cleanDesc = cleanDesc.substring(0, 200);
        }
        result.descripcion = cleanDesc;

        return result;
    }

    private static int matchCategoria(String lower, String tipo, List<Categoria> categorias) {
        if (categorias == null || categorias.isEmpty()) {
            return "INGRESO".equals(tipo) ? 1 : 31;
        }

        // Primero intentar coincidencia directa por nombre de categoria activa
        for (Categoria c : categorias) {
            if (!c.activa || !tipo.equals(c.tipo)) continue;
            String catLower = c.nombre.toLowerCase(Locale.ROOT);
            if (lower.contains(catLower)) {
                return c.id;
            }
        }

        // Coincidencia semantica por palabras clave
        String targetKeyword = null;
        if ("INGRESO".equals(tipo)) {
            if (lower.contains("sueldo") || lower.contains("salario") || lower.contains("quincena")) targetKeyword = "salario";
            else if (lower.contains("freelance") || lower.contains("cliente") || lower.contains("honorario")) targetKeyword = "honorarios";
            else if (lower.contains("dividendo") || lower.contains("interes") || lower.contains("inversion")) targetKeyword = "inversiones";
            else if (lower.contains("venta") || lower.contains("vendi")) targetKeyword = "ventas";
            else if (lower.contains("devolucion") || lower.contains("reembolso")) targetKeyword = "reembolsos";
        } else {
            if (lower.contains("super") || lower.contains("mercado") || lower.contains("despensa") || lower.contains("iveria") || lower.contains("supermaxi")) targetKeyword = "supermercado";
            else if (lower.contains("almuerzo") || lower.contains("cena") || lower.contains("desayuno") || lower.contains("comida") || lower.contains("cafe") || lower.contains("restaurante") || lower.contains("pizza")) targetKeyword = "restaurantes";
            else if (lower.contains("taxi") || lower.contains("uber") || lower.contains("bus") || lower.contains("metro") || lower.contains("pasaje")) targetKeyword = "transporte";
            else if (lower.contains("gasolina") || lower.contains("diesel") || lower.contains("combustible") || lower.contains("tanqueo")) targetKeyword = "combustible";
            else if (lower.contains("taller") || lower.contains("mecanico") || lower.contains("parqueo") || lower.contains("peaje")) targetKeyword = "vehiculo";
            else if (lower.contains("alquiler") || lower.contains("arriendo") || lower.contains("hipoteca") || lower.contains("condominio")) targetKeyword = "vivienda";
            else if (lower.contains("luz") || lower.contains("electricidad") || lower.contains("planilla electrica")) targetKeyword = "electricidad";
            else if (lower.contains("agua")) targetKeyword = "agua";
            else if (lower.contains("internet") || lower.contains("celular") || lower.contains("telefono") || lower.contains("fibra")) targetKeyword = "internet";
            else if (lower.contains("medico") || lower.contains("consulta") || lower.contains("clinica") || lower.contains("dentista")) targetKeyword = "salud";
            else if (lower.contains("farmacia") || lower.contains("medicina") || lower.contains("pastillas")) targetKeyword = "farmacia";
            else if (lower.contains("netflix") || lower.contains("spotify") || lower.contains("suscripcion") || lower.contains("prime")) targetKeyword = "suscripciones";
            else if (lower.contains("cine") || lower.contains("concierto") || lower.contains("juego")) targetKeyword = "entretenimiento";
            else if (lower.contains("curso") || lower.contains("libro") || lower.contains("universidad") || lower.contains("colegio")) targetKeyword = "educacion";
            else if (lower.contains("camisa") || lower.contains("zapatos") || lower.contains("pantalon") || lower.contains("ropa")) targetKeyword = "ropa";
            else if (lower.contains("perro") || lower.contains("gato") || lower.contains("veterinario") || lower.contains("mascota")) targetKeyword = "mascotas";
        }

        if (targetKeyword != null) {
            for (Categoria c : categorias) {
                if (c.activa && tipo.equals(c.tipo) && c.nombre.toLowerCase(Locale.ROOT).contains(targetKeyword)) {
                    return c.id;
                }
            }
        }

        // Fallback: primera categoria activa de ese tipo
        for (Categoria c : categorias) {
            if (c.activa && tipo.equals(c.tipo)) {
                return c.id;
            }
        }
        return categorias.get(0).id;
    }
}
