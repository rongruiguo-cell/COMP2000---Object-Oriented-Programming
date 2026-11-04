#!/bin/sh
set -eu
cd "$(dirname "$0")"
if [ ! -f dist/traffic-simulation.jar ]; then sh build.sh; fi
java -jar dist/traffic-simulation.jar "$@"
