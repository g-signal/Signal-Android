@echo off
echo ====================================================
echo      Generating Release Keystore for BA Chat
echo ====================================================
echo.
echo IMPORTANT: Please save the passwords you enter!
echo You will need them for future app updates!
echo.
echo ====================================================
echo.

set KEYSTORE_FILE=bachat-release.keystore
set KEY_ALIAS=bachat-release
set VALIDITY=9125

echo Creating keystore at: %CD%\%KEYSTORE_FILE%
echo Key alias: %KEY_ALIAS%
echo Validity: %VALIDITY% days (25 years)
echo.
echo Certificate Info:
echo   Organization: Baxs
echo   Country: US
echo.
echo ====================================================
echo.

keytool -genkey -v ^
  -keystore "%KEYSTORE_FILE%" ^
  -alias "%KEY_ALIAS%" ^
  -keyalg RSA ^
  -keysize 2048 ^
  -validity %VALIDITY% ^
  -dname "CN=BA Chat, O=Baxs, C=US"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ====================================================
    echo SUCCESS! Keystore created successfully!
    echo ====================================================
    echo.
    echo Keystore file: %CD%\%KEYSTORE_FILE%
    echo Key alias: %KEY_ALIAS%
    echo.
    echo IMPORTANT: Save your passwords in a secure location!
    echo.
    echo Next steps:
    echo 1. Keep the passwords safe
    echo 2. I will create the configuration file for you
    echo.
) else (
    echo.
    echo ====================================================
    echo ERROR: Failed to create keystore
    echo ====================================================
    echo.
)

pause
