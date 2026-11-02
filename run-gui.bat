@echo off
setlocal
if not defined JAVA_HOME (
    set "JAVA_HOME=C:\Users\nithin\.gemini\antigravity\scratch\jdk-21.0.13.0.2+1"
)
set "JAVA=%JAVA_HOME%\bin\java.exe"

if not exist "bin\com\fraud\Main.class" (
    echo Building project first...
    call compile.bat
)

echo Launching Desktop GUI...
start "" "%JAVA%" -cp bin com.fraud.Main --gui
endlocal
