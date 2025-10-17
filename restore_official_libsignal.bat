@echo off
echo ========================================
echo Restoring Signal-Android to use official libsignal
echo ========================================
echo.

echo [INFO] Commenting out local libsignal configuration...

REM 使用 PowerShell 注释掉配置行
powershell -Command "$content = Get-Content gradle.properties; $content = $content -replace '^libsignalClientPath=', '# libsignalClientPath='; $content = $content -replace '^org.gradle.dependency.verification=lenient', '# org.gradle.dependency.verification=lenient'; $content | Set-Content gradle.properties"

if %errorlevel% equ 0 (
    echo [OK] Configuration commented out
) else (
    echo [ERROR] Failed to modify gradle.properties
    pause
    exit /b 1
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
echo Restore Complete!
echo ========================================
echo.
echo Your Signal-Android will now use the official
echo libsignal libraries from Maven repositories.
echo.
echo Next steps:
echo   1. Build Signal-Android: gradlew assemblePlayProdDebug
echo   2. Or use Android Studio to build
echo.
pause
