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
        t.comercio = obj.isNull("comercio") ? null : obj.optString("comercio", null);
        t.notas = obj.isNull("notas") ? null : obj.optString("notas", null);
        t.esCompra = obj.optBoolean("es_compra", false);
        t.cantidadArticulos = obj.optInt("cantidad_articulos", 0);
        t.creadoEn = obj.isNull("creado_en") ? null : obj.optString("creado_en", null);
        t.actualizadoEn = obj.isNull("actualizado_en") ? null : obj.optString("actualizado_en", null);
        return t;
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
