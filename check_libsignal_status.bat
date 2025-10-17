@echo off
echo.
echo ========================================
echo libsignal Configuration Status Check
echo ========================================
echo.

REM 检查 gradle.properties 配置
echo [1/4] Checking gradle.properties...
findstr /C:"libsignalClientPath=" gradle.properties | findstr /V "^#" >nul 2>&1
if %errorlevel% equ 0 (
    echo   Status: ✅ Local libsignal ENABLED
    for /f "tokens=2 delims==" %%a in ('findstr /C:"libsignalClientPath=" gradle.properties ^| findstr /V "^#"') do (
        set LIBSIGNAL_PATH=%%a
        echo   Path: %%a
    )
) else (
    echo   Status: 📦 Using OFFICIAL libsignal from Maven
    echo   To enable local: run setup_local_libsignal.bat
    goto :skip_checks
)

echo.
echo [2/4] Checking libsignal project...
REM 解析相对路径
if defined LIBSIGNAL_PATH (
    set "FULL_PATH=%cd%\!LIBSIGNAL_PATH!\java"

    if exist "!FULL_PATH!" (
        echo   Path exists: ✅ !FULL_PATH!
    ) else (
        echo   Path exists: ❌ NOT FOUND
        echo   Expected: !FULL_PATH!
        goto :skip_native
    )

    echo.
    echo [3/4] Checking subprojects...
    if exist "!FULL_PATH!\client\build.gradle" (
        echo   client project: ✅ Found
    ) else (
        echo   client project: ❌ Not found
    )

    if exist "!FULL_PATH!\android\build.gradle" (
        echo   android project: ✅ Found
    ) else (
        echo   android project: ❌ Not found
    )

    echo.
    echo [4/4] Checking native libraries (.so files)...
    set "JNILIBS=!FULL_PATH!\android\src\main\jniLibs"

    if exist "!JNILIBS!" (
        echo   jniLibs directory: ✅ Found
        echo.
        echo   Architectures:

        set ARCH_COUNT=0
        for /d %%a in ("!JNILIBS!\*") do (
            set /a ARCH_COUNT+=1
            set ARCH_NAME=%%~nxa

            REM 检查 .so 文件
            if exist "%%a\libsignal_jni.so" (
                for %%s in ("%%a\libsignal_jni.so") do set SIZE=%%~zs
                set /a SIZE_MB=!SIZE!/1024/1024
                echo     - !ARCH_NAME!: ✅ libsignal_jni.so (!SIZE_MB! MB^)
            ) else (
                echo     - !ARCH_NAME!: ❌ libsignal_jni.so NOT FOUND
            )
        )

        if !ARCH_COUNT! equ 0 (
            echo     ⚠️  No architectures found!
            echo.
            echo   Action needed:
            echo   cd E:\code\libsignal\java
            echo   bash build_jni.sh android
        )
    ) else (
        echo   jniLibs directory: ❌ NOT FOUND
        echo.
        echo   Action needed:
        echo   cd E:\code\libsignal\java
        echo   bash build_jni.sh android
    )
) else (
    echo   Cannot determine libsignal path
)

:skip_native
echo.
echo ========================================
echo Status Check Complete
echo ========================================
echo.

:skip_checks
echo Recommendations:
echo   - View detailed guide: type SETTINGS_GRADLE_GUIDE.md
echo   - Switch versions: run switch_settings_version.bat
echo   - Enable local libsignal: run setup_local_libsignal.bat
echo.
pause
