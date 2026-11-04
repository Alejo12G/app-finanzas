'use strict';

// Calculos de amortizacion, ciclos de corte, pago minimo y deuda de tarjetas de credito.
// Toda la aritmetica interna se realiza en centavos enteros.

const MAX_INSTALLMENTS = 48;

function toCents(units) {
  return Math.round(Number(Number(units * 100).toPrecision(15)));
}

function fromCents(cents) {
  return Number((cents / 100).toFixed(2));
}

/**
 * Normaliza y valida una fecha asegurando que el dia no exceda los dias del mes.
 * Maneja desbordamiento de meses (mes > 12 o mes < 1).
 */
function clampDateStr(year, month, day) {
  let y = year;
  let m = month;
  while (m > 12) {
    y += 1;
    m -= 12;
  }
  while (m < 1) {
    y -= 1;
    m += 12;
  }
  const maxDay = new Date(Date.UTC(y, m, 0)).getUTCDate();
  const validDay = Math.max(1, Math.min(day, maxDay));
  const mm = String(m).padStart(2, '0');
  const dd = String(validDay).padStart(2, '0');
  return `${y}-${mm}-${dd}`;
}

/**
 * Calcula la primera fecha limite de pago para una compra con tarjeta de credito.
 * Regla:
 * 1. Si el dia de compra <= cutoff_day, entra en el corte del mes de la compra.
 *    Si el dia de compra > cutoff_day, entra en el corte del mes siguiente.
 * 2. Si cutoff_day >= payment_day (ej. corte 20, pago 10), el pago vence en el mes SIGUIENTE al corte.
 *    Si cutoff_day < payment_day (ej. corte 5, pago 20), el pago vence en el MISMO mes del corte.
 */
function getFirstPaymentDate(purchaseDateStr, cutoffDay, paymentDay) {
  const py = Number(purchaseDateStr.slice(0, 4));
  const pm = Number(purchaseDateStr.slice(5, 7));
  const pd = Number(purchaseDateStr.slice(8, 10));

  let cutoffYear = py;
  let cutoffMonth = pd <= cutoffDay ? pm : pm + 1;

  let paymentYear = cutoffYear;
  let paymentMonth = cutoffDay >= paymentDay ? cutoffMonth + 1 : cutoffMonth;

  return clampDateStr(paymentYear, paymentMonth, paymentDay);
}

/**
 * Calcula todas las fechas de vencimiento de las cuotas k = 1..n
 */
function getInstallmentDueDates(purchaseDateStr, installments, cutoffDay, paymentDay) {
  const firstDueDate = getFirstPaymentDate(purchaseDateStr, cutoffDay, paymentDay);
  const fy = Number(firstDueDate.slice(0, 4));
  const fm = Number(firstDueDate.slice(5, 7));

  const dueDates = [];
  for (let k = 0; k < installments; k++) {
    dueDates.push(clampDateStr(fy, fm + k, paymentDay));
  }
  return dueDates;
}

/**
 * Calcula cuantas cuotas ya han vencido/sido pagadas a la fecha actual (todayStr: YYYY-MM-DD).
 */
function countPaidInstallments(purchaseDateStr, installments, cutoffDay, paymentDay, todayStr) {
  const dueDates = getInstallmentDueDates(purchaseDateStr, installments, cutoffDay, paymentDay);
  let paid = 0;
  for (const dueDate of dueDates) {
    if (todayStr >= dueDate) {
      paid++;
    } else {
      break;
    }
  }
  return Math.min(installments, paid);
}

/**
 * Determina las fechas clave del ciclo actual para una tarjeta:
 * - prev_cutoff_date: Fecha del corte anterior.
 * - next_cutoff_date: Fecha del proximo corte.
 * - next_payment_date: Fecha limite de pago mas proxima exigible.
 * - statement_cutoff_date: Fecha de corte del extracto exigible en next_payment_date.
 */
