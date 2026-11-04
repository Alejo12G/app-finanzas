package com.finanzas.app.api;

import com.finanzas.app.model.PaginacionMeta;

public interface ApiCallback<T> {
    void onSuccess(T data, PaginacionMeta meta);

    void onError(ApiError error);
}
