#!/bin/sh
set -eu
cd "$(dirname "$0")"
rm -rf build/classes
mkdir -p build/classes dist
find src -name '*.java' -print | sort > build/sources.txt
if command -v javac >/dev/null 2>&1; then
    javac --release 17 -encoding UTF-8 -Xlint:all -d build/classes @build/sources.txt
else
    java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -encoding UTF-8 -Xlint:all -d build/classes @build/sources.txt
fi
java -m jdk.jartool/sun.tools.jar.Main --create --file dist/traffic-simulation.jar --main-class trafficsim.Main -C build/classes .
echo "Built dist/traffic-simulation.jar"
