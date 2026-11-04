'use strict';

const { startLocalServer } = require('./server');
const { openNativeAppWindow } = require('./launcher');
const { loadConfig } = require('./config');

async function main() {
  const args = process.argv.slice(2);
  const noOpen = args.includes('--no-open');
  const cfg = loadConfig();

  const { url, port } = await startLocalServer(cfg.puerto_cliente_local);
  console.log('[PC-APP] Cliente local activo en:', url);
  console.log('[PC-APP] Servidor remoto configurado:', cfg.url_base);
  console.log('[PC-APP] Puerto local:', port);

  if (!noOpen) {
    const launchInfo = openNativeAppWindow(url);
    console.log('[PC-APP] Ventana nativa iniciada:', launchInfo.modo, launchInfo.ejecutable || '');
  }
}

if (require.main === module) {
  main().catch((err) => {
    console.error('[PC-APP] Error fatal al iniciar el cliente de escritorio:', err.message);
    process.exit(1);
  });
}

module.exports = { main };
