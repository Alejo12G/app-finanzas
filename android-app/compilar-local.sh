#!/usr/bin/env bash
set -e

echo "============================================================================"
echo "Compilacion Local Ligera - Finanzas Personales Android (Java Nativo Puro)"
echo "Optimizado para bajo consumo de disco duro (menos de 10 GB libres) y RAM"
echo "============================================================================"

cd "$(dirname "$0")"

if [ -n "$JAVA_HOME" ]; then
    export PATH="$JAVA_HOME/bin:$PATH"
fi

chmod +x ./gradlew

echo "[1/2] Ejecutando compilacion APK Debug sin daemon ni cache pesada..."
./gradlew assembleDebug --no-daemon --no-build-cache --console=plain

echo "[2/2] Compilacion completada con exito."
echo "APK generado en: app/build/outputs/apk/debug/app-debug.apk"
