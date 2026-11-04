'use strict';

const { getDb, formatTransaccion } = require('../db');
const { AppError, sendSuccess } = require('../utils/response');
const {
  ENUMS,
  nowIsoUtc,
  isValidDateStr,
  checkNoEmojisInFields,
  parsePagination
} = require('../utils/validators');
const { MAX_INSTALLMENTS, buildSchedule } = require('../utils/credit');

const PAYMENT_METHOD_ALIASES = {
  CREDIT_CARD: 'CREDITO',
  CASH: 'EFECTIVO',
  DEBIT: 'DEBITO',
  TRANSFER: 'TRANSFERENCIA',
  OTHER: 'OTRO'
};

function normalizePaymentMethod(value) {
  if (typeof value !== 'string') return value;
  const trimmed = value.trim();
  return PAYMENT_METHOD_ALIASES[trimmed] || trimmed;
}

const BASE_TX_SELECT = `
  SELECT
    t.id, t.tipo, t.naturaleza, t.monto_centavos, t.fecha, t.descripcion,
    t.categoria_id, t.metodo_pago, t.comercio, t.notas, t.es_compra,
    t.creado_en, t.actualizado_en,
    t.card_id, t.installments, t.is_interest_exempt, t.applied_interest_rate,
    t.monthly_installment_centavos, t.total_interest_centavos, t.total_with_interest_centavos,
    c.id AS cat_id, c.nombre AS cat_nombre, c.tipo AS cat_tipo,
    c.icono AS cat_icono, c.color AS cat_color,
    CASE WHEN t.es_compra = 1
      THEN (SELECT COUNT(*) FROM detalle_compras dc WHERE dc.transaccion_id = t.id)
      ELSE 0
    END AS cantidad_articulos
  FROM transacciones t
  JOIN categorias c ON c.id = t.categoria_id
`;

function fetchTransaccionById(db, id) {
  const row = db.prepare(`${BASE_TX_SELECT} WHERE t.id = ?`).get(id);
  return row ? formatTransaccion(row) : null;
}

