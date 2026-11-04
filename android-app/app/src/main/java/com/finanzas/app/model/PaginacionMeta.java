package com.finanzas.app.model;

import org.json.JSONObject;

public class PaginacionMeta {
    public int pagina;
    public int porPagina;
    public int totalRegistros;
    public int totalPaginas;

    public static PaginacionMeta fromJson(JSONObject obj) {
        if (obj == null) {
            return null;
        }
        PaginacionMeta m = new PaginacionMeta();
        m.pagina = obj.optInt("pagina", 1);
        m.porPagina = obj.optInt("por_pagina", 50);
        m.totalRegistros = obj.optInt("total_registros", 0);
        m.totalPaginas = obj.optInt("total_paginas", 1);
        return m;
    }
}
