package com.finanzas.app.api;

import android.os.Handler;
import android.os.Looper;

import com.finanzas.app.model.AnalisisIA;
import com.finanzas.app.model.ArticuloCompra;
import com.finanzas.app.model.Categoria;
import com.finanzas.app.model.Compra;
import com.finanzas.app.model.DashboardData;
import com.finanzas.app.model.HealthInfo;
import com.finanzas.app.model.PaginacionMeta;
import com.finanzas.app.model.Presupuesto;
import com.finanzas.app.model.PresupuestosResumen;
import com.finanzas.app.model.Transaccion;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Cliente HTTP nativo para consumir todos los endpoints de API_CONTRACT.md.
 * Inyecta X-App-Private-Token y aplica timeouts estrictos (15s general, 90s IA).
 */
public class ApiClient {

    private static ApiClient instance;
    private final ExecutorService executor;
    private final Handler mainHandler;

    private interface DataParser<T> {
        T parse(Object rawData) throws Exception;
    }

    private ApiClient() {
        this.executor = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }

    private <T> void executeRequest(
            final String method,
            final String path,
            final JSONObject requestBody,
            final int timeoutMs,
            final DataParser<T> parser,
            final ApiCallback<T> callback) {

        executor.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(AppConfig.BASE_URL + path);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod(method);
                    conn.setConnectTimeout(timeoutMs);
                    conn.setReadTimeout(timeoutMs);
                    conn.setUseCaches(false);
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    conn.setRequestProperty(AppConfig.HEADER_TOKEN_NAME, AppConfig.PRIVATE_TOKEN);

                    if (requestBody != null && ("POST".equals(method) || "PUT".equals(method))) {
                        conn.setDoOutput(true);
                        byte[] payload = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                        conn.setFixedLengthStreamingMode(payload.length);
                        try (OutputStream os = conn.getOutputStream()) {
                            os.write(payload);
                            os.flush();
                        }
                    }

                    final int status = conn.getResponseCode();
                    InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
                    String responseText = readStream(is);

                    if (status >= 200 && status < 300) {
                        JSONObject root = new JSONObject(responseText);
                        boolean ok = root.optBoolean("ok", false);
                        if (!ok) {
                            final ApiError err = ApiError.fromJson(status, root);
                            postError(callback, err);
                            return;
                        }
                        Object rawData = root.opt("data");
                        JSONObject rawMeta = root.optJSONObject("meta");
                        final T parsedData = parser.parse(rawData);
                        final PaginacionMeta meta = PaginacionMeta.fromJson(rawMeta);
                        postSuccess(callback, parsedData, meta);
                    } else {
                        JSONObject errRoot = null;
                        if (responseText != null && !responseText.trim().isEmpty()) {
                            try {
                                errRoot = new JSONObject(responseText);
                            } catch (Exception ignored) {
                            }
                        }
                        final ApiError err = ApiError.fromJson(status, errRoot);
                        postError(callback, err);
                    }
                } catch (Exception e) {
                    postError(callback, ApiError.networkError());
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }
        });
    }

    private String readStream(InputStream is) throws Exception {
        if (is == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private <T> void postSuccess(final ApiCallback<T> callback, final T data, final PaginacionMeta meta) {
        if (callback == null) return;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(data, meta);
            }
        });
    }

    private <T> void postError(final ApiCallback<T> callback, final ApiError error) {
        if (callback == null) return;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(error);
            }
        });
    }

    private String buildQueryString(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            if (key != null && val != null && !val.trim().isEmpty()) {
                if (sb.length() == 0) {
                    sb.append("?");
                } else {
                    sb.append("&");
                }
                try {
                    sb.append(URLEncoder.encode(key, "UTF-8"))
                            .append("=")
                            .append(URLEncoder.encode(val.trim(), "UTF-8"));
                } catch (Exception ignored) {
                }
            }
        }
        return sb.toString();
    }

    // 7.1 GET /health
    public void getHealth(ApiCallback<HealthInfo> callback) {
        executeRequest("GET", "/health", null, AppConfig.TIMEOUT_DEFAULT_MS, new DataParser<HealthInfo>() {
            @Override
            public HealthInfo parse(Object rawData) {
                return HealthInfo.fromJson((JSONObject) rawData);
            }
        }, callback);
    }

    // 7.2.1 GET /categorias
    public void getCategorias(String tipo, boolean incluirInactivas, ApiCallback<List<Categoria>> callback) {
        StringBuilder path = new StringBuilder("/categorias?incluir_inactivas=").append(incluirInactivas ? "true" : "false");
        if (tipo != null && !tipo.trim().isEmpty()) {
            path.append("&tipo=").append(tipo.trim());
        }
        executeRequest("GET", path.toString(), null, AppConfig.TIMEOUT_DEFAULT_MS, new DataParser<List<Categoria>>() {
            @Override
            public List<Categoria> parse(Object rawData) {
                return Categoria.fromJsonArray((JSONArray) rawData);
            }
        }, callback);
    }

    // 7.2.2 POST /categorias
    public void crearCategoria(String nombre, String tipo, String icono, String color, ApiCallback<Categoria> callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("nombre", nombre != null ? nombre.trim() : "");
            body.put("tipo", tipo);
            body.put("icono", icono);
            body.put("color", (color != null && !color.isEmpty()) ? color : "#4A5568");
            executeRequest("POST", "/categorias", body, AppConfig.TIMEOUT_DEFAULT_MS, new DataParser<Categoria>() {
                @Override
                public Categoria parse(Object rawData) {
                    return Categoria.fromJson((JSONObject) rawData);
                }
            }, callback);
        } catch (Exception e) {
            postError(callback, new ApiError(400, "JSON_INVALIDO", "Datos invalidos.", false));
        }
    }

    // 7.2.3 PUT /categorias/{id}
    public void actualizarCategoria(int id, String nombre, String icono, String color, boolean activa, ApiCallback<Categoria> callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("nombre", nombre != null ? nombre.trim() : "");
            body.put("icono", icono);
            body.put("color", (color != null && !color.isEmpty()) ? color : "#4A5568");
            body.put("activa", activa);
            executeRequest("PUT", "/categorias/" + id, body, AppConfig.TIMEOUT_DEFAULT_MS, new DataParser<Categoria>() {
                @Override
                public Categoria parse(Object rawData) {
                    return Categoria.fromJson((JSONObject) rawData);
                }
            }, callback);
        } catch (Exception e) {
            postError(callback, new ApiError(400, "JSON_INVALIDO", "Datos invalidos.", false));
        }
    }

    // 7.2.4 DELETE /categorias/{id}
    public void eliminarCategoria(int id, ApiCallback<Boolean> callback) {
        executeRequest("DELETE", "/categorias/" + id, null, AppConfig.TIMEOUT_DEFAULT_MS, new DataParser<Boolean>() {
            @Override
            public Boolean parse(Object rawData) {
                if (rawData instanceof JSONObject) {
                    return ((JSONObject) rawData).optBoolean("eliminado", true);
                }
                return true;
            }
        }, callback);
    }

    // 7.3.1 GET /transacciones
    public void getTransacciones(Map<String, String> filtros, ApiCallback<List<Transaccion>> callback) {
        String path = "/transacciones" + buildQueryString(filtros);
        executeRequest("GET", path, null, AppConfig.TIMEOUT_DEFAULT_MS, new DataParser<List<Transaccion>>() {
            @Override
            public List<Transaccion> parse(Object rawData) {
                return Transaccion.fromJsonArray((JSONArray) rawData);
            }
        }, callback);
    }

    // 7.3.2 GET /transacciones/{id}
    public void getTransaccionDetalle(int id, ApiCallback<Transaccion> callback) {
        executeRequest("GET", "/transacciones/" + id, null, AppConfig.TIMEOUT_DEFAULT_MS, new DataParser<Transaccion>() {
            @Override
            public Transaccion parse(Object rawData) {
                return Transaccion.fromJson((JSONObject) rawData);
            }
        }, callback);
    }

    // 7.3.3 POST /transacciones
    public void crearTransaccion(
            String tipo,
            String naturaleza,
            long montoCentavos,
            String fecha,
            String descripcion,
            int categoriaId,
            String metodoPago,
            String comercio,
            String notas,
            ApiCallback<Transaccion> callback) {
        try {
            JSONObject body = buildTransaccionPayload(tipo, naturaleza, montoCentavos, fecha, descripcion, categoriaId, metodoPago, comercio, notas);
            executeRequest("POST", "/transacciones", body, AppConfig.TIMEOUT_DEFAULT_MS, new DataParser<Transaccion>() {
                @Override
                public Transaccion parse(Object rawData) {
                    return Transaccion.fromJson((JSONObject) rawData);
                }
            }, callback);
        } catch (Exception e) {
            postError(callback, new ApiError(400, "JSON_INVALIDO", "Datos invalidos.", false));
        }
    }

    // 7.3.4 PUT /transacciones/{id}
    public void actualizarTransaccion(
            int id,
            String tipo,
            String naturaleza,
            long montoCentavos,
            String fecha,
            String descripcion,
            int categoriaId,
            String metodoPago,
            String comercio,
            String notas,
            ApiCallback<Transaccion> callback) {
        try {
            JSONObject body = buildTransaccionPayload(tipo, naturaleza, montoCentavos, fecha, descripcion, categoriaId, metodoPago, comercio, notas);
            executeRequest("PUT", "/transacciones/" + id, body, AppConfig.TIMEOUT_DEFAULT_MS, new DataParser<Transaccion>() {
                @Override
                public Transaccion parse(Object rawData) {
                    return Transaccion.fromJson((JSONObject) rawData);
                }
            }, callback);
        } catch (Exception e) {
            postError(callback, new ApiError(400, "JSON_INVALIDO", "Datos invalidos.", false));
        }
    }

    private JSONObject buildTransaccionPayload(
            String tipo,
            String naturaleza,
            long montoCentavos,
            String fecha,
            String descripcion,
            int categoriaId,
            String metodoPago,
            String comercio,
            String notas) throws Exception {
        JSONObject body = new JSONObject();
        body.put("tipo", tipo);
        body.put("naturaleza", naturaleza);
        body.put("monto_centavos", montoCentavos);
        body.put("fecha", fecha != null ? fecha.trim() : "");
        body.put("descripcion", descripcion != null ? descripcion.trim() : "");
        body.put("categoria_id", categoriaId);
        body.put("metodo_pago", metodoPago);
        if (comercio == null || comercio.trim().isEmpty()) {
            body.put("comercio", JSONObject.NULL);
        } else {
            body.put("comercio", comercio.trim());
        }
        if (notas == null || notas.trim().isEmpty()) {
            body.put("notas", JSONObject.NULL);
        } else {
            body.put("notas", notas.trim());
        }
        return body;
    }

    // 7.3.5 DELETE /transacciones/{id}
    public void eliminarTransaccion(int id, ApiCallback<Boolean> callback) {
        executeRequest("DELETE", "/transacciones/" + id, null, AppConfig.TIMEOUT_DEFAULT_MS, new DataParser<Boolean>() {
            @Override
            public Boolean parse(Object rawData) {
                if (rawData instanceof JSONObject) {
                    return ((JSONObject) rawData).optBoolean("eliminado", true);
                }
                return true;
            }
        }, callback);
    }

    // 7.4.1 GET /compras
    public void getCompras(Map<String, String> filtros, ApiCallback<List<Transaccion>> callback) {
        String path = "/compras" + buildQueryString(filtros);
        executeRequest("GET", path, null, AppConfig.TIMEOUT_DEFAULT_MS, new DataParser<List<Transaccion>>() {
            @Override
            public List<Transaccion> parse(Object rawData) {
                return Transaccion.fromJsonArray((JSONArray) rawData);
            }
        }, callback);
    }

    // 7.4.2 GET /compras/{id}
    public void getCompraDetalle(int id, ApiCallback<Compra> callback) {
        executeRequest("GET", "/compras/" + id, null, AppConfig.TIMEOUT_DEFAULT_MS, new DataParser<Compra>() {
            @Override
            public Compra parse(Object rawData) {
                return Compra.fromCompraJson((JSONObject) rawData);
            }
        }, callback);
    }

    // 7.4.3 POST /compras
    public void crearCompra(
            String naturaleza,
            String fecha,
            String comercio,
            String descripcion,
            int categoriaId,
            String metodoPago,
            String notas,
            List<ArticuloCompra> articulos,
            ApiCallback<Compra> callback) {
        try {
            JSONObject body = buildCompraPayload(naturaleza, fecha, comercio, descripcion, categoriaId, metodoPago, notas, articulos);
            executeRequest("POST", "/compras", body, AppConfig.TIMEOUT_DEFAULT_MS, new DataParser<Compra>() {
                @Override
                public Compra parse(Object rawData) {
                    return Compra.fromCompraJson((JSONObject) rawData);
                }
            }, callback);
        } catch (Exception e) {
            postError(callback, new ApiError(400, "JSON_INVALIDO", "Datos invalidos.", false));
        }
    }

    // 7.4.4 PUT /compras/{id}
    public void actualizarCompra(
            int id,
            String naturaleza,
            String fecha,
            String comercio,
            String descripcion,
            int categoriaId,
            String metodoPago,
            String notas,
            List<ArticuloCompra> articulos,
            ApiCallback<Compra> callback) {
        try {
            JSONObject body = buildCompraPayload(naturaleza, fecha, comercio, descripcion, categoriaId, metodoPago, notas, articulos);
            executeRequest("PUT", "/compras/" + id, body, AppConfig.TIMEOUT_DEFAULT_MS, new DataParser<Compra>() {
                @Override
                public Compra parse(Object rawData) {
                    return Compra.fromCompraJson((JSONObject) rawData);
                }
            }, callback);
        } catch (Exception e) {
            postError(callback, new ApiError(400, "JSON_INVALIDO", "Datos invalidos.", false));
        }
    }

    private JSONObject buildCompraPayload(
            String naturaleza,
            String fecha,
            String comercio,
            String descripcion,
            int categoriaId,
            String metodoPago,
            String notas,
            List<ArticuloCompra> articulos) throws Exception {
        JSONObject body = new JSONObject();
        body.put("naturaleza", (naturaleza != null && !naturaleza.isEmpty()) ? naturaleza : "VARIABLE");
        body.put("fecha", fecha != null ? fecha.trim() : "");
        body.put("comercio", comercio != null ? comercio.trim() : "");
        if (descripcion == null || descripcion.trim().isEmpty()) {
            body.put("descripcion", JSONObject.NULL);
        } else {
            body.put("descripcion", descripcion.trim());
        }
        body.put("categoria_id", categoriaId);
        body.put("metodo_pago", metodoPago);
        if (notas == null || notas.trim().isEmpty()) {
            body.put("notas", JSONObject.NULL);
        } else {
            body.put("notas", notas.trim());
        }

        JSONArray arr = new JSONArray();
        if (articulos != null) {
            for (ArticuloCompra a : articulos) {
                JSONObject item = new JSONObject();
                item.put("articulo", a.articulo != null ? a.articulo.trim() : "");
                if (a.cantidad == (long) a.cantidad) {
                    item.put("cantidad", (long) a.cantidad);
                } else {
                    item.put("cantidad", a.cantidad);
                }
                item.put("unidad", (a.unidad != null && !a.unidad.isEmpty()) ? a.unidad : "UNIDAD");
                item.put("precio_unitario_centavos", a.precioUnitarioCentavos);
                arr.put(item);
            }
        }
        body.put("articulos", arr);
        return body;
    }

    // 7.4.5 DELETE /compras/{id}
    public void eliminarCompra(int id, ApiCallback<Boolean> callback) {
        executeRequest("DELETE", "/compras/" + id, null, AppConfig.TIMEOUT_DEFAULT_MS, new DataParser<Boolean>() {
            @Override
            public Boolean parse(Object rawData) {
                if (rawData instanceof JSONObject) {
                    return ((JSONObject) rawData).optBoolean("eliminado", true);
                }
                return true;
            }
        }, callback);
    }

    // 7.5.1 GET /presupuestos
    public void getPresupuestos(int anio, int mes, String estado, ApiCallback<PresupuestosResumen> callback) {
        StringBuilder path = new StringBuilder("/presupuestos?anio=").append(anio).append("&mes=").append(mes);
        if (estado != null && !estado.trim().isEmpty()) {
            path.append("&estado=").append(estado.trim());
        }
        executeRequest("GET", path.toString(), null, AppConfig.TIMEOUT_DEFAULT_MS, new DataParser<PresupuestosResumen>() {
            @Override
            public PresupuestosResumen parse(Object rawData) {
                return PresupuestosResumen.fromJson((JSONObject) rawData);
            }
        }, callback);
    }

    // 7.5.2 GET /presupuestos/{id}
    public void getPresupuestoDetalle(int id, ApiCallback<Presupuesto> callback) {
        executeRequest("GET", "/presupuestos/" + id, null, AppConfig.TIMEOUT_DEFAULT_MS, new DataParser<Presupuesto>() {
            @Override
            public Presupuesto parse(Object rawData) {
                return Presupuesto.fromJson((JSONObject) rawData);
            }
        }, callback);
    }

    // 7.5.3 POST /presupuestos
    public void crearPresupuesto(int categoriaId, int anio, int mes, long montoLimiteCentavos, String notas, ApiCallback<Presupuesto> callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("categoria_id", categoriaId);
            body.put("anio", anio);
            body.put("mes", mes);
            body.put("monto_limite_centavos", montoLimiteCentavos);
            if (notas == null || notas.trim().isEmpty()) {
                body.put("notas", JSONObject.NULL);
            } else {
                body.put("notas", notas.trim());
            }
            executeRequest("POST", "/presupuestos", body, AppConfig.TIMEOUT_DEFAULT_MS, new DataParser<Presupuesto>() {
                @Override
                public Presupuesto parse(Object rawData) {
                    return Presupuesto.fromJson((JSONObject) rawData);
                }
            }, callback);
        } catch (Exception e) {
            postError(callback, new ApiError(400, "JSON_INVALIDO", "Datos invalidos.", false));
        }
    }

    // 7.5.4 PUT /presupuestos/{id}
    public void actualizarPresupuesto(int id, long montoLimiteCentavos, String notas, ApiCallback<Presupuesto> callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("monto_limite_centavos", montoLimiteCentavos);
            if (notas == null || notas.trim().isEmpty()) {
                body.put("notas", JSONObject.NULL);
            } else {
                body.put("notas", notas.trim());
            }
            executeRequest("PUT", "/presupuestos/" + id, body, AppConfig.TIMEOUT_DEFAULT_MS, new DataParser<Presupuesto>() {
                @Override
                public Presupuesto parse(Object rawData) {
                    return Presupuesto.fromJson((JSONObject) rawData);
                }
            }, callback);
        } catch (Exception e) {
            postError(callback, new ApiError(400, "JSON_INVALIDO", "Datos invalidos.", false));
        }
    }

    // 7.5.5 DELETE /presupuestos/{id}
    public void eliminarPresupuesto(int id, ApiCallback<Boolean> callback) {
        executeRequest("DELETE", "/presupuestos/" + id, null, AppConfig.TIMEOUT_DEFAULT_MS, new DataParser<Boolean>() {
            @Override
            public Boolean parse(Object rawData) {
                if (rawData instanceof JSONObject) {
                    return ((JSONObject) rawData).optBoolean("eliminado", true);
                }
                return true;
            }
        }, callback);
    }

    // 7.6 GET /dashboard
    public void getDashboard(int anio, int mes, ApiCallback<DashboardData> callback) {
        String path = "/dashboard?anio=" + anio + "&mes=" + mes;
        executeRequest("GET", path, null, AppConfig.TIMEOUT_DEFAULT_MS, new DataParser<DashboardData>() {
            @Override
            public DashboardData parse(Object rawData) {
                return DashboardData.fromJson((JSONObject) rawData);
            }
        }, callback);
    }

    // 8 POST /ai/analyze (Timeout 90s segun API_CONTRACT.md Seccion 9)
    public void analizarConIa(String tipoAnalisis, int anio, int mes, String pregunta, ApiCallback<AnalisisIA> callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("tipo_analisis", tipoAnalisis);
            body.put("anio", anio);
            body.put("mes", mes);
            if ("PREGUNTA_LIBRE".equals(tipoAnalisis) && pregunta != null && !pregunta.trim().isEmpty()) {
                body.put("pregunta", pregunta.trim());
            } else {
                body.put("pregunta", JSONObject.NULL);
            }
            executeRequest("POST", "/ai/analyze", body, AppConfig.TIMEOUT_AI_MS, new DataParser<AnalisisIA>() {
                @Override
                public AnalisisIA parse(Object rawData) {
                    return AnalisisIA.fromJson((JSONObject) rawData);
                }
            }, callback);
        } catch (Exception e) {
            postError(callback, new ApiError(400, "JSON_INVALIDO", "Datos invalidos.", false));
        }
    }
}
