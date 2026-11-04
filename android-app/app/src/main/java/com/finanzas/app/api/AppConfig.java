package com.finanzas.app.api;

/**
 * Configuracion centralizada y unica de conexion con el backend VPS.
 * Cumple estrictamente API_CONTRACT.md Seccion 1 y Seccion 9.
 */
public final class AppConfig {
    public static final String SERVER_HOST = "51.161.47.99";
    public static final int SERVER_PORT = 54892;
    public static final String BASE_URL = "http://51.161.47.99:54892/api/v1";
    public static final String HEADER_TOKEN_NAME = "X-App-Private-Token";
    public static final String PRIVATE_TOKEN = "SZ6ydSFNmkAbrFLLrRahSEVxGnLkvABw";

    public static final int TIMEOUT_DEFAULT_MS = 15000;
    public static final int TIMEOUT_AI_MS = 90000;

    public static final String APP_VERSION = "1.0.0";
    public static final String DEFAULT_CURRENCY = "USD";

    private AppConfig() {
    }
}
