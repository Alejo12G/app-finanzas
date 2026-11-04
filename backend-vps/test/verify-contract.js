'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');

// Usar base de datos temporal de prueba antes de cargar modulos
const testDbPath = path.resolve(__dirname, '..', 'data', 'test_finanzas.db');
for (const ext of ['', '-wal', '-shm']) {
  if (fs.existsSync(testDbPath + ext)) {
    fs.unlinkSync(testDbPath + ext);
  }
}
process.env.APP_DB_PATH = testDbPath;
process.env.PORT = '54893';

const config = require('../src/config');
const { getDb } = require('../src/db');
const { handleRequest } = require('../src/app');
const { resetAiRateLimitForTests } = require('../src/routes/ai');
const { hasEmoji } = require('../src/utils/validators');

const TOKEN = config.APP_PRIVATE_TOKEN;
const BASE_URL = `http://127.0.0.1:${config.PORT}/api/v1`;

async function apiReq(method, route, { token = TOKEN, body = undefined, rawBody = undefined } = {}) {
  const headers = {};
  if (token !== null) {
    headers['X-App-Private-Token'] = token;
  }
  let payload;
  if (rawBody !== undefined) {
    headers['Content-Type'] = 'application/json; charset=utf-8';
    payload = rawBody;
  } else if (body !== undefined) {
    headers['Content-Type'] = 'application/json; charset=utf-8';
    payload = JSON.stringify(body);
  }

  const res = await fetch(`${BASE_URL}${route}`, {
    method,
    headers,
    body: payload
  });

  const text = await res.text();
  let json = null;
  if (text) {
    json = JSON.parse(text);
  }
  return { status: res.status, headers: res.headers, json };
}

