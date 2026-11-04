'use strict';

const { getDb, formatCategoria } = require('../db');
const { AppError, sendSuccess } = require('../utils/response');
const {
  ENUMS,
  nowIsoUtc,
  checkNoEmojisInFields
} = require('../utils/validators');

const ICONO_REGEX = /^fa-(solid|regular|brands) fa-[a-z0-9-]+$/;
const COLOR_REGEX = /^#[0-9A-Fa-f]{6}$/;

function listCategorias(req, res, query) {
  const db = getDb();
  const detalles = [];

  let filterTipo = null;
  if (query.tipo !== undefined && query.tipo !== '') {
    const t = String(query.tipo).trim();
    if (!ENUMS.TIPO.includes(t)) {
      detalles.push({ campo: 'tipo', mensaje: 'Debe ser INGRESO o GASTO.' });
    } else {
      filterTipo = t;
    }
  }

  let incluirInactivas = false;
  if (query.incluir_inactivas !== undefined && query.incluir_inactivas !== '') {
    const inc = String(query.incluir_inactivas).trim();
    if (inc !== 'true' && inc !== 'false') {
      detalles.push({ campo: 'incluir_inactivas', mensaje: 'Debe ser true o false.' });
    } else {
      incluirInactivas = inc === 'true';
    }
  }

  if (detalles.length > 0) {
    throw new AppError(422, 'VALIDACION_FALLIDA', 'Parametros de consulta no validos.', detalles);
  }

  const conditions = [];
  const params = [];

  if (filterTipo) {
    conditions.push('tipo = ?');
    params.push(filterTipo);
  }
  if (!incluirInactivas) {
    conditions.push('activa = 1');
  }

  const whereSql = conditions.length > 0 ? `WHERE ${conditions.join(' AND ')}` : '';
  const sql = `
    SELECT id, nombre, tipo, icono, color, activa, creado_en, actualizado_en
    FROM categorias
    ${whereSql}
    ORDER BY tipo ASC, nombre ASC
  `;

  const rows = db.prepare(sql).all(...params);
  sendSuccess(res, 200, rows.map(formatCategoria), null);
}

function createCategoria(req, res, body) {
  const db = getDb();
  const rawNombre = typeof body.nombre === 'string' ? body.nombre : body.nombre;
  const rawIcono = typeof body.icono === 'string' ? body.icono : body.icono;
  const rawColor = body.color === undefined || body.color === null ? '#4A5568' : body.color;
  const rawTipo = body.tipo;

  checkNoEmojisInFields({
    nombre: rawNombre,
    icono: rawIcono,
    color: rawColor,
    tipo: rawTipo
  });

  const detalles = [];

  const nombre = typeof rawNombre === 'string' ? rawNombre.trim() : '';
  if (typeof rawNombre !== 'string' || nombre.length < 1 || nombre.length > 60) {
    detalles.push({
      campo: 'nombre',
      mensaje: 'El nombre es obligatorio y debe tener entre 1 y 60 caracteres.'
    });
  }

  const tipo = typeof rawTipo === 'string' ? rawTipo.trim() : '';
  if (!ENUMS.TIPO.includes(tipo)) {
    detalles.push({
      campo: 'tipo',
      mensaje: 'El tipo debe ser INGRESO o GASTO.'
    });
  }

  const icono = typeof rawIcono === 'string' ? rawIcono.trim() : '';
  if (typeof rawIcono !== 'string' || !ICONO_REGEX.test(icono)) {
    detalles.push({
      campo: 'icono',
      mensaje: 'El icono debe comenzar por fa-solid fa-, fa-regular fa- o fa-brands fa-.'
    });
  }

  const color = typeof rawColor === 'string' ? rawColor.trim() : '';
  if (typeof rawColor !== 'string' || !COLOR_REGEX.test(color)) {
    detalles.push({
      campo: 'color',
      mensaje: 'El color debe tener formato hexadecimal de 7 caracteres (#RRGGBB).'
    });
  }

  if (detalles.length > 0) {
    throw new AppError(422, 'VALIDACION_FALLIDA', 'Uno o mas campos no son validos.', detalles);
  }

  const dup = db
    .prepare('SELECT id FROM categorias WHERE nombre = ? COLLATE NOCASE')
    .get(nombre);
  if (dup) {
    throw new AppError(
      409,
      'CATEGORIA_DUPLICADA',
      'Ya existe una categoria con ese nombre.',
      [{ campo: 'nombre', mensaje: 'Nombre de categoria duplicado.' }]
    );
  }

  const now = nowIsoUtc();
  const result = db
    .prepare(`
      INSERT INTO categorias (nombre, tipo, icono, color, activa, creado_en, actualizado_en)
      VALUES (?, ?, ?, ?, 1, ?, ?)
    `)
    .run(nombre, tipo, icono, color.toUpperCase(), now, now);

  const created = db
    .prepare('SELECT * FROM categorias WHERE id = ?')
    .get(Number(result.lastInsertRowid));

  sendSuccess(res, 201, formatCategoria(created), null);
}

