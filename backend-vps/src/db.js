'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { DatabaseSync } = require('node:sqlite');
const config = require('./config');
const { nowIsoUtc, roundHalfUp2 } = require('./utils/validators');

const SEED_CATEGORIAS = [
  { id: 1,  nombre: 'Salario',              tipo: 'INGRESO', icono: 'fa-solid fa-briefcase',                   color: '#2E7D32' },
  { id: 2,  nombre: 'Honorarios',           tipo: 'INGRESO', icono: 'fa-solid fa-laptop-code',                 color: '#2E7D32' },
  { id: 3,  nombre: 'Inversiones',          tipo: 'INGRESO', icono: 'fa-solid fa-chart-line',                  color: '#2E7D32' },
  { id: 4,  nombre: 'Ventas',               tipo: 'INGRESO', icono: 'fa-solid fa-hand-holding-dollar',         color: '#2E7D32' },
  { id: 5,  nombre: 'Reembolsos',           tipo: 'INGRESO', icono: 'fa-solid fa-rotate-left',                 color: '#2E7D32' },
  { id: 6,  nombre: 'Otros ingresos',       tipo: 'INGRESO', icono: 'fa-solid fa-coins',                       color: '#2E7D32' },
  { id: 7,  nombre: 'Vivienda',             tipo: 'GASTO',   icono: 'fa-solid fa-house-chimney',               color: '#1B2A4A' },
  { id: 8,  nombre: 'Electricidad',         tipo: 'GASTO',   icono: 'fa-solid fa-bolt',                        color: '#1B2A4A' },
  { id: 9,  nombre: 'Agua',                 tipo: 'GASTO',   icono: 'fa-solid fa-droplet',                     color: '#1B2A4A' },
  { id: 10, nombre: 'Internet y telefonia', tipo: 'GASTO',   icono: 'fa-solid fa-wifi',                        color: '#1B2A4A' },
  { id: 11, nombre: 'Supermercado',         tipo: 'GASTO',   icono: 'fa-solid fa-basket-shopping',             color: '#4A5568' },
  { id: 12, nombre: 'Restaurantes',         tipo: 'GASTO',   icono: 'fa-solid fa-utensils',                    color: '#4A5568' },
  { id: 13, nombre: 'Transporte',           tipo: 'GASTO',   icono: 'fa-solid fa-bus',                         color: '#4A5568' },
  { id: 14, nombre: 'Combustible',          tipo: 'GASTO',   icono: 'fa-solid fa-gas-pump',                    color: '#4A5568' },
  { id: 15, nombre: 'Vehiculo',             tipo: 'GASTO',   icono: 'fa-solid fa-car',                         color: '#4A5568' },
  { id: 16, nombre: 'Salud',                tipo: 'GASTO',   icono: 'fa-solid fa-heart-pulse',                 color: '#4A5568' },
  { id: 17, nombre: 'Farmacia',             tipo: 'GASTO',   icono: 'fa-solid fa-prescription-bottle-medical', color: '#4A5568' },
  { id: 18, nombre: 'Educacion',            tipo: 'GASTO',   icono: 'fa-solid fa-graduation-cap',              color: '#4A5568' },
  { id: 19, nombre: 'Entretenimiento',      tipo: 'GASTO',   icono: 'fa-solid fa-film',                        color: '#4A5568' },
  { id: 20, nombre: 'Suscripciones',        tipo: 'GASTO',   icono: 'fa-solid fa-repeat',                      color: '#1B2A4A' },
  { id: 21, nombre: 'Ropa',                 tipo: 'GASTO',   icono: 'fa-solid fa-shirt',                       color: '#4A5568' },
  { id: 22, nombre: 'Hogar',                tipo: 'GASTO',   icono: 'fa-solid fa-couch',                       color: '#4A5568' },
  { id: 23, nombre: 'Seguros',              tipo: 'GASTO',   icono: 'fa-solid fa-shield-halved',               color: '#1B2A4A' },
  { id: 24, nombre: 'Deudas y prestamos',   tipo: 'GASTO',   icono: 'fa-solid fa-credit-card',                 color: '#C62828' },
  { id: 25, nombre: 'Impuestos',            tipo: 'GASTO',   icono: 'fa-solid fa-landmark',                    color: '#1B2A4A' },
  { id: 26, nombre: 'Mascotas',             tipo: 'GASTO',   icono: 'fa-solid fa-paw',                         color: '#4A5568' },
  { id: 27, nombre: 'Regalos',              tipo: 'GASTO',   icono: 'fa-solid fa-gift',                        color: '#4A5568' },
  { id: 28, nombre: 'Viajes',               tipo: 'GASTO',   icono: 'fa-solid fa-plane',                       color: '#4A5568' },
  { id: 29, nombre: 'Cuidado personal',     tipo: 'GASTO',   icono: 'fa-solid fa-spa',                         color: '#4A5568' },
  { id: 30, nombre: 'Ahorro',               tipo: 'GASTO',   icono: 'fa-solid fa-piggy-bank',                  color: '#2E7D32' },
  { id: 31, nombre: 'Otros gastos',         tipo: 'GASTO',   icono: 'fa-solid fa-ellipsis',                    color: '#4A5568' }
];

