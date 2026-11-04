@echo off
setlocal
cd /d "%~dp0"
where javac >nul 2>nul
if errorlevel 1 (
  echo Java 17 or newer JDK is required to compile. Add its bin folder to PATH.
  exit /b 1
)
if exist build\classes rmdir /s /q build\classes
mkdir build\classes
if not exist dist mkdir dist
powershell -NoProfile -Command "$lines = Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { '{0}{1}{0}' -f [char]34, $_.FullName.Replace('\','/') }; [IO.File]::WriteAllLines((Join-Path (Get-Location) 'build/sources.txt'), [string[]]$lines, (New-Object Text.UTF8Encoding($false)))"
if errorlevel 1 exit /b 1
javac --release 17 -encoding UTF-8 -Xlint:all -d build\classes @build\sources.txt
if errorlevel 1 exit /b 1
jar --create --file dist\traffic-simulation.jar --main-class trafficsim.Main -C build\classes .
if errorlevel 1 exit /b 1
echo Built dist\traffic-simulation.jar
