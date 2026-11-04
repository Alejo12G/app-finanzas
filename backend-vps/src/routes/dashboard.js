'use strict';

const { getDb, formatCategoriaResumen, formatTransaccion } = require('../db');
const { BASE_TX_SELECT } = require('./transacciones');
const { getPresupuestosForPeriod } = require('./presupuestos');
const { AppError, sendSuccess } = require('../utils/response');
const { getMonthRange, roundHalfUp2 } = require('../utils/validators');

function getPreviousYearMonth(anio, mes, offsetMonths = 1) {
  let y = anio;
  let m = mes - offsetMonths;
  while (m <= 0) {
    m += 12;
    y -= 1;
  }
  return { anio: y, mes: m };
}

function computePeriodTotals(db, desde, hasta) {
  const row = db
    .prepare(`
      SELECT
        COALESCE(SUM(CASE WHEN tipo = 'INGRESO' THEN monto_centavos ELSE 0 END), 0) AS ingresos_centavos,
        COALESCE(SUM(CASE WHEN tipo = 'GASTO' THEN monto_centavos ELSE 0 END), 0) AS gastos_centavos,
        COALESCE(SUM(CASE WHEN tipo = 'GASTO' AND naturaleza = 'FIJO' THEN monto_centavos ELSE 0 END), 0) AS gastos_fijos_centavos,
        COALESCE(SUM(CASE WHEN tipo = 'GASTO' AND naturaleza = 'VARIABLE' THEN monto_centavos ELSE 0 END), 0) AS gastos_variables_centavos,
        COUNT(*) AS cantidad_transacciones
      FROM transacciones
      WHERE fecha >= ? AND fecha <= ?
    `)
    .get(desde, hasta);

  const ingresos = Number(row.ingresos_centavos || 0);
  const gastos = Number(row.gastos_centavos || 0);
  const fijos = Number(row.gastos_fijos_centavos || 0);
  const variables = Number(row.gastos_variables_centavos || 0);
  const balance = ingresos - gastos;
  const tasaAhorro = ingresos > 0 ? roundHalfUp2((balance / ingresos) * 100) : 0;
  const cantidad = Number(row.cantidad_transacciones || 0);

  return {
    ingresos_centavos: ingresos,
    gastos_centavos: gastos,
    gastos_fijos_centavos: fijos,
    gastos_variables_centavos: variables,
    balance_centavos: balance,
    tasa_ahorro_porcentaje: tasaAhorro,
    cantidad_transacciones: cantidad
  };
}

function computeCategoryBreakdown(db, tipo, desde, hasta, totalTipoCentavos) {
  if (totalTipoCentavos <= 0) {
    return [];
  }

  const rows = db
    .prepare(`
      SELECT
        c.id, c.nombre, c.tipo, c.icono, c.color,
        SUM(t.monto_centavos) AS total_centavos
      FROM transacciones t
      JOIN categorias c ON c.id = t.categoria_id
      WHERE t.tipo = ? AND t.fecha >= ? AND t.fecha <= ?
      GROUP BY c.id
      HAVING SUM(t.monto_centavos) > 0
      ORDER BY total_centavos DESC, c.id ASC
    `)
    .all(tipo, desde, hasta);

  return rows.map((r) => {
    const total = Number(r.total_centavos);
    return {
      categoria: formatCategoriaResumen(r),
      total_centavos: total,
      porcentaje: roundHalfUp2((total / totalTipoCentavos) * 100)
    };
  });
}

function buildDashboardData(db, anio, mes) {
  const { desde, hasta } = getMonthRange(anio, mes);
  const totales = computePeriodTotals(db, desde, hasta);

  // Comparativo mes anterior
  const prev = getPreviousYearMonth(anio, mes, 1);
  const prevRange = getMonthRange(prev.anio, prev.mes);
  const prevTotales = computePeriodTotals(db, prevRange.desde, prevRange.hasta);

  const ingresosVariacion =
    prevTotales.ingresos_centavos > 0
      ? roundHalfUp2(
          ((totales.ingresos_centavos - prevTotales.ingresos_centavos) /
            prevTotales.ingresos_centavos) *
            100
        )
      : null;

  const gastosVariacion =
    prevTotales.gastos_centavos > 0
      ? roundHalfUp2(
          ((totales.gastos_centavos - prevTotales.gastos_centavos) /
            prevTotales.gastos_centavos) *
            100
        )
      : null;

  const gastosPorCategoria = computeCategoryBreakdown(
    db,
    'GASTO',
    desde,
    hasta,
    totales.gastos_centavos
  );

  const ingresosPorCategoria = computeCategoryBreakdown(
    db,
    'INGRESO',
    desde,
    hasta,
    totales.ingresos_centavos
  );

  // Resumen de presupuestos
  const presData = getPresupuestosForPeriod(db, anio, mes, null);
  let cantidadNormal = 0;
  let cantidadAdvertencia = 0;
  let cantidadExcedido = 0;
  for (const p of presData.presupuestos) {
    if (p.estado === 'NORMAL') cantidadNormal += 1;
    else if (p.estado === 'ADVERTENCIA') cantidadAdvertencia += 1;
    else if (p.estado === 'EXCEDIDO') cantidadExcedido += 1;
  }

  // Evolucion mensual: exactamente 6 elementos en orden cronologico ascendente
  const evolucionMensual = [];
  for (let i = 5; i >= 0; i--) {
    const pm = getPreviousYearMonth(anio, mes, i);
    const pmRange = getMonthRange(pm.anio, pm.mes);
    const pmTot = computePeriodTotals(db, pmRange.desde, pmRange.hasta);
    evolucionMensual.push({
      anio: pm.anio,
      mes: pm.mes,
      ingresos_centavos: pmTot.ingresos_centavos,
      gastos_centavos: pmTot.gastos_centavos,
      balance_centavos: pmTot.balance_centavos
    });
  }

  // Ultimas 5 transacciones del periodo
  const ultimasRows = db
    .prepare(`
      ${BASE_TX_SELECT}
      WHERE t.fecha >= ? AND t.fecha <= ?
      ORDER BY t.fecha DESC, t.id DESC
      LIMIT 5
    `)
    .all(desde, hasta);

  return {
    periodo: {
      anio,
      mes,
      desde,
      hasta
    },
    totales,
    comparativo_mes_anterior: {
      ingresos_variacion_porcentaje: ingresosVariacion,
      gastos_variacion_porcentaje: gastosVariacion
    },
    gastos_por_categoria: gastosPorCategoria,
    ingresos_por_categoria: ingresosPorCategoria,
    presupuestos: {
      total_limite_centavos: presData.total_limite_centavos,
      total_gastado_centavos: presData.total_gastado_centavos,
      cantidad_normal: cantidadNormal,
      cantidad_advertencia: cantidadAdvertencia,
      cantidad_excedido: cantidadExcedido
    },
    evolucion_mensual: evolucionMensual,
    ultimas_transacciones: ultimasRows.map(formatTransaccion)
  };
}

function getDashboard(req, res, query) {
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

  if (detalles.length > 0) {
    throw new AppError(422, 'VALIDACION_FALLIDA', 'Parametros de consulta no validos.', detalles);
  }

  const data = buildDashboardData(db, anio, mes);
  sendSuccess(res, 200, data, null);
}

module.exports = {
  getPreviousYearMonth,
  buildDashboardData,
  getDashboard
};
