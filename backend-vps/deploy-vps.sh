#!/usr/bin/env bash
# Script de despliegue en VPS (51.161.47.99) para el Backend de Finanzas Personales
# Puerto: 54892 (TCP)
# Sin emojis ni dependencias con compilacion nativa.

set -euo pipefail

APP_NAME="gastos-backend-vps"
PORT="54892"
APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "[1/6] Verificando version de Node.js..."
if ! command -v node >/dev/null 2>&1; then
  echo "Error: Node.js no esta instalado. Se requiere Node.js >= 22.5.0."
  exit 1
fi
node -v

echo "[2/6] Preparando directorio de persistencia SQLite..."
mkdir -p "${APP_DIR}/data"

if [ ! -f "${APP_DIR}/.env" ]; then
  echo "Aviso: No se encontro archivo .env. Copiando desde .env.example..."
  cp "${APP_DIR}/.env.example" "${APP_DIR}/.env"
  echo "Importante: Revise y configure las variables en ${APP_DIR}/.env"
fi

echo "[3/6] Configurando cortafuegos UFW para abrir el puerto ${PORT}/tcp..."
if command -v ufw >/dev/null 2>&1; then
  sudo ufw allow "${PORT}/tcp" comment "Gastos Backend API"
  sudo ufw reload || true
  sudo ufw status | grep "${PORT}" || true
else
  echo "Aviso: UFW no esta instalado en este sistema."
fi

echo "[4/6] Verificando gestor de procesos PM2..."
if ! command -v pm2 >/dev/null 2>&1; then
  echo "Instalando PM2 de forma global..."
  sudo npm install -g pm2
fi

echo "[5/6] Iniciando o reiniciando servicio con PM2..."
cd "${APP_DIR}"
if pm2 describe "${APP_NAME}" >/dev/null 2>&1; then
  pm2 restart "${APP_NAME}" --update-env
else
  pm2 start src/server.js \
    --name "${APP_NAME}" \
    --node-args="--no-warnings" \
    --time
fi

pm2 save

echo "[6/6] Comprobando estado del endpoint /api/v1/health..."
sleep 2
curl -fsS "http://127.0.0.1:${PORT}/api/v1/health" || {
  echo "Error: El servidor no respondio en el puerto ${PORT}."
  pm2 logs "${APP_NAME}" --lines 20 --nostream
  exit 1
}

echo ""
echo "Despliegue finalizado correctamente en 0.0.0.0:${PORT}."
