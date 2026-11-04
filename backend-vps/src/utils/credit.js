'use strict';

// Calculos de amortizacion y deuda de tarjetas de credito.
// Toda la aritmetica interna se realiza en centavos enteros.

const MAX_INSTALLMENTS = 48;

function toCents(units) {
  return Math.round(Number(Number(units * 100).toPrecision(15)));
}

function fromCents(cents) {
  return Number((cents / 100).toFixed(2));
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

function monthsElapsed(fechaStr, today) {
  const y = Number(fechaStr.slice(0, 4));
  const m = Number(fechaStr.slice(5, 7));
  return (today.getUTCFullYear() - y) * 12 + (today.getUTCMonth() + 1 - m);
}

/**
 * Resumen de deuda por tarjeta a partir de las compras con tarjeta.
 * La primera cuota se considera pagada al cambiar de mes calendario.
 */
function getCardDebtSummary(db, today = new Date()) {
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

  const byCard = new Map();
  for (const card of cards) {
    byCard.set(Number(card.id), {
      card,
      debt_cents: 0,
      pending_interest_cents: 0,
      monthly_commitment_cents: 0,
      interest_purchases_cents: 0,
      exempt_purchases_cents: 0,
      total_interest_all_cents: 0,
      active: []
    });
  }

  for (const tx of txRows) {
    const entry = byCard.get(Number(tx.card_id));
    if (!entry) continue;
    const exempt = Number(tx.is_interest_exempt) === 1;
    const n = Number(tx.installments) || 1;
    const rate = Number(tx.applied_interest_rate) || 0;
    const principal = Number(tx.monto_centavos);
    const schedule = buildSchedule(principal, n, rate, exempt);

    const paid = Math.max(0, Math.min(n, monthsElapsed(tx.fecha, today)));
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
      cuota_mensual_centavos: schedule.installmentCents
    });
  }

  return Array.from(byCard.values());
}

module.exports = {
  MAX_INSTALLMENTS,
  toCents,
  fromCents,
  buildSchedule,
  formatScheduleRows,
  simulate,
  getCardDebtSummary
};
