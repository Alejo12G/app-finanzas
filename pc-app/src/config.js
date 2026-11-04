'use strict';

const fs = require('fs');
const path = require('path');

const CONFIG_FILE_PATH = path.join(__dirname, '..', 'config', 'app-config.json');

const DEFAULT_CONFIG = Object.freeze({
  host: '51.161.47.99',
  puerto: 54892,
  protocolo: 'http',
  ruta_base: '/api/v1',
  header_token: 'X-App-Private-Token',
  token_privado: 'SZ6ydSFNmkAbrFLLrRahSEVxGnLkvABw',
  timeout_general_ms: 15000,
  timeout_ia_ms: 90000,
  moneda_defecto: 'COP',
  puerto_cliente_local: 38492,
  version_app: '1.0.0'
});

function loadConfig() {
  try {
    if (fs.existsSync(CONFIG_FILE_PATH)) {
      const raw = fs.readFileSync(CONFIG_FILE_PATH, 'utf8');
      const parsed = JSON.parse(raw);
      return normalizeConfig(parsed);
    }
  } catch (_err) {
    // Si hay error de lectura, se retorna la configuracion por defecto
  }
  return normalizeConfig(DEFAULT_CONFIG);
}

function normalizeConfig(input) {
  const src = input && typeof input === 'object' ? input : {};
  const host = typeof src.host === 'string' && src.host.trim() ? src.host.trim() : DEFAULT_CONFIG.host;
  const puertoNum = Number(src.puerto);
  const puerto = Number.isInteger(puertoNum) && puertoNum >= 1 && puertoNum <= 65535
    ? puertoNum
    : DEFAULT_CONFIG.puerto;
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
    puerto_cliente_local: DEFAULT_CONFIG.puerto_cliente_local,
    version_app: DEFAULT_CONFIG.version_app
  };
}

function saveConfig(partial) {
  const current = loadConfig();
  const merged = normalizeConfig({ ...current, ...partial });
  const toStore = {
    host: merged.host,
    puerto: merged.puerto,
    protocolo: merged.protocolo,
    ruta_base: merged.ruta_base,
    header_token: merged.header_token,
    token_privado: merged.token_privado,
    timeout_general_ms: merged.timeout_general_ms,
    timeout_ia_ms: merged.timeout_ia_ms,
    moneda_defecto: merged.moneda_defecto,
    puerto_cliente_local: merged.puerto_cliente_local,
    version_app: merged.version_app
  };
  fs.mkdirSync(path.dirname(CONFIG_FILE_PATH), { recursive: true });
  fs.writeFileSync(CONFIG_FILE_PATH, JSON.stringify(toStore, null, 2) + '\n', 'utf8');
  return merged;
}

function resetConfig() {
  return saveConfig(DEFAULT_CONFIG);
}

module.exports = {
  DEFAULT_CONFIG,
  CONFIG_FILE_PATH,
  loadConfig,
  saveConfig,
  resetConfig,
  normalizeConfig
};
