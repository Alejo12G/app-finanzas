@echo off
setlocal

echo ============================================================================
echo Compilacion Local Ligera - Finanzas Personales Android (Java Nativo Puro)
echo Optimizado para bajo consumo de disco duro (menos de 10 GB libres) y RAM
echo ============================================================================

cd /d "%~dp0"

if defined JAVA_HOME (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

if not defined ANDROID_HOME (
    if exist "%LOCALAPPDATA%\Android\Sdk" (
        set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
    )
)

if not defined ANDROID_SDK_ROOT (
    if defined ANDROID_HOME (
        set "ANDROID_SDK_ROOT=%ANDROID_HOME%"
    )
)

echo [1/2] Ejecutando compilacion APK Debug sin daemon ni cache pesada...
call gradlew.bat assembleDebug --no-daemon --no-build-cache --console=plain

if %ERRORLEVEL% neq 0 (
    echo [ERROR] La compilacion fallo con codigo %ERRORLEVEL%.
    exit /b %ERRORLEVEL%
)

echo [2/2] Compilacion completada con exito.
echo APK generado en: app\build\outputs\apk\debug\app-debug.apk
endlocal
