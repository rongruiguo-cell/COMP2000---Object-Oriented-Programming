@echo off
setlocal
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
  echo Java 17 or newer is required. Install a JDK and add its bin folder to PATH.
  pause
  exit /b 1
)
if not exist dist\traffic-simulation.jar (
  call build.bat
  if errorlevel 1 (
    pause
    exit /b 1
  )
)
java -jar dist\traffic-simulation.jar %*
if errorlevel 1 pause
