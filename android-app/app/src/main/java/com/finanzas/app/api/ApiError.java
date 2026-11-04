package com.finanzas.app.api;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiError {
    public int httpStatus;
    public String codigo;
    public String mensaje;
    public List<DetalleError> detalles = new ArrayList<>();
    public boolean esErrorRed;

    public static class DetalleError {
        public String campo;
        public String mensaje;

        public DetalleError(String campo, String mensaje) {
            this.campo = campo;
            this.mensaje = mensaje;
        }
    }

    public ApiError(int httpStatus, String codigo, String mensaje, boolean esErrorRed) {
        this.httpStatus = httpStatus;
        this.codigo = codigo;
        this.mensaje = mensaje;
        this.esErrorRed = esErrorRed;
    }

    public static ApiError networkError() {
        return new ApiError(0, "ERROR_INTERNO", "No se pudo conectar con el servidor.", true);
    }

    public static ApiError fromJson(int httpStatus, JSONObject rootObj) {
        if (httpStatus == 401) {
            return new ApiError(401, "TOKEN_INVALIDO", "Token de acceso invalido.", false);
        }
        if (httpStatus == 429) {
            return new ApiError(429, "LIMITE_IA_EXCEDIDO", "Limite de analisis IA alcanzado. Intente mas tarde.", false);
        }
        if (rootObj == null) {
            return new ApiError(httpStatus, "ERROR_INTERNO", "Error al procesar la respuesta del servidor.", false);
        }
        JSONObject errObj = rootObj.optJSONObject("error");
        if (errObj == null) {
            return new ApiError(httpStatus, "ERROR_INTERNO", "Error inesperado del servidor.", false);
        }
        String codigo = errObj.optString("codigo", "ERROR_INTERNO");
        String mensaje = errObj.optString("mensaje", "Error en la solicitud.");
        if (httpStatus == 401 || "TOKEN_INVALIDO".equals(codigo)) {
            mensaje = "Token de acceso invalido.";
        } else if (httpStatus == 429 || "LIMITE_IA_EXCEDIDO".equals(codigo)) {
            mensaje = "Limite de analisis IA alcanzado. Intente mas tarde.";
        }
        ApiError apiError = new ApiError(httpStatus, codigo, mensaje, false);
        JSONArray detArr = errObj.optJSONArray("detalles");
        if (detArr != null) {
            for (int i = 0; i < detArr.length(); i++) {
                JSONObject d = detArr.optJSONObject(i);
                if (d != null) {
                    apiError.detalles.add(new DetalleError(
                            d.optString("campo", ""),
                            d.optString("mensaje", "")
                    ));
                }
            }
        }
        return apiError;
    }

    public Map<String, String> getMapaErroresPorCampo() {
        Map<String, String> map = new HashMap<>();
        if (detalles != null) {
            for (DetalleError d : detalles) {
                if (d.campo != null && !d.campo.isEmpty()) {
                    map.put(d.campo, d.mensaje);
                }
            }
        }
        return map;
    }
}
