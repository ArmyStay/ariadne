#!/usr/bin/env bash
# Domain test run: no Gradle, no network, no emulator. Compiles the pure-Kotlin
# cores with kotlinc and runs them under JUnit.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
T="${ARIADNE_TOOLS:-$HOME/.cache/tools}"
export JAVA_HOME="$T/jdk21"
CP="$(ls $T/libs/*.jar | tr '\n' ':')$T/kotlinc/lib/kotlin-stdlib.jar:$T/kotlinc/lib/kotlin-test.jar:$T/kotlinc/lib/kotlin-test-junit.jar"
OUT="${1:-/tmp/ariadne-tests}"
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
