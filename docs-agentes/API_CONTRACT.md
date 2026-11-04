# API_CONTRACT.md - Contrato Estricto de la API

**Version del contrato:** 1.0.0
**Estado:** CONGELADO. Ningun agente puede modificar nombres de campos, rutas, tipos o codigos sin aprobacion del Arquitecto y del usuario.
**Consumidores:** Agente Backend (`/backend-vps`), Agente PC (`/pc-app`), Agente Android (`/android-app`).

> Regla de oro: si algo no esta en este documento, NO existe. Ante cualquier duda, el agente debe registrar la pregunta en `STATUS.md` (seccion "Bloqueos y Preguntas") y NO inventar campos.

---

## 0. Indice

1. Conexion y seguridad
2. Convenciones globales
3. Sobre de respuesta y errores
4. Esquema SQLite (DDL exacto)
5. Datos semilla (categorias)
6. Objetos JSON canonicos
7. Endpoints
8. Modulo IA (Gemini)
9. Restricciones de entorno por agente
10. Checklist de conformidad

---

## 1. Conexion y seguridad

| Parametro | Valor exacto |
|---|---|
| URL base | `http://51.161.47.99:54892/api/v1` |
| Host | `51.161.47.99` |
| Puerto | `54892` (TCP, no estandar) |
| Protocolo | HTTP/1.1 (sin TLS en v1) |
| Header de autenticacion | `X-App-Private-Token` |
| Valor del token | `SZ6ydSFNmkAbrFLLrRahSEVxGnLkvABw` |
| Longitud del token | 32 caracteres alfanumericos |
| Content-Type peticiones | `application/json; charset=utf-8` |
| Content-Type respuestas | `application/json; charset=utf-8` |

### 1.1 Reglas de autenticacion

- TODOS los endpoints exigen el header `X-App-Private-Token`, EXCEPTO:
  - `GET /api/v1/health`
  - Peticiones `OPTIONS` (preflight CORS).
- Header ausente o valor distinto: HTTP `401` con codigo `TOKEN_INVALIDO`.
- El backend compara el token en **tiempo constante** (p. ej. `hmac.compare_digest` / `crypto.timingSafeEqual`).
- El backend lee el token desde la variable de entorno `APP_PRIVATE_TOKEN`. Su valor en produccion es el indicado arriba.
- Los clientes (PC y Android) guardan el token en un archivo/constante de configuracion unico (`config`), nunca disperso por el codigo.
- El backend NUNCA devuelve el token en ninguna respuesta ni lo escribe en logs.

> [!CAUTION]
> Este repositorio contiene el token en texto plano. El repositorio de GitHub DEBE ser **privado**.

### 1.2 CORS (obligatorio para el cliente PC)

El backend responde a todas las rutas con:

```
Access-Control-Allow-Origin: *
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, X-App-Private-Token
Access-Control-Max-Age: 86400
```

`OPTIONS` responde `204` sin cuerpo y sin exigir token.

### 1.3 Android: trafico en texto plano

Como v1 usa HTTP sin TLS, la app Android DEBE declarar un `network_security_config.xml` que permita cleartext **solo** para `51.161.47.99`. Prohibido `usesCleartextTraffic="true"` global.

---

## 2. Convenciones globales

| Aspecto | Regla |
|---|---|
| Nombres de campos JSON | `snake_case`, en espanol, sin tildes ni `n` con tilde (usar `anio`, no `ano`). |
| Identificadores | `INTEGER` positivos autoincrementales. En JSON: numero entero. |
| Dinero | **SIEMPRE enteros en centavos** (unidad minima). Campos terminan en `_centavos`. Prohibido `float`/`double` para dinero en las 3 capas. Ejemplo: `$12.50` = `1250`. |
| Moneda | Unica en v1. Codigo ISO 4217 definido por la variable de entorno `APP_MONEDA` (por defecto `USD`). Expuesta en `GET /health`. |
| Fechas (dia) | Texto `YYYY-MM-DD` (ej. `2026-11-03`). |
| Marcas de tiempo | ISO 8601 UTC con sufijo `Z`, sin milisegundos: `2026-11-04T02:14:40Z`. |
| Booleanos | En JSON `true`/`false`. En SQLite `INTEGER` 0/1. |
| Enumeraciones | Texto en MAYUSCULAS, valores exactos listados en la seccion 2.1. |
| Nulos | Un campo opcional sin valor se envia como `null`, nunca se omite en respuestas. En peticiones, omitir un campo opcional equivale a `null`. |
| Codificacion | UTF-8. |
| Emojis | PROHIBIDOS. Ver seccion 2.2. |
| Orden por defecto en listados | `fecha DESC, id DESC`. |
| Paginacion | Query `pagina` (min 1, def 1) y `por_pagina` (min 1, def 50, max 200). |

### 2.1 Enumeraciones (valores exactos)

| Enum | Valores permitidos |
|---|---|
| `tipo` (transaccion / categoria) | `INGRESO`, `GASTO` |
| `naturaleza` | `FIJO`, `VARIABLE` |
| `metodo_pago` | `EFECTIVO`, `DEBITO`, `CREDITO`, `TRANSFERENCIA`, `OTRO` |
| `unidad` (articulo) | `UNIDAD`, `KG`, `G`, `L`, `ML`, `PAQUETE`, `OTRO` |
| `estado` (presupuesto) | `NORMAL`, `ADVERTENCIA`, `EXCEDIDO` |
| `tipo_analisis` (IA) | `RESUMEN_MENSUAL`, `AHORRO`, `PRESUPUESTO`, `PREGUNTA_LIBRE` |
| `prioridad` (IA) | `ALTA`, `MEDIA`, `BAJA` |

### 2.2 Politica anti-emojis (las 3 capas)

