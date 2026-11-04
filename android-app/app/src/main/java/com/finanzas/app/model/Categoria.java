package com.finanzas.app.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Categoria {
    public int id;
    public String nombre;
    public String tipo;
    public String icono;
    public String color;
    public boolean activa;
    public String creadoEn;
    public String actualizadoEn;

    public static Categoria fromJson(JSONObject obj) {
        if (obj == null) {
            return null;
        }
        Categoria c = new Categoria();
        c.id = obj.optInt("id", 0);
        c.nombre = obj.optString("nombre", "");
        c.tipo = obj.optString("tipo", "GASTO");
        c.icono = obj.optString("icono", "fa-solid fa-tag");
        c.color = obj.optString("color", "#4A5568");
        c.activa = obj.optBoolean("activa", true);
        c.creadoEn = obj.isNull("creado_en") ? null : obj.optString("creado_en", null);
        c.actualizadoEn = obj.isNull("actualizado_en") ? null : obj.optString("actualizado_en", null);
        return c;
    }

    public static List<Categoria> fromJsonArray(JSONArray arr) {
        List<Categoria> list = new ArrayList<>();
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

    @Override
    public String toString() {
        return nombre != null ? nombre : "";
    }
}
