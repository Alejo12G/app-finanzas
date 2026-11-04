package com.finanzas.app.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Presupuesto {
    public int id;
    public int categoriaId;
    public Categoria categoria;
    public int anio;
    public int mes;
    public long montoLimiteCentavos;
    public long gastadoCentavos;
    public long disponibleCentavos;
    public double porcentajeUsado;
    public String estado;
    public String notas;
    public String creadoEn;
    public String actualizadoEn;

    public static Presupuesto fromJson(JSONObject obj) {
        if (obj == null) {
            return null;
        }
        Presupuesto p = new Presupuesto();
        p.id = obj.optInt("id", 0);
        p.categoriaId = obj.optInt("categoria_id", 0);
        JSONObject catObj = obj.optJSONObject("categoria");
        if (catObj != null) {
            p.categoria = Categoria.fromJson(catObj);
        }
        p.anio = obj.optInt("anio", 2026);
        p.mes = obj.optInt("mes", 1);
        p.montoLimiteCentavos = obj.optLong("monto_limite_centavos", 0L);
        p.gastadoCentavos = obj.optLong("gastado_centavos", 0L);
        p.disponibleCentavos = obj.optLong("disponible_centavos", 0L);
        p.porcentajeUsado = obj.optDouble("porcentaje_usado", 0.0);
        p.estado = obj.optString("estado", "NORMAL");
        p.notas = obj.isNull("notas") ? null : obj.optString("notas", null);
        p.creadoEn = obj.isNull("creado_en") ? null : obj.optString("creado_en", null);
        p.actualizadoEn = obj.isNull("actualizado_en") ? null : obj.optString("actualizado_en", null);
        return p;
    }

    public static List<Presupuesto> fromJsonArray(JSONArray arr) {
        List<Presupuesto> list = new ArrayList<>();
        if (arr == null) {
            return list;
        }
        for (int i = 0; i < arr.length(); i++) {
            JSONObject item = arr.optJSONObject(i);
            if (item != null) {
                list.add(fromJson(item));
            }
        }
        return list;
    }
}