- Clientes: validan antes de enviar y muestran el error de campo.
- Backend: rechaza cualquier campo de texto que contenga caracteres en los rangos Unicode de emoji/pictogramas (`U+1F000-U+1FAFF`, `U+2600-U+27BF`, `U+FE0F`, `U+200D`, `U+1F1E6-U+1F1FF`) con HTTP `422`, codigo `TEXTO_CON_EMOJI`.
- Backend: elimina cualquier emoji de la salida de Gemini antes de responder.

### 2.3 Limites de campos de texto

| Campo | Min | Max | Obligatorio |
|---|---|---|---|
| `descripcion` | 1 | 200 | Si (transacciones), No (compras) |
| `notas` | 0 | 1000 | No |
| `comercio` | 1 | 120 | Si en compras, No en transacciones |
| `articulo` | 1 | 120 | Si |
| `nombre` (categoria) | 1 | 60 | Si |
| `pregunta` (IA) | 5 | 500 | Solo si `tipo_analisis = PREGUNTA_LIBRE` |

Todos los textos se recortan (`trim`) en el backend antes de validar.

### 2.4 Limites numericos

| Campo | Regla |
|---|---|
| `monto_centavos` | Entero, `1 <= x <= 99999999999` |
| `monto_limite_centavos` | Entero, `1 <= x <= 99999999999` |
| `precio_unitario_centavos` | Entero, `0 <= x <= 9999999999` |
| `cantidad` | Numero, `0 < x <= 99999`, maximo 3 decimales |
| `anio` | Entero, `2000 <= x <= 2100` |
| `mes` | Entero, `1 <= x <= 12` |
| `articulos` por compra | `1 <= n <= 200` |

---

## 3. Sobre de respuesta y errores

### 3.1 Respuesta exitosa (unico formato)

```json
{
  "ok": true,
  "data": {},
  "meta": null
}
```

- `data`: objeto o arreglo segun el endpoint.
- `meta`: `null`, salvo en listados paginados:

```json
{
  "pagina": 1,
  "por_pagina": 50,
  "total_registros": 134,
  "total_paginas": 3
}
```

### 3.2 Respuesta de error (unico formato)

```json
{
  "ok": false,
  "error": {
    "codigo": "VALIDACION_FALLIDA",
    "mensaje": "Uno o mas campos no son validos.",
    "detalles": [
      { "campo": "monto_centavos", "mensaje": "Debe ser un entero mayor que 0." }
    ]
  }
}
```

- `detalles`: arreglo (vacio `[]` si no aplica). Nunca `null`.
- `mensaje`: texto en espanol apto para mostrarse al usuario.

### 3.3 Catalogo de codigos HTTP y de error

| HTTP | `codigo` | Cuando |
|---|---|---|
| 200 | - | Lectura / actualizacion correcta |
| 201 | - | Creacion correcta |
| 204 | - | Solo preflight `OPTIONS` |
| 400 | `JSON_INVALIDO` | Cuerpo no es JSON parseable |
| 401 | `TOKEN_INVALIDO` | Header ausente o incorrecto |
| 404 | `RECURSO_NO_ENCONTRADO` | ID inexistente |
| 404 | `RUTA_NO_ENCONTRADA` | Ruta no definida |
| 405 | `METODO_NO_PERMITIDO` | Metodo no soportado en la ruta |
| 409 | `CATEGORIA_EN_USO` | Borrar categoria con transacciones o presupuestos |
| 409 | `PRESUPUESTO_DUPLICADO` | Ya existe presupuesto para `categoria_id`+`anio`+`mes` |
| 409 | `CATEGORIA_DUPLICADA` | Nombre de categoria repetido |
| 409 | `USAR_ENDPOINT_COMPRAS` | `PUT /transacciones/{id}` sobre una transaccion con `es_compra = true` |
| 413 | `CUERPO_DEMASIADO_GRANDE` | Cuerpo > 256 KB |
| 422 | `VALIDACION_FALLIDA` | Campos invalidos (ver `detalles`) |
| 422 | `TEXTO_CON_EMOJI` | Algun texto contiene emojis |
| 422 | `CATEGORIA_TIPO_INCOMPATIBLE` | `tipo` de transaccion distinto al `tipo` de la categoria |
| 422 | `CATEGORIA_INACTIVA` | Se intenta usar una categoria con `activa = false` |
| 429 | `LIMITE_IA_EXCEDIDO` | Mas de 10 peticiones IA en 60 minutos |
| 500 | `ERROR_INTERNO` | Error no controlado (sin trazas en la respuesta) |
| 502 | `ERROR_SERVICIO_IA` | Gemini fallo o devolvio formato invalido |
| 504 | `TIEMPO_IA_AGOTADO` | Gemini no respondio en 60 s |

---

## 4. Esquema SQLite (DDL exacto)

Archivo de base de datos en el VPS: ruta definida por `APP_DB_PATH` (por defecto `./data/finanzas.db`).
Al abrir cada conexion el backend ejecuta:

```sql
PRAGMA foreign_keys = ON;
PRAGMA journal_mode = WAL;
PRAGMA busy_timeout = 5000;
```

### 4.1 Tabla `categorias`

```sql
CREATE TABLE IF NOT EXISTS categorias (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre        TEXT    NOT NULL UNIQUE COLLATE NOCASE,
    tipo          TEXT    NOT NULL CHECK (tipo IN ('INGRESO','GASTO')),
    icono         TEXT    NOT NULL,              -- clase FontAwesome 6, ej. 'fa-solid fa-basket-shopping'
    color         TEXT    NOT NULL DEFAULT '#4A5568' CHECK (length(color) = 7 AND substr(color,1,1) = '#'),
    activa        INTEGER NOT NULL DEFAULT 1 CHECK (activa IN (0,1)),
    creado_en     TEXT    NOT NULL,              -- ISO 8601 UTC
    actualizado_en TEXT   NOT NULL               -- ISO 8601 UTC
);
```

