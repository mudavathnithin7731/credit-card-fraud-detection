@echo off
setlocal
echo ========================================================
echo  Compiling Credit Card Fraud Detection System (Java)
echo ========================================================

REM Locate Java Compiler
if not defined JAVA_HOME (
    set "JAVA_HOME=C:\Users\nithin\.gemini\antigravity\scratch\jdk-21.0.13.0.2+1"
)
set "JAVAC=%JAVA_HOME%\bin\javac.exe"

if not exist "%JAVAC%" (
    echo [ERROR] javac.exe not found at %JAVAC%
    pause
    exit /b 1
)

if not exist "bin" mkdir "bin"

echo Compiling source files from src...
"%JAVAC%" -d bin -sourcepath src src\com\fraud\model\*.java src\com\fraud\engine\rules\*.java src\com\fraud\engine\ml\*.java src\com\fraud\engine\*.java src\com\fraud\data\*.java src\com\fraud\ui\*.java src\com\fraud\web\*.java src\com\fraud\*.java

if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Compilation complete. Bytecode stored in bin/
) else (
    echo [ERROR] Compilation failed.
)
endlocal
