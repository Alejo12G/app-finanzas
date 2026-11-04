'use strict';

const { getDb } = require('../db');
const { AppError, sendSuccess } = require('../utils/response');
const { nowIsoUtc, checkNoEmojisInFields } = require('../utils/validators');
const { MAX_INSTALLMENTS, fromCents, toCents, simulate, getCardDebtSummary } = require('../utils/credit');

function formatCard(card, debtCents) {
  const limitCents = Number(card.credit_limit_centavos);
  return {
    id: Number(card.id),
    name: card.name,
    bank: card.bank,
    credit_limit: fromCents(limitCents),
    available_limit: fromCents(limitCents - debtCents),
    default_interest_rate: Number(card.default_interest_rate),
    cutoff_day: Number(card.cutoff_day),
    payment_day: Number(card.payment_day)
  };
}

function debtMap(db) {
  const map = new Map();
  for (const entry of getCardDebtSummary(db)) {
    map.set(Number(entry.card.id), entry.debt_cents);
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
  const debts = debtMap(db);
  const rows = db.prepare('SELECT * FROM credit_cards ORDER BY id ASC').all();
  sendSuccess(res, 200, rows.map((c) => formatCard(c, debts.get(Number(c.id)) || 0)), null);
}

function getCard(req, res, id) {
  const db = getDb();
  const card = fetchCard(db, id);
  sendSuccess(res, 200, formatCard(card, debtMap(db).get(id) || 0), null);
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
  sendSuccess(res, 201, formatCard(fetchCard(db, id), 0), null);
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
  sendSuccess(res, 200, formatCard(fetchCard(db, id), debtMap(db).get(id) || 0), null);
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

module.exports = {
  listCards,
  getCard,
  createCard,
  updateCard,
  deleteCard,
  simulateCard
};
