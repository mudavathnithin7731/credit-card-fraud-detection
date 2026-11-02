@echo off
setlocal
if not defined JAVA_HOME (
    set "JAVA_HOME=C:\Users\nithin\.gemini\antigravity\scratch\jdk-21.0.13.0.2+1"
)
set "JAVA=%JAVA_HOME%\bin\java.exe"

if not exist "bin\com\fraud\web\FraudWebServer.class" (
    echo Building project...
    call compile.bat
)

echo ==================================================================
echo   Starting Credit Card Fraud Detection Web Server...
echo ==================================================================
echo URL: http://localhost:8080
echo Your browser will open automatically.
echo (Press Ctrl+C in this window to stop the server)
echo ==================================================================

start "" "http://localhost:8080"
"%JAVA%" -cp bin com.fraud.Main --web
endlocal