### 4.2 Tabla `transacciones`

```sql
CREATE TABLE IF NOT EXISTS transacciones (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    tipo           TEXT    NOT NULL CHECK (tipo IN ('INGRESO','GASTO')),
    naturaleza     TEXT    NOT NULL CHECK (naturaleza IN ('FIJO','VARIABLE')),
    monto_centavos INTEGER NOT NULL CHECK (monto_centavos > 0),
    fecha          TEXT    NOT NULL,             -- YYYY-MM-DD
    descripcion    TEXT    NOT NULL,
    categoria_id   INTEGER NOT NULL REFERENCES categorias(id) ON DELETE RESTRICT,
    metodo_pago    TEXT    NOT NULL CHECK (metodo_pago IN ('EFECTIVO','DEBITO','CREDITO','TRANSFERENCIA','OTRO')),
    comercio       TEXT,                         -- NULL permitido
    notas          TEXT,                         -- NULL permitido
    es_compra      INTEGER NOT NULL DEFAULT 0 CHECK (es_compra IN (0,1)),
    creado_en      TEXT    NOT NULL,
    actualizado_en TEXT    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_transacciones_fecha      ON transacciones(fecha);
CREATE INDEX IF NOT EXISTS idx_transacciones_categoria  ON transacciones(categoria_id);
CREATE INDEX IF NOT EXISTS idx_transacciones_tipo_fecha ON transacciones(tipo, fecha);
```

### 4.3 Tabla `detalle_compras`

```sql
CREATE TABLE IF NOT EXISTS detalle_compras (
    id                       INTEGER PRIMARY KEY AUTOINCREMENT,
    transaccion_id           INTEGER NOT NULL REFERENCES transacciones(id) ON DELETE CASCADE,
    articulo                 TEXT    NOT NULL,
    cantidad                 REAL    NOT NULL CHECK (cantidad > 0),
    unidad                   TEXT    NOT NULL DEFAULT 'UNIDAD' CHECK (unidad IN ('UNIDAD','KG','G','L','ML','PAQUETE','OTRO')),
    precio_unitario_centavos INTEGER NOT NULL CHECK (precio_unitario_centavos >= 0),
    subtotal_centavos        INTEGER NOT NULL CHECK (subtotal_centavos >= 0),
    orden                    INTEGER NOT NULL DEFAULT 0,   -- posicion del articulo en el ticket (0..n-1)
    creado_en                TEXT    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_detalle_transaccion ON detalle_compras(transaccion_id);
CREATE INDEX IF NOT EXISTS idx_detalle_articulo    ON detalle_compras(articulo COLLATE NOCASE);
```

**Regla de calculo (solo el backend la aplica, los clientes la replican para mostrar vista previa):**

```
subtotal_centavos = redondeo_mitad_hacia_arriba(cantidad * precio_unitario_centavos)
monto_centavos (transaccion) = SUMA(subtotal_centavos de sus articulos)
```

Si la suma resultante es `0`, el backend responde `422 VALIDACION_FALLIDA` (campo `articulos`).

### 4.4 Tabla `presupuestos`

```sql
CREATE TABLE IF NOT EXISTS presupuestos (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    categoria_id          INTEGER NOT NULL REFERENCES categorias(id) ON DELETE RESTRICT,
    anio                  INTEGER NOT NULL CHECK (anio BETWEEN 2000 AND 2100),
    mes                   INTEGER NOT NULL CHECK (mes BETWEEN 1 AND 12),
    monto_limite_centavos INTEGER NOT NULL CHECK (monto_limite_centavos > 0),
    notas                 TEXT,
    creado_en             TEXT    NOT NULL,
    actualizado_en        TEXT    NOT NULL,
    UNIQUE (categoria_id, anio, mes)
);

CREATE INDEX IF NOT EXISTS idx_presupuestos_periodo ON presupuestos(anio, mes);
```

Solo se permiten presupuestos sobre categorias de `tipo = 'GASTO'` (si no: `422 CATEGORIA_TIPO_INCOMPATIBLE`).

### 4.5 Tabla de control de esquema

```sql
CREATE TABLE IF NOT EXISTS schema_version (
    version     INTEGER NOT NULL,
    aplicado_en TEXT    NOT NULL
);
```

Version inicial: `1`.

---

## 5. Datos semilla (categorias)

El backend inserta estas filas **una unica vez** al crear la base de datos, con estos IDs exactos. Los clientes PUEDEN usar estos IDs como referencia, pero SIEMPRE deben cargar la lista real desde `GET /categorias`.

