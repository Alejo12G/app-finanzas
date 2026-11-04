'use strict';

const http = require('http');
const https = require('https');
const fs = require('fs');
const path = require('path');
const { URL } = require('url');
const { loadConfig, saveConfig, resetConfig } = require('./config');

const PUBLIC_DIR = path.join(__dirname, '..', 'public');

const MIME_TYPES = Object.freeze({
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.woff2': 'font/woff2',
  '.woff': 'font/woff',
  '.ttf': 'font/ttf',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.ico': 'image/x-icon'
});

function sendJson(res, statusCode, payload) {
  const body = JSON.stringify(payload);
  res.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=utf-8',
    'Cache-Control': 'no-store'
  });
  res.end(body);
}

function readRequestBody(req, maxBytes = 262144) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    let total = 0;
    req.on('data', (chunk) => {
      total += chunk.length;
      if (total > maxBytes) {
        reject(new Error('CUERPO_DEMASIADO_GRANDE'));
        req.destroy();
        return;
      }
      chunks.push(chunk);
    });
    req.on('end', () => {
      resolve(Buffer.concat(chunks).toString('utf8'));
    });
    req.on('error', reject);
  });
}

function handleProxyRequest(req, res, parsedUrl) {
  const cfg = loadConfig();
  const targetPath = parsedUrl.pathname.replace(/^\/api-proxy/, '') + (parsedUrl.search || '');
  const isAiEndpoint = targetPath.startsWith('/api/v1/ai/analyze');
  const timeoutMs = isAiEndpoint ? cfg.timeout_ia_ms : cfg.timeout_general_ms;

  readRequestBody(req)
    .then((rawBody) => {
      const transport = cfg.protocolo === 'https' ? https : http;
      const headers = {
        'Accept': 'application/json',
        'Content-Type': 'application/json; charset=utf-8',
        [cfg.header_token]: req.headers['x-app-private-token'] || cfg.token_privado
      };

      if (rawBody && req.method !== 'GET' && req.method !== 'HEAD') {
        headers['Content-Length'] = Buffer.byteLength(rawBody, 'utf8');
      }

      const proxyReq = transport.request(
        {
          hostname: cfg.host,
          port: cfg.puerto,
          path: targetPath,
          method: req.method,
          headers,
          timeout: timeoutMs
        },
        (proxyRes) => {
          const resChunks = [];
          proxyRes.on('data', (c) => resChunks.push(c));
          proxyRes.on('end', () => {
            const responseBuffer = Buffer.concat(resChunks);
            res.writeHead(proxyRes.statusCode || 500, {
              'Content-Type': proxyRes.headers['content-type'] || 'application/json; charset=utf-8',
              'Cache-Control': 'no-store'
            });
            res.end(responseBuffer);
          });
        }
      );

      proxyReq.on('timeout', () => {
        proxyReq.destroy(new Error('TIMEOUT'));
      });

      proxyReq.on('error', (err) => {
        const isTimeout = err && err.message === 'TIMEOUT';
        sendJson(res, isTimeout ? 504 : 502, {
          ok: false,
          error: {
            codigo: isTimeout && isAiEndpoint ? 'TIEMPO_IA_AGOTADO' : 'SIN_CONEXION_SERVIDOR',
            mensaje: 'No se pudo conectar con el servidor.',
            detalles: []
          }
        });
      });

      if (rawBody && req.method !== 'GET' && req.method !== 'HEAD') {
        proxyReq.write(rawBody);
      }
      proxyReq.end();
    })
    .catch((err) => {
      if (err && err.message === 'CUERPO_DEMASIADO_GRANDE') {
        sendJson(res, 413, {
          ok: false,
          error: {
            codigo: 'CUERPO_DEMASIADO_GRANDE',
            mensaje: 'El cuerpo de la peticion supera el limite permitido de 256 KB.',
            detalles: []
          }
        });
        return;
      }
      sendJson(res, 400, {
        ok: false,
        error: {
          codigo: 'JSON_INVALIDO',
          mensaje: 'No se pudo procesar la peticion local.',
          detalles: []
        }
      });
    });
}

function createLocalServer() {
  return http.createServer(async (req, res) => {
    try {
      const parsedUrl = new URL(req.url, 'http://127.0.0.1');

      if (parsedUrl.pathname === '/local-config') {
        if (req.method === 'GET') {
          sendJson(res, 200, { ok: true, data: loadConfig(), meta: null });
          return;
        }
        if (req.method === 'POST' || req.method === 'PUT') {
          const raw = await readRequestBody(req);
          const body = raw ? JSON.parse(raw) : {};
          const updated = saveConfig(body);
          sendJson(res, 200, { ok: true, data: updated, meta: null });
          return;
        }
        if (req.method === 'DELETE') {
          const restored = resetConfig();
          sendJson(res, 200, { ok: true, data: restored, meta: null });
          return;
        }
        sendJson(res, 405, {
          ok: false,
          error: { codigo: 'METODO_NO_PERMITIDO', mensaje: 'Metodo no permitido.', detalles: [] }
        });
        return;
      }

      if (parsedUrl.pathname.startsWith('/api-proxy/')) {
        handleProxyRequest(req, res, parsedUrl);
        return;
      }

      let safePath = path.normalize(decodeURIComponent(parsedUrl.pathname)).replace(/^(\.\.[/\\])+/, '');
      if (safePath === '/' || safePath === '\\' || safePath === '.') {
        safePath = '/index.html';
      }

      const fullPath = path.join(PUBLIC_DIR, safePath);
      if (!fullPath.startsWith(PUBLIC_DIR)) {
        res.writeHead(403, { 'Content-Type': 'text/plain; charset=utf-8' });
        res.end('Acceso denegado');
        return;
      }

      if (!fs.existsSync(fullPath) || fs.statSync(fullPath).isDirectory()) {
        res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
        res.end('Recurso no encontrado');
        return;
      }

      const ext = path.extname(fullPath).toLowerCase();
      const contentType = MIME_TYPES[ext] || 'application/octet-stream';
      const content = fs.readFileSync(fullPath);
      res.writeHead(200, {
        'Content-Type': contentType,
        'Cache-Control': ext === '.woff2' || ext === '.ttf' ? 'public, max-age=86400' : 'no-cache'
      });
      res.end(content);
    } catch (_err) {
      sendJson(res, 500, {
        ok: false,
        error: { codigo: 'ERROR_INTERNO', mensaje: 'Error interno del cliente local.', detalles: [] }
      });
    }
  });
}

function startLocalServer(preferredPort) {
  return new Promise((resolve, reject) => {
    const cfg = loadConfig();
    const portToTry = preferredPort !== undefined ? preferredPort : cfg.puerto_cliente_local;
    const server = createLocalServer();

    server.once('error', (err) => {
      if (err.code === 'EADDRINUSE' && portToTry !== 0) {
        const fallbackServer = createLocalServer();
        fallbackServer.listen(0, '127.0.0.1', () => {
          const addr = fallbackServer.address();
          resolve({ server: fallbackServer, port: addr.port, url: `http://127.0.0.1:${addr.port}` });
        });
        fallbackServer.once('error', reject);
      } else {
        reject(err);
      }
    });

    server.listen(portToTry, '127.0.0.1', () => {
      const addr = server.address();
      resolve({ server, port: addr.port, url: `http://127.0.0.1:${addr.port}` });
    });
  });
}

module.exports = {
  createLocalServer,
  startLocalServer
};