function listTransacciones(req, res, query) {
  const db = getDb();
  checkNoEmojisInFields({ buscar: query.buscar });

  const { pagina, por_pagina, detalles } = parsePagination(query);
  const conditions = [];
  const params = [];

  if (query.desde !== undefined && query.desde !== '') {
    const desde = String(query.desde).trim();
    if (!isValidDateStr(desde)) {
      detalles.push({ campo: 'desde', mensaje: 'Formato de fecha invalido (YYYY-MM-DD).' });
    } else {
      conditions.push('t.fecha >= ?');
      params.push(desde);
    }
  }

  if (query.hasta !== undefined && query.hasta !== '') {
    const hasta = String(query.hasta).trim();
    if (!isValidDateStr(hasta)) {
      detalles.push({ campo: 'hasta', mensaje: 'Formato de fecha invalido (YYYY-MM-DD).' });
    } else {
      conditions.push('t.fecha <= ?');
      params.push(hasta);
    }
  }

  if (query.tipo !== undefined && query.tipo !== '') {
    const tipo = String(query.tipo).trim();
    if (!ENUMS.TIPO.includes(tipo)) {
      detalles.push({ campo: 'tipo', mensaje: 'Debe ser INGRESO o GASTO.' });
    } else {
      conditions.push('t.tipo = ?');
      params.push(tipo);
    }
  }

  if (query.naturaleza !== undefined && query.naturaleza !== '') {
    const nat = String(query.naturaleza).trim();
    if (!ENUMS.NATURALEZA.includes(nat)) {
      detalles.push({ campo: 'naturaleza', mensaje: 'Debe ser FIJO o VARIABLE.' });
    } else {
      conditions.push('t.naturaleza = ?');
      params.push(nat);
    }
  }

  if (query.categoria_id !== undefined && query.categoria_id !== '') {
    if (!/^[1-9]\d*$/.test(String(query.categoria_id).trim())) {
      detalles.push({ campo: 'categoria_id', mensaje: 'Debe ser un entero positivo.' });
    } else {
      conditions.push('t.categoria_id = ?');
      params.push(Number(String(query.categoria_id).trim()));
    }
  }

  if (query.metodo_pago !== undefined && query.metodo_pago !== '') {
    const mp = normalizePaymentMethod(String(query.metodo_pago));
    if (!ENUMS.METODO_PAGO.includes(mp)) {
      detalles.push({ campo: 'metodo_pago', mensaje: 'Metodo de pago no valido.' });
    } else {
      conditions.push('t.metodo_pago = ?');
      params.push(mp);
    }
  }

  if (query.card_id !== undefined && query.card_id !== '') {
    if (!/^[1-9]\d*$/.test(String(query.card_id).trim())) {
      detalles.push({ campo: 'card_id', mensaje: 'Debe ser un entero positivo.' });
    } else {
      conditions.push('t.card_id = ?');
      params.push(Number(String(query.card_id).trim()));
    }
  }

  if (query.es_compra !== undefined && query.es_compra !== '') {
    const ec = String(query.es_compra).trim();
    if (ec !== 'true' && ec !== 'false') {
      detalles.push({ campo: 'es_compra', mensaje: 'Debe ser true o false.' });
    } else {
      conditions.push('t.es_compra = ?');
      params.push(ec === 'true' ? 1 : 0);
    }
  }

  if (query.buscar !== undefined && query.buscar !== '') {
    const buscar = String(query.buscar).trim();
    if (buscar.length > 100) {
      detalles.push({ campo: 'buscar', mensaje: 'Maximo 100 caracteres.' });
    } else if (buscar.length > 0) {
      const pattern = `%${buscar.toLowerCase()}%`;
      conditions.push(
        "(LOWER(t.descripcion) LIKE ? OR LOWER(COALESCE(t.comercio, '')) LIKE ? OR LOWER(COALESCE(t.notas, '')) LIKE ?)"
      );
      params.push(pattern, pattern, pattern);
    }
  }

  if (detalles.length > 0) {
    throw new AppError(422, 'VALIDACION_FALLIDA', 'Parametros de consulta no validos.', detalles);
  }

  const whereSql = conditions.length > 0 ? `WHERE ${conditions.join(' AND ')}` : '';
  const countRow = db
    .prepare(`SELECT COUNT(*) AS total FROM transacciones t ${whereSql}`)
    .get(...params);
  const totalRegistros = Number(countRow.total || 0);
  const totalPaginas = Math.ceil(totalRegistros / por_pagina);
  const offset = (pagina - 1) * por_pagina;

  const rows = db
    .prepare(`
      ${BASE_TX_SELECT}
      ${whereSql}
      ORDER BY t.fecha DESC, t.id DESC
      LIMIT ? OFFSET ?
    `)
    .all(...params, por_pagina, offset);

  sendSuccess(res, 200, rows.map(formatTransaccion), {
    pagina,
    por_pagina,
    total_registros: totalRegistros,
    total_paginas: totalPaginas
  });
}

function getTransaccion(req, res, id) {
  const db = getDb();
  const tx = fetchTransaccionById(db, id);
  if (!tx) {
    throw new AppError(404, 'RECURSO_NO_ENCONTRADO', 'La transaccion solicitada no existe.');
  }
  sendSuccess(res, 200, tx, null);
}

