'use strict';

(function (global) {
  // Rango exacto de emojis prohibidos segun API_CONTRACT.md seccion 2.2
  const EMOJI_REGEX = /[\u{1F000}-\u{1FAFF}\u{2600}-\u{27BF}\u{FE0F}\u{200D}\u{1F1E6}-\u{1F1FF}]/u;
  const EMOJI_REGEX_GLOBAL = /[\u{1F000}-\u{1FAFF}\u{2600}-\u{27BF}\u{FE0F}\u{200D}\u{1F1E6}-\u{1F1FF}]/gu;

  const MESES_NOMBRES = Object.freeze([
    'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
    'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
  ]);

  const MESES_CORTOS = Object.freeze([
    'Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun',
    'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'
  ]);

  const METODOS_PAGO = Object.freeze({
    EFECTIVO: { valor: 'EFECTIVO', etiqueta: 'Efectivo', icono: 'fa-solid fa-money-bill' },
    DEBITO: { valor: 'DEBITO', etiqueta: 'Tarjeta de debito', icono: 'fa-solid fa-money-check' },
    CREDITO: { valor: 'CREDITO', etiqueta: 'Tarjeta de credito', icono: 'fa-solid fa-credit-card' },
    TRANSFERENCIA: { valor: 'TRANSFERENCIA', etiqueta: 'Transferencia', icono: 'fa-solid fa-building-columns' },
    OTRO: { valor: 'OTRO', etiqueta: 'Otro', icono: 'fa-solid fa-circle-question' }
  });

  const UNIDADES_ARTICULO = Object.freeze({
    UNIDAD: { valor: 'UNIDAD', etiqueta: 'und' },
    KG: { valor: 'KG', etiqueta: 'kg' },
    G: { valor: 'G', etiqueta: 'g' },
    L: { valor: 'L', etiqueta: 'L' },
    ML: { valor: 'ML', etiqueta: 'ml' },
    PAQUETE: { valor: 'PAQUETE', etiqueta: 'paq' },
    OTRO: { valor: 'OTRO', etiqueta: 'otro' }
  });

  const NATURALEZAS = Object.freeze({
    FIJO: { valor: 'FIJO', etiqueta: 'Fijo', icono: 'fa-solid fa-thumbtack' },
    VARIABLE: { valor: 'VARIABLE', etiqueta: 'Variable', icono: 'fa-solid fa-shuffle' }
  });

  const ESTADOS_PRESUPUESTO = Object.freeze({
    NORMAL: { valor: 'NORMAL', etiqueta: 'Normal', color: '#2E7D32', icono: 'fa-solid fa-circle-check' },
    ADVERTENCIA: { valor: 'ADVERTENCIA', etiqueta: 'Advertencia', color: '#B26A00', icono: 'fa-solid fa-triangle-exclamation' },
    EXCEDIDO: { valor: 'EXCEDIDO', etiqueta: 'Excedido', color: '#C62828', icono: 'fa-solid fa-circle-xmark' }
  });

  const PRIORIDADES_IA = Object.freeze({
    ALTA: { valor: 'ALTA', etiqueta: 'Prioridad Alta', icono: 'fa-solid fa-circle-exclamation', clase: 'badge-danger' },
    MEDIA: { valor: 'MEDIA', etiqueta: 'Prioridad Media', icono: 'fa-solid fa-circle-half-stroke', clase: 'badge-warning' },
    BAJA: { valor: 'BAJA', etiqueta: 'Prioridad Baja', icono: 'fa-regular fa-circle', clase: 'badge-neutral' }
  });

  // Catalogo de iconos permitidos en UI_DESIGN.md tablas 5.6 y 5.7
  const ICONOS_CATEGORIAS_PERMITIDOS = Object.freeze([
    // Tabla 5.6 (Semilla)
    'fa-solid fa-briefcase',
    'fa-solid fa-laptop-code',
    'fa-solid fa-chart-line',
    'fa-solid fa-hand-holding-dollar',
    'fa-solid fa-rotate-left',
    'fa-solid fa-coins',
    'fa-solid fa-house-chimney',
    'fa-solid fa-bolt',
    'fa-solid fa-droplet',
    'fa-solid fa-wifi',
    'fa-solid fa-basket-shopping',
    'fa-solid fa-utensils',
    'fa-solid fa-bus',
    'fa-solid fa-gas-pump',
    'fa-solid fa-car',
    'fa-solid fa-heart-pulse',
    'fa-solid fa-prescription-bottle-medical',
    'fa-solid fa-graduation-cap',
    'fa-solid fa-film',
    'fa-solid fa-repeat',
    'fa-solid fa-shirt',
    'fa-solid fa-couch',
    'fa-solid fa-shield-halved',
    'fa-solid fa-credit-card',
    'fa-solid fa-landmark',
    'fa-solid fa-paw',
    'fa-solid fa-gift',
    'fa-solid fa-plane',
    'fa-solid fa-spa',
    'fa-solid fa-piggy-bank',
    'fa-solid fa-ellipsis',
    // Tabla 5.7 (Sugeridos para categorias nuevas)
    'fa-solid fa-dumbbell',
    'fa-solid fa-baby',
    'fa-solid fa-book',
    'fa-solid fa-tooth',
    'fa-solid fa-wrench',
    'fa-solid fa-mobile-screen',
    'fa-solid fa-tv',
    'fa-solid fa-gamepad',
    'fa-solid fa-mug-hot',
    'fa-solid fa-scissors',
    'fa-solid fa-hand-holding-heart',
    'fa-solid fa-building',
    'fa-solid fa-truck',
    'fa-solid fa-children',
    'fa-solid fa-tag'
  ]);

  const COLORES_CATEGORIAS_PERMITIDOS = Object.freeze([
    { hex: '#1B2A4A', nombre: 'Azul Marino (Primario)' },
    { hex: '#4A5568', nombre: 'Gris Pizarra (Secundario)' },
    { hex: '#2E7D32', nombre: 'Verde Contable (Ingreso / Ahorro)' },
    { hex: '#C62828', nombre: 'Rojo Alerta (Deuda / Critico)' }
  ]);

  function containsEmoji(str) {
    if (typeof str !== 'string' || !str) return false;
    return EMOJI_REGEX.test(str);
  }

  function stripEmojis(str) {
    if (typeof str !== 'string' || !str) return '';
    return str.replace(EMOJI_REGEX_GLOBAL, '');
  }

  function roundHalfUp(num) {
    const n = Number(num);
    if (!Number.isFinite(n)) return 0;
    const normalized = Number(n.toFixed(8));
    if (normalized >= 0) {
      return Math.floor(normalized + 0.5);
    }
    return Math.ceil(normalized - 0.5);
  }

  function calcSubtotalCentavos(cantidad, precioUnitarioCentavos) {
    const cant = Number(cantidad);
    const precio = Number(precioUnitarioCentavos);
    if (!Number.isFinite(cant) || !Number.isFinite(precio) || cant <= 0 || precio < 0) {
      return 0;
    }
    return roundHalfUp(cant * precio);
  }

  function parseDecimalToCentavos(value) {
    if (value === null || value === undefined) return NaN;
    const clean = String(value).replace(/,/g, '').trim();
    if (!clean) return NaN;
    if (!/^\d+(\.\d{1,2})?$/.test(clean)) {
      const floatVal = Number(clean);
      if (!Number.isFinite(floatVal) || floatVal < 0) return NaN;
      return roundHalfUp(floatVal * 100);
    }
    const parts = clean.split('.');
    const whole = parseInt(parts[0], 10);
    const fracStr = (parts[1] || '').padEnd(2, '0').slice(0, 2);
    const frac = parseInt(fracStr, 10);
    return whole * 100 + frac;
  }

  function centavosToDecimalInput(centavos) {
    const n = Number(centavos);
    if (!Number.isFinite(n)) return '0.00';
    return (n / 100).toFixed(2);
  }

  function formatNumberWithCommas(centavos) {
    const absCentavos = Math.abs(Math.trunc(Number(centavos) || 0));
    const whole = Math.floor(absCentavos / 100);
    const cents = String(absCentavos % 100).padStart(2, '0');
    const wholeFormatted = String(whole).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    return `${wholeFormatted}.${cents}`;
  }

  /**
   * Formatea centavos segun UI_DESIGN.md 2.3:
   * - INGRESO: '+ 1,250.00'
   * - GASTO: '- 1,250.00'
   * - BALANCE: '1,250.00' o '- 1,250.00'
   * - NEUTRO: '1,250.00'
   * - TOTAL_MONEDA: 'USD 1,250.00' (o 'USD - 1,250.00' si negativo)
   */
  function formatCentavos(centavos, modo = 'NEUTRO', moneda = null) {
    const val = Math.trunc(Number(centavos) || 0);
    const base = formatNumberWithCommas(val);
    const curr = moneda || (global.AppConfig ? global.AppConfig.getCurrency() : 'COP');

    switch (modo) {
      case 'INGRESO':
        return `+ ${base}`;
      case 'GASTO':
        return `- ${base}`;
      case 'BALANCE':
        return val < 0 ? `- ${base}` : base;
      case 'TOTAL_MONEDA':
        return val < 0 ? `${curr} - ${base}` : `${curr} ${base}`;
      case 'NEUTRO':
      default:
        return val < 0 ? `- ${base}` : base;
    }
  }

  function getTodayIso() {
    const d = new Date();
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  function getCurrentPeriod() {
    const d = new Date();
    return {
      anio: d.getFullYear(),
      mes: d.getMonth() + 1
    };
  }

  function formatMonthYear(anio, mes) {
    const mIdx = Number(mes) - 1;
    const nombre = MESES_NOMBRES[mIdx] || `Mes ${mes}`;
    return `${nombre} ${anio}`;
  }

  function formatShortMonthYear(anio, mes) {
    const mIdx = Number(mes) - 1;
    const corto = MESES_CORTOS[mIdx] || String(mes);
    return `${corto} ${anio}`;
  }

  function resolveCategoryIcon(iconClass) {
    if (typeof iconClass !== 'string' || !iconClass.trim()) {
      return 'fa-solid fa-tag';
    }
    const clean = iconClass.trim();
    const validPrefix =
      clean.startsWith('fa-solid fa-') ||
      clean.startsWith('fa-regular fa-') ||
      clean.startsWith('fa-brands fa-');
    if (!validPrefix) {
      return 'fa-solid fa-tag';
    }
    return clean;
  }

  function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  global.AppUtils = {
    EMOJI_REGEX,
    MESES_NOMBRES,
    MESES_CORTOS,
    METODOS_PAGO,
    UNIDADES_ARTICULO,
    NATURALEZAS,
    ESTADOS_PRESUPUESTO,
    PRIORIDADES_IA,
    ICONOS_CATEGORIAS_PERMITIDOS,
    COLORES_CATEGORIAS_PERMITIDOS,
    containsEmoji,
    stripEmojis,
    roundHalfUp,
    calcSubtotalCentavos,
    parseDecimalToCentavos,
    centavosToDecimalInput,
    formatNumberWithCommas,
    formatCentavos,
    getTodayIso,
    getCurrentPeriod,
    formatMonthYear,
    formatShortMonthYear,
    resolveCategoryIcon,
    escapeHtml
  };
})(typeof window !== 'undefined' ? window : globalThis);
