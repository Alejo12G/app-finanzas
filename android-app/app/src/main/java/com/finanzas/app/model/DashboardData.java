package com.finanzas.app.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DashboardData {
    public int anio;
    public int mes;
    public String desde;
    public String hasta;

    // Totales
    public long ingresosCentavos;
    public long gastosCentavos;
    public long gastosFijosCentavos;
    public long gastosVariablesCentavos;
    public long balanceCentavos;
    public double tasaAhorroPorcentaje;
    public int cantidadTransacciones;

    // Comparativo mes anterior (puede ser null si anterior = 0)
    public Double ingresosVariacionPorcentaje;
    public Double gastosVariacionPorcentaje;

    // Desglose por categoria
    public List<MontoCategoriaItem> gastosPorCategoria = new ArrayList<>();
    public List<MontoCategoriaItem> ingresosPorCategoria = new ArrayList<>();

    // Resumen presupuestos
    public long presupuestosTotalLimiteCentavos;
    public long presupuestosTotalGastadoCentavos;
    public int presupuestosCantidadNormal;
    public int presupuestosCantidadAdvertencia;
    public int presupuestosCantidadExcedido;

    // Evolucion 6 meses
    public List<EvolucionMensualItem> evolucionMensual = new ArrayList<>();

    // Ultimas 5 transacciones
    public List<Transaccion> ultimasTransacciones = new ArrayList<>();

    public static class MontoCategoriaItem {
        public Categoria categoria;
        public long totalCentavos;
        public double porcentaje;
    }

    public static class EvolucionMensualItem {
        public int anio;
        public int mes;
        public long ingresosCentavos;
        public long gastosCentavos;
        public long balanceCentavos;
    }

    public static DashboardData fromJson(JSONObject obj) {
        DashboardData d = new DashboardData();
        if (obj == null) {
            return d;
        }
        JSONObject per = obj.optJSONObject("periodo");
        if (per != null) {
            d.anio = per.optInt("anio", 2026);
            d.mes = per.optInt("mes", 1);
            d.desde = per.optString("desde", "");
            d.hasta = per.optString("hasta", "");
        }
        JSONObject tot = obj.optJSONObject("totales");
        if (tot != null) {
            d.ingresosCentavos = tot.optLong("ingresos_centavos", 0L);
            d.gastosCentavos = tot.optLong("gastos_centavos", 0L);
            d.gastosFijosCentavos = tot.optLong("gastos_fijos_centavos", 0L);
            d.gastosVariablesCentavos = tot.optLong("gastos_variables_centavos", 0L);
            d.balanceCentavos = tot.optLong("balance_centavos", 0L);
            d.tasaAhorroPorcentaje = tot.optDouble("tasa_ahorro_porcentaje", 0.0);
            d.cantidadTransacciones = tot.optInt("cantidad_transacciones", 0);
        }
        JSONObject comp = obj.optJSONObject("comparativo_mes_anterior");
        if (comp != null) {
            d.ingresosVariacionPorcentaje = comp.isNull("ingresos_variacion_porcentaje")
                    ? null : comp.optDouble("ingresos_variacion_porcentaje", 0.0);
            d.gastosVariacionPorcentaje = comp.isNull("gastos_variacion_porcentaje")
                    ? null : comp.optDouble("gastos_variacion_porcentaje", 0.0);
        }

        JSONArray gCat = obj.optJSONArray("gastos_por_categoria");
        if (gCat != null) {
            for (int i = 0; i < gCat.length(); i++) {
                JSONObject item = gCat.optJSONObject(i);
                if (item != null) {
                    MontoCategoriaItem m = new MontoCategoriaItem();
                    m.categoria = Categoria.fromJson(item.optJSONObject("categoria"));
                    m.totalCentavos = item.optLong("total_centavos", 0L);
                    m.porcentaje = item.optDouble("porcentaje", 0.0);
                    d.gastosPorCategoria.add(m);
                }
            }
        }

        JSONArray iCat = obj.optJSONArray("ingresos_por_categoria");
        if (iCat != null) {
            for (int i = 0; i < iCat.length(); i++) {
                JSONObject item = iCat.optJSONObject(i);
                if (item != null) {
                    MontoCategoriaItem m = new MontoCategoriaItem();
                    m.categoria = Categoria.fromJson(item.optJSONObject("categoria"));
                    m.totalCentavos = item.optLong("total_centavos", 0L);
                    m.porcentaje = item.optDouble("porcentaje", 0.0);
                    d.ingresosPorCategoria.add(m);
                }
            }
        }

        JSONObject pres = obj.optJSONObject("presupuestos");
        if (pres != null) {
            d.presupuestosTotalLimiteCentavos = pres.optLong("total_limite_centavos", 0L);
            d.presupuestosTotalGastadoCentavos = pres.optLong("total_gastado_centavos", 0L);
            d.presupuestosCantidadNormal = pres.optInt("cantidad_normal", 0);
            d.presupuestosCantidadAdvertencia = pres.optInt("cantidad_advertencia", 0);
            d.presupuestosCantidadExcedido = pres.optInt("cantidad_excedido", 0);
        }

        JSONArray evo = obj.optJSONArray("evolucion_mensual");
        if (evo != null) {
            for (int i = 0; i < evo.length(); i++) {
                JSONObject item = evo.optJSONObject(i);
                if (item != null) {
                    EvolucionMensualItem e = new EvolucionMensualItem();
                    e.anio = item.optInt("anio", 2026);
                    e.mes = item.optInt("mes", 1);
                    e.ingresosCentavos = item.optLong("ingresos_centavos", 0L);
                    e.gastosCentavos = item.optLong("gastos_centavos", 0L);
                    e.balanceCentavos = item.optLong("balance_centavos", 0L);
                    d.evolucionMensual.add(e);
                }
            }
        }

        d.ultimasTransacciones = Transaccion.fromJsonArray(obj.optJSONArray("ultimas_transacciones"));
        return d;
    }
}
