'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');

const testDbPath = path.resolve(__dirname, '..', 'data', 'test_cards.db');
for (const ext of ['', '-wal', '-shm']) {
  if (fs.existsSync(testDbPath + ext)) fs.unlinkSync(testDbPath + ext);
}
process.env.APP_DB_PATH = testDbPath;
process.env.PORT = '54894';

const config = require('../src/config');
const { getDb } = require('../src/db');
const { handleRequest } = require('../src/app');
const {
  getFirstPaymentDate,
  getInstallmentDueDates,
  countPaidInstallments,
  getCardBillingCycle
} = require('../src/utils/credit');

const BASE = `http://127.0.0.1:${config.PORT}/api/v1`;

async function api(method, route, body, token = config.APP_PRIVATE_TOKEN) {
  const headers = {};
  if (token) headers['X-App-Private-Token'] = token;
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const res = await fetch(`${BASE}${route}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  return { status: res.status, json: await res.json() };
}

async function main() {
  getDb();
  const server = http.createServer((req, res) => handleRequest(req, res));
  await new Promise((r) => server.listen(config.PORT, '127.0.0.1', r));
  try {
    // 0. Pruebas unitarias directas de ciclos de corte y limite de pago
    // Caso 1: corte 20, pago 10 (corte > pago -> toma el mes siguiente)
    assert.equal(getFirstPaymentDate('2026-03-05', 20, 10), '2026-04-10'); // dia 5 <= 20
    assert.equal(getFirstPaymentDate('2026-03-25', 20, 10), '2026-05-10'); // dia 25 > 20
    const dues = getInstallmentDueDates('2026-03-05', 3, 20, 10);
    assert.deepEqual(dues, ['2026-04-10', '2026-05-10', '2026-06-10']);
    assert.equal(countPaidInstallments('2026-03-05', 3, 20, 10, '2026-04-09'), 0);
    assert.equal(countPaidInstallments('2026-03-05', 3, 20, 10, '2026-04-10'), 1);
    assert.equal(countPaidInstallments('2026-03-05', 3, 20, 10, '2026-05-10'), 2);
    assert.equal(countPaidInstallments('2026-03-05', 3, 20, 10, '2026-06-10'), 3);

    // Caso 2: corte 5, pago 25 (corte < pago -> mismo mes de corte)
    assert.equal(getFirstPaymentDate('2026-03-02', 5, 25), '2026-03-25');
    assert.equal(getFirstPaymentDate('2026-03-10', 5, 25), '2026-04-25');

    // Token obligatorio
    assert.equal((await api('GET', '/credit-cards', undefined, null)).status, 401);
    assert.equal((await api('POST', '/credit-cards/simulate', {}, null)).status, 401);
    assert.equal((await api('POST', '/credit-cards/1/payments', {}, null)).status, 401);

    // Simulacion exenta
    let r = await api('POST', '/credit-cards/simulate', {
      amount: 1200, installments: 12, interest_rate: 2.5, is_interest_exempt: true
    });
    assert.equal(r.status, 200);
    assert.equal(r.json.data.monthly_installment_amount, 100);
    assert.equal(r.json.data.total_interest_amount, 0);
    assert.equal(r.json.data.total_with_interest, 1200);

    // Simulacion con interes
    r = await api('POST', '/credit-cards/simulate', {
      amount: 1000000, installments: 12, interest_rate: 2, is_interest_exempt: false
    });
    assert.equal(r.status, 200);
    const d = r.json.data;
    assert.equal(d.monthly_installment_amount, 94559.6);
    assert.equal(d.amortization_table.length, 12);
    assert.equal(d.amortization_table[0].interest, 20000);
    assert.equal(d.amortization_table[11].remaining_balance, 0);
    const sumP = d.amortization_table.reduce((a, x) => a + x.principal, 0);
    assert.ok(Math.abs(sumP - 1000000) < 0.005);
    assert.ok(Math.abs(d.total_with_interest - 1000000 - d.total_interest_amount) < 0.005);

    // Tasa 0 equivale a exenta
    r = await api('POST', '/credit-cards/simulate', {
      amount: 90, installments: 3, interest_rate: 0, is_interest_exempt: false
    });
    assert.equal(r.json.data.total_interest_amount, 0);

    // Validaciones de simulador
    assert.equal((await api('POST', '/credit-cards/simulate', { amount: 0, installments: 1 })).status, 422);
    assert.equal((await api('POST', '/credit-cards/simulate', { amount: 10, installments: 49 })).status, 422);

    // CRUD de tarjetas (creando tarjeta con corte 20 y pago 10)
    r = await api('POST', '/credit-cards', {
      name: 'Visa Clasica', bank: 'Banco X', credit_limit: 5000000,
      default_interest_rate: 2.15, cutoff_day: 20, payment_day: 10
    });
    assert.equal(r.status, 201);
    const card = r.json.data;
    assert.deepEqual(Object.keys(card).sort(), [
      'available_limit', 'bank', 'credit_limit', 'cutoff_day', 'default_interest_rate',
      'full_payment', 'id', 'minimum_payment', 'name', 'next_cutoff_date', 'next_payment_date',
      'payment_day', 'total_debt'
    ]);
    assert.equal(card.available_limit, 5000000);
    assert.equal(card.total_debt, 0);
    assert.equal(card.minimum_payment, 0);
    assert.equal(card.full_payment, 0);
    assert.ok(card.next_cutoff_date);
    assert.ok(card.next_payment_date);

    assert.equal((await api('POST', '/credit-cards', { name: 'x' })).status, 422);
    assert.equal((await api('POST', '/credit-cards', {
      name: 'Visa', bank: 'B', credit_limit: 1, default_interest_rate: 1, cutoff_day: 32, payment_day: 1
    })).status, 422);

    // Transaccion con tarjeta, tasa por defecto
    const today = new Date().toISOString().slice(0, 10);
    const base = {
      tipo: 'GASTO', naturaleza: 'VARIABLE', monto_centavos: 100000000, fecha: today,
      descripcion: 'Televisor', categoria_id: 22, payment_method: 'CREDIT_CARD', card_id: card.id
    };
    r = await api('POST', '/transactions', { ...base, installments: 12 });
    assert.equal(r.status, 201, JSON.stringify(r.json));
    assert.equal(r.json.data.applied_interest_rate, 2.15);
    assert.equal(r.json.data.payment_method, 'CREDIT_CARD');
    assert.equal(r.json.data.is_interest_exempt, false);
    assert.ok(r.json.data.total_interest_amount > 0);
    assert.ok(
      Math.abs(r.json.data.total_with_interest - 1000000 - r.json.data.total_interest_amount) < 0.005
    );

    // Compra exenta fuerza tasa 0
    r = await api('POST', '/transactions', {
      ...base, monto_centavos: 60000, installments: 3, is_interest_exempt: true, applied_interest_rate: 5
    });
    assert.equal(r.status, 201);
    assert.equal(r.json.data.applied_interest_rate, 0);
    assert.equal(r.json.data.total_interest_amount, 0);
    assert.equal(r.json.data.monthly_installment_amount, 200);
    assert.equal(r.json.data.total_with_interest, 600);

    // Tarjeta inexistente y card_id faltante
    assert.equal((await api('POST', '/transactions', { ...base, card_id: 999 })).status, 422);
    const noCard = { ...base };
    delete noCard.card_id;
    assert.equal((await api('POST', '/transactions', noCard)).status, 422);
    assert.equal((await api('POST', '/transactions', { ...base, installments: 60 })).status, 422);

    // Transaccion normal sin campos de tarjeta
    r = await api('POST', '/transactions', {
      ...base, payment_method: undefined, metodo_pago: 'EFECTIVO', card_id: undefined
    });
    assert.equal(r.status, 201);
    assert.equal(r.json.data.card_id, null);

    // Cupo disponible refleja la deuda y pagos minimos / totales calculados
    r = await api('GET', `/credit-cards/${card.id}`);
    assert.equal(r.status, 200);
    const cardUpdated = r.json.data;
    assert.ok(cardUpdated.available_limit < 5000000);
    assert.ok(cardUpdated.total_debt > 0);
    assert.ok(cardUpdated.next_cutoff_date);
    assert.ok(cardUpdated.next_payment_date);

    // Listado de transacciones con campos de tarjeta
    r = await api('GET', '/transactions?metodo_pago=CREDIT_CARD');
    assert.equal(r.json.data.length, 2);

    // Registrar pago a tarjeta (Pagar Minimo / Total)
    r = await api('POST', `/credit-cards/${card.id}/payments`, {
      amount: 50000,
      payment_type: 'MINIMO',
      payment_method: 'DEBITO',
      notes: 'Pago minimo mensual de prueba'
    });
    assert.equal(r.status, 201);
    assert.equal(r.json.data.amount, 50000);
    assert.equal(r.json.data.payment_type, 'MINIMO');
    assert.ok(r.json.data.transaction_id > 0);

    // Listar pagos de tarjeta
    r = await api('GET', `/credit-cards/${card.id}/payments`);
    assert.equal(r.status, 200);
    assert.equal(r.json.data.length, 1);
    assert.equal(r.json.data[0].amount, 50000);

    // No se puede eliminar tarjeta en uso ni con pagos
    assert.equal((await api('DELETE', `/credit-cards/${card.id}`)).status, 409);

    // PUT y DELETE de tarjeta libre
    r = await api('POST', '/credit-cards', {
      name: 'Master', bank: 'Banco Y', credit_limit: 1000, default_interest_rate: 0, cutoff_day: 1, payment_day: 5
    });
    const id2 = r.json.data.id;
    r = await api('PUT', `/credit-cards/${id2}`, {
      name: 'Master Oro', bank: 'Banco Y', credit_limit: 2000, default_interest_rate: 1.5, cutoff_day: 2, payment_day: 6
    });
    assert.equal(r.status, 200);
    assert.equal(r.json.data.credit_limit, 2000);
    assert.equal((await api('DELETE', `/credit-cards/${id2}`)).status, 200);
    assert.equal((await api('GET', `/credit-cards/${id2}`)).status, 404);

    console.log('verify-credit-cards: OK');
  } finally {
    server.close();
  }
}

main().then(
  () => process.exit(0),
  (err) => {
    console.error(err);
    process.exit(1);
  }
);
