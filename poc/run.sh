#!/bin/sh
# Compile and start the PoC. Extra arguments are passed on, e.g. ./run.sh --demo-patients
cd "$(dirname "$0")" || exit 1
rm -rf out
javac -encoding UTF-8 -d out $(find src -name "*.java") && java -cp out ats.Main "$@"
