'use strict';

const { getDb, formatPresupuesto } = require('../db');
const { AppError, sendSuccess } = require('../utils/response');
const {
  ENUMS,
  nowIsoUtc,
  roundHalfUp2,
  checkNoEmojisInFields
} = require('../utils/validators');

const BASE_PRESUPUESTO_SELECT = `
  SELECT
    p.id, p.categoria_id, p.anio, p.mes, p.monto_limite_centavos, p.notas,
    p.creado_en, p.actualizado_en,
    c.id AS cat_id, c.nombre AS cat_nombre, c.tipo AS cat_tipo,
    c.icono AS cat_icono, c.color AS cat_color,
    COALESCE((
      SELECT SUM(t.monto_centavos)
      FROM transacciones t
      WHERE t.categoria_id = p.categoria_id
        AND t.tipo = 'GASTO'
        AND t.fecha >= printf('%04d-%02d-01', p.anio, p.mes)
        AND t.fecha <= printf('%04d-%02d-31', p.anio, p.mes)
    ), 0) AS gastado_centavos
  FROM presupuestos p
  JOIN categorias c ON c.id = p.categoria_id
`;

function fetchPresupuestoById(db, id) {
  const row = db.prepare(`${BASE_PRESUPUESTO_SELECT} WHERE p.id = ?`).get(id);
  return row ? formatPresupuesto(row) : null;
}

function getPresupuestosForPeriod(db, anio, mes, estadoFilter = null) {
  const rows = db
    .prepare(`${BASE_PRESUPUESTO_SELECT} WHERE p.anio = ? AND p.mes = ? ORDER BY p.id ASC`)
    .all(anio, mes);

  let presupuestos = rows.map(formatPresupuesto);
  presupuestos.sort((a, b) => {
    if (b.porcentaje_usado !== a.porcentaje_usado) {
      return b.porcentaje_usado - a.porcentaje_usado;
    }
    return a.id - b.id;
  });

  if (estadoFilter) {
    presupuestos = presupuestos.filter((p) => p.estado === estadoFilter);
  }

  const totalLimite = presupuestos.reduce((acc, p) => acc + p.monto_limite_centavos, 0);
  const totalGastado = presupuestos.reduce((acc, p) => acc + p.gastado_centavos, 0);
  const totalDisponible = totalLimite - totalGastado;
  const porcentajeGlobal =
    totalLimite > 0 ? roundHalfUp2((totalGastado / totalLimite) * 100) : 0;

  return {
    anio,
    mes,
    total_limite_centavos: totalLimite,
    total_gastado_centavos: totalGastado,
    total_disponible_centavos: totalDisponible,
    porcentaje_usado_global: porcentajeGlobal,
    presupuestos
  };
}

function listPresupuestos(req, res, query) {
  const db = getDb();
  const detalles = [];

  let anio = null;
  if (query.anio === undefined || query.anio === '' || !/^-?\d+$/.test(String(query.anio).trim())) {
    detalles.push({ campo: 'anio', mensaje: 'El anio es obligatorio y debe estar entre 2000 y 2100.' });
  } else {
    anio = Number(String(query.anio).trim());
    if (!Number.isInteger(anio) || anio < 2000 || anio > 2100) {
      detalles.push({ campo: 'anio', mensaje: 'El anio debe estar entre 2000 y 2100.' });
    }
  }

  let mes = null;
  if (query.mes === undefined || query.mes === '' || !/^-?\d+$/.test(String(query.mes).trim())) {
    detalles.push({ campo: 'mes', mensaje: 'El mes es obligatorio y debe estar entre 1 y 12.' });
  } else {
    mes = Number(String(query.mes).trim());
    if (!Number.isInteger(mes) || mes < 1 || mes > 12) {
      detalles.push({ campo: 'mes', mensaje: 'El mes debe estar entre 1 y 12.' });
    }
  }

  let estadoFilter = null;
  if (query.estado !== undefined && query.estado !== '') {
    const est = String(query.estado).trim();
    if (!ENUMS.ESTADO_PRESUPUESTO.includes(est)) {
      detalles.push({ campo: 'estado', mensaje: 'El estado debe ser NORMAL, ADVERTENCIA o EXCEDIDO.' });
    } else {
      estadoFilter = est;
    }
  }

  if (detalles.length > 0) {
    throw new AppError(422, 'VALIDACION_FALLIDA', 'Parametros de consulta no validos.', detalles);
  }

  const data = getPresupuestosForPeriod(db, anio, mes, estadoFilter);
  sendSuccess(res, 200, data, null);
}

function getPresupuesto(req, res, id) {
  const db = getDb();
  const pres = fetchPresupuestoById(db, id);
  if (!pres) {
    throw new AppError(404, 'RECURSO_NO_ENCONTRADO', 'El presupuesto solicitado no existe.');
  }
  sendSuccess(res, 200, pres, null);
}

