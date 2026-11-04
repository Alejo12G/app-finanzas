package com.finanzas.app.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AnalisisIA {
    public String tipoAnalisis;
    public int anio;
    public int mes;
    public String pregunta;
    public String resumen;
    public int puntuacionSaludFinanciera;
    public List<Recomendacion> recomendaciones = new ArrayList<>();
    public List<Alerta> alertas = new ArrayList<>();
    public String modelo;
    public String generadoEn;

    public static class Recomendacion {
        public String titulo;
        public String detalle;
        public String prioridad;
        public Long ahorroEstimadoCentavos;
        public Integer categoriaId;
    }

    public static class Alerta {
        public String codigo;
        public String mensaje;
        public Integer categoriaId;
    }

    public static AnalisisIA fromJson(JSONObject obj) {
        AnalisisIA a = new AnalisisIA();
        if (obj == null) {
            return a;
        }
        a.tipoAnalisis = obj.optString("tipo_analisis", "RESUMEN_MENSUAL");
        JSONObject per = obj.optJSONObject("periodo");
        if (per != null) {
            a.anio = per.optInt("anio", 2026);
            a.mes = per.optInt("mes", 1);
        }
        a.pregunta = obj.isNull("pregunta") ? null : obj.optString("pregunta", null);
        a.resumen = obj.optString("resumen", "");
        a.puntuacionSaludFinanciera = obj.optInt("puntuacion_salud_financiera", 0);
        a.modelo = obj.optString("modelo", "gemini-2.5-flash");
        a.generadoEn = obj.optString("generado_en", "");

        JSONArray recs = obj.optJSONArray("recomendaciones");
        if (recs != null) {
            for (int i = 0; i < recs.length(); i++) {
                JSONObject item = recs.optJSONObject(i);
                if (item != null) {
                    Recomendacion r = new Recomendacion();
                    r.titulo = item.optString("titulo", "");
                    r.detalle = item.optString("detalle", "");
                    r.prioridad = item.optString("prioridad", "MEDIA");
                    r.ahorroEstimadoCentavos = item.isNull("ahorro_estimado_centavos")
                            ? null : item.optLong("ahorro_estimado_centavos", 0L);
                    r.categoriaId = item.isNull("categoria_id")
                            ? null : item.optInt("categoria_id", 0);
                    a.recomendaciones.add(r);
                }
            }
        }

        JSONArray alrs = obj.optJSONArray("alertas");
        if (alrs != null) {
            for (int i = 0; i < alrs.length(); i++) {
                JSONObject item = alrs.optJSONObject(i);
                if (item != null) {
                    Alerta al = new Alerta();
                    al.codigo = item.optString("codigo", "OTRO");
                    al.mensaje = item.optString("mensaje", "");
                    al.categoriaId = item.isNull("categoria_id")
                            ? null : item.optInt("categoria_id", 0);
                    a.alertas.add(al);
                }
            }
        }

        return a;
    }
}
