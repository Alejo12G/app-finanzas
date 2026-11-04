'use strict';

const { getDb } = require('../db');
const { AppError, sendSuccess } = require('../utils/response');
const { nowIsoUtc, checkNoEmojisInFields, isValidDateStr } = require('../utils/validators');
const {
  MAX_INSTALLMENTS,
  fromCents,
  toCents,
  simulate,
  getCardDebtSummary
} = require('../utils/credit');

function formatCard(card, cardSummary) {
  const limitCents = Number(card.credit_limit_centavos);
  const debtCents = cardSummary ? cardSummary.debt_cents : 0;
  return {
    id: Number(card.id),
    name: card.name,
    bank: card.bank,
    credit_limit: fromCents(limitCents),
    available_limit: fromCents(Math.max(0, limitCents - debtCents)),
    default_interest_rate: Number(card.default_interest_rate),
    cutoff_day: Number(card.cutoff_day),
    payment_day: Number(card.payment_day),
    next_cutoff_date: cardSummary && cardSummary.cycle ? cardSummary.cycle.next_cutoff_date : null,
    next_payment_date: cardSummary && cardSummary.cycle ? cardSummary.cycle.next_payment_date : null,
    minimum_payment: cardSummary ? fromCents(cardSummary.minimum_payment_cents) : 0,
    full_payment: cardSummary ? fromCents(cardSummary.full_payment_cents) : 0,
    total_debt: fromCents(debtCents)
  };
}

function summaryMap(db) {
  const map = new Map();
  for (const entry of getCardDebtSummary(db)) {
    map.set(Number(entry.card.id), entry);
  }
  return map;
}

function isIntInRange(v, min, max) {
  return typeof v === 'number' && Number.isInteger(v) && v >= min && v <= max;
}

function isFiniteNumber(v) {
  return typeof v === 'number' && Number.isFinite(v);
}

function validateCardPayload(body) {
  checkNoEmojisInFields({ name: body.name, bank: body.bank });
  const detalles = [];

  const name = typeof body.name === 'string' ? body.name.trim() : '';
  if (name.length < 1 || name.length > 80) {
    detalles.push({ campo: 'name', mensaje: 'Obligatorio, entre 1 y 80 caracteres.' });
  }
  const bank = typeof body.bank === 'string' ? body.bank.trim() : '';
  if (bank.length < 1 || bank.length > 80) {
    detalles.push({ campo: 'bank', mensaje: 'Obligatorio, entre 1 y 80 caracteres.' });
  }
  if (!isFiniteNumber(body.credit_limit) || body.credit_limit <= 0 || body.credit_limit > 999999999999) {
    detalles.push({ campo: 'credit_limit', mensaje: 'Debe ser un numero mayor a 0.' });
  }
  if (
    !isFiniteNumber(body.default_interest_rate) ||
    body.default_interest_rate < 0 ||
    body.default_interest_rate > 100
  ) {
    detalles.push({ campo: 'default_interest_rate', mensaje: 'Debe ser un numero entre 0 y 100.' });
  }
  if (!isIntInRange(body.cutoff_day, 1, 31)) {
    detalles.push({ campo: 'cutoff_day', mensaje: 'Debe ser un entero entre 1 y 31.' });
  }
  if (!isIntInRange(body.payment_day, 1, 31)) {
    detalles.push({ campo: 'payment_day', mensaje: 'Debe ser un entero entre 1 y 31.' });
  }
  if (detalles.length > 0) {
    throw new AppError(422, 'VALIDACION_FALLIDA', 'Uno o mas campos no son validos.', detalles);
  }
  return {
    name,
    bank,
    credit_limit_centavos: toCents(body.credit_limit),
    default_interest_rate: body.default_interest_rate,
    cutoff_day: body.cutoff_day,
    payment_day: body.payment_day
  };
}

function fetchCard(db, id) {
  const card = db.prepare('SELECT * FROM credit_cards WHERE id = ?').get(id);
  if (!card) {
    throw new AppError(404, 'RECURSO_NO_ENCONTRADO', 'La tarjeta solicitada no existe.');
  }
  return card;
}

function listCards(req, res) {
  const db = getDb();
  const summaries = summaryMap(db);
  const rows = db.prepare('SELECT * FROM credit_cards ORDER BY id ASC').all();
  sendSuccess(res, 200, rows.map((c) => formatCard(c, summaries.get(Number(c.id)))), null);
}

