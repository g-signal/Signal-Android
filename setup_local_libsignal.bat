@echo off
echo ========================================
echo Setting up Signal-Android to use local libsignal
echo ========================================
echo.

REM 检查 gradle.properties 是否已经配置
findstr /C:"libsignalClientPath=../libsignal" gradle.properties >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Local libsignal is already configured.
) else (
    echo [INFO] Configuring gradle.properties...
    echo. >> gradle.properties
    echo # Use local libsignal for development >> gradle.properties
    echo libsignalClientPath=../libsignal >> gradle.properties
    echo org.gradle.dependency.verification=lenient >> gradle.properties
    echo [OK] Configuration added!
)

echo.
echo [INFO] Cleaning build cache...
call gradlew clean >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Build cache cleaned
) else (
    echo [WARN] Failed to clean build cache, but continuing...
)

echo.
echo ========================================
echo Setup Complete!
echo ========================================
echo.
echo Your Signal-Android is now configured to use:
echo   E:\code\libsignal
echo.
echo Next steps:
echo   1. Build Signal-Android: gradlew assemblePlayProdDebug
echo   2. Or use Android Studio to build
echo.
echo The native libraries (.so files) from your custom
echo libsignal build will be included automatically.
echo.
pause