| id | nombre | tipo | icono | color |
|---|---|---|---|---|
| 1 | Salario | INGRESO | `fa-solid fa-briefcase` | `#2E7D32` |
| 2 | Honorarios | INGRESO | `fa-solid fa-laptop-code` | `#2E7D32` |
| 3 | Inversiones | INGRESO | `fa-solid fa-chart-line` | `#2E7D32` |
| 4 | Ventas | INGRESO | `fa-solid fa-hand-holding-dollar` | `#2E7D32` |
| 5 | Reembolsos | INGRESO | `fa-solid fa-rotate-left` | `#2E7D32` |
| 6 | Otros ingresos | INGRESO | `fa-solid fa-coins` | `#2E7D32` |
| 7 | Vivienda | GASTO | `fa-solid fa-house-chimney` | `#1B2A4A` |
| 8 | Electricidad | GASTO | `fa-solid fa-bolt` | `#1B2A4A` |
| 9 | Agua | GASTO | `fa-solid fa-droplet` | `#1B2A4A` |
| 10 | Internet y telefonia | GASTO | `fa-solid fa-wifi` | `#1B2A4A` |
| 11 | Supermercado | GASTO | `fa-solid fa-basket-shopping` | `#4A5568` |
| 12 | Restaurantes | GASTO | `fa-solid fa-utensils` | `#4A5568` |
| 13 | Transporte | GASTO | `fa-solid fa-bus` | `#4A5568` |
| 14 | Combustible | GASTO | `fa-solid fa-gas-pump` | `#4A5568` |
| 15 | Vehiculo | GASTO | `fa-solid fa-car` | `#4A5568` |
| 16 | Salud | GASTO | `fa-solid fa-heart-pulse` | `#4A5568` |
| 17 | Farmacia | GASTO | `fa-solid fa-prescription-bottle-medical` | `#4A5568` |
| 18 | Educacion | GASTO | `fa-solid fa-graduation-cap` | `#4A5568` |
| 19 | Entretenimiento | GASTO | `fa-solid fa-film` | `#4A5568` |
| 20 | Suscripciones | GASTO | `fa-solid fa-repeat` | `#1B2A4A` |
| 21 | Ropa | GASTO | `fa-solid fa-shirt` | `#4A5568` |
| 22 | Hogar | GASTO | `fa-solid fa-couch` | `#4A5568` |
| 23 | Seguros | GASTO | `fa-solid fa-shield-halved` | `#1B2A4A` |
| 24 | Deudas y prestamos | GASTO | `fa-solid fa-credit-card` | `#C62828` |
| 25 | Impuestos | GASTO | `fa-solid fa-landmark` | `#1B2A4A` |
| 26 | Mascotas | GASTO | `fa-solid fa-paw` | `#4A5568` |
| 27 | Regalos | GASTO | `fa-solid fa-gift` | `#4A5568` |
| 28 | Viajes | GASTO | `fa-solid fa-plane` | `#4A5568` |
| 29 | Cuidado personal | GASTO | `fa-solid fa-spa` | `#4A5568` |
| 30 | Ahorro | GASTO | `fa-solid fa-piggy-bank` | `#2E7D32` |
| 31 | Otros gastos | GASTO | `fa-solid fa-ellipsis` | `#4A5568` |

---

## 6. Objetos JSON canonicos

Estos son los UNICOS formatos de objeto. Los nombres de llave son exactos y sensibles a mayusculas.

### 6.1 `CategoriaResumen` (embebida, solo lectura)

```json
{
  "id": 11,
  "nombre": "Supermercado",
  "tipo": "GASTO",
  "icono": "fa-solid fa-basket-shopping",
  "color": "#4A5568"
}
```

### 6.2 `Categoria`

```json
{
  "id": 11,
  "nombre": "Supermercado",
  "tipo": "GASTO",
  "icono": "fa-solid fa-basket-shopping",
  "color": "#4A5568",
  "activa": true,
  "creado_en": "2026-11-04T02:14:40Z",
  "actualizado_en": "2026-11-04T02:14:40Z"
}
```

### 6.3 `Transaccion`

```json
{
  "id": 1024,
  "tipo": "GASTO",
  "naturaleza": "VARIABLE",
  "monto_centavos": 4575,
  "fecha": "2026-11-03",
  "descripcion": "Compra semanal",
  "categoria_id": 11,
  "categoria": { "id": 11, "nombre": "Supermercado", "tipo": "GASTO", "icono": "fa-solid fa-basket-shopping", "color": "#4A5568" },
  "metodo_pago": "DEBITO",
  "comercio": "Supermaxi",
  "notas": null,
  "es_compra": true,
  "cantidad_articulos": 3,
  "creado_en": "2026-11-04T02:14:40Z",
  "actualizado_en": "2026-11-04T02:14:40Z"
}
```

- `cantidad_articulos`: entero calculado. `0` si `es_compra = false`.

### 6.4 `ArticuloCompra`

```json
{
  "id": 501,
  "articulo": "Leche entera 1L",
  "cantidad": 2,
  "unidad": "UNIDAD",
  "precio_unitario_centavos": 125,
  "subtotal_centavos": 250,
  "orden": 0
}
```

### 6.5 `Compra` (= `Transaccion` + `articulos`)

```json
{
  "id": 1024,
  "tipo": "GASTO",
  "naturaleza": "VARIABLE",
  "monto_centavos": 4575,
  "fecha": "2026-11-03",
  "descripcion": "Compra semanal",
  "categoria_id": 11,
  "categoria": { "id": 11, "nombre": "Supermercado", "tipo": "GASTO", "icono": "fa-solid fa-basket-shopping", "color": "#4A5568" },
  "metodo_pago": "DEBITO",
  "comercio": "Supermaxi",
  "notas": null,
  "es_compra": true,
  "cantidad_articulos": 3,
  "creado_en": "2026-11-04T02:14:40Z",
  "actualizado_en": "2026-11-04T02:14:40Z",
  "articulos": [
    { "id": 501, "articulo": "Leche entera 1L", "cantidad": 2,     "unidad": "UNIDAD", "precio_unitario_centavos": 125,  "subtotal_centavos": 250,  "orden": 0 },
    { "id": 502, "articulo": "Carne molida",    "cantidad": 1.5,   "unidad": "KG",     "precio_unitario_centavos": 850,  "subtotal_centavos": 1275, "orden": 1 },
    { "id": 503, "articulo": "Detergente",      "cantidad": 1,     "unidad": "PAQUETE","precio_unitario_centavos": 3050, "subtotal_centavos": 3050, "orden": 2 }
  ]
}
```

### 6.6 `Presupuesto`

