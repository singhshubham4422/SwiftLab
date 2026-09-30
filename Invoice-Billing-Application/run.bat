@echo off
title SwiftLab Invoice Billing Application
echo ======================================================================
echo    Starting SwiftLab Invoice Billing Application...
echo ======================================================================
cd /d "%~dp0"

REM Use official Microsoft-signed java runtime
start "" javaw -jar "target\invoice-app-1.0.0.jar"

echo Application launched! Opening http://localhost:8080/ ...
timeout /t 2 >nul
start "" "http://localhost:8080/"
exit
