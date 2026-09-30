@echo off
setlocal enabledelayedexpansion

echo ======================================================================
echo    SwiftLab Invoice Billing Application - Windows Packager
echo ======================================================================

set MAVEN_CMD=.\apache-maven-3.9.6\bin\mvn.cmd
if not exist %MAVEN_CMD% set MAVEN_CMD=mvn

echo [1/3] Building Spring Boot application with Maven...
call %MAVEN_CMD% clean package -DskipTests
if errorlevel 1 (
    echo [ERROR] Maven build failed.
    exit /b 1
)

echo [2/3] Setting up WiX Toolset environment...
set PATH=%~dp0wix-bin;%PATH%

echo [3/3] Generating native Windows application and installer EXE...
if not exist dist mkdir dist

REM Clean previous app-image if it exists
if exist "dist\InvoiceApp" rd /s /q "dist\InvoiceApp"
if exist "dist\InvoiceAppInstaller-1.0.exe" del /f /q "dist\InvoiceAppInstaller-1.0.exe"

REM 1. Create portable app image with standalone InvoiceApp.exe
jpackage ^
    --name "InvoiceApp" ^
    --input target ^
    --main-jar invoice-app-1.0.0.jar ^
    --main-class org.springframework.boot.loader.launch.JarLauncher ^
    --dest dist ^
    --type app-image

REM 2. Create Windows installer EXE
jpackage ^
    --name "InvoiceAppInstaller" ^
    --input target ^
    --main-jar invoice-app-1.0.0.jar ^
    --main-class org.springframework.boot.loader.launch.JarLauncher ^
    --dest dist ^
    --type exe ^
    --win-dir-chooser ^
    --win-menu ^
    --win-shortcut

if exist "dist\InvoiceAppInstaller-1.0.exe" (
    copy /y "dist\InvoiceAppInstaller-1.0.exe" "InvoiceApp-Installer.exe" >nul
)

echo ======================================================================
echo  BUILD SUCCESSFUL!
echo  Portable Executable: dist\InvoiceApp\InvoiceApp.exe
echo  Windows Installer:   InvoiceApp-Installer.exe
echo ======================================================================
