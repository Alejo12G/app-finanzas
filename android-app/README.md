# Cliente Android Nativo - Finanzas Personales

Aplicacion Android nativa desarrollada en **Java puro** (cero Kotlin, cero librerias pesadas externas) y layouts XML clasicos, siguiendo estrictamente `API_CONTRACT.md` v1.0.0 y `UI_DESIGN.md` v1.0.0.

## Caracteristicas Principales

1. **Conformidad total con `API_CONTRACT.md`:**
   - Conexion a `http://51.161.47.99:54892/api/v1` mediante `network_security_config.xml` restringido exclusivamente al host `51.161.47.99`.
   - Inyeccion automatica del header `X-App-Private-Token` desde una unica clase centralizada (`AppConfig.java`).
   - Manejo monetario exclusivamente en enteros largos de centavos (`long _centavos`) sin uso de `float` ni `double` para dinero.
   - Timeouts HTTP de 15 segundos para operaciones generales y 90 segundos para `/ai/analyze`.
   - Validacion anti-emojis en el cliente antes de enviar peticiones y mapeo de errores de validacion por campo (`error.detalles`).

2. **Conformidad total con `UI_DESIGN.md`:**
   - Cero emojis en codigo, recursos y pantallas.
   - Fuentes oficiales **FontAwesome Free 6.5.2** (`fa-solid-900.ttf`, `fa-regular-400.ttf`, `fa-brands-400.ttf`) integradas en `app/src/main/assets/fonts/` y gestionadas por el componente unico `FaIcon.java`.
   - Paleta bancaria clasica (`#1B2A4A` primario, `#4A5568` secundario, `#2E7D32` exito, `#C62828` peligro, `#F4F6F8` fondo) y cifras con numeros tabulares (`tnum`).
   - Las 7 pantallas completas:
     1. **Panel (Dashboard):** Selector de mes, 4 KPIs, grafico nativo en Canvas de gastos por categoria, grafico nativo en Canvas de evolucion de 6 meses, estado de presupuestos y ultimos 5 movimientos.
     2. **Transacciones:** Listado paginado con filtros avanzados, alta, edicion, eliminacion con confirmacion y acceso directo a creacion rapida con IA.
     3. **Compras con desglose:** Listado paginado filtrable por comercio o articulo, vista de ticket detallado y formulario dinamico de articulos con calculo en vivo de subtotales (`redondeo_mitad_hacia_arriba`) y total.
     4. **Presupuestos:** Barras de progreso por categoria (limitadas visualmente al 100 por ciento con porcentaje real en texto), colores e iconos por estado (`NORMAL`, `ADVERTENCIA`, `EXCEDIDO`).
     5. **Categorias:** Agrupadas por `INGRESO` y `GASTO`, selector visual de iconos FontAwesome oficiales y flujo de desactivacion ante `409 CATEGORIA_EN_USO`.
     6. **Asesor IA (Gemini):** Creacion rapida de gasto en lenguaje natural por texto con IA + analisis financiero completo (`RESUMEN_MENSUAL`, `AHORRO`, `PRESUPUESTO`, `PREGUNTA_LIBRE`) con puntuacion 0-100, alertas y recomendaciones priorizadas.
     7. **Configuracion:** Verificacion en vivo de `GET /health`, moneda activa, version de API y hora UTC del servidor.

## Compilacion en Equipos con Poco Espacio en Disco (< 10 GB)

### Opcion A: Compilacion Local Ligera
Ejecute el script optimizado con `--no-daemon` y `--no-build-cache`:
- En Windows:
  ```bat
  compilar-local.bat
  ```
- En Linux / macOS:
  ```bash
  ./compilar-local.sh
  ```
El archivo APK se generara en `app/build/outputs/apk/debug/app-debug.apk`.

### Opcion B: Compilacion en la Nube (0 MB de disco local) con GitHub Actions
El archivo `.github/workflows/build-apk.yml` permite compilar el APK gratuitamente en los servidores de GitHub Actions. Si el repositorio raiz es la carpeta superior del proyecto, copie `.github/workflows/build-apk.yml` a la raiz del repositorio (`/.github/workflows/build-apk.yml`) y ejecute el workflow desde la pestana **Actions** en GitHub para descargar el artefacto `FinanzasPersonales-debug-apk`.