function validateTransaccionPayload(db, rawBody) {
  const body = { ...rawBody };
  if (body.metodo_pago === undefined && body.payment_method !== undefined) {
    body.metodo_pago = normalizePaymentMethod(body.payment_method);
  } else if (body.metodo_pago !== undefined) {
    body.metodo_pago = normalizePaymentMethod(body.metodo_pago);
  }
  checkNoEmojisInFields({
    payment_method: rawBody.payment_method,
    tipo: body.tipo,
    naturaleza: body.naturaleza,
    fecha: body.fecha,
    descripcion: body.descripcion,
    metodo_pago: body.metodo_pago,
    comercio: body.comercio,
    notas: body.notas
  });

  const detalles = [];

  const tipo = typeof body.tipo === 'string' ? body.tipo.trim() : '';
  if (!ENUMS.TIPO.includes(tipo)) {
    detalles.push({ campo: 'tipo', mensaje: 'El tipo debe ser INGRESO o GASTO.' });
  }

  const naturaleza = typeof body.naturaleza === 'string' ? body.naturaleza.trim() : '';
  if (!ENUMS.NATURALEZA.includes(naturaleza)) {
    detalles.push({ campo: 'naturaleza', mensaje: 'La naturaleza debe ser FIJO o VARIABLE.' });
  }

  const montoCentavos = body.monto_centavos;
  if (
    typeof montoCentavos !== 'number' ||
    !Number.isInteger(montoCentavos) ||
    montoCentavos < 1 ||
    montoCentavos > 99999999999
  ) {
    detalles.push({
      campo: 'monto_centavos',
      mensaje: 'Debe ser un entero entre 1 y 99999999999.'
    });
  }

  const fecha = typeof body.fecha === 'string' ? body.fecha.trim() : '';
  if (!isValidDateStr(fecha)) {
    detalles.push({
      campo: 'fecha',
      mensaje: 'La fecha es obligatoria y debe tener formato YYYY-MM-DD.'
    });
  }

  const descripcion = typeof body.descripcion === 'string' ? body.descripcion.trim() : '';
  if (typeof body.descripcion !== 'string' || descripcion.length < 1 || descripcion.length > 200) {
    detalles.push({
      campo: 'descripcion',
      mensaje: 'La descripcion es obligatoria y debe tener entre 1 y 200 caracteres.'
    });
  }

  const categoriaId = body.categoria_id;
  if (typeof categoriaId !== 'number' || !Number.isInteger(categoriaId) || categoriaId <= 0) {
    detalles.push({
      campo: 'categoria_id',
      mensaje: 'El campo categoria_id debe ser un entero positivo.'
    });
  }

  const metodoPago = typeof body.metodo_pago === 'string' ? body.metodo_pago.trim() : '';
  if (!ENUMS.METODO_PAGO.includes(metodoPago)) {
    detalles.push({
      campo: 'metodo_pago',
      mensaje: 'El metodo de pago debe ser EFECTIVO, DEBITO, CREDITO, TRANSFERENCIA u OTRO.'
    });
  }

  let comercio = null;
  if (body.comercio !== undefined && body.comercio !== null) {
    if (typeof body.comercio !== 'string') {
      detalles.push({ campo: 'comercio', mensaje: 'Debe ser texto o null.' });
    } else {
      const trimmedComercio = body.comercio.trim();
      if (trimmedComercio.length > 120) {
        detalles.push({ campo: 'comercio', mensaje: 'Maximo 120 caracteres.' });
      } else if (trimmedComercio.length > 0) {
        comercio = trimmedComercio;
      }
    }
  }

  let notas = null;
  if (body.notas !== undefined && body.notas !== null) {
    if (typeof body.notas !== 'string') {
      detalles.push({ campo: 'notas', mensaje: 'Debe ser texto o null.' });
    } else {
      const trimmedNotas = body.notas.trim();
      if (trimmedNotas.length > 1000) {
        detalles.push({ campo: 'notas', mensaje: 'Maximo 1000 caracteres.' });
      } else if (trimmedNotas.length > 0) {
        notas = trimmedNotas;
      }
    }
  }

  if (detalles.length > 0) {
    throw new AppError(422, 'VALIDACION_FALLIDA', 'Uno o mas campos no son validos.', detalles);
  }

  const categoria = db.prepare('SELECT * FROM categorias WHERE id = ?').get(categoriaId);
  if (!categoria) {
    throw new AppError(422, 'VALIDACION_FALLIDA', 'La categoria indicada no existe.', [
      { campo: 'categoria_id', mensaje: 'La categoria no existe.' }
    ]);
  }
  if (Number(categoria.activa) !== 1) {
    throw new AppError(422, 'CATEGORIA_INACTIVA', 'No se puede usar una categoria inactiva.', [
      { campo: 'categoria_id', mensaje: 'La categoria seleccionada esta inactiva.' }
    ]);
  }
  if (categoria.tipo !== tipo) {
    throw new AppError(
      422,
      'CATEGORIA_TIPO_INCOMPATIBLE',
      'El tipo de la transaccion no coincide con el tipo de la categoria.',
      [{ campo: 'categoria_id', mensaje: `La categoria es de tipo ${categoria.tipo}.` }]
    );
  }

  const card = validateCardFields(db, body, { tipo, metodoPago, montoCentavos, fecha });

  return {
    ...card,
    tipo,
    naturaleza,
    monto_centavos: montoCentavos,
    fecha,
    descripcion,
    categoria_id: categoriaId,
    metodo_pago: metodoPago,
    comercio,
    notas
  };
}

