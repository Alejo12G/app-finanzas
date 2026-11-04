'use strict';

const CORS_HEADERS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, X-App-Private-Token',
  'Access-Control-Max-Age': '86400'
};

class AppError extends Error {
  constructor(statusCode, codigo, mensaje, detalles = []) {
    super(mensaje);
    this.name = 'AppError';
    this.statusCode = statusCode;
    this.codigo = codigo;
    this.mensaje = mensaje;
    this.detalles = Array.isArray(detalles) ? detalles : [];
  }
}

function setCorsHeaders(res) {
  for (const [k, v] of Object.entries(CORS_HEADERS)) {
    res.setHeader(k, v);
  }
}

function sendSuccess(res, statusCode, data, meta = null) {
  setCorsHeaders(res);
  res.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=utf-8'
  });
  res.end(
    JSON.stringify({
      ok: true,
      data: data === undefined ? null : data,
      meta: meta === undefined ? null : meta
    })
  );
}

function sendError(res, statusCode, codigo, mensaje, detalles = []) {
  setCorsHeaders(res);
  res.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=utf-8'
  });
  res.end(
    JSON.stringify({
      ok: false,
      error: {
        codigo,
        mensaje,
        detalles: Array.isArray(detalles) ? detalles : []
      }
    })
  );
}

function sendNoContent(res) {
  setCorsHeaders(res);
  res.writeHead(204);
  res.end();
}

module.exports = {
  CORS_HEADERS,
  AppError,
  setCorsHeaders,
  sendSuccess,
  sendError,
  sendNoContent
};
