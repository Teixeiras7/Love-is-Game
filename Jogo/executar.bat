@echo off
setlocal
cd /d "%~dp0"

where javac >nul 2>nul
if errorlevel 1 (
    echo [ERRO] Java JDK nao encontrado. Instale o JDK 21 ou superior:
    echo        https://adoptium.net/
    pause
    exit /b 1
)

rem Usa o javaw da mesma pasta do javac (o PATH pode ter um Java 8 antigo na frente).
for /f "delims=" %%i in ('where javac') do (
    set "JDKBIN=%%~dpi"
    goto :found
)
:found

if not exist bin mkdir bin

echo Compilando...
javac -encoding UTF-8 -d bin -sourcepath src src\app\Main.java
if errorlevel 1 (
    echo [ERRO] Falha na compilacao.
    pause
    exit /b 1
)

echo Iniciando o jogo...
start "" "%JDKBIN%javaw.exe" -cp bin app.Main
endlocal