function emptyCardFields() {
  return {
    card_id: null,
    installments: null,
    is_interest_exempt: null,
    applied_interest_rate: null,
    monthly_installment_centavos: null,
    total_interest_centavos: null,
    total_with_interest_centavos: null
  };
}

function validateCardFields(db, body, ctx) {
  if (ctx.metodoPago !== 'CREDITO') {
    return emptyCardFields();
  }
  const detalles = [];

  if (ctx.tipo !== 'GASTO') {
    detalles.push({ campo: 'payment_method', mensaje: 'CREDIT_CARD solo aplica a transacciones de tipo GASTO.' });
  }

  const cardId = body.card_id;
  if (typeof cardId !== 'number' || !Number.isInteger(cardId) || cardId <= 0) {
    detalles.push({ campo: 'card_id', mensaje: 'Obligatorio con CREDIT_CARD: entero positivo.' });
  }

  let installments = 1;
  if (body.installments !== undefined && body.installments !== null) {
    installments = body.installments;
    if (typeof installments !== 'number' || !Number.isInteger(installments) || installments < 1 || installments > MAX_INSTALLMENTS) {
      detalles.push({ campo: 'installments', mensaje: `Debe ser un entero entre 1 y ${MAX_INSTALLMENTS}.` });
    }
  }

  let exempt = false;
  if (body.is_interest_exempt !== undefined && body.is_interest_exempt !== null) {
    if (typeof body.is_interest_exempt !== 'boolean') {
      detalles.push({ campo: 'is_interest_exempt', mensaje: 'Debe ser booleano.' });
    } else {
      exempt = body.is_interest_exempt;
    }
  }

  let providedRate = null;
  if (body.applied_interest_rate !== undefined && body.applied_interest_rate !== null) {
    providedRate = body.applied_interest_rate;
    if (typeof providedRate !== 'number' || !Number.isFinite(providedRate) || providedRate < 0 || providedRate > 100) {
      detalles.push({ campo: 'applied_interest_rate', mensaje: 'Debe ser un numero entre 0 y 100.' });
    }
  }

  if (detalles.length > 0) {
    throw new AppError(422, 'VALIDACION_FALLIDA', 'Uno o mas campos no son validos.', detalles);
  }

  const card = db.prepare('SELECT * FROM credit_cards WHERE id = ?').get(cardId);
  if (!card) {
    throw new AppError(422, 'VALIDACION_FALLIDA', 'La tarjeta indicada no existe.', [
      { campo: 'card_id', mensaje: 'La tarjeta no existe.' }
    ]);
  }

  const rate = exempt ? 0 : providedRate !== null ? providedRate : Number(card.default_interest_rate);
  const schedule = buildSchedule(ctx.montoCentavos, installments, rate, exempt);

  return {
    card_id: cardId,
    installments,
    is_interest_exempt: exempt ? 1 : 0,
    applied_interest_rate: rate,
    monthly_installment_centavos: schedule.installmentCents,
    total_interest_centavos: schedule.totalInterestCents,
    total_with_interest_centavos: schedule.totalWithInterestCents
  };
}

