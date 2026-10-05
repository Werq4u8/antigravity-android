@echo off
setlocal enabledelayedexpansion

echo ========================================================
echo        Antigravity Mobile - APK Build Script
echo ========================================================

where javac >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] JDK 17+ is not detected in your PATH.
    echo.
    echo To build on Windows:
    echo 1. Install Android Studio (recommended) and open this folder.
    echo    OR install JDK 17 (e.g. from https://adoptium.net/temurin/releases/).
    echo.
    echo Alternatively, push this folder to GitHub:
    echo GitHub Actions will automatically build the APK for you!
    pause
    exit /b 1
)

echo JDK detected! Launching Gradle build...
if exist "gradlew.bat" (
    call gradlew.bat assembleDebug
) else (
    call gradle assembleDebug
)

if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo.
    echo ========================================================
    echo SUCCESS! APK successfully generated at:
    echo app\build\outputs\apk\debug\app-debug.apk
    echo ========================================================
) else (
    echo [ERROR] Build failed or output APK not found.
)

pause
