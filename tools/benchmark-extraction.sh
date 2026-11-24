#!/usr/bin/env bash
# Cold-run the extraction benchmark (see tools/extraction-bench/BenchMain.kt):
# parse latency p50 over the committed receipt fixtures, using the same kotlinc
# harness as tools/test-domain.sh. Output goes to stdout and to
# docs/benchmarks/extraction-p50.txt.
#
#   tools/benchmark-extraction.sh      # prints + writes the committed file
#
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
T="${ARIADNE_TOOLS:-$HOME/.cache/tools}"
export JAVA_HOME="$T/jdk21"
CP="$(ls $T/libs/*.jar | tr '\n' ':')$T/kotlinc/lib/kotlin-stdlib.jar"
OUT="$(mktemp -d)"
SRCS=$(find "$ROOT"/core/extract -name "*.kt" \( -path "*commonMain*" -o -path "*commonTest*" \))

"$T/kotlinc/bin/kotlinc" -nowarn -jvm-target 17 -cp "$CP" -module-name ariadne.extract \
  $SRCS "$ROOT/tools/extraction-bench/BenchMain.kt" -d "$OUT"

RES="$( \
  "$JAVA_HOME/bin/java" -cp "$CP:$OUT" com.ariadne.extract.bench.BenchKt 2>&1 )"
echo "$RES"
mkdir -p "$ROOT/docs/benchmarks"
echo "$RES" > "$ROOT/docs/benchmarks/extraction-p50.txt"
echo "wrote docs/benchmarks/extraction-p50.txt"
