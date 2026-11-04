'use strict';

const crypto = require('node:crypto');
const { URL } = require('node:url');
const config = require('./config');
const {
  AppError,
  sendError,
  sendNoContent
} = require('./utils/response');
const { parsePositiveIntId } = require('./utils/validators');

const { handleHealth } = require('./routes/health');
const {
  listCategorias,
  createCategoria,
  updateCategoria,
  deleteCategoria
} = require('./routes/categorias');
const {
  listTransacciones,
  getTransaccion,
  createTransaccion,
  updateTransaccion,
  deleteTransaccion
} = require('./routes/transacciones');
const {
  listCompras,
  getCompra,
  createCompra,
  updateCompra,
  deleteCompra
} = require('./routes/compras');
const {
  listPresupuestos,
  getPresupuesto,
  createPresupuesto,
  updatePresupuesto,
  deletePresupuesto
} = require('./routes/presupuestos');
const {
  listCards,
  getCard,
  createCard,
  updateCard,
  deleteCard,
  simulateCard,
  createCardPayment,
  listCardPayments
} = require('./routes/tarjetas');
const { getDashboard } = require('./routes/dashboard');
const { analyzeFinances, handleParseCompraLibre } = require('./routes/ai');

function verifyPrivateToken(providedToken) {
  if (typeof providedToken !== 'string' || !providedToken) {
    return false;
  }
  const expected = config.APP_PRIVATE_TOKEN;
  if (typeof expected !== 'string' || !expected) {
    return false;
  }
  const bufProvided = Buffer.from(providedToken, 'utf8');
  const bufExpected = Buffer.from(expected, 'utf8');
  if (bufProvided.length !== bufExpected.length) {
    return false;
  }
  return crypto.timingSafeEqual(bufProvided, bufExpected);
}

function readJsonBody(req) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    let totalBytes = 0;
    let aborted = false;

    req.on('data', (chunk) => {
      if (aborted) return;
      totalBytes += chunk.length;
      if (totalBytes > config.MAX_BODY_BYTES) {
        aborted = true;
        reject(
          new AppError(
            413,
            'CUERPO_DEMASIADO_GRANDE',
            'El cuerpo de la peticion supera el limite maximo de 256 KB.'
          )
        );
        return;
      }
      chunks.push(chunk);
    });

    req.on('end', () => {
      if (aborted) return;
      const raw = Buffer.concat(chunks).toString('utf8').trim();
      if (!raw) {
        reject(
          new AppError(
            400,
            'JSON_INVALIDO',
            'El cuerpo de la peticion no es un JSON valido.'
          )
        );
        return;
      }
      try {
        const parsed = JSON.parse(raw);
        if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
          reject(
            new AppError(
              400,
              'JSON_INVALIDO',
              'El cuerpo de la peticion debe ser un objeto JSON.'
            )
          );
          return;
        }
        resolve(parsed);
      } catch (_err) {
        reject(
          new AppError(
            400,
            'JSON_INVALIDO',
            'El cuerpo de la peticion no es un JSON valido.'
          )
        );
      }
    });

    req.on('error', () => {
      if (!aborted) {
        reject(new AppError(400, 'JSON_INVALIDO', 'Error al leer el cuerpo de la peticion.'));
      }
    });
  });
}

function parseQueryObject(searchParams) {
  const query = {};
  for (const [k, v] of searchParams.entries()) {
    query[k] = v;
  }
  return query;
}

function ensureMethod(method, allowedMethods) {
  if (!allowedMethods.includes(method)) {
    throw new AppError(
      405,
      'METODO_NO_PERMITIDO',
      `El metodo ${method} no esta permitido en esta ruta.`
    );
  }
}

