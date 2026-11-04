# Cliente de Escritorio PC (`gastos-pc-app`)

Aplicacion de escritorio ultraligera en Node.js para el Sistema de Gestion Contable y Presupuestaria. Cumple al 100 por ciento las especificaciones de `API_CONTRACT.md` (v1.0.0) y `UI_DESIGN.md` (v1.0.0).

## Caracteristicas Principales

- **Huella en disco minima (< 2 MB totales, 0 MB en `node_modules`):** Sin dependencias pesadas ni paquetes con compilacion nativa (`node-gyp`).
- **Modo Ventana Nativa (`--app`):** Inicia un servidor local en `127.0.0.1` y abre automaticamente una ventana nativa de escritorio utilizando el motor Microsoft Edge o Google Chrome instalado en el sistema operativo.
- **Conectividad y Seguridad:** Configurado por defecto hacia `http://51.161.47.99:54892/api/v1` enviando el header `X-App-Private-Token: SZ6ydSFNmkAbrFLLrRahSEVxGnLkvABw`.
- **Cero Emojis:** Iconografia exclusivamente basada en FontAwesome Free 6.5.2 (incluida localmente en `public/css/all.min.css` + `public/webfonts/` y respaldada por CDN).

## Estructura del Proyecto

- `config/app-config.json`: Archivo unico de configuracion persistente.
- `src/config.js`: Gestor centralizado de configuracion y validacion de parametros.
- `src/server.js`: Servidor HTTP local de archivos estaticos, persistencia de configuracion (`/local-config`) y proxy opcional (`/api-proxy`).
- `src/launcher.js`: Detector de binarios de Microsoft Edge / Google Chrome y lanzador en modo `--app`.
- `src/main.js`: Punto de entrada principal de la aplicacion de escritorio.
- `public/index.html`: Interfaz contable de 7 vistas completas.
- `public/css/styles.css`: Sistema de diseno bancario estricto.
- `public/js/`: Logica modular del cliente (`config.js`, `utils.js`, `api.js`, `app.js`).
- `tests/verify.js`: Suite automatizada de pruebas de conformidad con el contrato.

## Ejecucion

```bash
cd pc-app
npm start
```

Para iniciar solo el servidor local sin abrir ventana automatica:

```bash
npm run serve
```

Para ejecutar la suite de verificacion de contrato y cero emojis:

```bash
npm test
```