function getCardBillingCycle(cutoffDay, paymentDay, todayStr) {
  const ty = Number(todayStr.slice(0, 4));
  const tm = Number(todayStr.slice(5, 7));
  const td = Number(todayStr.slice(8, 10));

  let nextCutoffYear = ty;
  let nextCutoffMonth = td <= cutoffDay ? tm : tm + 1;
  const nextCutoffDate = clampDateStr(nextCutoffYear, nextCutoffMonth, cutoffDay);

  let prevCutoffYear = ty;
  let prevCutoffMonth = td <= cutoffDay ? tm - 1 : tm;
  const prevCutoffDate = clampDateStr(prevCutoffYear, prevCutoffMonth, cutoffDay);

  let prevPaymentYear = prevCutoffYear;
  let prevPaymentMonth = cutoffDay >= paymentDay ? prevCutoffMonth + 1 : prevCutoffMonth;
  const prevPaymentDate = clampDateStr(prevPaymentYear, prevPaymentMonth, paymentDay);

  let nextPaymentYear = nextCutoffYear;
  let nextPaymentMonth = cutoffDay >= paymentDay ? nextCutoffMonth + 1 : nextCutoffMonth;
  const nextPaymentDateForNextCutoff = clampDateStr(nextPaymentYear, nextPaymentMonth, paymentDay);

  let nextPaymentDate;
  let statementCutoffDate;
  if (todayStr <= prevPaymentDate) {
    nextPaymentDate = prevPaymentDate;
    statementCutoffDate = prevCutoffDate;
  } else {
    nextPaymentDate = nextPaymentDateForNextCutoff;
    statementCutoffDate = nextCutoffDate;
  }

  return {
    prev_cutoff_date: prevCutoffDate,
    next_cutoff_date: nextCutoffDate,
    next_payment_date: nextPaymentDate,
    statement_cutoff_date: statementCutoffDate
  };
}

/**
 * Genera el plan de pagos (sistema frances, cuota fija).
 * @param {number} principalCents capital en centavos
 * @param {number} installments numero de cuotas
 * @param {number} ratePct tasa mensual en porcentaje (2.15 = 2.15% M.V.)
 * @param {boolean} exempt true para compra a 0% de interes
 */
function buildSchedule(principalCents, installments, ratePct, exempt) {
  const n = installments;
  const noInterest = exempt === true || !(ratePct > 0);
  const rows = [];

  if (noInterest) {
    const base = Math.floor(principalCents / n);
    let balance = principalCents;
    for (let k = 1; k <= n; k++) {
      const principal = k === n ? balance : base;
      balance -= principal;
      rows.push({ k, principal, interest: 0, balance });
    }
    return {
      installmentCents: Math.round(principalCents / n),
      totalInterestCents: 0,
      totalWithInterestCents: principalCents,
      rows
    };
  }

  const r = ratePct / 100;
  const installment = Math.round((principalCents * r) / (1 - Math.pow(1 + r, -n)));
  let balance = principalCents;
  let totalInterest = 0;
  for (let k = 1; k <= n; k++) {
    const interest = Math.round(balance * r);
    const principal = k === n ? balance : installment - interest;
    balance -= principal;
    totalInterest += interest;
    rows.push({ k, principal, interest, balance });
  }
  return {
    installmentCents: installment,
    totalInterestCents: totalInterest,
    totalWithInterestCents: principalCents + totalInterest,
    rows
  };
}

function formatScheduleRows(rows) {
  return rows.map((row) => ({
    installment_number: row.k,
    principal: fromCents(row.principal),
    interest: fromCents(row.interest),
    remaining_balance: fromCents(row.balance)
  }));
}

function simulate(amountUnits, installments, ratePct, exempt) {
  const principalCents = toCents(amountUnits);
  const schedule = buildSchedule(principalCents, installments, ratePct, exempt);
  const noInterest = exempt === true || !(ratePct > 0);
  return {
    amount: fromCents(principalCents),
    installments,
    interest_rate: noInterest ? 0 : ratePct,
    is_interest_exempt: exempt === true,
    monthly_installment_amount: fromCents(schedule.installmentCents),
    total_interest_amount: fromCents(schedule.totalInterestCents),
    total_with_interest: fromCents(schedule.totalWithInterestCents),
    amortization_table: formatScheduleRows(schedule.rows)
  };
}

/**
 * Resumen de deuda y facturacion por tarjeta a partir de compras y pagos.
 */
