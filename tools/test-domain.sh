#!/usr/bin/env bash
# Domain test run: no Gradle, no network, no emulator. Compiles the pure-Kotlin
# cores with kotlinc and runs them under JUnit.
#
#   tools/test-domain.sh [targets…]      default: jvm
#     jvm   — the kotlinc + JUnitCore run below (all six cores)
#     ios   — ./gradlew <module>:iosSimulatorArm64Test for every core module
#     wasm — ./gradlew <module>:wasmJsNodeTest for every core module
#
# Every target is run independently and logged as PASS/FAIL; the script exits
# non-zero if any leg fails. The JVM leg also exercises the extraction parser
# fixtures, so a regression anywhere in the domain is caught before it reaches
# iOS or Wasm.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
T="${ARIADNE_TOOLS:-$HOME/.cache/tools}"
MODULES=(model queue extract fx search time)
if [ $# -eq 0 ]; then set -- jvm; fi

# ── jvm ──────────────────────────────────────────────────────────────────────
run_jvm() {
  export JAVA_HOME="$T/jdk21"
  CP="$(ls $T/libs/*.jar | tr '\n' ':')$T/kotlinc/lib/kotlin-stdlib.jar:$T/kotlinc/lib/kotlin-test.jar:$T/kotlinc/lib/kotlin-test-junit.jar"
  OUT="${OUT:-/tmp/ariadne-tests}"
  SRCS=$(find "$ROOT"/core/{model,queue,extract,fx,search,time} -name "*.kt" \
    \( -path "*commonMain*" -o -path "*commonTest*" \) | grep -v -E "AriadneStore|PhotoStore")
  rm -rf "$OUT"
  "$T/kotlinc/bin/kotlinc" -nowarn -jvm-target 17 -cp "$CP" -d "$OUT" $SRCS
  "$JAVA_HOME/bin/java" -cp "$CP:$OUT" org.junit.runner.JUnitCore \
    com.ariadne.model.MoneyTest com.ariadne.model.LocalDayTest \
    com.ariadne.fx.FxTest \
    com.ariadne.extract.ReceiptParserFixtureTest \
    com.ariadne.queue.OutboxTest com.ariadne.queue.QueueStateMachineTest \
    com.ariadne.search.SearchIndexTest
}

# ── ios / wasm: the same suite, through Gradle on the native targets ──────────
run_gradle_leg() {  # $1 = leg name, $2 = gradle tasks
  local leg="$1"; shift
  echo "== $leg =="
  ( cd "$ROOT" && ./gradlew "$@" --console=plain )
}

run_ios() {
  local tasks=()
  for m in "${MODULES[@]}"; do tasks+=(":core:$m:iosSimulatorArm64Test"); done
  run_gradle_leg "ios:iosSimulatorArm64Test" "${tasks[@]}"
}

run_wasm() {
  local tasks=()
  for m in "${MODULES[@]}"; do tasks+=(":core:$m:wasmJsNodeTest"); done
  run_gradle_leg "wasm:wasmJsNodeTest" "${tasks[@]}"
}

fail=0
for target in "$@"; do
  case "$target" in
    jvm) run_jvm && echo "TARGET=jvm PASS" ;;
    ios) run_ios && echo "TARGET=ios PASS" ;;
    wasm) run_wasm && echo "TARGET=wasm PASS" ;;
    *) echo "unknown target: $target" >&2; fail=1 ;;
  esac || { echo "TARGET=$target FAIL" >&2; fail=1; }
done
exit $fail