```json
{
  "id": 77,
  "categoria_id": 11,
  "categoria": { "id": 11, "nombre": "Supermercado", "tipo": "GASTO", "icono": "fa-solid fa-basket-shopping", "color": "#4A5568" },
  "anio": 2026,
  "mes": 11,
  "monto_limite_centavos": 40000,
  "gastado_centavos": 32500,
  "disponible_centavos": 7500,
  "porcentaje_usado": 81.25,
  "estado": "ADVERTENCIA",
  "notas": null,
  "creado_en": "2026-11-01T12:00:00Z",
  "actualizado_en": "2026-11-01T12:00:00Z"
}
```

Campos calculados por el backend (nunca se envian en peticiones):

- `gastado_centavos` = suma de `monto_centavos` de transacciones `tipo = 'GASTO'` de esa categoria con `fecha` dentro de `anio`/`mes`.
- `disponible_centavos` = `monto_limite_centavos - gastado_centavos` (puede ser negativo).
- `porcentaje_usado` = `gastado / limite * 100`, numero con **2 decimales** (redondeo mitad hacia arriba).
- `estado`: `NORMAL` si `porcentaje_usado < 80`; `ADVERTENCIA` si `80 <= porcentaje_usado < 100`; `EXCEDIDO` si `porcentaje_usado >= 100`.

### 6.7 `Dashboard`

Ver endpoint 7.6.

### 6.8 `AnalisisIA`

Ver endpoint 8.

---

## 7. Endpoints

Todas las rutas son relativas a `http://51.161.47.99:54892/api/v1`.

### Resumen de rutas

| # | Metodo | Ruta | Token | Exito |
|---|---|---|---|---|
| 7.1 | GET | `/health` | No | 200 |
| 7.2.1 | GET | `/categorias` | Si | 200 |
| 7.2.2 | POST | `/categorias` | Si | 201 |
| 7.2.3 | PUT | `/categorias/{id}` | Si | 200 |
| 7.2.4 | DELETE | `/categorias/{id}` | Si | 200 |
| 7.3.1 | GET | `/transacciones` | Si | 200 |
| 7.3.2 | GET | `/transacciones/{id}` | Si | 200 |
| 7.3.3 | POST | `/transacciones` | Si | 201 |
| 7.3.4 | PUT | `/transacciones/{id}` | Si | 200 |
| 7.3.5 | DELETE | `/transacciones/{id}` | Si | 200 |
| 7.4.1 | GET | `/compras` | Si | 200 |
| 7.4.2 | GET | `/compras/{id}` | Si | 200 |
| 7.4.3 | POST | `/compras` | Si | 201 |
| 7.4.4 | PUT | `/compras/{id}` | Si | 200 |
| 7.4.5 | DELETE | `/compras/{id}` | Si | 200 |
| 7.5.1 | GET | `/presupuestos` | Si | 200 |
| 7.5.2 | GET | `/presupuestos/{id}` | Si | 200 |
| 7.5.3 | POST | `/presupuestos` | Si | 201 |
| 7.5.4 | PUT | `/presupuestos/{id}` | Si | 200 |
| 7.5.5 | DELETE | `/presupuestos/{id}` | Si | 200 |
| 7.6 | GET | `/dashboard` | Si | 200 |
| 8 | POST | `/ai/analyze` | Si | 200 |

`{id}` es un entero positivo. Si no es entero: `404 RECURSO_NO_ENCONTRADO`.

---

### 7.1 `GET /health`

Sin token. Para comprobar conectividad desde los clientes.

**Response 200**

```json
{
  "ok": true,
  "data": {
    "estado": "OK",
    "version_api": "1.0.0",
    "moneda": "USD",
    "hora_servidor": "2026-11-04T02:14:40Z"
  },
  "meta": null
}
```

---

### 7.2 Categorias

#### 7.2.1 `GET /categorias`

Query (opcionales):

| Param | Tipo | Valores |
|---|---|---|
| `tipo` | texto | `INGRESO` / `GASTO` |
| `incluir_inactivas` | texto | `true` / `false` (def `false`) |

Orden: `tipo ASC, nombre ASC`. Sin paginacion.

**Response 200:** `data` = arreglo de `Categoria` (6.2). `meta` = `null`.

#### 7.2.2 `POST /categorias`

**Request**

```json
{
  "nombre": "Gimnasio",
  "tipo": "GASTO",
  "icono": "fa-solid fa-dumbbell",
  "color": "#4A5568"
}
```

- `color` opcional (def `#4A5568`).
- `icono` debe empezar por `fa-solid fa-`, `fa-regular fa-` o `fa-brands fa-`.

**Response 201:** `data` = `Categoria`.

#### 7.2.3 `PUT /categorias/{id}`

**Request** (todos obligatorios, reemplazo completo; `tipo` NO es modificable):

```json
{
  "nombre": "Gimnasio y deporte",
  "icono": "fa-solid fa-dumbbell",
  "color": "#4A5568",
  "activa": true
}
```

**Response 200:** `data` = `Categoria`.

#### 7.2.4 `DELETE /categorias/{id}`

- Si tiene transacciones o presupuestos: `409 CATEGORIA_EN_USO` (el cliente debe sugerir desactivarla con PUT `activa: false`).

**Response 200**

```json
{ "ok": true, "data": { "id": 32, "eliminado": true }, "meta": null }
```

---

### 7.3 Transacciones

#### 7.3.1 `GET /transacciones`

Query (todos opcionales):

| Param | Tipo | Regla |
|---|---|---|
| `desde` | `YYYY-MM-DD` | `fecha >= desde` |
| `hasta` | `YYYY-MM-DD` | `fecha <= hasta` |
| `tipo` | enum | `INGRESO` / `GASTO` |
| `naturaleza` | enum | `FIJO` / `VARIABLE` |
| `categoria_id` | entero | |
| `metodo_pago` | enum | |
| `es_compra` | `true` / `false` | |
| `buscar` | texto (max 100) | Coincidencia parcial sin distinguir mayusculas en `descripcion`, `comercio`, `notas` |
| `pagina` | entero | def 1 |
| `por_pagina` | entero | def 50, max 200 |

