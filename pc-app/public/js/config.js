'use strict';

(function (global) {
  const STORAGE_KEY = 'gastos_pc_config_v1';

  const DEFAULT_CONFIG = Object.freeze({
    host: '51.161.47.99',
    puerto: 54892,
    protocolo: 'http',
    ruta_base: '/api/v1',
    header_token: 'X-App-Private-Token',
    token_privado: 'SZ6ydSFNmkAbrFLLrRahSEVxGnLkvABw',
    timeout_general_ms: 15000,
    timeout_ia_ms: 90000,
    moneda_defecto: 'USD',
    version_app: '1.0.0'
  });

  let state = Object.assign({}, DEFAULT_CONFIG);
  let activeCurrency = DEFAULT_CONFIG.moneda_defecto;
  let tokenBlocked = false;

  function sanitize(raw) {
    const src = raw && typeof raw === 'object' ? raw : {};
    const host = typeof src.host === 'string' && src.host.trim() ? src.host.trim() : DEFAULT_CONFIG.host;
    const portNum = Number(src.puerto);
    const puerto = Number.isInteger(portNum) && portNum >= 1 && portNum <= 65535 ? portNum : DEFAULT_CONFIG.puerto;
    const protocolo = src.protocolo === 'https' ? 'https' : 'http';
    const ruta_base = typeof src.ruta_base === 'string' && src.ruta_base.trim()
      ? src.ruta_base.trim().replace(/\/+$/, '')
      : DEFAULT_CONFIG.ruta_base;
    const token_privado = typeof src.token_privado === 'string' && src.token_privado.trim()
      ? src.token_privado.trim()
      : DEFAULT_CONFIG.token_privado;

    return {
      host,
      puerto,
      protocolo,
      ruta_base,
      url_base: `${protocolo}://${host}:${puerto}${ruta_base}`,
      header_token: DEFAULT_CONFIG.header_token,
      token_privado,
      timeout_general_ms: DEFAULT_CONFIG.timeout_general_ms,
      timeout_ia_ms: DEFAULT_CONFIG.timeout_ia_ms,
      moneda_defecto: typeof src.moneda_defecto === 'string' && src.moneda_defecto.trim()
        ? src.moneda_defecto.trim().toUpperCase()
        : DEFAULT_CONFIG.moneda_defecto,
      version_app: DEFAULT_CONFIG.version_app
    };
  }

  function loadFromLocalStorage() {
    try {
      const saved = global.localStorage ? global.localStorage.getItem(STORAGE_KEY) : null;
      if (saved) {
        state = sanitize(JSON.parse(saved));
      } else {
        state = sanitize(DEFAULT_CONFIG);
      }
    } catch (_e) {
      state = sanitize(DEFAULT_CONFIG);
    }
    return state;
  }

  function saveToLocalStorage(cfg) {
    try {
      if (global.localStorage) {
        global.localStorage.setItem(STORAGE_KEY, JSON.stringify(cfg));
      }
    } catch (_e) {
      // Ignorar si localStorage no esta disponible
    }
  }

  async function initConfig() {
    loadFromLocalStorage();
    try {
      const resp = await fetch('/local-config', { method: 'GET', cache: 'no-store' });
      if (resp.ok) {
        const json = await resp.json();
        if (json && json.ok && json.data) {
          const localStored = global.localStorage ? global.localStorage.getItem(STORAGE_KEY) : null;
          if (!localStored) {
            state = sanitize(json.data);
            saveToLocalStorage(state);
          }
        }
      }
    } catch (_e) {
      // Continua con configuracion en memoria/localStorage
    }
    return getConfig();
  }

  function getConfig() {
    return Object.assign({}, state, {
      url_base: `${state.protocolo}://${state.host}:${state.puerto}${state.ruta_base}`
    });
  }

  async function updateConfig(partial) {
    state = sanitize(Object.assign({}, state, partial));
    tokenBlocked = false;
    saveToLocalStorage(state);
    try {
      await fetch('/local-config', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json; charset=utf-8' },
        body: JSON.stringify(state)
      });
    } catch (_e) {
      // Guardado local en navegador ya aplicado
    }
    return getConfig();
  }

  async function resetToDefault() {
    state = sanitize(DEFAULT_CONFIG);
    tokenBlocked = false;
    saveToLocalStorage(state);
    try {
      await fetch('/local-config', { method: 'DELETE' });
    } catch (_e) {
      // Ignorar error de red local
    }
    return getConfig();
  }

  function getCurrency() {
    return activeCurrency || state.moneda_defecto || 'USD';
  }

  function setCurrency(isoCode) {
    if (typeof isoCode === 'string' && isoCode.trim()) {
      activeCurrency = isoCode.trim().toUpperCase();
    }
  }

  function setTokenBlocked(blocked) {
    tokenBlocked = Boolean(blocked);
  }

  function isTokenBlocked() {
    return tokenBlocked;
  }

  loadFromLocalStorage();

  global.AppConfig = {
    DEFAULT_CONFIG,
    initConfig,
    getConfig,
    updateConfig,
    resetToDefault,
    getCurrency,
    setCurrency,
    setTokenBlocked,
    isTokenBlocked
  };
})(typeof window !== 'undefined' ? window : globalThis);