function getCardDebtSummary(db, today = new Date()) {
  const todayStr = typeof today === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(today)
    ? today
    : (today instanceof Date ? today : new Date()).toISOString().slice(0, 10);

  const cards = db.prepare('SELECT * FROM credit_cards ORDER BY id ASC').all();
  const txRows = db
    .prepare(`
      SELECT id, card_id, monto_centavos, fecha, descripcion, comercio, installments,
             is_interest_exempt, applied_interest_rate
      FROM transacciones
      WHERE card_id IS NOT NULL AND tipo = 'GASTO'
      ORDER BY fecha ASC, id ASC
    `)
    .all();

  // Pagos registrados agrupados por tarjeta
  let paymentsRows = [];
  try {
    paymentsRows = db
      .prepare(`
        SELECT card_id, fecha, monto_centavos
        FROM credit_card_payments
        ORDER BY fecha ASC, id ASC
      `)
      .all();
  } catch (_e) {
    paymentsRows = [];
  }

  const byCard = new Map();
  for (const card of cards) {
    const cutoffDay = Number(card.cutoff_day);
    const paymentDay = Number(card.payment_day);
    const cycle = getCardBillingCycle(cutoffDay, paymentDay, todayStr);

    byCard.set(Number(card.id), {
      card,
      cycle,
      debt_cents: 0,
      pending_interest_cents: 0,
      monthly_commitment_cents: 0,
      minimum_payment_cents: 0,
      full_payment_cents: 0,
      interest_purchases_cents: 0,
      exempt_purchases_cents: 0,
      total_interest_all_cents: 0,
      active: []
    });
  }

  for (const tx of txRows) {
    const entry = byCard.get(Number(tx.card_id));
    if (!entry) continue;

    const cutoffDay = Number(entry.card.cutoff_day);
    const paymentDay = Number(entry.card.payment_day);
    const exempt = Number(tx.is_interest_exempt) === 1;
    const n = Number(tx.installments) || 1;
    const rate = Number(tx.applied_interest_rate) || 0;
    const principal = Number(tx.monto_centavos);
    const schedule = buildSchedule(principal, n, rate, exempt);

    const paid = countPaidInstallments(tx.fecha, n, cutoffDay, paymentDay, todayStr);
    entry.total_interest_all_cents += schedule.totalInterestCents;
    if (exempt || rate === 0) {
      entry.exempt_purchases_cents += principal;
    } else {
      entry.interest_purchases_cents += principal;
    }

    if (paid >= n) continue;

    const remainingPrincipal = paid === 0 ? principal : schedule.rows[paid - 1].balance;
    let pendingInterest = 0;
    for (let k = paid; k < n; k++) {
      pendingInterest += schedule.rows[k].interest;
    }
    entry.debt_cents += remainingPrincipal;
    entry.pending_interest_cents += pendingInterest;
    entry.monthly_commitment_cents += schedule.installmentCents;

    // Verificar si alguna cuota vence en o antes de next_payment_date
    const dueDates = getInstallmentDueDates(tx.fecha, n, cutoffDay, paymentDay);
    const isDueOnNextPayment = dueDates.some((d, idx) => idx >= paid && d <= entry.cycle.next_payment_date);

    if (isDueOnNextPayment) {
      if (n === 1) {
        entry.full_payment_cents += principal;
        const min1 = exempt ? principal : Math.max(Math.round(principal * 0.05), Math.min(principal, 5000000));
        entry.minimum_payment_cents += min1;
      } else {
        entry.full_payment_cents += schedule.installmentCents;
        entry.minimum_payment_cents += schedule.installmentCents;
      }
    }

    entry.active.push({
      transaccion_id: Number(tx.id),
      descripcion: tx.descripcion,
      comercio: tx.comercio || null,
      fecha: tx.fecha,
      cuotas_totales: n,
      cuotas_pagadas: paid,
      tasa_mensual_aplicada: exempt ? 0 : rate,
      exenta_de_interes: exempt || rate === 0,
      saldo_capital_pendiente_centavos: remainingPrincipal,
      intereses_pendientes_centavos: pendingInterest,
      cuota_mensual_centavos: schedule.installmentCents,
      primer_vencimiento: dueDates[0] || null,
      proximo_vencimiento: dueDates[paid] || null
    });
  }

  // Descontar pagos registrados en el ciclo actual
  for (const p of paymentsRows) {
    const entry = byCard.get(Number(p.card_id));
    if (!entry) continue;
    if (p.fecha > entry.cycle.prev_cutoff_date && p.fecha <= entry.cycle.next_payment_date) {
      const pCentavos = Number(p.monto_centavos);
      entry.minimum_payment_cents = Math.max(0, entry.minimum_payment_cents - pCentavos);
      entry.full_payment_cents = Math.max(0, entry.full_payment_cents - pCentavos);
      entry.debt_cents = Math.max(0, entry.debt_cents - pCentavos);
    }
  }

  return Array.from(byCard.values());
}

module.exports = {
  MAX_INSTALLMENTS,
  toCents,
  fromCents,
  clampDateStr,
  getFirstPaymentDate,
  getInstallmentDueDates,
  countPaidInstallments,
  getCardBillingCycle,
  buildSchedule,
  formatScheduleRows,
  simulate,
  getCardDebtSummary
};