async function runTests() {
  getDb();
  const server = http.createServer((req, res) => handleRequest(req, res));
  await new Promise((resolve) => server.listen(config.PORT, '127.0.0.1', resolve));

  try {
    console.log('1. Verificando OPTIONS (CORS preflight) y GET /health sin token...');
    const optRes = await apiReq('OPTIONS', '/transacciones', { token: null });
    assert.equal(optRes.status, 204);
    assert.equal(optRes.headers.get('access-control-allow-origin'), '*');

    const healthRes = await apiReq('GET', '/health', { token: null });
    assert.equal(healthRes.status, 200);
    assert.equal(healthRes.json.ok, true);
    assert.equal(healthRes.json.data.estado, 'OK');
    assert.equal(healthRes.json.data.version_api, '1.0.0');
    assert.equal(healthRes.json.data.moneda, config.APP_MONEDA);
    assert.match(healthRes.json.data.hora_servidor, /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$/);

    console.log('2. Verificando middleware de seguridad X-App-Private-Token...');
    const noTokenRes = await apiReq('GET', '/categorias', { token: null });
    assert.equal(noTokenRes.status, 401);
    assert.equal(noTokenRes.json.ok, false);
    assert.equal(noTokenRes.json.error.codigo, 'TOKEN_INVALIDO');

    const badTokenRes = await apiReq('GET', '/categorias', { token: 'token_invalido_123' });
    assert.equal(badTokenRes.status, 401);
    assert.equal(badTokenRes.json.error.codigo, 'TOKEN_INVALIDO');

    console.log('3. Verificando 31 categorias semilla y CRUD de /categorias...');
    const catsRes = await apiReq('GET', '/categorias');
    assert.equal(catsRes.status, 200);
    assert.equal(catsRes.json.data.length, 31);

    const newCatRes = await apiReq('POST', '/categorias', {
      body: {
        nombre: 'Gimnasio',
        tipo: 'GASTO',
        icono: 'fa-solid fa-dumbbell',
        color: '#4A5568'
      }
    });
    assert.equal(newCatRes.status, 201);
    assert.equal(newCatRes.json.data.id, 32);
    assert.equal(newCatRes.json.data.activa, true);

    const dupCatRes = await apiReq('POST', '/categorias', {
      body: {
        nombre: 'gimnasio',
        tipo: 'GASTO',
        icono: 'fa-solid fa-dumbbell'
      }
    });
    assert.equal(dupCatRes.status, 409);
    assert.equal(dupCatRes.json.error.codigo, 'CATEGORIA_DUPLICADA');

    const emojiCatRes = await apiReq('POST', '/categorias', {
      body: {
        nombre: 'Cafe \u{1F600}',
        tipo: 'GASTO',
        icono: 'fa-solid fa-mug-hot'
      }
    });
    assert.equal(emojiCatRes.status, 422);
    assert.equal(emojiCatRes.json.error.codigo, 'TEXTO_CON_EMOJI');

    console.log('4. Verificando CRUD y validaciones de /transacciones...');
    const ingresoRes = await apiReq('POST', '/transacciones', {
      body: {
        tipo: 'INGRESO',
        naturaleza: 'FIJO',
        monto_centavos: 250000,
        fecha: '2026-11-01',
        descripcion: 'Salario noviembre',
        categoria_id: 1,
        metodo_pago: 'TRANSFERENCIA',
        comercio: null,
        notas: 'Pago mensual'
      }
    });
    assert.equal(ingresoRes.status, 201);
    assert.equal(ingresoRes.json.data.es_compra, false);
    assert.equal(ingresoRes.json.data.cantidad_articulos, 0);

    const incompatibleRes = await apiReq('POST', '/transacciones', {
      body: {
        tipo: 'GASTO',
        naturaleza: 'FIJO',
        monto_centavos: 1000,
        fecha: '2026-11-02',
        descripcion: 'Incompatible',
        categoria_id: 1, // Salario es INGRESO
        metodo_pago: 'EFECTIVO'
      }
    });
    assert.equal(incompatibleRes.status, 422);
    assert.equal(incompatibleRes.json.error.codigo, 'CATEGORIA_TIPO_INCOMPATIBLE');

    console.log('5. Verificando CRUD y calculo de /compras con redondeo mitad hacia arriba...');
    const compraRes = await apiReq('POST', '/compras', {
      body: {
        naturaleza: 'VARIABLE',
        fecha: '2026-11-03',
        comercio: 'Supermaxi',
        descripcion: null,
        categoria_id: 11,
        metodo_pago: 'DEBITO',
        notas: null,
        articulos: [
          { articulo: 'Leche entera 1L', cantidad: 2, unidad: 'UNIDAD', precio_unitario_centavos: 125 },
          { articulo: 'Carne molida', cantidad: 1.5, unidad: 'KG', precio_unitario_centavos: 850 },
          { articulo: 'Detergente', cantidad: 1, unidad: 'PAQUETE', precio_unitario_centavos: 3050 }
        ]
      }
    });
    assert.equal(compraRes.status, 201);
    assert.equal(compraRes.json.data.monto_centavos, 4575);
    assert.equal(compraRes.json.data.descripcion, 'Compra en Supermaxi');
    assert.equal(compraRes.json.data.es_compra, true);
    assert.equal(compraRes.json.data.cantidad_articulos, 3);
    assert.equal(compraRes.json.data.articulos.length, 3);

    const compraId = compraRes.json.data.id;

    // Intentar editar compra desde /transacciones/{id} debe devolver 409 USAR_ENDPOINT_COMPRAS
    const putCompraViaTx = await apiReq('PUT', `/transacciones/${compraId}`, {
      body: {
        tipo: 'GASTO',
        naturaleza: 'VARIABLE',
        monto_centavos: 4575,
        fecha: '2026-11-03',
        descripcion: 'Intento invalido',
        categoria_id: 11,
        metodo_pago: 'DEBITO'
      }
    });
    assert.equal(putCompraViaTx.status, 409);
    assert.equal(putCompraViaTx.json.error.codigo, 'USAR_ENDPOINT_COMPRAS');

    console.log('6. Verificando /presupuestos y /dashboard...');
    const presRes = await apiReq('POST', '/presupuestos', {
      body: {
        categoria_id: 11,
        anio: 2026,
        mes: 11,
        monto_limite_centavos: 5000,
        notas: null
      }
    });
    assert.equal(presRes.status, 201);
    assert.equal(presRes.json.data.gastado_centavos, 4575);
    assert.equal(presRes.json.data.disponible_centavos, 425);
    assert.equal(presRes.json.data.porcentaje_usado, 91.5);
    assert.equal(presRes.json.data.estado, 'ADVERTENCIA');

    const dashRes = await apiReq('GET', '/dashboard?anio=2026&mes=11');
    assert.equal(dashRes.status, 200);
    assert.equal(dashRes.json.data.totales.ingresos_centavos, 250000);
    assert.equal(dashRes.json.data.totales.gastos_centavos, 4575);
    assert.equal(dashRes.json.data.totales.balance_centavos, 245425);
    assert.equal(dashRes.json.data.evolucion_mensual.length, 6);
    assert.equal(dashRes.json.data.presupuestos.cantidad_advertencia, 1);

    console.log('7. Verificando integracion real con Gemini IA (RESUMEN_MENSUAL)...');
    resetAiRateLimitForTests();
    const aiRes = await apiReq('POST', '/ai/analyze', {
      body: {
        tipo_analisis: 'RESUMEN_MENSUAL',
        anio: 2026,
        mes: 11,
        pregunta: null
      }
    });
    assert.equal(aiRes.status, 200);
    assert.equal(aiRes.json.ok, true);
    assert.equal(aiRes.json.data.tipo_analisis, 'RESUMEN_MENSUAL');
    assert.equal(typeof aiRes.json.data.resumen, 'string');
    assert.equal(hasEmoji(aiRes.json.data.resumen), false);
    assert.equal(
      aiRes.json.data.puntuacion_salud_financiera >= 0 &&
        aiRes.json.data.puntuacion_salud_financiera <= 100,
      true
    );

    console.log('8. Verificando parseo IA de compra en texto libre y guardado automatico...');
    const parseRes = await apiReq('POST', '/ai/analyze', {
      body: {
        texto: 'Compre 2 litros de leche a 8000 y pan a 5000 en el Exito',
        fecha: '2026-11-03'
      }
    });
    assert.equal(parseRes.status, 201);
    assert.equal(parseRes.json.ok, true);
    assert.equal(parseRes.json.data.es_compra, true);
    assert.equal(parseRes.json.data.guardado, true);
    assert.equal(parseRes.json.data.articulos.length >= 2, true);

    console.log('9. Verificando cero emojis en todos los archivos de /backend-vps...');
    const backendRoot = path.resolve(__dirname, '..');
    function scanDirForEmojis(dir) {
      for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
        if (entry.name === 'data' || entry.name === 'node_modules') continue;
        const full = path.join(dir, entry.name);
        if (entry.isDirectory()) {
          scanDirForEmojis(full);
        } else if (entry.name !== 'verify-contract.js') {
          const text = fs.readFileSync(full, 'utf8');
          assert.equal(hasEmoji(text), false, `Archivo contiene emoji: ${full}`);
        }
      }
    }
    scanDirForEmojis(backendRoot);

    console.log('TODAS LAS PRUEBAS DEL CONTRATO SUPERADAS CON EXITO.');
  } finally {
    await new Promise((resolve) => server.close(resolve));
    try {
      getDb().close();
    } catch (_) {}
    for (const ext of ['', '-wal', '-shm']) {
      if (fs.existsSync(testDbPath + ext)) {
        try {
          fs.unlinkSync(testDbPath + ext);
        } catch (_) {}
      }
    }
  }
}

runTests().catch((err) => {
  console.error('Fallo en pruebas:', err);
  process.exit(1);
});
