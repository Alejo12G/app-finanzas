'use strict';

const http = require('node:http');
const config = require('./config');
const { getDb } = require('./db');
const { handleRequest } = require('./app');

function startServer() {
  // Inicializa la base de datos SQLite, pragmas y datos semilla al arrancar
  getDb();

  const server = http.createServer((req, res) => {
    handleRequest(req, res);
  });

  server.listen(config.PORT, config.HOST, () => {
    console.log(
      `[BACKEND-VPS] Servidor HTTP escuchando en http://${config.HOST}:${config.PORT}/api/v1 (Moneda: ${config.APP_MONEDA})`
    );
  });

  const shutdown = () => {
    console.log('[BACKEND-VPS] Cerrando servidor...');
    server.close(() => {
      process.exit(0);
    });
  };

  process.on('SIGINT', shutdown);
  process.on('SIGTERM', shutdown);

  return server;
}

if (require.main === module) {
  startServer();
}

module.exports = {
  startServer
};