let dbInstance = null;

function getDb() {
  if (dbInstance) {
    return dbInstance;
  }

  const dbDir = path.dirname(config.APP_DB_PATH);
  if (!fs.existsSync(dbDir)) {
    fs.mkdirSync(dbDir, { recursive: true });
  }

  const db = new DatabaseSync(config.APP_DB_PATH);

  db.exec(`
    PRAGMA foreign_keys = ON;
    PRAGMA journal_mode = WAL;
    PRAGMA busy_timeout = 5000;
  `);

  db.exec(`
    CREATE TABLE IF NOT EXISTS categorias (
        id            INTEGER PRIMARY KEY AUTOINCREMENT,
        nombre        TEXT    NOT NULL UNIQUE COLLATE NOCASE,
        tipo          TEXT    NOT NULL CHECK (tipo IN ('INGRESO','GASTO')),
        icono         TEXT    NOT NULL,
        color         TEXT    NOT NULL DEFAULT '#4A5568' CHECK (length(color) = 7 AND substr(color,1,1) = '#'),
        activa        INTEGER NOT NULL DEFAULT 1 CHECK (activa IN (0,1)),
        creado_en     TEXT    NOT NULL,
        actualizado_en TEXT   NOT NULL
    );

    CREATE TABLE IF NOT EXISTS transacciones (
        id             INTEGER PRIMARY KEY AUTOINCREMENT,
        tipo           TEXT    NOT NULL CHECK (tipo IN ('INGRESO','GASTO')),
        naturaleza     TEXT    NOT NULL CHECK (naturaleza IN ('FIJO','VARIABLE')),
        monto_centavos INTEGER NOT NULL CHECK (monto_centavos > 0),
        fecha          TEXT    NOT NULL,
        descripcion    TEXT    NOT NULL,
        categoria_id   INTEGER NOT NULL REFERENCES categorias(id) ON DELETE RESTRICT,
        metodo_pago    TEXT    NOT NULL CHECK (metodo_pago IN ('EFECTIVO','DEBITO','CREDITO','TRANSFERENCIA','OTRO')),
        comercio       TEXT,
        notas          TEXT,
        es_compra      INTEGER NOT NULL DEFAULT 0 CHECK (es_compra IN (0,1)),
        creado_en      TEXT    NOT NULL,
        actualizado_en TEXT    NOT NULL
    );

    CREATE INDEX IF NOT EXISTS idx_transacciones_fecha      ON transacciones(fecha);
    CREATE INDEX IF NOT EXISTS idx_transacciones_categoria  ON transacciones(categoria_id);
    CREATE INDEX IF NOT EXISTS idx_transacciones_tipo_fecha ON transacciones(tipo, fecha);

    CREATE TABLE IF NOT EXISTS detalle_compras (
        id                       INTEGER PRIMARY KEY AUTOINCREMENT,
        transaccion_id           INTEGER NOT NULL REFERENCES transacciones(id) ON DELETE CASCADE,
        articulo                 TEXT    NOT NULL,
        cantidad                 REAL    NOT NULL CHECK (cantidad > 0),
        unidad                   TEXT    NOT NULL DEFAULT 'UNIDAD' CHECK (unidad IN ('UNIDAD','KG','G','L','ML','PAQUETE','OTRO')),
        precio_unitario_centavos INTEGER NOT NULL CHECK (precio_unitario_centavos >= 0),
        subtotal_centavos        INTEGER NOT NULL CHECK (subtotal_centavos >= 0),
        orden                    INTEGER NOT NULL DEFAULT 0,
        creado_en                TEXT    NOT NULL
    );

    CREATE INDEX IF NOT EXISTS idx_detalle_transaccion ON detalle_compras(transaccion_id);
    CREATE INDEX IF NOT EXISTS idx_detalle_articulo    ON detalle_compras(articulo COLLATE NOCASE);

    CREATE TABLE IF NOT EXISTS presupuestos (
        id                    INTEGER PRIMARY KEY AUTOINCREMENT,
        categoria_id          INTEGER NOT NULL REFERENCES categorias(id) ON DELETE RESTRICT,
        anio                  INTEGER NOT NULL CHECK (anio BETWEEN 2000 AND 2100),
        mes                   INTEGER NOT NULL CHECK (mes BETWEEN 1 AND 12),
        monto_limite_centavos INTEGER NOT NULL CHECK (monto_limite_centavos > 0),
        notas                 TEXT,
        creado_en             TEXT    NOT NULL,
        actualizado_en        TEXT    NOT NULL,
        UNIQUE (categoria_id, anio, mes)
    );

    CREATE INDEX IF NOT EXISTS idx_presupuestos_periodo ON presupuestos(anio, mes);

    CREATE TABLE IF NOT EXISTS schema_version (
        version     INTEGER NOT NULL,
        aplicado_en TEXT    NOT NULL
    );
  `);

  // Modulo de tarjetas de credito (migracion idempotente)
  db.exec(`
    CREATE TABLE IF NOT EXISTS credit_cards (
        id                    INTEGER PRIMARY KEY AUTOINCREMENT,
        name                  TEXT    NOT NULL,
        bank                  TEXT    NOT NULL,
        credit_limit_centavos INTEGER NOT NULL CHECK (credit_limit_centavos > 0),
        default_interest_rate REAL    NOT NULL DEFAULT 0 CHECK (default_interest_rate >= 0 AND default_interest_rate <= 100),
        cutoff_day            INTEGER NOT NULL CHECK (cutoff_day BETWEEN 1 AND 31),
        payment_day           INTEGER NOT NULL CHECK (payment_day BETWEEN 1 AND 31),
        creado_en             TEXT    NOT NULL,
        actualizado_en        TEXT    NOT NULL
    );
  `);

  const txColumns = new Set(
    db.prepare('PRAGMA table_info(transacciones)').all().map((c) => c.name)
  );
  const cardColumns = [
    ['card_id', 'INTEGER REFERENCES credit_cards(id) ON DELETE RESTRICT'],
    ['installments', 'INTEGER'],
    ['is_interest_exempt', 'INTEGER'],
    ['applied_interest_rate', 'REAL'],
    ['monthly_installment_centavos', 'INTEGER'],
    ['total_interest_centavos', 'INTEGER'],
    ['total_with_interest_centavos', 'INTEGER']
  ];
  for (const [col, def] of cardColumns) {
    if (!txColumns.has(col)) {
      db.exec(`ALTER TABLE transacciones ADD COLUMN ${col} ${def}`);
    }
  }
  db.exec('CREATE INDEX IF NOT EXISTS idx_transacciones_card ON transacciones(card_id)');

  const versionRow = db.prepare('SELECT version FROM schema_version LIMIT 1').get();
  if (!versionRow) {
    const now = nowIsoUtc();
    db.exec('BEGIN TRANSACTION');
    try {
      const insertCat = db.prepare(`
        INSERT OR IGNORE INTO categorias (id, nombre, tipo, icono, color, activa, creado_en, actualizado_en)
        VALUES (?, ?, ?, ?, ?, 1, ?, ?)
      `);
      for (const c of SEED_CATEGORIAS) {
        insertCat.run(c.id, c.nombre, c.tipo, c.icono, c.color, now, now);
      }
      db.prepare('INSERT INTO schema_version (version, aplicado_en) VALUES (1, ?)').run(now);
      db.exec('COMMIT');
    } catch (err) {
      db.exec('ROLLBACK');
      throw err;
    }
  }

  dbInstance = db;
  return dbInstance;
}

