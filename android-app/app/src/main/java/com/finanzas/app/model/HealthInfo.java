package com.finanzas.app.model;

import org.json.JSONObject;

public class HealthInfo {
    public String estado;
    public String versionApi;
    public String moneda;
    public String horaServidor;

    public static HealthInfo fromJson(JSONObject obj) {
        HealthInfo info = new HealthInfo();
        if (obj == null) {
            info.estado = "DESCONOCIDO";
            info.versionApi = "-";
            info.moneda = "COP";
            info.horaServidor = "-";
            return info;
        }
        info.estado = obj.optString("estado", "OK");
        info.versionApi = obj.optString("version_api", "1.0.0");
        info.moneda = obj.optString("moneda", "COP");
        info.horaServidor = obj.optString("hora_servidor", "-");
        return info;
    }
}
