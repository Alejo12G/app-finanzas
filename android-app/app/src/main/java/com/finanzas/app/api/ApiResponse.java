package com.finanzas.app.api;

import com.finanzas.app.model.PaginacionMeta;

public class ApiResponse<T> {
    public final boolean ok;
    public final T data;
    public final PaginacionMeta meta;
    public final ApiError error;

    public ApiResponse(boolean ok, T data, PaginacionMeta meta, ApiError error) {
        this.ok = ok;
        this.data = data;
        this.meta = meta;
        this.error = error;
    }
}
