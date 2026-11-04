'use strict';

const { AppError } = require('./response');

// Rangos de emojis prohibidos segun seccion 2.2 de API_CONTRACT.md:
// U+1F000-U+1FAFF, U+2600-U+27BF, U+FE0F, U+200D, U+1F1E6-U+1F1FF
const EMOJI_REGEX = /[\u{1F000}-\u{1FAFF}\u{2600}-\u{27BF}\u{FE0F}\u{200D}\u{1F1E6}-\u{1F1FF}]/u;
const EMOJI_GLOBAL_REGEX = /[\u{1F000}-\u{1FAFF}\u{2600}-\u{27BF}\u{FE0F}\u{200D}\u{1F1E6}-\u{1F1FF}]/gu;

const ENUMS = {
  TIPO: ['INGRESO', 'GASTO'],
  NATURALEZA: ['FIJO', 'VARIABLE'],
  METODO_PAGO: ['EFECTIVO', 'DEBITO', 'CREDITO', 'TRANSFERENCIA', 'OTRO'],
  UNIDAD: ['UNIDAD', 'KG', 'G', 'L', 'ML', 'PAQUETE', 'OTRO'],
  ESTADO_PRESUPUESTO: ['NORMAL', 'ADVERTENCIA', 'EXCEDIDO'],
  TIPO_ANALISIS: ['RESUMEN_MENSUAL', 'AHORRO', 'PRESUPUESTO', 'PREGUNTA_LIBRE'],
  PRIORIDAD_IA: ['ALTA', 'MEDIA', 'BAJA'],
  CODIGO_ALERTA_IA: [
    'PRESUPUESTO_EXCEDIDO',
    'PRESUPUESTO_EN_RIESGO',
    'BALANCE_NEGATIVO',
    'GASTO_INUSUAL',
    'AHORRO_BAJO',
    'DEUDA_TARJETA',
    'OTRO'
  ]
};

function hasEmoji(text) {
  if (typeof text !== 'string') return false;
  return EMOJI_REGEX.test(text);
}

function stripEmojis(text) {
  if (typeof text !== 'string') return '';
  return text.replace(EMOJI_GLOBAL_REGEX, '').replace(/\s{2,}/g, ' ').trim();
}

function nowIsoUtc() {
  return new Date().toISOString().replace(/\.\d{3}Z$/, 'Z');
}

function isValidDateStr(str) {
  if (typeof str !== 'string') return false;
  if (!/^\d{4}-\d{2}-\d{2}$/.test(str)) return false;
  const [yStr, mStr, dStr] = str.split('-');
  const y = Number(yStr);
  const m = Number(mStr);
  const d = Number(dStr);
  if (y < 1900 || y > 2200 || m < 1 || m > 12 || d < 1) return false;
  const maxDays = new Date(Date.UTC(y, m, 0)).getUTCDate();
  return d <= maxDays;
}

function getMonthRange(anio, mes) {
  const maxDays = new Date(Date.UTC(anio, mes, 0)).getUTCDate();
  const mm = String(mes).padStart(2, '0');
  const dd = String(maxDays).padStart(2, '0');
  return {
    desde: `${anio}-${mm}-01`,
    hasta: `${anio}-${mm}-${dd}`
  };
}

function roundHalfUp(value) {
  const clean = Number(Number(value).toPrecision(15));
  if (clean >= 0) {
    return Math.floor(clean + 0.5);
  }
  return Math.ceil(clean - 0.5);
}

function roundHalfUp2(value) {
  const scaled = Number(Number(value * 100).toPrecision(15));
  const rounded = scaled >= 0 ? Math.floor(scaled + 0.5) : Math.ceil(scaled - 0.5);
  return Number((rounded / 100).toFixed(2));
}

function hasMax3Decimals(num) {
  if (typeof num !== 'number' || !Number.isFinite(num)) return false;
  const str = String(num);
  if (str.includes('e') || str.includes('E')) {
    const fixed = num.toFixed(6).replace(/0+$/, '');
    const parts = fixed.split('.');
    return !parts[1] || parts[1].length <= 3;
  }
  const parts = str.split('.');
  return !parts[1] || parts[1].length <= 3;
}

function parsePositiveIntId(rawId) {
  if (typeof rawId !== 'string' || !/^[1-9]\d*$/.test(rawId)) {
    return null;
  }
  const id = Number(rawId);
  if (!Number.isSafeInteger(id) || id <= 0) {
    return null;
  }
  return id;
}

function checkNoEmojisInFields(fieldMap) {
  const emojiDetails = [];
  for (const [campo, valor] of Object.entries(fieldMap)) {
    if (typeof valor === 'string' && hasEmoji(valor)) {
      emojiDetails.push({
        campo,
        mensaje: 'El texto no puede contener emojis ni pictogramas.'
      });
    }
  }
  if (emojiDetails.length > 0) {
    throw new AppError(
      422,
      'TEXTO_CON_EMOJI',
      'Uno o mas campos contienen emojis no permitidos.',
      emojiDetails
    );
  }
}

function parsePagination(query) {
  const detalles = [];
  let pagina = 1;
  let por_pagina = 50;

  if (query.pagina !== undefined && query.pagina !== '') {
    if (!/^-?\d+$/.test(String(query.pagina))) {
      detalles.push({ campo: 'pagina', mensaje: 'Debe ser un entero mayor o igual a 1.' });
    } else {
      pagina = Number(query.pagina);
      if (!Number.isSafeInteger(pagina) || pagina < 1) {
        detalles.push({ campo: 'pagina', mensaje: 'Debe ser un entero mayor o igual a 1.' });
      }
    }
  }

  if (query.por_pagina !== undefined && query.por_pagina !== '') {
    if (!/^-?\d+$/.test(String(query.por_pagina))) {
      detalles.push({ campo: 'por_pagina', mensaje: 'Debe ser un entero entre 1 y 200.' });
    } else {
      por_pagina = Number(query.por_pagina);
      if (!Number.isSafeInteger(por_pagina) || por_pagina < 1 || por_pagina > 200) {
        detalles.push({ campo: 'por_pagina', mensaje: 'Debe ser un entero entre 1 y 200.' });
      }
    }
  }

  return { pagina, por_pagina, detalles };
}

module.exports = {
  ENUMS,
  hasEmoji,
  stripEmojis,
  nowIsoUtc,
  isValidDateStr,
  getMonthRange,
  roundHalfUp,
  roundHalfUp2,
  hasMax3Decimals,
  parsePositiveIntId,
  checkNoEmojisInFields,
  parsePagination
};
