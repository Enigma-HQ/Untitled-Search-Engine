@echo off
chcp 65001 > nul
setlocal

echo ============================================
echo   Bilingual IR Search Engine - Build Script
echo ============================================

REM ---- CONFIGURATION: Set your paths here ----
set JAVA_HOME=C:\Program Files\Java\jdk-17
set JAVAFX_LIB=C:\javafx-sdk-21\lib

REM ---- Derived paths ----
set JAVAC=%JAVA_HOME%\bin\javac
set JAVA=%JAVA_HOME%\bin\java
set SRC_DIR=src
set OUT_DIR=out\classes

REM ---- Create output directory ----
if not exist %OUT_DIR% mkdir %OUT_DIR%

echo.
echo [1/3] Compiling Java sources...
"%JAVAC%" ^
    --module-path "%JAVAFX_LIB%" ^
    --add-modules javafx.controls,javafx.fxml ^
    -encoding UTF-8 ^
    -d %OUT_DIR% ^
    -sourcepath %SRC_DIR% ^
    %SRC_DIR%\engine\*.java ^
    %SRC_DIR%\ui\MainApp.java

if errorlevel 1 (
    echo [ERROR] Compilation failed!
    pause
    exit /b 1
)

echo [2/3] Copying resources (CSS)...
xcopy /Y /Q "%SRC_DIR%\ui\styles.css" "%OUT_DIR%\ui\" > nul 2>&1

echo [3/3] Launching application...
echo.
"%JAVA%" ^
    --module-path "%JAVAFX_LIB%" ^
    --add-modules javafx.controls,javafx.fxml ^
    -cp %OUT_DIR% ^
    ui.MainApp

pause