**Response 200**

```json
{
  "ok": true,
  "data": [ { "...": "Transaccion (6.3)" } ],
  "meta": { "pagina": 1, "por_pagina": 50, "total_registros": 134, "total_paginas": 3 }
}
```

#### 7.3.2 `GET /transacciones/{id}`

**Response 200:** `data` = `Transaccion` (sin `articulos`; para ellos usar `GET /compras/{id}`).

#### 7.3.3 `POST /transacciones`

Para ingresos y gastos SIN desglose de articulos. Crea siempre `es_compra = false`.

**Request**

```json
{
  "tipo": "GASTO",
  "naturaleza": "FIJO",
  "monto_centavos": 65000,
  "fecha": "2026-11-01",
  "descripcion": "Alquiler noviembre",
  "categoria_id": 7,
  "metodo_pago": "TRANSFERENCIA",
  "comercio": null,
  "notas": "Pago al propietario"
}
```

| Campo | Obligatorio |
|---|---|
| `tipo`, `naturaleza`, `monto_centavos`, `fecha`, `descripcion`, `categoria_id`, `metodo_pago` | Si |
| `comercio`, `notas` | No |

Validaciones extra: la categoria debe existir (`422 VALIDACION_FALLIDA`, campo `categoria_id`), estar activa (`422 CATEGORIA_INACTIVA`) y coincidir en `tipo` (`422 CATEGORIA_TIPO_INCOMPATIBLE`).

**Response 201:** `data` = `Transaccion`.

#### 7.3.4 `PUT /transacciones/{id}`

Reemplazo completo. Mismo cuerpo y validaciones que 7.3.3.
Si la transaccion tiene `es_compra = true`: `409 USAR_ENDPOINT_COMPRAS`.

**Response 200:** `data` = `Transaccion`.

#### 7.3.5 `DELETE /transacciones/{id}`

Elimina tambien sus `detalle_compras` (CASCADE). Valido para compras.

**Response 200**

```json
{ "ok": true, "data": { "id": 1024, "eliminado": true }, "meta": null }
```

---

### 7.4 Compras (transaccion de gasto con desglose de articulos)

Una compra es una fila de `transacciones` con `tipo = 'GASTO'` y `es_compra = 1`, mas sus filas en `detalle_compras`. El backend crea/actualiza ambas en **una sola transaccion SQL** (todo o nada).

#### 7.4.1 `GET /compras`

Mismos filtros que 7.3.1 excepto `tipo` y `es_compra` (implicitos). Filtro adicional:

| Param | Tipo | Regla |
|---|---|---|
| `articulo` | texto (max 100) | Compras que contengan un articulo cuyo nombre coincida parcialmente |

**Response 200:** `data` = arreglo de `Transaccion` (6.3, SIN `articulos`), `meta` paginado.

#### 7.4.2 `GET /compras/{id}`

Si el id existe pero `es_compra = false`: `404 RECURSO_NO_ENCONTRADO`.

**Response 200:** `data` = `Compra` (6.5), `articulos` ordenados por `orden ASC`.

#### 7.4.3 `POST /compras`

**Request**

```json
{
  "naturaleza": "VARIABLE",
  "fecha": "2026-11-03",
  "comercio": "Supermaxi",
  "descripcion": "Compra semanal",
  "categoria_id": 11,
  "metodo_pago": "DEBITO",
  "notas": null,
  "articulos": [
    { "articulo": "Leche entera 1L", "cantidad": 2,   "unidad": "UNIDAD",  "precio_unitario_centavos": 125 },
    { "articulo": "Carne molida",    "cantidad": 1.5, "unidad": "KG",      "precio_unitario_centavos": 850 },
    { "articulo": "Detergente",      "cantidad": 1,   "unidad": "PAQUETE", "precio_unitario_centavos": 3050 }
  ]
}
```

| Campo | Obligatorio | Notas |
|---|---|---|
| `fecha`, `comercio`, `categoria_id`, `metodo_pago`, `articulos` | Si | `categoria_id` debe ser `tipo = GASTO` |
| `naturaleza` | No | def `VARIABLE` |
| `descripcion` | No | Si es `null` o vacia, el backend guarda `"Compra en " + comercio` (truncado a 200) |
| `notas` | No | |
| `articulos[].unidad` | No | def `UNIDAD` |

- El cliente **NO** envia `monto_centavos`, `subtotal_centavos`, `tipo` ni `es_compra`. Si los envia, el backend los ignora.
- `orden` se asigna segun la posicion en el arreglo (0..n-1).
- Errores de articulos usan `campo` con notacion de indice: `articulos[1].cantidad`.

**Response 201:** `data` = `Compra` (6.5).

#### 7.4.4 `PUT /compras/{id}`

Reemplazo completo. Mismo cuerpo que 7.4.3. El backend **borra todos los articulos anteriores y reinserta** los enviados, y recalcula `monto_centavos`.
Si el id existe pero `es_compra = false`: `404 RECURSO_NO_ENCONTRADO`.

**Response 200:** `data` = `Compra`.

#### 7.4.5 `DELETE /compras/{id}`

**Response 200**

```json
{ "ok": true, "data": { "id": 1024, "eliminado": true }, "meta": null }
```

---

### 7.5 Presupuestos

#### 7.5.1 `GET /presupuestos`

Query:

| Param | Tipo | Regla |
|---|---|---|
| `anio` | entero | Obligatorio |
| `mes` | entero | Obligatorio |
| `estado` | enum | Opcional: `NORMAL` / `ADVERTENCIA` / `EXCEDIDO` |

