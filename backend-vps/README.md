# Backend VPS - Sistema de Finanzas Personales y Compras

Servidor HTTP API REST ligero desarrollado en Node.js con persistencia SQLite nativa (`node:sqlite`) e integracion con Google Gemini IA.

## Especificaciones de Red y Seguridad

- **Host de escucha:** `0.0.0.0`
- **Puerto TCP:** `54892`
- **URL Base:** `http://51.161.47.99:54892/api/v1`
- **Autenticacion:** Header `X-App-Private-Token` validado en tiempo constante (`crypto.timingSafeEqual`).
- **Excepciones de token:** `GET /api/v1/health` y peticiones preflight `OPTIONS`.
- **Politica anti-emojis:** Validacion estricta de entrada (`422 TEXTO_CON_EMOJI`) y sanitizacion de salida en el modulo IA.
- **Sin compilacion nativa externa:** Utiliza exclusivamente modulos nativos de Node.js (`node:http`, `node:sqlite`, `node:crypto`).

## Estructura del Proyecto

```
/backend-vps
  .env                  # Variables de entorno locales / produccion (ignorado en Git)
  .env.example          # Plantilla de variables de entorno sin secretos
  deploy-vps.sh         # Script de apertura UFW (54892/tcp) y despliegue con PM2
  package.json          # Scripts de ejecucion y pruebas
  src/
    config.js           # Carga de configuracion y variables de entorno
    db.js               # Conexion SQLite, PRAGMAs (WAL, FK), DDL y 31 categorias semilla
    app.js              # Router HTTP, CORS, limite 256 KB, middleware de token
    server.js           # Punto de entrada HTTP en 0.0.0.0:54892
    utils/
      response.js       # Sobre estandarizado ok/data/meta y ok/error
      validators.js     # Validadores de tipos, fechas, redondeo mitad hacia arriba y anti-emojis
    routes/
      health.js         # GET /api/v1/health
      categorias.js     # CRUD /api/v1/categorias
      transacciones.js  # CRUD /api/v1/transacciones
      compras.js        # CRUD transaccional /api/v1/compras con desglose de articulos
      presupuestos.js   # CRUD y calculo de estado /api/v1/presupuestos
      dashboard.js      # KPIs, comparativo mensual y evolucion de 6 meses /api/v1/dashboard
      ai.js             # Analisis financiero Gemini y parseo de compras en texto libre
  test/
    verify-contract.js  # Suite de verificacion integral del contrato API_CONTRACT.md
```

## Comandos

```bash
# Iniciar servidor en produccion
npm start

# Ejecutar suite de pruebas de conformidad del contrato
npm test

# Desplegar en el VPS (Ubuntu/Debian con UFW y PM2)
chmod +x deploy-vps.sh
./deploy-vps.sh
```
