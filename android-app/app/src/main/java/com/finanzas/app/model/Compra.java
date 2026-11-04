package com.finanzas.app.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Compra extends Transaccion {
    public List<ArticuloCompra> articulos = new ArrayList<>();

    public static Compra fromCompraJson(JSONObject obj) {
        if (obj == null) {
            return null;
        }
        Compra c = new Compra();
        c.id = obj.optInt("id", 0);
        c.tipo = obj.optString("tipo", "GASTO");
        c.naturaleza = obj.optString("naturaleza", "VARIABLE");
        c.montoCentavos = obj.optLong("monto_centavos", 0L);
        c.fecha = obj.optString("fecha", "");
        c.descripcion = obj.optString("descripcion", "");
        c.categoriaId = obj.optInt("categoria_id", 0);
        JSONObject catObj = obj.optJSONObject("categoria");
        if (catObj != null) {
            c.categoria = Categoria.fromJson(catObj);
        }
        c.metodoPago = obj.optString("metodo_pago", "EFECTIVO");
        c.comercio = obj.isNull("comercio") ? null : obj.optString("comercio", null);
        c.notas = obj.isNull("notas") ? null : obj.optString("notas", null);
        c.esCompra = obj.optBoolean("es_compra", true);
        c.cantidadArticulos = obj.optInt("cantidad_articulos", 0);
        c.creadoEn = obj.isNull("creado_en") ? null : obj.optString("creado_en", null);
        c.actualizadoEn = obj.isNull("actualizado_en") ? null : obj.optString("actualizado_en", null);
        JSONArray artArr = obj.optJSONArray("articulos");
        c.articulos = ArticuloCompra.fromJsonArray(artArr);
        return c;
    }
}
