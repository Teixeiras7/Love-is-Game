@echo off
setlocal
cd /d "%~dp0"
rem Gera web\arte (imagens recortadas + falas) a partir de arte\ e do Falas.java.
rem Rode sempre que mudar a arte ou as falas, e depois suba a pasta web pro GitHub.

where javac >nul 2>nul
if errorlevel 1 (
    echo [ERRO] Java JDK nao encontrado.
    pause
    exit /b 1
)
for /f "delims=" %%i in ('where javac') do (
    set "JDKBIN=%%~dpi"
    goto :found
)
:found

if not exist bin mkdir bin
javac -encoding UTF-8 -d bin -sourcepath src ferramentas\ExportarWeb.java
if errorlevel 1 (
    echo [ERRO] Falha na compilacao.
    pause
    exit /b 1
)
"%JDKBIN%java.exe" -cp bin view.ExportarWeb
pause
