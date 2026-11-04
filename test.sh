#!/bin/sh
set -eu
cd "$(dirname "$0")"
sh build.sh
mkdir -p build/test-classes
find tests -name '*.java' -print | sort > build/test-sources.txt
if command -v javac >/dev/null 2>&1; then
    javac --release 17 -encoding UTF-8 -Xlint:all -cp build/classes -d build/test-classes @build/test-sources.txt
else
    java -m jdk.compiler/com.sun.tools.javac.Main --release 17 -encoding UTF-8 -Xlint:all -cp build/classes -d build/test-classes @build/test-sources.txt
fi
java -Djava.awt.headless=true -cp build/classes:build/test-classes trafficsim.tests.RegressionTests