function createTransaccion(req, res, body) {
  const db = getDb();
  const valid = validateTransaccionPayload(db, body);
  const now = nowIsoUtc();

  const result = db
    .prepare(`
      INSERT INTO transacciones (
        tipo, naturaleza, monto_centavos, fecha, descripcion,
        categoria_id, metodo_pago, comercio, notas, es_compra,
        card_id, installments, is_interest_exempt, applied_interest_rate,
        monthly_installment_centavos, total_interest_centavos, total_with_interest_centavos,
        creado_en, actualizado_en
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `)
    .run(
      valid.tipo,
      valid.naturaleza,
      valid.monto_centavos,
      valid.fecha,
      valid.descripcion,
      valid.categoria_id,
      valid.metodo_pago,
      valid.comercio,
      valid.notas,
      valid.card_id,
      valid.installments,
      valid.is_interest_exempt,
      valid.applied_interest_rate,
      valid.monthly_installment_centavos,
      valid.total_interest_centavos,
      valid.total_with_interest_centavos,
      now,
      now
    );

  const created = fetchTransaccionById(db, Number(result.lastInsertRowid));
  sendSuccess(res, 201, created, null);
}

function updateTransaccion(req, res, id, body) {
  const db = getDb();
  const existing = db.prepare('SELECT id, es_compra FROM transacciones WHERE id = ?').get(id);
  if (!existing) {
    throw new AppError(404, 'RECURSO_NO_ENCONTRADO', 'La transaccion solicitada no existe.');
  }
  if (Number(existing.es_compra) === 1) {
    throw new AppError(
      409,
      'USAR_ENDPOINT_COMPRAS',
      'Esta transaccion es una compra con articulos; utilice el endpoint /compras/{id}.'
    );
  }

  const valid = validateTransaccionPayload(db, body);
  const now = nowIsoUtc();

  db.prepare(`
    UPDATE transacciones
    SET tipo = ?, naturaleza = ?, monto_centavos = ?, fecha = ?, descripcion = ?,
        categoria_id = ?, metodo_pago = ?, comercio = ?, notas = ?,
        card_id = ?, installments = ?, is_interest_exempt = ?, applied_interest_rate = ?,
        monthly_installment_centavos = ?, total_interest_centavos = ?, total_with_interest_centavos = ?,
        actualizado_en = ?
    WHERE id = ?
  `).run(
    valid.tipo,
    valid.naturaleza,
    valid.monto_centavos,
    valid.fecha,
    valid.descripcion,
    valid.categoria_id,
    valid.metodo_pago,
    valid.comercio,
    valid.notas,
    valid.card_id,
    valid.installments,
    valid.is_interest_exempt,
    valid.applied_interest_rate,
    valid.monthly_installment_centavos,
    valid.total_interest_centavos,
    valid.total_with_interest_centavos,
    now,
    id
  );

  const updated = fetchTransaccionById(db, id);
  sendSuccess(res, 200, updated, null);
}

function deleteTransaccion(req, res, id) {
  const db = getDb();
  const existing = db.prepare('SELECT id FROM transacciones WHERE id = ?').get(id);
  if (!existing) {
    throw new AppError(404, 'RECURSO_NO_ENCONTRADO', 'La transaccion solicitada no existe.');
  }

  db.prepare('DELETE FROM transacciones WHERE id = ?').run(id);
  sendSuccess(res, 200, { id, eliminado: true }, null);
}

module.exports = {
  BASE_TX_SELECT,
  fetchTransaccionById,
  listTransacciones,
  getTransaccion,
  createTransaccion,
  updateTransaccion,
  deleteTransaccion
};
