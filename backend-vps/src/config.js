'use strict';

const fs = require('node:fs');
const path = require('node:path');

function loadEnvFile() {
  const envPath = path.resolve(__dirname, '..', '.env');
  if (!fs.existsSync(envPath)) {
    return;
  }
  const content = fs.readFileSync(envPath, 'utf8');
  const lines = content.split(/\r?\n/);
  for (const rawLine of lines) {
    const line = rawLine.trim();
    if (!line || line.startsWith('#')) continue;
    const eqIndex = line.indexOf('=');
    if (eqIndex === -1) continue;
    const key = line.slice(0, eqIndex).trim();
    let val = line.slice(eqIndex + 1).trim();
    if (
      (val.startsWith('"') && val.endsWith('"')) ||
      (val.startsWith("'") && val.endsWith("'"))
    ) {
      val = val.slice(1, -1);
    }
    if (process.env[key] === undefined) {
      process.env[key] = val;
    }
  }
}

loadEnvFile();

const ROOT_DIR = path.resolve(__dirname, '..');
const rawDbPath = process.env.APP_DB_PATH || './data/finanzas.db';
const resolvedDbPath = path.isAbsolute(rawDbPath)
  ? rawDbPath
  : path.resolve(ROOT_DIR, rawDbPath);

const config = {
  HOST: process.env.HOST || '0.0.0.0',
  PORT: parseInt(process.env.PORT || '54892', 10),
  APP_PRIVATE_TOKEN: process.env.APP_PRIVATE_TOKEN || 'SZ6ydSFNmkAbrFLLrRahSEVxGnLkvABw',
  APP_DB_PATH: resolvedDbPath,
  APP_MONEDA: (process.env.APP_MONEDA || 'USD').trim().toUpperCase(),
  GEMINI_API_KEY: process.env.GEMINI_API_KEY || '',
  GEMINI_MODEL: process.env.GEMINI_MODEL || 'gemini-2.5-flash',
  VERSION_API: '1.0.0',
  MAX_BODY_BYTES: 256 * 1024, // 256 KB
  AI_TIMEOUT_MS: 60000,       // 60 s
  AI_RATE_LIMIT_MAX: 10,      // 10 peticiones por ventana de 60 minutos
  AI_RATE_LIMIT_WINDOW_MS: 60 * 60 * 1000
};

module.exports = config;