function getCard(req, res, id) {
  const db = getDb();
  const card = fetchCard(db, id);
  sendSuccess(res, 200, formatCard(card, summaryMap(db).get(id)), null);
}

function createCard(req, res, body) {
  const db = getDb();
  const v = validateCardPayload(body);
  const now = nowIsoUtc();
  const result = db
    .prepare(`
      INSERT INTO credit_cards (
        name, bank, credit_limit_centavos, default_interest_rate,
        cutoff_day, payment_day, creado_en, actualizado_en
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    `)
    .run(v.name, v.bank, v.credit_limit_centavos, v.default_interest_rate, v.cutoff_day, v.payment_day, now, now);
  const id = Number(result.lastInsertRowid);
  sendSuccess(res, 201, formatCard(fetchCard(db, id), summaryMap(db).get(id)), null);
}

function updateCard(req, res, id, body) {
  const db = getDb();
  fetchCard(db, id);
  const v = validateCardPayload(body);
  db.prepare(`
    UPDATE credit_cards
    SET name = ?, bank = ?, credit_limit_centavos = ?, default_interest_rate = ?,
        cutoff_day = ?, payment_day = ?, actualizado_en = ?
    WHERE id = ?
  `).run(v.name, v.bank, v.credit_limit_centavos, v.default_interest_rate, v.cutoff_day, v.payment_day, nowIsoUtc(), id);
  sendSuccess(res, 200, formatCard(fetchCard(db, id), summaryMap(db).get(id)), null);
}

function deleteCard(req, res, id) {
  const db = getDb();
  fetchCard(db, id);
  const inUse = db.prepare('SELECT COUNT(*) AS total FROM transacciones WHERE card_id = ?').get(id);
  if (Number(inUse.total) > 0) {
    throw new AppError(
      409,
      'TARJETA_EN_USO',
      'No se puede eliminar una tarjeta con transacciones asociadas.'
    );
  }
  const inPayments = db.prepare('SELECT COUNT(*) AS total FROM credit_card_payments WHERE card_id = ?').get(id);
  if (Number(inPayments.total) > 0) {
    throw new AppError(
      409,
      'TARJETA_EN_USO',
      'No se puede eliminar una tarjeta con pagos asociados.'
    );
  }
  db.prepare('DELETE FROM credit_cards WHERE id = ?').run(id);
  sendSuccess(res, 200, { id, eliminado: true }, null);
}

function simulateCard(req, res, body) {
  const detalles = [];
  if (!isFiniteNumber(body.amount) || body.amount <= 0 || body.amount > 999999999999) {
    detalles.push({ campo: 'amount', mensaje: 'Debe ser un numero mayor a 0.' });
  }
  if (!isIntInRange(body.installments, 1, MAX_INSTALLMENTS)) {
    detalles.push({ campo: 'installments', mensaje: `Debe ser un entero entre 1 y ${MAX_INSTALLMENTS}.` });
  }
  const exempt = body.is_interest_exempt === undefined ? false : body.is_interest_exempt;
  if (typeof exempt !== 'boolean') {
    detalles.push({ campo: 'is_interest_exempt', mensaje: 'Debe ser booleano.' });
  }
  let rate = body.interest_rate === undefined || body.interest_rate === null ? 0 : body.interest_rate;
  if (!isFiniteNumber(rate) || rate < 0 || rate > 100) {
    detalles.push({ campo: 'interest_rate', mensaje: 'Debe ser un numero entre 0 y 100.' });
  }
  if (detalles.length > 0) {
    throw new AppError(422, 'VALIDACION_FALLIDA', 'Uno o mas campos no son validos.', detalles);
  }
  sendSuccess(res, 200, simulate(body.amount, body.installments, rate, exempt), null);
}

