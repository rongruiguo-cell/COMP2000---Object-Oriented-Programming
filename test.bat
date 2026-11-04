@echo off
setlocal
cd /d "%~dp0"
call build.bat
if errorlevel 1 exit /b 1
if not exist build\test-classes mkdir build\test-classes
powershell -NoProfile -Command "$lines = Get-ChildItem -Recurse tests -Filter *.java | ForEach-Object { '{0}{1}{0}' -f [char]34, $_.FullName.Replace('\','/') }; [IO.File]::WriteAllLines((Join-Path (Get-Location) 'build/test-sources.txt'), [string[]]$lines, (New-Object Text.UTF8Encoding($false)))"
if errorlevel 1 exit /b 1
javac --release 17 -encoding UTF-8 -Xlint:all -cp build\classes -d build\test-classes @build\test-sources.txt
if errorlevel 1 exit /b 1
java -Djava.awt.headless=true -cp "build\classes;build\test-classes" trafficsim.tests.RegressionTests
