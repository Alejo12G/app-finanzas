# Guia de Ejecucion y Despliegue - Sistema de Finanzas Personales y Control de Gastos

Documento operativo paso a paso para desplegar el Backend en el VPS (`51.161.47.99:54892`), iniciar la aplicacion de escritorio en PC (`/pc-app`) y compilar/instalar el cliente nativo de Android (`/android-app`).

---

## 1. Parametros de Integracion Verificados

Las tres aplicaciones se comunican bajo los siguientes parametros unificados (definidos en `/docs-agentes/API_CONTRACT.md`):

- **Host del Servidor VPS:** `51.161.47.99`
- **Puerto TCP:** `54892`
- **URL Base API REST:** `http://51.161.47.99:54892/api/v1`
- **Cabecera de Seguridad:** `X-App-Private-Token`
- **Token Privado:** `SZ6ydSFNmkAbrFLLrRahSEVxGnLkvABw`
- **Moneda Principal:** `COP` (montos procesados en centavos enteros)

---

## 2. Subir y Desplegar el Backend en el VPS (`51.161.47.99`)

### 2.1 Requisitos en el VPS (Ubuntu / Debian)
- Node.js **v22.5.0 o superior** (requerido para el modulo nativo `node:sqlite` sin dependencias externas).
- `pm2` y `ufw` (el script `deploy-vps.sh` los verifica y configura automaticamente).

### 2.2 Paso 1: Subir `/backend-vps` desde tu PC al VPS
Abre PowerShell o terminal en la raiz del proyecto y ejecuta:

```powershell
# Crear carpeta destino en el VPS (reemplaza 'ubuntu' o 'root' por tu usuario SSH)
ssh root@51.161.47.99 "mkdir -p /opt/finanzas-backend"

# Copiar los archivos del backend al VPS (incluyendo el archivo .env configurado)
scp -r ./backend-vps/src ./backend-vps/package.json ./backend-vps/deploy-vps.sh ./backend-vps/.env ./backend-vps/.env.example root@51.161.47.99:/opt/finanzas-backend/
```

### 2.3 Paso 2: Configurar y lanzar el servicio en el VPS
Conectate por SSH al VPS y ejecuta el script automatizado de despliegue:

```bash
ssh root@51.161.47.99
cd /opt/finanzas-backend

# Verificar el contenido de .env (PORT=54892, APP_PRIVATE_TOKEN y GEMINI_API_KEY)
cat .env

# Dar permisos de ejecucion y lanzar el despliegue (crea DB SQLite WAL, abre puerto 54892 en UFW e inicia PM2)
chmod +x deploy-vps.sh
./deploy-vps.sh
```

### 2.4 Paso 3: Verificar que el Backend responde
Desde el VPS o desde tu propio PC, ejecuta:

```powershell
# Comprobar estado de salud (no requiere token)
curl http://51.161.47.99:54892/api/v1/health

# Comprobar autenticacion y las 31 categorias semilla
curl -H "X-App-Private-Token: SZ6ydSFNmkAbrFLLrRahSEVxGnLkvABw" http://51.161.47.99:54892/api/v1/categorias
```

---

## 3. Abrir el Cliente de Escritorio en el PC (`/pc-app`)

La aplicacion de escritorio (`/pc-app`) tiene **cero dependencias externas** (`node_modules` no existe, peso total ~1.5 MB). Levanta un servidor local en `127.0.0.1:54880` y abre una ventana nativa dedicada (`--app`) usando Microsoft Edge o Google Chrome.

### Opcion A: Inicio rapido con doble clic o CMD
Desde el Explorador de Windows, haz doble clic en:
```text
pc-app\iniciar-pc-app.bat
```
O desde PowerShell en la raiz del proyecto:
```powershell
.\pc-app\iniciar-pc-app.bat
```

### Opcion B: Inicio desde terminal con Node.js
```powershell
cd pc-app
npm start
```

### Opcion C: Ejecutar pruebas de verificacion del cliente PC
```powershell
cd pc-app
npm test
```

> **Nota:** Si deseas apuntar temporalmente a un backend local (`127.0.0.1:54892`) o restaurar la conexion al VPS (`51.161.47.99:54892`), puedes hacerlo directamente desde la vista **Configuracion** dentro de la aplicacion de PC.

---

## 4. Compilar y Ejecutar el APK de Android (`/android-app`)

La aplicacion Android esta escrita en **Java 17 puro** usando el Android SDK estandar (`HttpURLConnection`, `org.json`, vistas nativas y fuentes FontAwesome 6.5.2 en `assets/fonts`), sin librerias externas pesadas (APK resultante de ~481 KB).

### Opcion A: Compilacion en la nube sin usar disco local (GitHub Actions - Recomendado)
El repositorio incluye el flujo `.github/workflows/build-apk.yml`. Al hacer `git push` de tu rama a GitHub:
1. Abre tu repositorio en GitHub y entra a la pestana **Actions**.
2. Selecciona el workflow **Compilar APK Android (Cero Disco Local)** (tambien puedes lanzarlo manualmente con el boton **Run workflow**).
3. Al finalizar, descarga el artefacto **`FinanzasPersonales-debug-apk`** que contiene `app-debug.apk`.

### Opcion B: Compilacion local en Windows (Ahorro de Disco Estricto)
Requiere JDK 17+ y Android SDK (`ANDROID_HOME` configurado). Los scripts desactivan el daemon y la cache de Gradle (`--no-daemon --no-build-cache`) para proteger el limite de disco:

```powershell
cd android-app
.\compilar-local.bat
```
O directamente con Gradle Wrapper:
```powershell
cd android-app
.\gradlew.bat assembleDebug --no-daemon --no-build-cache --console=plain
```

### Ubicacion del APK compilado e instalacion por ADB
Una vez compilado, el archivo instalable se encuentra en:
```text
android-app\app\build\outputs\apk\debug\app-debug.apk
```

Para instalarlo en un telefono Android conectado por USB con Depuracion USB activa:
```powershell
adb install -r android-app\app\build\outputs\apk\debug\app-debug.apk
```
