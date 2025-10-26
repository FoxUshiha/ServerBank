@echo off
setlocal ENABLEDELAYEDEXPANSION
pushd "%~dp0"

REM ====== CONFIG ======
set JAR_NAME=Serverbank.jar
set OUT=out
set SRC=src
set RES=resources
set MAINPKG=com\foxsrv\serverbank

REM ====== DEPENDENCIAS ======
REM Ajuste os caminhos conforme seu servidor/ambiente:
set SPIGOT_API=spigot-api-1.21.6-R0.1-SNAPSHOT.jar
set VAULT=Vault.jar

if not exist "%SPIGOT_API%" echo [ERRO] Nao achei %SPIGOT_API% && goto :end
if not exist "%VAULT%" echo [ERRO] Nao achei %VAULT% && goto :end

if exist "%OUT%" rd /s /q "%OUT%"
mkdir "%OUT%"

echo Compilando fontes...
javac -encoding UTF-8 -Xlint:deprecation -Xlint:unchecked ^
 -classpath "%SPIGOT_API%;%VAULT%" ^
 -d "%OUT%" ^
 %SRC%\%MAINPKG%\*.java

if errorlevel 1 (
  echo [ERRO] Falha na compilacao.
  goto :end
)

echo Copiando plugin.yml e resources...
copy /Y "plugin.yml" "%OUT%" >nul
if exist "%RES%" xcopy /E /I /Y "%RES%\*" "%OUT%\" >nul

echo Empacotando JAR...
pushd "%OUT%"
jar cvf "%JAR_NAME%" *
popd

move /Y "%OUT%\%JAR_NAME%" "%~dp0" >nul
echo.
echo [OK] Build concluido: %JAR_NAME%

:end
echo.
pause
popd
