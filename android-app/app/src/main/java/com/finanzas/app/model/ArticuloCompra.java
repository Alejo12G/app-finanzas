package com.finanzas.app.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class ArticuloCompra {
    public int id;
    public String articulo;
    public double cantidad;
    public String unidad;
    public long precioUnitarioCentavos;
    public long subtotalCentavos;
    public int orden;

    public ArticuloCompra() {
        this.articulo = "";
        this.cantidad = 1.0;
        this.unidad = "UNIDAD";
        this.precioUnitarioCentavos = 0L;
        this.subtotalCentavos = 0L;
        this.orden = 0;
    }

    /**
     * Regla del contrato (API_CONTRACT.md 4.3):
     * subtotal_centavos = redondeo_mitad_hacia_arriba(cantidad * precio_unitario_centavos)
     */
    public long calcularSubtotalCentavos() {
        if (cantidad <= 0 || precioUnitarioCentavos <= 0) {
            this.subtotalCentavos = 0L;
            return 0L;
        }
        BigDecimal cantBd = BigDecimal.valueOf(cantidad);
        BigDecimal precioBd = BigDecimal.valueOf(precioUnitarioCentavos);
        BigDecimal sub = cantBd.multiply(precioBd).setScale(0, RoundingMode.HALF_UP);
        this.subtotalCentavos = sub.longValue();
        return this.subtotalCentavos;
    }

    public static ArticuloCompra fromJson(JSONObject obj) {
        if (obj == null) {
            return null;
        }
        ArticuloCompra a = new ArticuloCompra();
        a.id = obj.optInt("id", 0);
        a.articulo = obj.optString("articulo", "");
        a.cantidad = obj.optDouble("cantidad", 1.0);
        a.unidad = obj.optString("unidad", "UNIDAD");
        a.precioUnitarioCentavos = obj.optLong("precio_unitario_centavos", 0L);
        a.subtotalCentavos = obj.optLong("subtotal_centavos", 0L);
        a.orden = obj.optInt("orden", 0);
        return a;
    }

    public static List<ArticuloCompra> fromJsonArray(JSONArray arr) {
        List<ArticuloCompra> list = new ArrayList<>();
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
