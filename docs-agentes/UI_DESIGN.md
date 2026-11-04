# UI_DESIGN.md - Sistema de Diseno Obligatorio

**Version:** 1.0.0
**Estado:** CONGELADO.
**Aplica a:** `/pc-app` y `/android-app` (y a cualquier texto devuelto por `/backend-vps`).
**Estilo:** Clasico, bancario, sobrio. Prioridad: legibilidad de cifras, jerarquia clara, cero decoracion superflua.

---

## 1. Regla absoluta: CERO EMOJIS

> [!CAUTION]
> Esta prohibido usar emojis en cualquier lugar: interfaz, textos, etiquetas, notificaciones, placeholders, mensajes de error, comentarios de codigo, nombres de archivos, mensajes de commit y documentacion.

- Toda representacion grafica se hace **exclusivamente** con **FontAwesome 6 Free** (estilos `solid`, `regular` y `brands`).
- Version fijada: **FontAwesome Free 6.5.2**. Si hay discrepancia de codigo Unicode, prevalece el archivo oficial `metadata/icons.json` de esa version; el **nombre de clase** de este documento es la fuente de verdad.
- Prohibidos tambien: simbolos Unicode decorativos (flechas, estrellas, checks, bullets graficos), imagenes PNG de iconos, Material Icons u otras librerias de iconos.
- Revision obligatoria: antes de marcar una tarea como COMPLETADA, el agente debe buscar emojis en su carpeta (rangos Unicode definidos en `API_CONTRACT.md` seccion 2.2).

### 1.1 Integracion de FontAwesome por plataforma

| Plataforma | Metodo |
|---|---|
| PC | Archivos locales de FontAwesome Free 6.5.2 (`css/all.min.css` + `webfonts/`) incluidos en el proyecto. No depender de CDN en tiempo de ejecucion. Uso: `<i class="fa-solid fa-wallet" aria-hidden="true"></i>`. |
| Android | Fuentes `fa-solid-900.ttf`, `fa-regular-400.ttf`, `fa-brands-400.ttf` en `assets/fonts/`. Un unico componente/helper `FaIcon` que traduce el nombre de clase (ej. `fa-solid fa-wallet`) a su glifo Unicode. El campo `icono` de las categorias que llega de la API se resuelve con ese mismo mapa. |

---

## 2. Paleta de colores

### 2.1 Colores principales (obligatorios)

| Token | Hex | Uso |
|---|---|---|
| `color-primario` | `#1B2A4A` | Azul marino. Barra superior, menu lateral, encabezados de tabla, botones primarios, titulos. |
| `color-secundario` | `#4A5568` | Gris pizarra. Texto secundario, iconos inactivos, bordes de enfasis, botones secundarios. |
| `color-exito` | `#2E7D32` | Verde contable. Ingresos, montos positivos, balance positivo, presupuesto `NORMAL`, confirmaciones. |
| `color-peligro` | `#C62828` | Rojo alerta. Gastos en cifras de balance, montos negativos, presupuesto `EXCEDIDO`, errores, acciones destructivas. |
| `color-fondo` | `#F4F6F8` | Fondo general de la aplicacion. |

### 2.2 Colores de soporte (derivados, permitidos)

| Token | Hex | Uso |
|---|---|---|
| `color-superficie` | `#FFFFFF` | Tarjetas, tablas, formularios, dialogos. |
| `color-primario-hover` | `#24375F` | Hover/pulsado de elementos primarios. |
| `color-primario-suave` | `#E8ECF3` | Fila seleccionada, item de menu activo, fondos de chips. |
| `color-borde` | `#D9DEE5` | Bordes de tarjetas, separadores, bordes de inputs. |
| `color-texto` | `#1A202C` | Texto principal. |
| `color-texto-secundario` | `#4A5568` | Etiquetas, subtitulos, ayudas. |
| `color-texto-deshabilitado` | `#A0AEC0` | Controles deshabilitados. |
| `color-texto-invertido` | `#FFFFFF` | Texto sobre `color-primario`, `color-exito`, `color-peligro`. |
| `color-advertencia` | `#B26A00` | Presupuesto `ADVERTENCIA` (80 a 99.99 por ciento). |
| `color-exito-suave` | `#E8F5E9` | Fondo de mensajes de exito. |
| `color-peligro-suave` | `#FDECEA` | Fondo de mensajes de error. |
| `color-advertencia-suave` | `#FFF4E5` | Fondo de mensajes de advertencia. |