Sin paginacion. Orden: `porcentaje_usado DESC`.

**Response 200**

```json
{
  "ok": true,
  "data": {
    "anio": 2026,
    "mes": 11,
    "total_limite_centavos": 150000,
    "total_gastado_centavos": 98000,
    "total_disponible_centavos": 52000,
    "porcentaje_usado_global": 65.33,
    "presupuestos": [ { "...": "Presupuesto (6.6)" } ]
  },
  "meta": null
}
```

#### 7.5.2 `GET /presupuestos/{id}`

**Response 200:** `data` = `Presupuesto`.

#### 7.5.3 `POST /presupuestos`

**Request**

```json
{
  "categoria_id": 11,
  "anio": 2026,
  "mes": 11,
  "monto_limite_centavos": 40000,
  "notas": null
}
```

Duplicado: `409 PRESUPUESTO_DUPLICADO`.

**Response 201:** `data` = `Presupuesto`.

#### 7.5.4 `PUT /presupuestos/{id}`

**Request** (solo estos campos son editables; `categoria_id`, `anio`, `mes` son inmutables):

```json
{
  "monto_limite_centavos": 45000,
  "notas": "Ajustado por inflacion"
}
```

**Response 200:** `data` = `Presupuesto`.

#### 7.5.5 `DELETE /presupuestos/{id}`

**Response 200**

```json
{ "ok": true, "data": { "id": 77, "eliminado": true }, "meta": null }
```

---

### 7.6 `GET /dashboard`

Query:

| Param | Tipo | Regla |
|---|---|---|
| `anio` | entero | Obligatorio |
| `mes` | entero | Obligatorio |

**Response 200**

```json
{
  "ok": true,
  "data": {
    "periodo": { "anio": 2026, "mes": 11, "desde": "2026-11-01", "hasta": "2026-11-30" },
    "totales": {
      "ingresos_centavos": 250000,
      "gastos_centavos": 182500,
      "gastos_fijos_centavos": 110000,
      "gastos_variables_centavos": 72500,
      "balance_centavos": 67500,
      "tasa_ahorro_porcentaje": 27.00,
      "cantidad_transacciones": 48
    },
    "comparativo_mes_anterior": {
      "ingresos_variacion_porcentaje": 0.00,
      "gastos_variacion_porcentaje": -4.25
    },
    "gastos_por_categoria": [
      {
        "categoria": { "id": 7, "nombre": "Vivienda", "tipo": "GASTO", "icono": "fa-solid fa-house-chimney", "color": "#1B2A4A" },
        "total_centavos": 65000,
        "porcentaje": 35.62
      }
    ],
    "ingresos_por_categoria": [
      {
        "categoria": { "id": 1, "nombre": "Salario", "tipo": "INGRESO", "icono": "fa-solid fa-briefcase", "color": "#2E7D32" },
        "total_centavos": 250000,
        "porcentaje": 100.00
      }
    ],
    "presupuestos": {
      "total_limite_centavos": 150000,
      "total_gastado_centavos": 98000,
      "cantidad_normal": 4,
      "cantidad_advertencia": 1,
      "cantidad_excedido": 1
    },
    "evolucion_mensual": [
      { "anio": 2026, "mes": 6,  "ingresos_centavos": 240000, "gastos_centavos": 190000, "balance_centavos": 50000 },
      { "anio": 2026, "mes": 7,  "ingresos_centavos": 240000, "gastos_centavos": 175000, "balance_centavos": 65000 },
      { "anio": 2026, "mes": 8,  "ingresos_centavos": 245000, "gastos_centavos": 201000, "balance_centavos": 44000 },
      { "anio": 2026, "mes": 9,  "ingresos_centavos": 250000, "gastos_centavos": 188000, "balance_centavos": 62000 },
      { "anio": 2026, "mes": 10, "ingresos_centavos": 250000, "gastos_centavos": 190600, "balance_centavos": 59400 },
      { "anio": 2026, "mes": 11, "ingresos_centavos": 250000, "gastos_centavos": 182500, "balance_centavos": 67500 }
    ],
    "ultimas_transacciones": [ { "...": "Transaccion (6.3), maximo 5" } ]
  },
  "meta": null
}
```

Reglas:

- `balance_centavos` = `ingresos_centavos - gastos_centavos` (puede ser negativo).
- `tasa_ahorro_porcentaje` = `balance / ingresos * 100` con 2 decimales; si `ingresos_centavos = 0` entonces `0.00`.
- `*_variacion_porcentaje` = `(actual - anterior) / anterior * 100` con 2 decimales; si `anterior = 0` entonces `null`.
- `gastos_por_categoria` e `ingresos_por_categoria`: solo categorias con total > 0, orden `total_centavos DESC`. `porcentaje` con 2 decimales sobre el total de su tipo.
- `evolucion_mensual`: exactamente **6** elementos, orden cronologico ascendente, el ultimo es el mes consultado. Meses sin datos se rellenan con `0`.
- `ultimas_transacciones`: las 5 mas recientes del periodo (orden global por defecto).

---

## 8. Modulo IA - `POST /ai/analyze`

### 8.1 Configuracion (solo backend)

| Variable de entorno | Valor |
|---|---|
| `GEMINI_API_KEY` | Clave de Google AI Studio. **Solo existe en el VPS.** Los clientes NUNCA la conocen. |
| `GEMINI_MODEL` | Por defecto `gemini-2.5-flash` |
| Tiempo maximo | 60 segundos |
| Limite | 10 peticiones por ventana movil de 60 minutos (global) |

### 8.2 Request

```json
{
  "tipo_analisis": "RESUMEN_MENSUAL",
  "anio": 2026,
  "mes": 11,
  "pregunta": null
}
```

