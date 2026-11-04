'use strict';

const { getDb, formatTransaccion, formatCompra } = require('../db');
const { BASE_TX_SELECT } = require('./transacciones');
const { AppError, sendSuccess } = require('../utils/response');
const {
  ENUMS,
  hasEmoji,
  nowIsoUtc,
  isValidDateStr,
  roundHalfUp,
  hasMax3Decimals,
  parsePagination
} = require('../utils/validators');

function fetchCompraById(db, id) {
  const txRow = db
    .prepare(`${BASE_TX_SELECT} WHERE t.id = ? AND t.es_compra = 1`)
    .get(id);
  if (!txRow) {
    return null;
  }
  const articulosRows = db
    .prepare('SELECT * FROM detalle_compras WHERE transaccion_id = ? ORDER BY orden ASC, id ASC')
    .all(id);
  return formatCompra(txRow, articulosRows);
}

function listCompras(req, res, query) {
  const db = getDb();

  const emojiDetails = [];
  if (typeof query.buscar === 'string' && hasEmoji(query.buscar)) {
    emojiDetails.push({ campo: 'buscar', mensaje: 'El texto no puede contener emojis.' });
  }
  if (typeof query.articulo === 'string' && hasEmoji(query.articulo)) {
    emojiDetails.push({ campo: 'articulo', mensaje: 'El texto no puede contener emojis.' });
  }
  if (emojiDetails.length > 0) {
    throw new AppError(422, 'TEXTO_CON_EMOJI', 'Uno o mas campos contienen emojis no permitidos.', emojiDetails);
  }

  const { pagina, por_pagina, detalles } = parsePagination(query);
  const conditions = ["t.tipo = 'GASTO'", 't.es_compra = 1'];
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
    const mp = String(query.metodo_pago).trim();
    if (!ENUMS.METODO_PAGO.includes(mp)) {
      detalles.push({ campo: 'metodo_pago', mensaje: 'Metodo de pago no valido.' });
    } else {
      conditions.push('t.metodo_pago = ?');
      params.push(mp);
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

  if (query.articulo !== undefined && query.articulo !== '') {
    const articulo = String(query.articulo).trim();
    if (articulo.length > 100) {
      detalles.push({ campo: 'articulo', mensaje: 'Maximo 100 caracteres.' });
    } else if (articulo.length > 0) {
      const pattern = `%${articulo.toLowerCase()}%`;
      conditions.push(
        'EXISTS (SELECT 1 FROM detalle_compras dc WHERE dc.transaccion_id = t.id AND LOWER(dc.articulo) LIKE ?)'
      );
      params.push(pattern);
    }
  }

  if (detalles.length > 0) {
    throw new AppError(422, 'VALIDACION_FALLIDA', 'Parametros de consulta no validos.', detalles);
  }

  const whereSql = `WHERE ${conditions.join(' AND ')}`;
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

function getCompra(req, res, id) {
  const db = getDb();
  const compra = fetchCompraById(db, id);
  if (!compra) {
    throw new AppError(404, 'RECURSO_NO_ENCONTRADO', 'La compra solicitada no existe.');
  }
  sendSuccess(res, 200, compra, null);
}

function validateCompraPayload(db, body) {
  const emojiDetails = [];
  const topFields = ['naturaleza', 'fecha', 'comercio', 'descripcion', 'metodo_pago', 'notas'];
  for (const f of topFields) {
    if (typeof body[f] === 'string' && hasEmoji(body[f])) {
      emojiDetails.push({ campo: f, mensaje: 'El texto no puede contener emojis.' });
    }
  }
  if (Array.isArray(body.articulos)) {
    body.articulos.forEach((item, idx) => {
      if (item && typeof item === 'object') {
        if (typeof item.articulo === 'string' && hasEmoji(item.articulo)) {
          emojiDetails.push({
            campo: `articulos[${idx}].articulo`,
            mensaje: 'El texto no puede contener emojis.'
          });
        }
        if (typeof item.unidad === 'string' && hasEmoji(item.unidad)) {
          emojiDetails.push({
            campo: `articulos[${idx}].unidad`,
            mensaje: 'El texto no puede contener emojis.'
          });
        }
      }
    });
  }
  if (emojiDetails.length > 0) {
    throw new AppError(422, 'TEXTO_CON_EMOJI', 'Uno o mas campos contienen emojis no permitidos.', emojiDetails);
  }

  const detalles = [];

  const rawNat = body.naturaleza === undefined || body.naturaleza === null ? 'VARIABLE' : body.naturaleza;
  const naturaleza = typeof rawNat === 'string' ? rawNat.trim() : '';
  if (!ENUMS.NATURALEZA.includes(naturaleza)) {
    detalles.push({ campo: 'naturaleza', mensaje: 'La naturaleza debe ser FIJO o VARIABLE.' });
  }

  const fecha = typeof body.fecha === 'string' ? body.fecha.trim() : '';
  if (!isValidDateStr(fecha)) {
    detalles.push({ campo: 'fecha', mensaje: 'La fecha es obligatoria y debe tener formato YYYY-MM-DD.' });
  }

  const comercio = typeof body.comercio === 'string' ? body.comercio.trim() : '';
  if (typeof body.comercio !== 'string' || comercio.length < 1 || comercio.length > 120) {
    detalles.push({
      campo: 'comercio',
      mensaje: 'El comercio es obligatorio en compras y debe tener entre 1 y 120 caracteres.'
    });
  }

  let descripcion = '';
  if (body.descripcion === undefined || body.descripcion === null) {
    descripcion = `Compra en ${comercio}`.slice(0, 200);
  } else if (typeof body.descripcion !== 'string') {
    detalles.push({ campo: 'descripcion', mensaje: 'Debe ser texto o null.' });
  } else {
    const trimmedDesc = body.descripcion.trim();
    if (trimmedDesc.length === 0) {
      descripcion = `Compra en ${comercio}`.slice(0, 200);
    } else if (trimmedDesc.length > 200) {
      detalles.push({ campo: 'descripcion', mensaje: 'Maximo 200 caracteres.' });
    } else {
      descripcion = trimmedDesc;
    }
  }

  const categoriaId = body.categoria_id;
  if (typeof categoriaId !== 'number' || !Number.isInteger(categoriaId) || categoriaId <= 0) {
    detalles.push({ campo: 'categoria_id', mensaje: 'El campo categoria_id debe ser un entero positivo.' });
  }

  const metodoPago = typeof body.metodo_pago === 'string' ? body.metodo_pago.trim() : '';
  if (!ENUMS.METODO_PAGO.includes(metodoPago)) {
    detalles.push({
      campo: 'metodo_pago',
      mensaje: 'El metodo de pago debe ser EFECTIVO, DEBITO, CREDITO, TRANSFERENCIA u OTRO.'
    });
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

  const articulosProcesados = [];
  if (!Array.isArray(body.articulos) || body.articulos.length < 1 || body.articulos.length > 200) {
    detalles.push({
      campo: 'articulos',
      mensaje: 'Debe incluir entre 1 y 200 articulos.'
    });
  } else {
    body.articulos.forEach((item, idx) => {
      if (!item || typeof item !== 'object') {
        detalles.push({
          campo: `articulos[${idx}]`,
          mensaje: 'El articulo debe ser un objeto valido.'
        });
        return;
      }

      const nombreArt = typeof item.articulo === 'string' ? item.articulo.trim() : '';
      if (typeof item.articulo !== 'string' || nombreArt.length < 1 || nombreArt.length > 120) {
        detalles.push({
          campo: `articulos[${idx}].articulo`,
          mensaje: 'El nombre del articulo es obligatorio y debe tener entre 1 y 120 caracteres.'
        });
      }

      const cantidad = item.cantidad;
      if (
        typeof cantidad !== 'number' ||
        !Number.isFinite(cantidad) ||
        cantidad <= 0 ||
        cantidad > 99999 ||
        !hasMax3Decimals(cantidad)
      ) {
        detalles.push({
          campo: `articulos[${idx}].cantidad`,
          mensaje: 'La cantidad debe ser un numero mayor a 0, maximo 99999 y hasta 3 decimales.'
        });
      }

      const rawUnidad = item.unidad === undefined || item.unidad === null ? 'UNIDAD' : item.unidad;
      const unidad = typeof rawUnidad === 'string' ? rawUnidad.trim() : '';
      if (!ENUMS.UNIDAD.includes(unidad)) {
        detalles.push({
          campo: `articulos[${idx}].unidad`,
          mensaje: 'Unidad no valida (UNIDAD, KG, G, L, ML, PAQUETE, OTRO).'
        });
      }

      const precioUnitario = item.precio_unitario_centavos;
      if (
        typeof precioUnitario !== 'number' ||
        !Number.isInteger(precioUnitario) ||
        precioUnitario < 0 ||
        precioUnitario > 9999999999
      ) {
        detalles.push({
          campo: `articulos[${idx}].precio_unitario_centavos`,
          mensaje: 'Debe ser un entero entre 0 y 9999999999.'
        });
      }

      if (
        typeof cantidad === 'number' &&
        Number.isFinite(cantidad) &&
        typeof precioUnitario === 'number' &&
        Number.isInteger(precioUnitario)
      ) {
        const subtotal = roundHalfUp(cantidad * precioUnitario);
        articulosProcesados.push({
          articulo: nombreArt,
          cantidad,
          unidad,
          precio_unitario_centavos: precioUnitario,
          subtotal_centavos: subtotal,
          orden: idx
        });
      }
    });
  }

  let montoTotalCentavos = 0;
  if (detalles.length === 0) {
    montoTotalCentavos = articulosProcesados.reduce((acc, it) => acc + it.subtotal_centavos, 0);
    if (montoTotalCentavos <= 0 || montoTotalCentavos > 99999999999) {
      detalles.push({
        campo: 'articulos',
        mensaje: 'La suma total de los articulos debe ser mayor a 0 centavos.'
      });
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
  if (categoria.tipo !== 'GASTO') {
    throw new AppError(
      422,
      'CATEGORIA_TIPO_INCOMPATIBLE',
      'Las compras solo pueden asociarse a categorias de tipo GASTO.',
      [{ campo: 'categoria_id', mensaje: 'La categoria debe ser de tipo GASTO.' }]
    );
  }

  return {
    naturaleza,
    fecha,
    comercio,
    descripcion,
    categoria_id: categoriaId,
    metodo_pago: metodoPago,
    notas,
    monto_centavos: montoTotalCentavos,
    articulos: articulosProcesados
  };
}

function insertCompraInDb(db, valid) {
  const now = nowIsoUtc();
  db.exec('BEGIN TRANSACTION');
  try {
    const resTx = db
      .prepare(`
        INSERT INTO transacciones (
          tipo, naturaleza, monto_centavos, fecha, descripcion,
          categoria_id, metodo_pago, comercio, notas, es_compra,
          creado_en, actualizado_en
        ) VALUES ('GASTO', ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)
      `)
      .run(
        valid.naturaleza,
        valid.monto_centavos,
        valid.fecha,
        valid.descripcion,
        valid.categoria_id,
        valid.metodo_pago,
        valid.comercio,
        valid.notas,
        now,
        now
      );

    const txId = Number(resTx.lastInsertRowid);
    const insertDetalle = db.prepare(`
      INSERT INTO detalle_compras (
        transaccion_id, articulo, cantidad, unidad,
        precio_unitario_centavos, subtotal_centavos, orden, creado_en
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    `);

    for (const art of valid.articulos) {
      insertDetalle.run(
        txId,
        art.articulo,
        art.cantidad,
        art.unidad,
        art.precio_unitario_centavos,
        art.subtotal_centavos,
        art.orden,
        now
      );
    }

    db.exec('COMMIT');
    return fetchCompraById(db, txId);
  } catch (err) {
    db.exec('ROLLBACK');
    throw err;
  }
}

function createCompra(req, res, body) {
  const db = getDb();
  const valid = validateCompraPayload(db, body);
  const created = insertCompraInDb(db, valid);
  sendSuccess(res, 201, created, null);
}

function updateCompra(req, res, id, body) {
  const db = getDb();
  const existing = db.prepare('SELECT id, es_compra FROM transacciones WHERE id = ?').get(id);
  if (!existing || Number(existing.es_compra) !== 1) {
    throw new AppError(404, 'RECURSO_NO_ENCONTRADO', 'La compra solicitada no existe.');
  }

  const valid = validateCompraPayload(db, body);
  const now = nowIsoUtc();

  db.exec('BEGIN TRANSACTION');
  try {
    db.prepare(`
      UPDATE transacciones
      SET naturaleza = ?, monto_centavos = ?, fecha = ?, descripcion = ?,
          categoria_id = ?, metodo_pago = ?, comercio = ?, notas = ?, actualizado_en = ?
      WHERE id = ?
    `).run(
      valid.naturaleza,
      valid.monto_centavos,
      valid.fecha,
      valid.descripcion,
      valid.categoria_id,
      valid.metodo_pago,
      valid.comercio,
      valid.notas,
      now,
      id
    );

    db.prepare('DELETE FROM detalle_compras WHERE transaccion_id = ?').run(id);

    const insertDetalle = db.prepare(`
      INSERT INTO detalle_compras (
        transaccion_id, articulo, cantidad, unidad,
        precio_unitario_centavos, subtotal_centavos, orden, creado_en
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    `);

    for (const art of valid.articulos) {
      insertDetalle.run(
        id,
        art.articulo,
        art.cantidad,
        art.unidad,
        art.precio_unitario_centavos,
        art.subtotal_centavos,
        art.orden,
        now
      );
    }

    db.exec('COMMIT');
  } catch (err) {
    db.exec('ROLLBACK');
    throw err;
  }

  const updated = fetchCompraById(db, id);
  sendSuccess(res, 200, updated, null);
}

function deleteCompra(req, res, id) {
  const db = getDb();
  const existing = db.prepare('SELECT id, es_compra FROM transacciones WHERE id = ?').get(id);
  if (!existing || Number(existing.es_compra) !== 1) {
    throw new AppError(404, 'RECURSO_NO_ENCONTRADO', 'La compra solicitada no existe.');
  }

  db.prepare('DELETE FROM transacciones WHERE id = ?').run(id);
  sendSuccess(res, 200, { id, eliminado: true }, null);
}

module.exports = {
  fetchCompraById,
  validateCompraPayload,
  insertCompraInDb,
  listCompras,
  getCompra,
  createCompra,
  updateCompra,
  deleteCompra
};