Prohibido introducir colores fuera de estas dos tablas (excepto el campo `color` que devuelve la API para categorias, que ya usa esta paleta).

### 2.3 Semantica de cifras

| Situacion | Color | Formato |
|---|---|---|
| Ingreso | `#2E7D32` | `+ 1,250.00` |
| Gasto | `#C62828` | `- 1,250.00` |
| Balance positivo | `#2E7D32` | `1,250.00` |
| Balance negativo | `#C62828` | `- 1,250.00` |
| Neutro (limites, totales informativos) | `#1A202C` | `1,250.00` |

- La API entrega centavos enteros; la UI divide entre 100 solo para mostrar, con 2 decimales fijos.
- Separador de miles `,` y decimal `.`. Codigo de moneda (de `GET /health`, campo `moneda`) a la izquierda en encabezados de totales: `USD 1,250.00`.
- Cifras alineadas a la derecha en tablas y con numeros tabulares (`font-variant-numeric: tabular-nums` en PC; fuente monoespaciada para numeros o `fontFeatureSettings = "tnum"` en Android).

### 2.4 Estado de presupuestos (barra de progreso)

| `estado` (API) | Color barra | Icono |
|---|---|---|
| `NORMAL` | `#2E7D32` | `fa-solid fa-circle-check` |
| `ADVERTENCIA` | `#B26A00` | `fa-solid fa-triangle-exclamation` |
| `EXCEDIDO` | `#C62828` | `fa-solid fa-circle-xmark` |

La barra se limita visualmente al 100 por ciento; el porcentaje real se muestra como texto (ej. `112.40 %`).

---

## 3. Tipografia

| Elemento | PC | Android | Tamano | Peso |
|---|---|---|---|---|
| Familia | `"Segoe UI", Roboto, Arial, sans-serif` | Roboto (sistema) | - | - |
| Titulo de pantalla | | | 22 px / 22 sp | 600 |
| Subtitulo / encabezado de tarjeta | | | 16 px / 16 sp | 600 |
| Texto base | | | 14 px / 14 sp | 400 |
| Etiqueta / ayuda | | | 12 px / 12 sp | 400 |
| Cifra destacada (KPI) | | | 28 px / 28 sp | 600 |

Sin fuentes decorativas, sin cursivas en cifras, sin textos en mayusculas sostenidas salvo encabezados de tabla (12 px, peso 600, `letter-spacing` 0.5 px).

---

## 4. Componentes y espaciado

| Aspecto | Regla |
|---|---|
| Unidad base | 8 px / 8 dp (multiplos: 4, 8, 16, 24, 32) |
| Radio de borde | 4 px (tarjetas, botones, inputs). Sin esquinas muy redondeadas. |
| Sombras | Minimas: `0 1px 2px rgba(27,42,74,0.08)`. Sin sombras dramaticas ni degradados. |
| Botones | Alto 36 px (PC) / 48 dp (Android). Icono a la izquierda + texto. |
| Boton primario | Fondo `#1B2A4A`, texto `#FFFFFF`. |
| Boton secundario | Fondo `#FFFFFF`, borde y texto `#4A5568`. |
| Boton peligro | Fondo `#C62828`, texto `#FFFFFF`. Siempre con dialogo de confirmacion. |
| Tablas | Encabezado `#1B2A4A` con texto blanco; filas alternas `#FFFFFF` / `#F4F6F8`; altura de fila 40 px. |
| Inputs | Borde `#D9DEE5`; foco con borde `#1B2A4A` 2 px; error con borde `#C62828` y mensaje debajo en 12 px. |
| Animaciones | Solo transiciones de 150 ms en hover/foco. Sin rebotes ni animaciones decorativas. |
| Estados vacios | Icono FontAwesome 48 px color `#A0AEC0` + texto explicativo + boton de accion. |
| Carga | Icono `fa-solid fa-spinner fa-spin` (PC) o indicador de progreso nativo color `#1B2A4A` (Android). |

---

## 5. Catalogo de iconos FontAwesome 6

### 5.1 Navegacion principal (identica en PC y Android)

