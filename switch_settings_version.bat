@echo off
setlocal enabledelayedexpansion

echo.
echo ========================================
echo settings.gradle.kts Version Switcher
echo ========================================
echo.
echo Available versions:
echo   1) Original  - Signal official version (current)
echo   2) Enhanced  - With diagnostics and validation
echo   3) Reference - With detailed Chinese comments
echo   4) Restore from backup
echo.

set /p choice="Select version (1-4): "

if "%choice%"=="1" (
    if exist settings.gradle.kts.backup (
        echo.
        echo [INFO] Restoring original version from backup...
        copy /Y settings.gradle.kts.backup settings.gradle.kts >nul
        echo [OK] Original version restored
    ) else (
        echo.
        echo [INFO] Already using original version
    )
) else if "%choice%"=="2" (
    if not exist settings.gradle.kts.enhanced (
        echo.
        echo [ERROR] Enhanced version not found!
        echo [INFO] Please ensure settings.gradle.kts.enhanced exists
        pause
        exit /b 1
    )

    echo.
    if not exist settings.gradle.kts.backup (
        echo [INFO] Creating backup of current settings...
        copy /Y settings.gradle.kts settings.gradle.kts.backup >nul
        echo [OK] Backup created: settings.gradle.kts.backup
    )

    echo [INFO] Switching to enhanced version...
    copy /Y settings.gradle.kts.enhanced settings.gradle.kts >nul
    echo [OK] Enhanced version activated
    echo.
    echo Features enabled:
    echo   - Automatic path validation
    echo   - Native library checking
    echo   - Architecture detection
    echo   - Detailed error messages
) else if "%choice%"=="3" (
    echo.
    echo [INFO] Reference version is read-only
    echo [INFO] Location: settings.gradle.kts.reference
    echo.
    echo To view: notepad settings.gradle.kts.reference
    pause
    exit /b 0
) else if "%choice%"=="4" (
    if not exist settings.gradle.kts.backup (
        echo.
        echo [ERROR] No backup found!
        pause
        exit /b 1
    )

    echo.
    echo [INFO] Restoring from backup...
    copy /Y settings.gradle.kts.backup settings.gradle.kts >nul
    echo [OK] Settings restored from backup
) else (
    echo.
    echo [ERROR] Invalid choice!
    pause
    exit /b 1
)

echo.
echo ========================================
echo Switch Complete!
echo ========================================
echo.
echo Next steps:
echo   1. Run: gradlew --refresh-dependencies
echo   2. Or sync project in Android Studio
echo.
pause
