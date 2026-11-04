'use strict';

const assert = require('assert');
const fs = require('fs');
const path = require('path');
const http = require('http');
const { execFileSync } = require('child_process');

const { loadConfig, DEFAULT_CONFIG } = require('../src/config');
const { startLocalServer } = require('../src/server');

// Cargamos modulos de cliente en el contexto global para pruebas unitarias
global.window = global;
require('../public/js/config.js');
require('../public/js/utils.js');
require('../public/js/api.js');

const EMOJI_REGEX = /[\u{1F000}-\u{1FAFF}\u{2600}-\u{27BF}\u{FE0F}\u{200D}\u{1F1E6}-\u{1F1FF}]/u;

function walkFiles(dir, result = []) {
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const entry of entries) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      walkFiles(full, result);
    } else {
      result.push(full);
    }
  }
  return result;
}

async function runTests() {
  console.log('[TEST] 1. Validando sintaxis de todos los archivos JS...');
  const pcAppRoot = path.join(__dirname, '..');
  const allFiles = walkFiles(pcAppRoot);
  const jsFiles = allFiles.filter((f) => f.endsWith('.js') && !f.endsWith('.min.js'));

  for (const jsFile of jsFiles) {
    execFileSync(process.execPath, ['--check', jsFile]);
  }
  console.log(`[OK] ${jsFiles.length} archivos JS validados sin errores sintacticos.`);

  console.log('[TEST] 2. Validando regla CERO EMOJIS en toda la carpeta /pc-app...');
  const textExtensions = new Set(['.js', '.html', '.css', '.json', '.md', '.bat']);
  for (const file of allFiles) {
    const ext = path.extname(file).toLowerCase();
    if (textExtensions.has(ext)) {
      const content = fs.readFileSync(file, 'utf8');
      assert.strictEqual(
        EMOJI_REGEX.test(content),
        false,
        `Se encontro un emoji prohibido en el archivo: ${file}`
      );
    }
  }
  console.log('[OK] Cero emojis detectados en todos los archivos de /pc-app.');

  console.log('[TEST] 3. Validando configuracion por defecto (API_CONTRACT.md seccion 1)...');
  const cfg = loadConfig();
  assert.strictEqual(cfg.host, '51.161.47.99');
  assert.strictEqual(cfg.puerto, 54892);
  assert.strictEqual(cfg.url_base, 'http://51.161.47.99:54892/api/v1');
  assert.strictEqual(cfg.header_token, 'X-App-Private-Token');
  assert.strictEqual(cfg.token_privado, 'SZ6ydSFNmkAbrFLLrRahSEVxGnLkvABw');
  assert.strictEqual(cfg.token_privado.length, 32);
  assert.strictEqual(cfg.timeout_general_ms, 15000);
  assert.strictEqual(cfg.timeout_ia_ms, 90000);
  console.log('[OK] Configuracion preestablecida verificada.');

  console.log('[TEST] 4. Validando aritmetica de centavos y redondeo mitad hacia arriba (API_CONTRACT.md 4.3 y 6.5)...');
  const { calcSubtotalCentavos, parseDecimalToCentavos, formatCentavos, containsEmoji } = global.AppUtils;
  assert.strictEqual(parseDecimalToCentavos('12.50'), 1250);
  assert.strictEqual(parseDecimalToCentavos('19.99'), 1999);
  assert.strictEqual(parseDecimalToCentavos('650'), 65000);

  // Caso canonico 6.5 del contrato:
  // Leche: 2 * 125 = 250; Carne: 1.5 * 850 = 1275; Detergente: 1 * 3050 = 3050; Total = 4575
  const sub1 = calcSubtotalCentavos(2, 125);
  const sub2 = calcSubtotalCentavos(1.5, 850);
  const sub3 = calcSubtotalCentavos(1, 3050);
  assert.strictEqual(sub1, 250);
  assert.strictEqual(sub2, 1275);
  assert.strictEqual(sub3, 3050);
  assert.strictEqual(sub1 + sub2 + sub3, 4575);

  // Redondeo mitad hacia arriba: 1.25 * 333 = 416.25 -> 416; 1.5 * 333 = 499.5 -> 500
  assert.strictEqual(calcSubtotalCentavos(1.25, 333), 416);
  assert.strictEqual(calcSubtotalCentavos(1.5, 333), 500);

  // Formateo segun UI_DESIGN.md 2.3
  assert.strictEqual(formatCentavos(125000, 'INGRESO', 'COP'), '+ 1,250.00');
  assert.strictEqual(formatCentavos(125000, 'GASTO', 'COP'), '- 1,250.00');
  assert.strictEqual(formatCentavos(125000, 'BALANCE', 'COP'), '1,250.00');
  assert.strictEqual(formatCentavos(-125000, 'BALANCE', 'COP'), '- 1,250.00');
  assert.strictEqual(formatCentavos(125000, 'TOTAL_MONEDA', 'COP'), 'COP 1,250.00');

  // Detector anti-emoji en cliente
  assert.strictEqual(containsEmoji('Texto contable valido'), false);
  assert.strictEqual(containsEmoji('Texto con \u{1F600}'), true);
  console.log('[OK] Aritmetica de centavos, formateo y filtro anti-emojis verificados.');

  console.log('[TEST] 5. Validando servidor local de escritorio y activos estaticos...');
  const { server, url } = await startLocalServer(0);
  try {
    const resHtml = await fetch(`${url}/`);
    assert.strictEqual(resHtml.status, 200);
    const htmlText = await resHtml.text();
    assert(htmlText.includes('css/all.min.css'), 'Debe incluir FontAwesome local');
    assert(htmlText.includes('chart.js'), 'Debe incluir Chart.js');

    const resCfg = await fetch(`${url}/local-config`);
    assert.strictEqual(resCfg.status, 200);
    const cfgJson = await resCfg.json();
    assert.strictEqual(cfgJson.ok, true);
    assert.strictEqual(cfgJson.data.host, '51.161.47.99');
    assert.strictEqual(cfgJson.data.puerto, 54892);
    assert.strictEqual(cfgJson.data.token_privado, DEFAULT_CONFIG.token_privado);
  } finally {
    server.close();
  }
  console.log('[OK] Servidor local y endpoints de configuracion verificados.');

  console.log('[TEST] 6. Validando cliente HTTP contra servidor mock del contrato API...');
  let receivedHeaders = null;
  let receivedBody = null;
  const mockApiServer = http.createServer((req, res) => {
    receivedHeaders = req.headers;
    const chunks = [];
    req.on('data', (c) => chunks.push(c));
    req.on('end', () => {
      receivedBody = chunks.length ? JSON.parse(Buffer.concat(chunks).toString('utf8')) : null;
      res.writeHead(200, {
        'Content-Type': 'application/json; charset=utf-8',
        'Access-Control-Allow-Origin': '*'
      });
      res.end(JSON.stringify({
        ok: true,
        data: { estado: 'OK', version_api: '1.0.0', moneda: 'COP', hora_servidor: '2026-11-04T02:14:40Z' },
        meta: null
      }));
    });
  });

  await new Promise((resolve) => mockApiServer.listen(0, '127.0.0.1', resolve));
  const mockPort = mockApiServer.address().port;

  try {
    await global.AppConfig.updateConfig({ host: '127.0.0.1', puerto: mockPort });
    await global.ApiClient.createCompra({
      naturaleza: 'VARIABLE',
      fecha: '2026-11-03',
      comercio: 'Supermaxi',
      descripcion: 'Compra semanal',
      categoria_id: 11,
      metodo_pago: 'DEBITO',
      notas: null,
      articulos: [
        { articulo: 'Leche entera 1L', cantidad: 2, unidad: 'UNIDAD', precio_unitario_centavos: 125 }
      ]
    });

    assert.strictEqual(
      receivedHeaders['x-app-private-token'],
      'SZ6ydSFNmkAbrFLLrRahSEVxGnLkvABw',
      'El cliente debe enviar siempre el header X-App-Private-Token'
    );
    assert.strictEqual(receivedBody.comercio, 'Supermaxi');
    assert.strictEqual(receivedBody.articulos[0].precio_unitario_centavos, 125);
  } finally {
    await global.AppConfig.resetToDefault();
    mockApiServer.close();
  }
  console.log('[OK] Cliente HTTP envia X-App-Private-Token y payloads canonicos correctamente.');

  console.log('[TEST] 7. Validando sistema de temas (Modo Oscuro por defecto, paleta oficial y Chart.js)...');
  const { resolveAppTheme, getChartThemeConfig, THEME_STORAGE_KEY } = global.AppUtils;

  // 7.1 Persistencia y valores por defecto
  assert.strictEqual(THEME_STORAGE_KEY, 'app_theme', 'La clave de persistencia debe ser app_theme');
  assert.strictEqual(resolveAppTheme(null), 'dark', 'Si no hay valor guardado, debe retornar dark por defecto');
  assert.strictEqual(resolveAppTheme(undefined), 'dark', 'Si es undefined, debe retornar dark');
  assert.strictEqual(resolveAppTheme(''), 'dark', 'Si es string vacio, debe retornar dark');
  assert.strictEqual(resolveAppTheme('random_value'), 'dark', 'Valores desconocidos deben caer a dark');
  assert.strictEqual(resolveAppTheme('light'), 'light', 'light debe retornar light');
  assert.strictEqual(resolveAppTheme('LIGHT'), 'light', 'Debe ignorar mayusculas y retornar light');

  // 7.2 Configuracion de colores para Chart.js
  const darkChartCfg = getChartThemeConfig('dark');
  assert.strictEqual(darkChartCfg.gridColor, '#2D3748', 'Cuadricula oscura debe ser #2D3748');
  assert.strictEqual(darkChartCfg.textColor, '#94A3B8', 'Texto secundario oscuro debe ser #94A3B8');
  assert.strictEqual(darkChartCfg.ingresoColor, '#38A169', 'Ingresos oscuro debe ser #38A169');
  assert.strictEqual(darkChartCfg.gastoColor, '#E53E3E', 'Gastos oscuro debe ser #E53E3E');
  assert.strictEqual(darkChartCfg.balanceColor, '#2B6CB0', 'Balance / acento institucional oscuro debe ser #2B6CB0');
  assert.strictEqual(darkChartCfg.emptyBarColor, '#2D3748', 'Barra vacia oscura debe ser #2D3748');

  const lightChartCfg = getChartThemeConfig('light');
  assert.strictEqual(lightChartCfg.gridColor, '#D9DEE5', 'Cuadricula clara debe ser #D9DEE5');
  assert.strictEqual(lightChartCfg.textColor, '#4A5568', 'Texto claro debe ser #4A5568');
  assert.strictEqual(lightChartCfg.ingresoColor, '#2E7D32', 'Ingresos claro debe ser #2E7D32');
  assert.strictEqual(lightChartCfg.gastoColor, '#C62828', 'Gastos claro debe ser #C62828');
  assert.strictEqual(lightChartCfg.balanceColor, '#1B2A4A', 'Balance claro debe ser #1B2A4A');
  assert.strictEqual(lightChartCfg.emptyBarColor, '#D9DEE5', 'Barra vacia clara debe ser #D9DEE5');

  // 7.3 Verificacion de variables CSS en styles.css
  const cssPath = path.join(pcAppRoot, 'public', 'css', 'styles.css');
  const cssContent = fs.readFileSync(cssPath, 'utf8');
  assert(cssContent.includes(':root'), 'styles.css debe contener :root');
  assert(cssContent.includes('[data-theme="light"]'), 'styles.css debe contener [data-theme="light"]');
  assert(cssContent.includes('--color-fondo: #12161F;'), 'styles.css debe definir fondo oscuro #12161F');
  assert(cssContent.includes('--color-superficie: #1A202C;'), 'styles.css debe definir superficie oscura #1A202C');
  assert(cssContent.includes('--color-tabla-zebra: #222938;'), 'styles.css debe definir filas zebra oscura #222938');
  assert(cssContent.includes('--color-borde: #2D3748;'), 'styles.css debe definir bordes oscuros #2D3748');
  assert(cssContent.includes('--color-texto: #E2E8F0;'), 'styles.css debe definir texto principal oscuro #E2E8F0');
  assert(cssContent.includes('--color-texto-secundario: #94A3B8;'), 'styles.css debe definir texto secundario oscuro #94A3B8');
  assert(cssContent.includes('--color-primario: #2B6CB0;'), 'styles.css debe definir azul institucional #2B6CB0');
  assert(cssContent.includes('--color-primario-hover: #3182CE;'), 'styles.css debe definir hover institucional #3182CE');
  assert(cssContent.includes('--color-exito: #38A169;'), 'styles.css debe definir verde contable #38A169');
  assert(cssContent.includes('--color-peligro: #E53E3E;'), 'styles.css debe definir rojo contable #E53E3E');

  // 7.4 Verificacion de componentes en index.html
  const htmlPath = path.join(pcAppRoot, 'public', 'index.html');
  const htmlContent = fs.readFileSync(htmlPath, 'utf8');
  assert(htmlContent.includes('id="btn-topbar-theme"'), 'index.html debe contener el boton btn-topbar-theme');
  assert(htmlContent.includes('id="btn-cfg-theme-dark"'), 'index.html debe contener btn-cfg-theme-dark');
  assert(htmlContent.includes('id="btn-cfg-theme-light"'), 'index.html debe contener btn-cfg-theme-light');
  assert(htmlContent.includes('id="btn-cfg-theme-toggle"'), 'index.html debe contener btn-cfg-theme-toggle');
  assert(htmlContent.includes('id="diag-tema"'), 'index.html debe contener diag-tema en diagnostico');
  assert(htmlContent.includes('data-theme'), 'index.html debe aplicar data-theme en el script inicial');
  console.log('[OK] Sistema de temas, persistencia y variables CSS validados correctamente.');

  console.log('[TEST] 8. Validando modulo de Tarjetas de Credito y Sistema de Cuotas...');
  const { simulateCreditPurchase } = global.AppUtils;

  // 8.1 Validacion de calculos de simulacion
  const simExenta = simulateCreditPurchase(1200000, 12, 2.5, true);
  assert.strictEqual(simExenta.is_interest_exempt, true);
  assert.strictEqual(simExenta.total_interest_amount, 0);
  assert.strictEqual(simExenta.monthly_installment_amount, 100000);
  assert.strictEqual(simExenta.total_with_interest, 1200000);

  const simConInteres = simulateCreditPurchase(1000000, 6, 2.15, false);
  assert.strictEqual(simConInteres.is_interest_exempt, false);
  assert(simConInteres.total_interest_amount > 0, 'Debe generar intereses positivos');
  assert(simConInteres.monthly_installment_amount > 166666, 'Cuota debe ser superior al capital dividido');
  assert.strictEqual(
    Number((simConInteres.amount + simConInteres.total_interest_amount).toFixed(2)),
    simConInteres.total_with_interest,
    'Costo total debe ser capital mas intereses'
  );

  const simUnaCuota = simulateCreditPurchase(500000, 1, 2.15, false);
  assert.strictEqual(simUnaCuota.monthly_installment_amount, 500000);
  assert.strictEqual(simUnaCuota.total_interest_amount, 0);
  assert.strictEqual(simUnaCuota.total_with_interest, 500000);

  // 8.2 Validacion de estructura en index.html y FontAwesome icons requeridos
  assert(htmlContent.includes('id="view-tarjetas"'), 'index.html debe contener view-tarjetas');
  assert(htmlContent.includes('id="form-tarjeta"'), 'index.html debe contener form-tarjeta');
  assert(htmlContent.includes('id="tx-panel-credito"'), 'index.html debe contener tx-panel-credito');
  assert(htmlContent.includes('id="compra-panel-credito"'), 'index.html debe contener compra-panel-credito');
  assert(htmlContent.includes('id="tx-simulador-box"'), 'index.html debe contener tx-simulador-box');
  assert(htmlContent.includes('id="compra-simulador-box"'), 'index.html debe contener compra-simulador-box');
  assert(htmlContent.includes('id="modal-pago-tarjeta"'), 'index.html debe contener modal-pago-tarjeta');
  assert(htmlContent.includes('id="form-pago-tarjeta"'), 'index.html debe contener form-pago-tarjeta');
  assert(htmlContent.includes('id="pago-tarjeta-amount"'), 'index.html debe contener pago-tarjeta-amount');
  assert(htmlContent.includes('id="pago-tarjeta-method"'), 'index.html debe contener pago-tarjeta-method');
  assert(htmlContent.includes('id="pago-tarjeta-date"'), 'index.html debe contener pago-tarjeta-date');
  assert(htmlContent.includes('id="pago-tarjeta-create-tx"'), 'index.html debe contener pago-tarjeta-create-tx');
  assert(htmlContent.includes('fa-solid fa-credit-card'), 'Debe usar fa-solid fa-credit-card');
  assert(htmlContent.includes('fa-solid fa-percent'), 'Debe usar fa-solid fa-percent');
  assert(htmlContent.includes('fa-solid fa-calendar-days'), 'Debe usar fa-solid fa-calendar-days');
  assert(htmlContent.includes('fa-solid fa-shield-halved'), 'Debe usar fa-solid fa-shield-halved');
  assert(htmlContent.includes('fa-money-bill-wave'), 'Debe usar fa-money-bill-wave');

  const appJsContent = fs.readFileSync(path.join(pcAppRoot, 'public', 'js', 'app.js'), 'utf8');
  assert(appJsContent.includes('fa-calendar-check'), 'app.js debe usar icono fa-calendar-check para limite de pago');
  assert(appJsContent.includes('fa-calendar'), 'app.js debe usar icono fa-calendar para proximo corte');
  assert(appJsContent.includes('#D97706'), 'app.js debe incluir advertencia ambar #D97706');

  // 8.3 Validacion de utilidades de fecha y moneda para tarjetas
  const { formatCurrency, getDaysUntilDate } = global.AppUtils;
  assert.strictEqual(typeof formatCurrency, 'function', 'formatCurrency debe ser una funcion');
  assert.strictEqual(typeof getDaysUntilDate, 'function', 'getDaysUntilDate debe ser una funcion');
  assert(formatCurrency(150000).includes('150,000.00'), 'formatCurrency debe formatear monto contable');

  const d2 = new Date();
  d2.setDate(d2.getDate() + 2);
  const d2Str = `${d2.getFullYear()}-${String(d2.getMonth() + 1).padStart(2, '0')}-${String(d2.getDate()).padStart(2, '0')}`;
  const daysDiff = getDaysUntilDate(d2Str);
  assert.strictEqual(daysDiff, 2, 'getDaysUntilDate debe calcular 2 dias de diferencia');
  assert(daysDiff <= 3, 'Fecha a 2 dias debe activar advertencia ambar (<= 3 dias)');

  // 8.4 Validacion de endpoints de ApiClient para credit-cards contra mock server
  const mockCardServer = http.createServer((req, res) => {
    let raw = '';
    req.on('data', (c) => { raw += c; });
    req.on('end', () => {
      const url = req.url || '';
      const method = req.method;
      const body = raw ? JSON.parse(raw) : {};

      if (url === '/api/v1/credit-cards' && method === 'GET') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          ok: true,
          data: [
            {
              id: 1,
              name: 'Visa Clásica',
              bank: 'Bancolombia',
              credit_limit: 5000000,
              available_limit: 4500000,
              default_interest_rate: 2.15,
              cutoff_day: 15,
              payment_day: 5,
              next_cutoff_date: '2026-11-15',
              next_payment_date: d2Str,
              minimum_payment: 50000,
              full_payment: 500000,
              total_debt: 500000
            }
          ]
        }));
        return;
      }

      if (url === '/api/v1/credit-cards' && method === 'POST') {
        res.writeHead(201, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ ok: true, data: { id: 2, ...body, available_limit: body.credit_limit } }));
        return;
      }

      if (url === '/api/v1/credit-cards/simulate' && method === 'POST') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          ok: true,
          data: {
            amount: body.amount,
            installments: body.installments,
            interest_rate: body.interest_rate,
            is_interest_exempt: body.is_interest_exempt,
            monthly_installment_amount: 179555.22,
            total_interest_amount: 77331.32,
            total_with_interest: 1077331.32,
            amortization_table: []
          }
        }));
        return;
      }

      if (url === '/api/v1/credit-cards/1/payments' && method === 'POST') {
        assert(req.headers['x-app-private-token'], 'Debe enviar header X-App-Private-Token');
        res.writeHead(201, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          ok: true,
          data: {
            id: 101,
            card_id: 1,
            amount: body.amount,
            payment_type: body.payment_type,
            payment_method: body.payment_method,
            date: body.date,
            transaction_id: body.create_transaction ? 501 : null
          }
        }));
        return;
      }

      if (url === '/api/v1/credit-cards/1/payments' && method === 'GET') {
        assert(req.headers['x-app-private-token'], 'Debe enviar header X-App-Private-Token');
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          ok: true,
          data: [
            {
              id: 101,
              card_id: 1,
              amount: 50000,
              payment_type: 'MINIMO',
              payment_method: 'DEBITO',
              date: '2026-11-04'
            }
          ]
        }));
        return;
      }

      if (url.startsWith('/api/v1/credit-cards/1') && method === 'DELETE') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ ok: true, data: { id: 1, eliminado: true } }));
        return;
      }

      res.writeHead(404, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ ok: false, error: { codigo: 'NO_ENCONTRADO' } }));
    });
  });

  await new Promise((resolve) => mockCardServer.listen(0, '127.0.0.1', resolve));
  const cardPort = mockCardServer.address().port;

  try {
    await global.AppConfig.updateConfig({
      host: '127.0.0.1',
      puerto: cardPort
    });

    const listRes = await global.ApiClient.getCreditCards();
    assert.strictEqual(listRes.ok, true);
    assert.strictEqual(listRes.data.length, 1);
    assert.strictEqual(listRes.data[0].bank, 'Bancolombia');

    const createRes = await global.ApiClient.createCreditCard({
      name: 'Mastercard Black',
      bank: 'Davivienda',
      credit_limit: 12000000,
      default_interest_rate: 1.95,
      cutoff_day: 20,
      payment_day: 10
    });
    assert.strictEqual(createRes.ok, true);
    assert.strictEqual(createRes.data.name, 'Mastercard Black');
    assert.strictEqual(createRes.data.credit_limit, 12000000);

    const simRes = await global.ApiClient.simulateCreditCard({
      amount: 1000000,
      installments: 6,
      interest_rate: 2.15,
      is_interest_exempt: false
    });
    assert.strictEqual(simRes.ok, true);
    assert.strictEqual(simRes.data.monthly_installment_amount, 179555.22);
    assert.strictEqual(simRes.data.total_interest_amount, 77331.32);

    // Registro de pago de tarjeta (API_CONTRACT.md 11.2)
    assert.strictEqual(typeof global.api.creditCards.pay, 'function');
    assert.strictEqual(typeof global.api.creditCards.getPayments, 'function');

    const payRes = await global.api.creditCards.pay(1, {
      amount: 150000,
      payment_type: 'MINIMO',
      payment_method: 'DEBITO',
      date: '2026-11-04',
      notes: 'Pago cuota sin emojis',
      create_transaction: true
    });
    assert.strictEqual(payRes.ok, true);
    assert.strictEqual(payRes.data.amount, 150000);
    assert.strictEqual(payRes.data.payment_type, 'MINIMO');
    assert.strictEqual(payRes.data.transaction_id, 501);

    const histRes = await global.api.creditCards.getPayments(1);
    assert.strictEqual(histRes.ok, true);
    assert.strictEqual(histRes.data.length, 1);
    assert.strictEqual(histRes.data[0].amount, 50000);

    const delRes = await global.ApiClient.deleteCreditCard(1);
    assert.strictEqual(delRes.ok, true);
    assert.strictEqual(delRes.data.eliminado, true);
  } finally {
    await global.AppConfig.resetToDefault();
    mockCardServer.close();
  }

  console.log('[OK] Modulo de Tarjetas de Credito y Simulador contable validados correctamente.');

  console.log('\n=== TODAS LAS PRUEBAS DE CONFORMIDAD SUPERADAS EXITOSAMENTE ===');
}

runTests().catch((err) => {
  console.error('[FALLO EN PRUEBAS]:', err);
  process.exit(1);
});
