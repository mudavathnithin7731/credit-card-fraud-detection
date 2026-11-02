@echo off
setlocal
echo ==================================================================
echo   Pushing Credit Card Fraud Detection System to GitHub
echo   Target: https://github.com/mudavathnithin7731/credit-card-fraud-detection
echo ==================================================================
echo.
echo Make sure you have created the empty repository on GitHub first:
echo https://github.com/new (Name: credit-card-fraud-detection)
echo.
git push -u origin main

if %ERRORLEVEL% equ 0 (
    echo.
    echo [SUCCESS] Project successfully pushed to GitHub!
    echo View it at: https://github.com/mudavathnithin7731/credit-card-fraud-detection
) else (
    echo.
    echo [NOTICE] If prompted, sign in with your GitHub credentials or Personal Access Token.
)
echo ==================================================================
pause
endlocal
