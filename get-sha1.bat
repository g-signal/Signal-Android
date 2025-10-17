@echo off
echo ========================================
echo Getting Debug Keystore SHA-1 Fingerprint
echo ========================================
echo.

keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android | findstr "SHA1:"
keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android | findstr "SHA256:"

echo.
echo ========================================
echo Add these fingerprints to Firebase Console:
echo 1. Go to https://console.firebase.google.com/
echo 2. Select project: ba-chat-2298a
echo 3. Go to Project Settings
echo 4. Find app: org.thoughtcrime.securesms
echo 5. Click "Add fingerprint" and paste the SHA-1 and SHA-256 values above
echo ========================================
pause