function formatCategoriaResumen(row) {
  return {
    id: Number(row.cat_id !== undefined ? row.cat_id : row.id),
    nombre: row.cat_nombre !== undefined ? row.cat_nombre : row.nombre,
    tipo: row.cat_tipo !== undefined ? row.cat_tipo : row.tipo,
    icono: row.cat_icono !== undefined ? row.cat_icono : row.icono,
    color: row.cat_color !== undefined ? row.cat_color : row.color
  };
}

function formatCategoria(row) {
  return {
    id: Number(row.id),
    nombre: row.nombre,
    tipo: row.tipo,
    icono: row.icono,
    color: row.color,
    activa: Number(row.activa) === 1,
    creado_en: row.creado_en,
    actualizado_en: row.actualizado_en
  };
}

function formatTransaccion(row) {
  const esCompra = Number(row.es_compra) === 1;
  const isCard = row.metodo_pago === 'CREDITO' && row.card_id !== undefined && row.card_id !== null;
  const cents = (v) => (v === undefined || v === null ? null : Number((Number(v) / 100).toFixed(2)));
  return {
    id: Number(row.id),
    tipo: row.tipo,
    naturaleza: row.naturaleza,
    monto_centavos: Number(row.monto_centavos),
    fecha: row.fecha,
    descripcion: row.descripcion,
    categoria_id: Number(row.categoria_id),
    categoria: formatCategoriaResumen(row),
    metodo_pago: row.metodo_pago,
    payment_method: row.metodo_pago === 'CREDITO' ? 'CREDIT_CARD' : row.metodo_pago,
    card_id: isCard ? Number(row.card_id) : null,
    installments: isCard ? Number(row.installments || 1) : null,
    is_interest_exempt: isCard ? Number(row.is_interest_exempt) === 1 : null,
    applied_interest_rate: isCard ? Number(row.applied_interest_rate || 0) : null,
    monthly_installment_amount: isCard ? cents(row.monthly_installment_centavos) : null,
    total_interest_amount: isCard ? cents(row.total_interest_centavos) : null,
    total_with_interest: isCard ? cents(row.total_with_interest_centavos) : null,
    comercio: row.comercio === undefined || row.comercio === null ? null : row.comercio,
    notas: row.notas === undefined || row.notas === null ? null : row.notas,
    es_compra: esCompra,
    cantidad_articulos: esCompra ? Number(row.cantidad_articulos || 0) : 0,
    creado_en: row.creado_en,
    actualizado_en: row.actualizado_en
  };
}