async function handleRequest(req, res) {
  try {
    const method = (req.method || 'GET').toUpperCase();

    // 1. Preflight CORS sin exigir token
    if (method === 'OPTIONS') {
      sendNoContent(res);
      return;
    }

    const parsedUrl = new URL(req.url || '/', `http://${req.headers.host || 'localhost'}`);
    let pathname = parsedUrl.pathname;
    if (pathname.length > 1 && pathname.endsWith('/')) {
      pathname = pathname.slice(0, -1);
    }
    const query = parseQueryObject(parsedUrl.searchParams);

    // 2. Endpoint publico GET /api/v1/health (y alias /health)
    if (pathname === '/api/v1/health' || pathname === '/health') {
      ensureMethod(method, ['GET']);
      handleHealth(req, res);
      return;
    }

    // 3. Middleware de autenticacion por token privado en tiempo constante
    const rawHeaderToken = req.headers['x-app-private-token'];
    const tokenValue = Array.isArray(rawHeaderToken) ? rawHeaderToken[0] : rawHeaderToken;
    if (!verifyPrivateToken(tokenValue)) {
      const authFailStatus = process.env.AUTH_ERROR_STATUS
        ? Number(process.env.AUTH_ERROR_STATUS)
        : 401;
      sendError(res, authFailStatus, 'TOKEN_INVALIDO', 'Token de acceso invalido.', []);
      return;
    }

    // Verificar prefijo /api/v1
    if (!pathname.startsWith('/api/v1/')) {
      throw new AppError(404, 'RUTA_NO_ENCONTRADA', 'La ruta solicitada no existe.');
    }

    const subPath = pathname.slice('/api/v1'.length); // ej. /categorias o /categorias/12
    const segments = subPath.split('/').filter(Boolean);

    // /api/v1/categorias y /api/v1/categorias/{id}
    if (segments[0] === 'categorias') {
      if (segments.length === 1) {
        ensureMethod(method, ['GET', 'POST']);
        if (method === 'GET') {
          listCategorias(req, res, query);
          return;
        }
        const body = await readJsonBody(req);
        createCategoria(req, res, body);
        return;
      }
      if (segments.length === 2) {
        ensureMethod(method, ['PUT', 'DELETE']);
        const id = parsePositiveIntId(segments[1]);
        if (id === null) {
          throw new AppError(404, 'RECURSO_NO_ENCONTRADO', 'Identificador de categoria invalido.');
        }
        if (method === 'PUT') {
          const body = await readJsonBody(req);
          updateCategoria(req, res, id, body);
          return;
        }
        deleteCategoria(req, res, id);
        return;
      }
      throw new AppError(404, 'RUTA_NO_ENCONTRADA', 'La ruta solicitada no existe.');
    }

    // /api/v1/credit-cards, /api/v1/credit-cards/simulate y /api/v1/credit-cards/{id}
    if (segments[0] === 'credit-cards') {
      if (segments.length === 1) {
        ensureMethod(method, ['GET', 'POST']);
        if (method === 'GET') {
          listCards(req, res);
          return;
        }
        const body = await readJsonBody(req);
        createCard(req, res, body);
        return;
      }
      if (segments.length === 2) {
        if (segments[1] === 'simulate') {
          ensureMethod(method, ['POST']);
          const body = await readJsonBody(req);
          simulateCard(req, res, body);
          return;
        }
        ensureMethod(method, ['GET', 'PUT', 'DELETE']);
        const id = parsePositiveIntId(segments[1]);
        if (id === null) {
          throw new AppError(404, 'RECURSO_NO_ENCONTRADO', 'Identificador de tarjeta invalido.');
        }
        if (method === 'GET') {
          getCard(req, res, id);
          return;
        }
        if (method === 'PUT') {
          const body = await readJsonBody(req);
          updateCard(req, res, id, body);
          return;
        }
        deleteCard(req, res, id);
        return;
      }
      if (segments.length === 3 && segments[2] === 'payments') {
        ensureMethod(method, ['GET', 'POST']);
        const id = parsePositiveIntId(segments[1]);
        if (id === null) {
          throw new AppError(404, 'RECURSO_NO_ENCONTRADO', 'Identificador de tarjeta invalido.');
        }
        if (method === 'GET') {
          listCardPayments(req, res, id);
          return;
        }
        const body = await readJsonBody(req);
        createCardPayment(req, res, id, body);
        return;
      }
      throw new AppError(404, 'RUTA_NO_ENCONTRADA', 'La ruta solicitada no existe.');
    }

    // /api/v1/transacciones (alias /transactions) y /{id}
    if (segments[0] === 'transacciones' || segments[0] === 'transactions') {
      if (segments.length === 1) {
        ensureMethod(method, ['GET', 'POST']);
        if (method === 'GET') {
          listTransacciones(req, res, query);
          return;
        }
        const body = await readJsonBody(req);
        createTransaccion(req, res, body);
        return;
      }
      if (segments.length === 2) {
        ensureMethod(method, ['GET', 'PUT', 'DELETE']);
        const id = parsePositiveIntId(segments[1]);
        if (id === null) {
          throw new AppError(404, 'RECURSO_NO_ENCONTRADO', 'Identificador de transaccion invalido.');
        }
        if (method === 'GET') {
          getTransaccion(req, res, id);
          return;
        }
        if (method === 'PUT') {
          const body = await readJsonBody(req);
          updateTransaccion(req, res, id, body);
          return;
        }
        deleteTransaccion(req, res, id);
        return;
      }
      throw new AppError(404, 'RUTA_NO_ENCONTRADA', 'La ruta solicitada no existe.');
    }

    // /api/v1/compras y /api/v1/compras/{id}
    if (segments[0] === 'compras') {
      if (segments.length === 1) {
        ensureMethod(method, ['GET', 'POST']);
        if (method === 'GET') {
          listCompras(req, res, query);
          return;
        }
        const body = await readJsonBody(req);
        createCompra(req, res, body);
        return;
      }
      if (segments.length === 2) {
        ensureMethod(method, ['GET', 'PUT', 'DELETE']);
        const id = parsePositiveIntId(segments[1]);
        if (id === null) {
          throw new AppError(404, 'RECURSO_NO_ENCONTRADO', 'Identificador de compra invalido.');
        }
        if (method === 'GET') {
          getCompra(req, res, id);
          return;
        }
        if (method === 'PUT') {
          const body = await readJsonBody(req);
          updateCompra(req, res, id, body);
          return;
        }
        deleteCompra(req, res, id);
        return;
      }
      throw new AppError(404, 'RUTA_NO_ENCONTRADA', 'La ruta solicitada no existe.');
    }

    // /api/v1/presupuestos y /api/v1/presupuestos/{id}
    if (segments[0] === 'presupuestos') {
      if (segments.length === 1) {
        ensureMethod(method, ['GET', 'POST']);
        if (method === 'GET') {
          listPresupuestos(req, res, query);
          return;
        }
        const body = await readJsonBody(req);
        createPresupuesto(req, res, body);
        return;
      }
      if (segments.length === 2) {
        ensureMethod(method, ['GET', 'PUT', 'DELETE']);
        const id = parsePositiveIntId(segments[1]);
        if (id === null) {
          throw new AppError(404, 'RECURSO_NO_ENCONTRADO', 'Identificador de presupuesto invalido.');
        }
        if (method === 'GET') {
          getPresupuesto(req, res, id);
          return;
        }
        if (method === 'PUT') {
          const body = await readJsonBody(req);
          updatePresupuesto(req, res, id, body);
          return;
        }
        deletePresupuesto(req, res, id);
        return;
      }
      throw new AppError(404, 'RUTA_NO_ENCONTRADA', 'La ruta solicitada no existe.');
    }

    // /api/v1/dashboard
    if (segments[0] === 'dashboard' && segments.length === 1) {
      ensureMethod(method, ['GET']);
      getDashboard(req, res, query);
      return;
    }

    // /api/v1/ai/analyze y /api/v1/ai/parse-compra
    if (segments[0] === 'ai' && segments.length === 2) {
      if (segments[1] === 'analyze') {
        ensureMethod(method, ['POST']);
        const body = await readJsonBody(req);
        await analyzeFinances(req, res, body);
        return;
      }
      if (segments[1] === 'parse-compra' || segments[1] === 'compra') {
        ensureMethod(method, ['POST']);
        const body = await readJsonBody(req);
        await handleParseCompraLibre(req, res, body);
        return;
      }
    }

    throw new AppError(404, 'RUTA_NO_ENCONTRADA', 'La ruta solicitada no existe.');
  } catch (err) {
    if (err instanceof AppError) {
      sendError(res, err.statusCode, err.codigo, err.mensaje, err.detalles);
      return;
    }
    // Error no controlado: sin trazas ni secretos en la respuesta ni en el log
    console.error('[ERROR_INTERNO]', err && err.message ? err.message : 'Error desconocido');
    sendError(res, 500, 'ERROR_INTERNO', 'Ocurrio un error interno en el servidor.', []);
  }
}

module.exports = {
  handleRequest,
  verifyPrivateToken
};
