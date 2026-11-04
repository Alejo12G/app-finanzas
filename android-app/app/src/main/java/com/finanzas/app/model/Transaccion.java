package com.finanzas.app.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Transaccion {
    public int id;
    public String tipo;
    public String naturaleza;
    public long montoCentavos;
    public String fecha;
    public String descripcion;
    public int categoriaId;
    public Categoria categoria;
    public String metodoPago;
    public String paymentMethod;
    public Integer cardId;
    public Integer installments;
    public Boolean isInterestExempt;
    public Double appliedInterestRate;
    public Double monthlyInstallmentAmount;
    public Double totalInterestAmount;
    public Double totalWithInterest;
    public String comercio;
    public String notas;
    public boolean esCompra;
    public int cantidadArticulos;
    public String creadoEn;
    public String actualizadoEn;

    public static Transaccion fromJson(JSONObject obj) {
        if (obj == null) {
            return null;
        }
        Transaccion t = new Transaccion();
        t.id = obj.optInt("id", 0);
        t.tipo = obj.optString("tipo", "GASTO");
        t.naturaleza = obj.optString("naturaleza", "VARIABLE");
        t.montoCentavos = obj.optLong("monto_centavos", 0L);
        t.fecha = obj.optString("fecha", "");
        t.descripcion = obj.optString("descripcion", "");
        t.categoriaId = obj.optInt("categoria_id", 0);
        JSONObject catObj = obj.optJSONObject("categoria");
        if (catObj != null) {
            t.categoria = Categoria.fromJson(catObj);
        }
        t.metodoPago = obj.optString("metodo_pago", "EFECTIVO");
        t.paymentMethod = obj.isNull("payment_method")
                ? ("CREDITO".equals(t.metodoPago) ? "CREDIT_CARD" : t.metodoPago)
                : obj.optString("payment_method", t.metodoPago);

        if (obj.has("card_id") && !obj.isNull("card_id")) {
            t.cardId = obj.optInt("card_id");
        }
        if (obj.has("installments") && !obj.isNull("installments")) {
            t.installments = obj.optInt("installments", 1);
        }
        if (obj.has("is_interest_exempt") && !obj.isNull("is_interest_exempt")) {
            t.isInterestExempt = obj.optBoolean("is_interest_exempt", false);
        }
        if (obj.has("applied_interest_rate") && !obj.isNull("applied_interest_rate")) {
            t.appliedInterestRate = obj.optDouble("applied_interest_rate", 0.0);
        }
        if (obj.has("monthly_installment_amount") && !obj.isNull("monthly_installment_amount")) {
            t.monthlyInstallmentAmount = obj.optDouble("monthly_installment_amount", 0.0);
        }
        if (obj.has("total_interest_amount") && !obj.isNull("total_interest_amount")) {
            t.totalInterestAmount = obj.optDouble("total_interest_amount", 0.0);
        }
        if (obj.has("total_with_interest") && !obj.isNull("total_with_interest")) {
            t.totalWithInterest = obj.optDouble("total_with_interest", 0.0);
        }

        t.comercio = obj.isNull("comercio") ? null : obj.optString("comercio", null);
        t.notas = obj.isNull("notas") ? null : obj.optString("notas", null);
        t.esCompra = obj.optBoolean("es_compra", false);
        t.cantidadArticulos = obj.optInt("cantidad_articulos", 0);
        t.creadoEn = obj.isNull("creado_en") ? null : obj.optString("creado_en", null);
        t.actualizadoEn = obj.isNull("actualizado_en") ? null : obj.optString("actualizado_en", null);
        return t;
    }

    public boolean isCreditCardTransaction() {
        return "CREDIT_CARD".equalsIgnoreCase(paymentMethod)
                || "CREDITO".equalsIgnoreCase(metodoPago)
                || (cardId != null && cardId > 0);
    }

    public static List<Transaccion> fromJsonArray(JSONArray arr) {
        List<Transaccion> list = new ArrayList<>();
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