function validatePaymentPayload(body) {
  checkNoEmojisInFields({ notes: body.notes });
  const detalles = [];

  if (!isFiniteNumber(body.amount) || body.amount <= 0 || body.amount > 999999999999) {
    detalles.push({ campo: 'amount', mensaje: 'Debe ser un numero mayor a 0.' });
  }

  const type = typeof body.payment_type === 'string' ? body.payment_type.trim().toUpperCase() : 'MINIMO';
  if (!['MINIMO', 'TOTAL', 'OTRO'].includes(type)) {
    detalles.push({ campo: 'payment_type', mensaje: 'Debe ser MINIMO, TOTAL u OTRO.' });
  }

  const method = typeof body.payment_method === 'string' ? body.payment_method.trim().toUpperCase() : 'DEBITO';
  if (!['EFECTIVO', 'DEBITO', 'TRANSFERENCIA', 'OTRO'].includes(method)) {
    detalles.push({ campo: 'payment_method', mensaje: 'Debe ser EFECTIVO, DEBITO, TRANSFERENCIA u OTRO.' });
  }

  let date = nowIsoUtc().slice(0, 10);
  if (body.date !== undefined && body.date !== null) {
    if (typeof body.date !== 'string' || !isValidDateStr(body.date.trim())) {
      detalles.push({ campo: 'date', mensaje: 'Formato de fecha invalido (YYYY-MM-DD).' });
    } else {
      date = body.date.trim();
    }
  }

  let notes = null;
  if (body.notes !== undefined && body.notes !== null) {
    if (typeof body.notes !== 'string') {
      detalles.push({ campo: 'notes', mensaje: 'Debe ser texto o null.' });
    } else {
      const trimmed = body.notes.trim();
      if (trimmed.length > 500) {
        detalles.push({ campo: 'notes', mensaje: 'Maximo 500 caracteres.' });
      } else if (trimmed.length > 0) {
        notes = trimmed;
      }
    }
  }

  const createTx = body.create_transaction !== false;

  if (detalles.length > 0) {
    throw new AppError(422, 'VALIDACION_FALLIDA', 'Uno o mas campos no son validos.', detalles);
  }

  return {
    amount: body.amount,
    monto_centavos: toCents(body.amount),
    payment_type: type,
    payment_method: method,
    date,
    notes,
    create_transaction: createTx
  };
}

function createCardPayment(req, res, cardId, body) {
  const db = getDb();
  const card = fetchCard(db, cardId);
  const v = validatePaymentPayload(body);
  const now = nowIsoUtc();

  let transactionId = null;
  if (v.create_transaction) {
    const cat = db.prepare("SELECT id FROM categorias WHERE id = 24 AND tipo = 'GASTO'").get()
      || db.prepare("SELECT id FROM categorias WHERE tipo = 'GASTO' AND activa = 1 LIMIT 1").get();
    const catId = cat ? Number(cat.id) : 24;

    const txResult = db.prepare(`
      INSERT INTO transacciones (
        tipo, naturaleza, monto_centavos, fecha, descripcion,
        categoria_id, metodo_pago, comercio, notas, es_compra,
        creado_en, actualizado_en
      ) VALUES ('GASTO', 'FIJO', ?, ?, ?, ?, ?, ?, ?, 0, ?, ?)
    `).run(
      v.monto_centavos,
      v.date,
      `Pago tarjeta ${card.name} (${v.payment_type})`,
      catId,
      v.payment_method,
      card.bank,
      v.notes,
      now,
      now
    );
    transactionId = Number(txResult.lastInsertRowid);
  }

  const result = db.prepare(`
    INSERT INTO credit_card_payments (
      card_id, monto_centavos, fecha, tipo_pago, metodo_pago,
      notas, transaccion_id, creado_en
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
  `).run(
    cardId,
    v.monto_centavos,
    v.date,
    v.payment_type,
    v.payment_method,
    v.notes,
    transactionId,
    now
  );

  const paymentId = Number(result.lastInsertRowid);
  sendSuccess(res, 201, {
    id: paymentId,
    card_id: cardId,
    amount: v.amount,
    payment_type: v.payment_type,
    payment_method: v.payment_method,
    date: v.date,
    notes: v.notes,
    transaction_id: transactionId,
    created_at: now
  }, null);
}

function listCardPayments(req, res, cardId) {
  const db = getDb();
  fetchCard(db, cardId);
  const rows = db.prepare(`
    SELECT id, card_id, monto_centavos, fecha, tipo_pago, metodo_pago,
           notas, transaccion_id, creado_en
    FROM credit_card_payments
    WHERE card_id = ?
    ORDER BY fecha DESC, id DESC
  `).all(cardId);

  sendSuccess(res, 200, rows.map((r) => ({
    id: Number(r.id),
    card_id: Number(r.card_id),
    amount: fromCents(r.monto_centavos),
    payment_type: r.tipo_pago,
    payment_method: r.metodo_pago,
    date: r.fecha,
    notes: r.notas,
    transaction_id: r.transaccion_id ? Number(r.transaccion_id) : null,
    created_at: r.creado_en
  })), null);
}

module.exports = {
  listCards,
  getCard,
  createCard,
  updateCard,
  deleteCard,
  simulateCard,
  createCardPayment,
  listCardPayments
};