| Campo | Obligatorio | Regla |
|---|---|---|
| `tipo_analisis` | Si | Enum 2.1 |
| `anio`, `mes` | Si | Periodo a analizar |
| `pregunta` | Condicional | Obligatoria (5-500) si `PREGUNTA_LIBRE`; debe ser `null` en los demas |

### 8.3 Datos enviados a Gemini (privacidad)

El backend construye el contexto con **datos agregados** del periodo y de los 3 meses previos: totales del dashboard, gastos por categoria, estado de presupuestos y los 20 articulos con mayor gasto acumulado. **Nunca** envia `notas`, ni el token, ni identificadores internos.

### 8.4 Instrucciones obligatorias al modelo (system prompt)

El backend DEBE exigir a Gemini:

1. Responder en espanol, tono profesional y sobrio de asesor financiero.
2. **No usar emojis** ni simbolos decorativos.
3. Devolver **unicamente JSON** que cumpla el esquema de 8.5 (usar `responseMimeType: "application/json"` y `responseSchema`).

Si la salida no es JSON valido o no cumple el esquema: `502 ERROR_SERVICIO_IA`. Antes de responder, el backend elimina cualquier emoji residual (2.2).

### 8.5 Response 200

```json
{
  "ok": true,
  "data": {
    "tipo_analisis": "RESUMEN_MENSUAL",
    "periodo": { "anio": 2026, "mes": 11 },
    "pregunta": null,
    "resumen": "Durante noviembre los gastos representaron el 73 por ciento de los ingresos...",
    "puntuacion_salud_financiera": 72,
    "recomendaciones": [
      {
        "titulo": "Reducir gasto en restaurantes",
        "detalle": "El gasto en restaurantes supera en 18 por ciento el promedio de los ultimos tres meses...",
        "prioridad": "ALTA",
        "ahorro_estimado_centavos": 8000,
        "categoria_id": 12
      }
    ],
    "alertas": [
      {
        "codigo": "PRESUPUESTO_EXCEDIDO",
        "mensaje": "El presupuesto de Supermercado se excedio en 12.50 USD.",
        "categoria_id": 11
      }
    ],
    "modelo": "gemini-2.5-flash",
    "generado_en": "2026-11-04T02:14:40Z"
  },
  "meta": null
}
```

| Campo | Tipo | Regla |
|---|---|---|
| `resumen` | texto | 1 a 2000 caracteres |
| `puntuacion_salud_financiera` | entero | 0 a 100 |
| `recomendaciones` | arreglo | 0 a 8 elementos |
| `recomendaciones[].ahorro_estimado_centavos` | entero o `null` | |
| `recomendaciones[].categoria_id` | entero o `null` | Debe existir; si no, el backend lo pone a `null` |
| `alertas` | arreglo | 0 a 10 elementos |
| `alertas[].codigo` | texto | `PRESUPUESTO_EXCEDIDO`, `PRESUPUESTO_EN_RIESGO`, `BALANCE_NEGATIVO`, `GASTO_INUSUAL`, `AHORRO_BAJO`, `OTRO` |
| `alertas[].categoria_id` | entero o `null` | |

---

## 9. Restricciones de entorno por agente

| Agente | Restriccion obligatoria |
|---|---|
| Todos | PC local con menos de 10 GB libres. Prohibidas dependencias pesadas y cualquier paquete que requiera compilacion nativa local (node-gyp, compiladores C/C++). Solo dependencias puras o con binarios precompilados oficiales. |
| Todos | Cero emojis en codigo, comentarios, commits, textos y UI. Iconografia exclusiva FontAwesome 6 Free (ver `UI_DESIGN.md`). |
| Backend | Escucha en `0.0.0.0:54892`. SQLite como unico almacenamiento. Variables de entorno: `APP_PRIVATE_TOKEN`, `APP_DB_PATH`, `APP_MONEDA`, `GEMINI_API_KEY`, `GEMINI_MODEL`. Archivo `.env.example` sin valores secretos reales. Logs sin token ni clave Gemini. |
| PC | Consume solo esta API. No accede a SQLite directamente. Timeout HTTP 15 s (90 s para `/ai/analyze`). |
| Android | Consume solo esta API. `network_security_config` restringido a `51.161.47.99`. Timeout HTTP 15 s (90 s para `/ai/analyze`). Iconos via fuente FontAwesome 6 Free empaquetada en `assets`. |

### 9.1 Comportamiento comun de clientes ante errores

| Situacion | Accion del cliente |
|---|---|
| Sin red / timeout | Mensaje "No se pudo conectar con el servidor." + boton Reintentar. No duplicar POST automaticamente. |
| `401` | Mensaje "Token de acceso invalido." y bloquear operaciones. |
| `409` / `422` | Mostrar `error.mensaje` y marcar cada campo de `error.detalles`. |
| `429` | Mostrar "Limite de analisis IA alcanzado. Intente mas tarde." |
| `5xx` | Mostrar `error.mensaje` generico, sin trazas. |
| Doble envio | Deshabilitar el boton de envio mientras la peticion esta en curso. |

---

## 10. Checklist de conformidad (cada agente lo valida antes de marcar COMPLETADO)

- [ ] Todas las llaves JSON coinciden exactamente con este documento.
- [ ] Dinero manejado solo como enteros `_centavos`.
- [ ] Fechas `YYYY-MM-DD` y marcas de tiempo ISO 8601 UTC con `Z`.
- [ ] Sobre `ok/data/meta` y `ok/error` en todas las respuestas.
- [ ] Header `X-App-Private-Token` enviado (clientes) / validado (backend).
- [ ] Codigos de error tomados exclusivamente de la tabla 3.3.
- [ ] Cero emojis.
- [ ] Ninguna dependencia con compilacion nativa local.