function createPresupuesto(req, res, body) {
  const db = getDb();
  checkNoEmojisInFields({ notas: body.notas });

  const detalles = [];

  const categoriaId = body.categoria_id;
  if (typeof categoriaId !== 'number' || !Number.isInteger(categoriaId) || categoriaId <= 0) {
    detalles.push({ campo: 'categoria_id', mensaje: 'Debe ser un entero positivo.' });
  }

  const anio = body.anio;
  if (typeof anio !== 'number' || !Number.isInteger(anio) || anio < 2000 || anio > 2100) {
    detalles.push({ campo: 'anio', mensaje: 'El anio debe ser un entero entre 2000 y 2100.' });
  }

  const mes = body.mes;
  if (typeof mes !== 'number' || !Number.isInteger(mes) || mes < 1 || mes > 12) {
    detalles.push({ campo: 'mes', mensaje: 'El mes debe ser un entero entre 1 y 12.' });
  }

  const montoLimite = body.monto_limite_centavos;
  if (
    typeof montoLimite !== 'number' ||
    !Number.isInteger(montoLimite) ||
    montoLimite < 1 ||
    montoLimite > 99999999999
  ) {
    detalles.push({
      campo: 'monto_limite_centavos',
      mensaje: 'Debe ser un entero entre 1 y 99999999999.'
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
    throw new AppError(422, 'CATEGORIA_INACTIVA', 'No se puede crear un presupuesto sobre una categoria inactiva.', [
      { campo: 'categoria_id', mensaje: 'La categoria seleccionada esta inactiva.' }
    ]);
  }
  if (categoria.tipo !== 'GASTO') {
    throw new AppError(
      422,
      'CATEGORIA_TIPO_INCOMPATIBLE',
      'Solo se permiten presupuestos sobre categorias de tipo GASTO.',
      [{ campo: 'categoria_id', mensaje: 'La categoria debe ser de tipo GASTO.' }]
    );
  }

  const dup = db
    .prepare('SELECT id FROM presupuestos WHERE categoria_id = ? AND anio = ? AND mes = ?')
    .get(categoriaId, anio, mes);
  if (dup) {
    throw new AppError(
      409,
      'PRESUPUESTO_DUPLICADO',
      'Ya existe un presupuesto para esa categoria en el periodo indicado.',
      [{ campo: 'categoria_id', mensaje: 'Presupuesto duplicado para este anio y mes.' }]
    );
  }

  const now = nowIsoUtc();
  const result = db
    .prepare(`
      INSERT INTO presupuestos (
        categoria_id, anio, mes, monto_limite_centavos, notas, creado_en, actualizado_en
      ) VALUES (?, ?, ?, ?, ?, ?, ?)
    `)
    .run(categoriaId, anio, mes, montoLimite, notas, now, now);

  const created = fetchPresupuestoById(db, Number(result.lastInsertRowid));
  sendSuccess(res, 201, created, null);
}

function updatePresupuesto(req, res, id, body) {
  const db = getDb();
  const existing = db.prepare('SELECT id FROM presupuestos WHERE id = ?').get(id);
  if (!existing) {
    throw new AppError(404, 'RECURSO_NO_ENCONTRADO', 'El presupuesto solicitado no existe.');
  }

  checkNoEmojisInFields({ notas: body.notas });

  const detalles = [];
  const montoLimite = body.monto_limite_centavos;
  if (
    typeof montoLimite !== 'number' ||
    !Number.isInteger(montoLimite) ||
    montoLimite < 1 ||
    montoLimite > 99999999999
  ) {
    detalles.push({
      campo: 'monto_limite_centavos',
      mensaje: 'Debe ser un entero entre 1 y 99999999999.'
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

  if (detalles.length > 0) {
    throw new AppError(422, 'VALIDACION_FALLIDA', 'Uno o mas campos no son validos.', detalles);
  }

  const now = nowIsoUtc();
  db.prepare(`
    UPDATE presupuestos
    SET monto_limite_centavos = ?, notas = ?, actualizado_en = ?
    WHERE id = ?
  `).run(montoLimite, notas, now, id);

  const updated = fetchPresupuestoById(db, id);
  sendSuccess(res, 200, updated, null);
}

function deletePresupuesto(req, res, id) {
  const db = getDb();
  const existing = db.prepare('SELECT id FROM presupuestos WHERE id = ?').get(id);
  if (!existing) {
    throw new AppError(404, 'RECURSO_NO_ENCONTRADO', 'El presupuesto solicitado no existe.');
  }

  db.prepare('DELETE FROM presupuestos WHERE id = ?').run(id);
  sendSuccess(res, 200, { id, eliminado: true }, null);
}

module.exports = {
  BASE_PRESUPUESTO_SELECT,
  fetchPresupuestoById,
  getPresupuestosForPeriod,
  listPresupuestos,
  getPresupuesto,
  createPresupuesto,
  updatePresupuesto,
  deletePresupuesto
};