function formatArticuloCompra(row) {
  return {
    id: Number(row.id),
    articulo: row.articulo,
    cantidad: Number(row.cantidad),
    unidad: row.unidad,
    precio_unitario_centavos: Number(row.precio_unitario_centavos),
    subtotal_centavos: Number(row.subtotal_centavos),
    orden: Number(row.orden)
  };
}

function formatCompra(txRow, articulosRows) {
  const formattedArticulos = articulosRows.map(formatArticuloCompra);
  const base = formatTransaccion({
    ...txRow,
    es_compra: 1,
    cantidad_articulos: formattedArticulos.length
  });
  return {
    ...base,
    articulos: formattedArticulos
  };
}

function formatPresupuesto(row) {
  const limite = Number(row.monto_limite_centavos);
  const gastado = Number(row.gastado_centavos || 0);
  const disponible = limite - gastado;
  const porcentajeUsado = limite > 0 ? roundHalfUp2((gastado / limite) * 100) : 0;

  let estado = 'NORMAL';
  if (porcentajeUsado >= 100) {
    estado = 'EXCEDIDO';
  } else if (porcentajeUsado >= 80) {
    estado = 'ADVERTENCIA';
  }

  return {
    id: Number(row.id),
    categoria_id: Number(row.categoria_id),
    categoria: formatCategoriaResumen(row),
    anio: Number(row.anio),
    mes: Number(row.mes),
    monto_limite_centavos: limite,
    gastado_centavos: gastado,
    disponible_centavos: disponible,
    porcentaje_usado: porcentajeUsado,
    estado,
    notas: row.notas === undefined || row.notas === null ? null : row.notas,
    creado_en: row.creado_en,
    actualizado_en: row.actualizado_en
  };
}

module.exports = {
  getDb,
  SEED_CATEGORIAS,
  formatCategoriaResumen,
  formatCategoria,
  formatTransaccion,
  formatArticuloCompra,
  formatCompra,
  formatPresupuesto
};
