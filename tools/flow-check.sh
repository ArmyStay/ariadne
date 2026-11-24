#!/usr/bin/env bash
# Walks the capture flow the way a person walks it: create a trip, shoot a
# frame, choose one of the two ways out, edit the amount, then the demo path.
#
# Rendering a page proves it lays out; this proves the flow behind it works.
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
T="${ARIADNE_TOOLS:-$HOME/.cache/tools}"
export JAVA_HOME="$T/jdk21"
OUT="$ROOT/build/flow"
CP=""
for j in "$T"/libs/*.jar; do [ -s "$j" ] || continue; CP="$CP$j:"; done
CP="$CP$T/kotlinc/lib/kotlin-stdlib.jar:$T/kotlinc/lib/kotlin-stdlib-jdk8.jar:$T/kotlinc/lib/kotlin-reflect.jar:"
COMPILER_CP="$T/kotlinc/lib/kotlin-compiler-embeddable-2.4.21.jar:$T/kotlinc/lib/kotlin-stdlib.jar:$T/kotlinc/lib/kotlin-reflect.jar:$T/kotlinc/lib/annotations-13.0.jar:$T/kotlinc/lib/kotlinx-coroutines-core-jvm.jar:"
SOURCES=$(find "$ROOT/core/model" "$ROOT/core/queue" "$ROOT/core/extract" "$ROOT/core/fx" \
  "$ROOT/core/search" "$ROOT/core/time" "$ROOT/core/design" "$ROOT/core/store" "$ROOT/ui" \
  -name '*.kt' -path '*commonMain*' ! -name 'AriadneStore.kt' ! -name 'PhotoStore.kt' ! -name 'Open.kt')
rm -rf "$OUT"; mkdir -p "$OUT"
"$JAVA_HOME/bin/java" -cp "$COMPILER_CP" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
  -nowarn -jvm-target 17 -Xplugin="$T/kotlinc/lib/compose-compiler-plugin.jar" \
  -cp "$CP" -d "$OUT" $SOURCES \
  "$ROOT/preview/src/main/kotlin/com/ariadne/preview/FlowCheck.kt" > "$OUT/.log" 2>&1
if grep -qE "error:|^exception:" "$OUT/.log"; then
  grep -E "error:|^exception:" "$OUT/.log" | head -20
  echo "flow-check: compilation failed" >&2; exit 1
fi
"$JAVA_HOME/bin/java" -cp "$CP$OUT" com.ariadne.preview.FlowCheckKt
