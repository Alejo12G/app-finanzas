package com.finanzas.app.model;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PresupuestosResumen {
    public int anio;
    public int mes;
    public long totalLimiteCentavos;
    public long totalGastadoCentavos;
    public long totalDisponibleCentavos;
    public double porcentajeUsadoGlobal;
    public List<Presupuesto> presupuestos = new ArrayList<>();

    public static PresupuestosResumen fromJson(JSONObject obj) {
        PresupuestosResumen r = new PresupuestosResumen();
        if (obj == null) {
            return r;
        }
        r.anio = obj.optInt("anio", 2026);
        r.mes = obj.optInt("mes", 1);
        r.totalLimiteCentavos = obj.optLong("total_limite_centavos", 0L);
        r.totalGastadoCentavos = obj.optLong("total_gastado_centavos", 0L);
        r.totalDisponibleCentavos = obj.optLong("total_disponible_centavos", 0L);
        r.porcentajeUsadoGlobal = obj.optDouble("porcentaje_usado_global", 0.0);
        r.presupuestos = Presupuesto.fromJsonArray(obj.optJSONArray("presupuestos"));
        return r;
    }
}