| Seccion | Clase | Unicode |
|---|---|---|
| Panel / Dashboard | `fa-solid fa-chart-pie` | `f200` |
| Transacciones | `fa-solid fa-money-bill-transfer` | `e528` |
| Compras | `fa-solid fa-cart-shopping` | `f07a` |
| Presupuestos | `fa-solid fa-wallet` | `f555` |
| Categorias | `fa-solid fa-tags` | `f02c` |
| Asesor IA | `fa-solid fa-robot` | `f544` |
| Configuracion | `fa-solid fa-gear` | `f013` |
| Inicio (logo/app) | `fa-solid fa-building-columns` | `f19c` |

### 5.2 Botones y acciones

| Accion | Clase | Unicode |
|---|---|---|
| Nuevo / Agregar | `fa-solid fa-plus` | `2b` |
| Editar | `fa-solid fa-pen` | `f304` |
| Eliminar | `fa-solid fa-trash-can` | `f2ed` |
| Guardar | `fa-solid fa-floppy-disk` | `f0c7` |
| Cancelar / Cerrar | `fa-solid fa-xmark` | `f00d` |
| Confirmar | `fa-solid fa-check` | `f00c` |
| Buscar | `fa-solid fa-magnifying-glass` | `f002` |
| Filtrar | `fa-solid fa-filter` | `f0b0` |
| Limpiar filtros | `fa-solid fa-filter-circle-xmark` | `e17b` |
| Recargar / Sincronizar | `fa-solid fa-arrows-rotate` | `f021` |
| Reintentar | `fa-solid fa-rotate-right` | `f2f9` |
| Exportar | `fa-solid fa-file-export` | `f56e` |
| Exportar CSV | `fa-solid fa-file-csv` | `f6dd` |
| Imprimir | `fa-solid fa-print` | `f02f` |
| Ver detalle | `fa-solid fa-eye` | `f06e` |
| Ocultar | `fa-solid fa-eye-slash` | `f070` |
| Agregar articulo (compra) | `fa-solid fa-square-plus` | `f0fe` |
| Quitar articulo (compra) | `fa-solid fa-square-minus` | `f146` |
| Ticket / desglose | `fa-solid fa-receipt` | `f543` |
| Selector de fecha | `fa-solid fa-calendar-days` | `f073` |
| Mes anterior | `fa-solid fa-chevron-left` | `f053` |
| Mes siguiente | `fa-solid fa-chevron-right` | `f054` |
| Menu (Android / PC compacto) | `fa-solid fa-bars` | `f0c9` |
| Mas opciones | `fa-solid fa-ellipsis-vertical` | `f142` |
| Ordenar | `fa-solid fa-sort` | `f0dc` |
| Ordenar ascendente | `fa-solid fa-sort-up` | `f0de` |
| Ordenar descendente | `fa-solid fa-sort-down` | `f0dd` |
| Generar analisis IA | `fa-solid fa-wand-magic-sparkles` | `e2ca` |
| Enviar pregunta IA | `fa-solid fa-paper-plane` | `f1d8` |
| Copiar texto | `fa-solid fa-copy` | `f0c5` |

### 5.3 Indicadores y estados

| Uso | Clase | Unicode |
|---|---|---|
| Ingreso | `fa-solid fa-arrow-trend-up` | `e098` |
| Gasto | `fa-solid fa-arrow-trend-down` | `e097` |
| Balance | `fa-solid fa-scale-balanced` | `f24e` |
| Tasa de ahorro | `fa-solid fa-piggy-bank` | `f4d3` |
| Gasto fijo (`FIJO`) | `fa-solid fa-thumbtack` | `f08d` |
| Gasto variable (`VARIABLE`) | `fa-solid fa-shuffle` | `f074` |
| Exito | `fa-solid fa-circle-check` | `f058` |
| Advertencia | `fa-solid fa-triangle-exclamation` | `f071` |
| Error | `fa-solid fa-circle-xmark` | `f057` |
| Informacion | `fa-solid fa-circle-info` | `f05a` |
| Cargando | `fa-solid fa-spinner` | `f110` |
| Servidor conectado | `fa-solid fa-server` | `f233` |
| Sin conexion | `fa-solid fa-plug-circle-xmark` | `e560` |
| Token / seguridad | `fa-solid fa-key` | `f084` |
| Bloqueado (401) | `fa-solid fa-lock` | `f023` |
| Recomendacion IA | `fa-solid fa-lightbulb` | `f0eb` |
| Prioridad ALTA (IA) | `fa-solid fa-circle-exclamation` | `f06a` |
| Prioridad MEDIA (IA) | `fa-solid fa-circle-half-stroke` | `f042` |
| Prioridad BAJA (IA) | `fa-regular fa-circle` | `f111` |
| Salud financiera | `fa-solid fa-gauge-high` | `f625` |
| Estado vacio | `fa-regular fa-folder-open` | `f07c` |