function updateCategoria(req, res, id, body) {
  const db = getDb();
  const existing = db.prepare('SELECT * FROM categorias WHERE id = ?').get(id);
  if (!existing) {
    throw new AppError(404, 'RECURSO_NO_ENCONTRADO', 'La categoria solicitada no existe.');
  }

  checkNoEmojisInFields({
    nombre: body.nombre,
    icono: body.icono,
    color: body.color
  });

  const detalles = [];

  const nombre = typeof body.nombre === 'string' ? body.nombre.trim() : '';
  if (typeof body.nombre !== 'string' || nombre.length < 1 || nombre.length > 60) {
    detalles.push({
      campo: 'nombre',
      mensaje: 'El nombre es obligatorio y debe tener entre 1 y 60 caracteres.'
    });
  }

  const icono = typeof body.icono === 'string' ? body.icono.trim() : '';
  if (typeof body.icono !== 'string' || !ICONO_REGEX.test(icono)) {
    detalles.push({
      campo: 'icono',
      mensaje: 'El icono debe comenzar por fa-solid fa-, fa-regular fa- o fa-brands fa-.'
    });
  }

  const color = typeof body.color === 'string' ? body.color.trim() : '';
  if (typeof body.color !== 'string' || !COLOR_REGEX.test(color)) {
    detalles.push({
      campo: 'color',
      mensaje: 'El color debe tener formato hexadecimal de 7 caracteres (#RRGGBB).'
    });
  }

  if (typeof body.activa !== 'boolean') {
    detalles.push({
      campo: 'activa',
      mensaje: 'El campo activa es obligatorio y debe ser booleano (true o false).'
    });
  }

  if (detalles.length > 0) {
    throw new AppError(422, 'VALIDACION_FALLIDA', 'Uno o mas campos no son validos.', detalles);
  }

  const dup = db
    .prepare('SELECT id FROM categorias WHERE nombre = ? COLLATE NOCASE AND id != ?')
    .get(nombre, id);
  if (dup) {
    throw new AppError(
      409,
      'CATEGORIA_DUPLICADA',
      'Ya existe otra categoria con ese nombre.',
      [{ campo: 'nombre', mensaje: 'Nombre de categoria duplicado.' }]
    );
  }

  const now = nowIsoUtc();
  db.prepare(`
    UPDATE categorias
    SET nombre = ?, icono = ?, color = ?, activa = ?, actualizado_en = ?
    WHERE id = ?
  `).run(nombre, icono, color.toUpperCase(), body.activa ? 1 : 0, now, id);

  const updated = db.prepare('SELECT * FROM categorias WHERE id = ?').get(id);
  sendSuccess(res, 200, formatCategoria(updated), null);
}

function deleteCategoria(req, res, id) {
  const db = getDb();
  const existing = db.prepare('SELECT id FROM categorias WHERE id = ?').get(id);
  if (!existing) {
    throw new AppError(404, 'RECURSO_NO_ENCONTRADO', 'La categoria solicitada no existe.');
  }

  const txRef = db
    .prepare('SELECT id FROM transacciones WHERE categoria_id = ? LIMIT 1')
    .get(id);
  const presRef = db
    .prepare('SELECT id FROM presupuestos WHERE categoria_id = ? LIMIT 1')
    .get(id);

  if (txRef || presRef) {
    throw new AppError(
      409,
      'CATEGORIA_EN_USO',
      'No se puede eliminar la categoria porque tiene transacciones o presupuestos asociados.'
    );
  }

  db.prepare('DELETE FROM categorias WHERE id = ?').run(id);
  sendSuccess(res, 200, { id, eliminado: true }, null);
}

module.exports = {
  listCategorias,
  createCategoria,
  updateCategoria,
  deleteCategoria
};