### 5.4 Metodos de pago (`metodo_pago`)

| Valor API | Etiqueta UI | Clase | Unicode |
|---|---|---|---|
| `EFECTIVO` | Efectivo | `fa-solid fa-money-bill` | `f0d6` |
| `DEBITO` | Tarjeta de debito | `fa-solid fa-money-check` | `f53c` |
| `CREDITO` | Tarjeta de credito | `fa-solid fa-credit-card` | `f09d` |
| `TRANSFERENCIA` | Transferencia | `fa-solid fa-building-columns` | `f19c` |
| `OTRO` | Otro | `fa-solid fa-circle-question` | `f059` |

### 5.5 Unidades de articulo (`unidad`)

| Valor API | Etiqueta UI |
|---|---|
| `UNIDAD` | und |
| `KG` | kg |
| `G` | g |
| `L` | L |
| `ML` | ml |
| `PAQUETE` | paq |
| `OTRO` | otro |

### 5.6 Categorias semilla (coincide 1:1 con `API_CONTRACT.md` seccion 5)

| id | Categoria | Tipo | Clase | Unicode |
|---|---|---|---|---|
| 1 | Salario | INGRESO | `fa-solid fa-briefcase` | `f0b1` |
| 2 | Honorarios | INGRESO | `fa-solid fa-laptop-code` | `f5fc` |
| 3 | Inversiones | INGRESO | `fa-solid fa-chart-line` | `f201` |
| 4 | Ventas | INGRESO | `fa-solid fa-hand-holding-dollar` | `f4c0` |
| 5 | Reembolsos | INGRESO | `fa-solid fa-rotate-left` | `f2ea` |
| 6 | Otros ingresos | INGRESO | `fa-solid fa-coins` | `f51e` |
| 7 | Vivienda | GASTO | `fa-solid fa-house-chimney` | `e3af` |
| 8 | Electricidad | GASTO | `fa-solid fa-bolt` | `f0e7` |
| 9 | Agua | GASTO | `fa-solid fa-droplet` | `f043` |
| 10 | Internet y telefonia | GASTO | `fa-solid fa-wifi` | `f1eb` |
| 11 | Supermercado | GASTO | `fa-solid fa-basket-shopping` | `f291` |
| 12 | Restaurantes | GASTO | `fa-solid fa-utensils` | `f2e7` |
| 13 | Transporte | GASTO | `fa-solid fa-bus` | `f207` |
| 14 | Combustible | GASTO | `fa-solid fa-gas-pump` | `f52f` |
| 15 | Vehiculo | GASTO | `fa-solid fa-car` | `f1b9` |
| 16 | Salud | GASTO | `fa-solid fa-heart-pulse` | `f21e` |
| 17 | Farmacia | GASTO | `fa-solid fa-prescription-bottle-medical` | `f486` |
| 18 | Educacion | GASTO | `fa-solid fa-graduation-cap` | `f19d` |
| 19 | Entretenimiento | GASTO | `fa-solid fa-film` | `f008` |
| 20 | Suscripciones | GASTO | `fa-solid fa-repeat` | `f363` |
| 21 | Ropa | GASTO | `fa-solid fa-shirt` | `f553` |
| 22 | Hogar | GASTO | `fa-solid fa-couch` | `f4b8` |
| 23 | Seguros | GASTO | `fa-solid fa-shield-halved` | `f3ed` |
| 24 | Deudas y prestamos | GASTO | `fa-solid fa-credit-card` | `f09d` |
| 25 | Impuestos | GASTO | `fa-solid fa-landmark` | `f66f` |
| 26 | Mascotas | GASTO | `fa-solid fa-paw` | `f1b0` |
| 27 | Regalos | GASTO | `fa-solid fa-gift` | `f06b` |
| 28 | Viajes | GASTO | `fa-solid fa-plane` | `f072` |
| 29 | Cuidado personal | GASTO | `fa-solid fa-spa` | `f5bb` |
| 30 | Ahorro | GASTO | `fa-solid fa-piggy-bank` | `f4d3` |
| 31 | Otros gastos | GASTO | `fa-solid fa-ellipsis` | `f141` |

**Icono por defecto** si la clase recibida no esta en el mapa del cliente: `fa-solid fa-tag` (`f02b`).

### 5.7 Iconos sugeridos para categorias nuevas (selector en UI)

`fa-solid fa-dumbbell`, `fa-solid fa-baby`, `fa-solid fa-book`, `fa-solid fa-tooth`, `fa-solid fa-wrench`, `fa-solid fa-mobile-screen`, `fa-solid fa-tv`, `fa-solid fa-gamepad`, `fa-solid fa-mug-hot`, `fa-solid fa-scissors`, `fa-solid fa-hand-holding-heart`, `fa-solid fa-building`, `fa-solid fa-truck`, `fa-solid fa-children`, `fa-solid fa-tag`.

Los clientes muestran solo iconos de las tablas 5.6 y 5.7 en el selector de categorias.

---

## 6. Estructura de pantallas (comun PC y Android)

| Pantalla | Contenido minimo | Endpoint principal |
|---|---|---|
| Dashboard | Selector de mes, 4 KPIs (Ingresos, Gastos, Balance, Tasa de ahorro), grafico de gastos por categoria (barras horizontales o dona), evolucion 6 meses (barras agrupadas ingresos/gastos), resumen de presupuestos, ultimas 5 transacciones. | `GET /dashboard` |
| Transacciones | Tabla/lista paginada con filtros, alta/edicion/borrado. Formulario con tipo, naturaleza, monto, fecha, descripcion, categoria, metodo de pago, comercio, notas. | `/transacciones` |
| Compras | Lista paginada; detalle tipo ticket con articulos; formulario con tabla editable de articulos y total calculado en vivo (vista previa con la regla de redondeo del contrato). | `/compras` |
| Presupuestos | Selector de mes, barras de progreso por categoria con color por `estado`, totales globales. | `/presupuestos` |
| Categorias | Lista agrupada por tipo, alta/edicion, activar/desactivar. | `/categorias` |
| Asesor IA | Selector de `tipo_analisis` y mes, campo de pregunta (solo `PREGUNTA_LIBRE`), resultado con resumen, puntuacion (0-100), recomendaciones con prioridad y alertas. | `POST /ai/analyze` |
| Configuracion | Estado de conexion (`GET /health`), URL del servidor (solo lectura), version de la app. | `GET /health` |

### 6.1 Graficos

- Colores de series: Ingresos `#2E7D32`, Gastos `#C62828`, Balance `#1B2A4A`. Categorias: usar el campo `color` de la API.
- Sin efectos 3D, sin sombras, sin animaciones de entrada mayores a 300 ms.
- Ejes y cuadricula en `#D9DEE5`; etiquetas en `#4A5568`.
- Librerias livianas solamente (PC: libreria JS ligera o SVG/Canvas propio; Android: Canvas propio o libreria pura sin NDK).

---

## 7. Textos y tono

- Idioma: espanol neutro, formal (usted o impersonal). Ejemplos: "Guardar cambios", "Eliminar transaccion", "No hay registros para este periodo."
- Confirmaciones destructivas: "Esta accion no se puede deshacer. Desea eliminar esta transaccion?"
- Sin signos de exclamacion multiples, sin jerga, sin emojis, sin simbolos decorativos.
- Mensajes de error de la API (`error.mensaje`) se muestran tal cual.

---

## 8. Accesibilidad minima

- Contraste minimo 4.5:1 para texto (la paleta lo cumple sobre `#FFFFFF` y `#F4F6F8`).
- Iconos decorativos con `aria-hidden="true"` (PC) o `contentDescription = null` (Android); botones solo-icono con etiqueta accesible.
- El color nunca es el unico indicador: los montos llevan signo `+` / `-` y los estados llevan icono y texto.
