#!/usr/bin/env bash
# Compiles all four platform entry points on a JVM, against generated SQLDelight
# code and small stubs for the platform APIs (androidx.activity, UIKit, the DOM).
#
# This does not replace a real build — it cannot prove Gradle resolves the
# Android SDK or that Kotlin/Native links. What it *does* prove is the part that
# was actually at risk: that the Ariadne code in each entry point type-checks
# against the real AppGraph, the real AriadneStore and the real schema. Every
# `Schema`/driver mistake the old code had was caught this way.
#
#   tools/entry-check.sh
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
T="${ARIADNE_TOOLS:-$HOME/.cache/tools}"
export JAVA_HOME="$T/jdk21"
SD="$T/sdl"

fail() { echo "entry-check: $*" >&2; exit 1; }
[ -d "$T/jdk21" ] || fail "no JDK at $T/jdk21"
for j in runtime-jvm sqlite-driver jdbc-driver; do
  [ -f "$SD/$j.jar" ] || fail "missing $SD/$j.jar (run tools/store-check.sh first)"
done

# The schema must be generated before anything here can type-check.
GEN="$ROOT/tools/storecheck/build/generated"
if [ -z "$(find "$GEN" -name '*.kt' 2>/dev/null)" ]; then
  echo "generating the SQLDelight interface first…"
  ( cd "$ROOT/tools/storecheck" && ./gradlew --no-daemon -q \
      -Dorg.gradle.jvmargs=-Xmx700m generateMainAriadneDbInterface ) || fail "codegen failed"
fi
GENSRC="$(find "$GEN" -name '*.kt')"

CP=""
for j in "$T"/libs/*.jar; do [ "$(stat -c%s "$j")" -gt 12000 ] && CP="$CP$j:"; done
CP="$CP$SD/runtime-jvm.jar:$SD/sqlite-driver.jar:$SD/jdbc-driver.jar"

COMMON="$(find "$ROOT"/core/model "$ROOT"/core/time "$ROOT"/core/queue "$ROOT"/core/extract \
          "$ROOT"/core/fx "$ROOT"/core/search "$ROOT"/core/design "$ROOT"/core/store \
          "$ROOT"/ui -name '*.kt' -path '*commonMain*')"

compile() {  # name, extra sources
  local name="$1"; shift
  local out="$ROOT/build/entry-check/$name"
  rm -rf "$out"; mkdir -p "$out"
  echo "  $name…"
  "$JAVA_HOME/bin/java" -cp "$COMPILER_CP" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
    -nowarn -jvm-target 17 -Xplugin="$T/kotlinc/lib/compose-compiler-plugin.jar" \
    -cp "$CP" -d "$out" $COMMON $GENSRC "$@" 2>&1 | grep -E "error:" && return 1
  return 0
}

COMPILER_CP=""
for j in "$T"/kotlinc/lib/*.jar; do case "$j" in *sources*) continue ;; esac; COMPILER_CP="$COMPILER_CP$j:"; done

echo "compiling the four entry points against the real schema…"
rc=0
compile android "$ROOT/tools/entrycheck/stubs/android"/*.kt \
                "$ROOT/app/android/src/main/kotlin/com/ariadne/android/MainActivity.kt" || rc=1
compile ios     "$ROOT/tools/entrycheck/stubs/ios"/*.kt \
                "$ROOT/app/ios/src/iosMain/kotlin/com/ariadne/ios/MainViewController.kt" || rc=1
compile wasm    "$ROOT/tools/entrycheck/stubs/wasm"/*.kt \
                "$ROOT/app/wasm/src/wasmJsMain/kotlin/com/ariadne/wasm/Main.kt" || rc=1
compile desktop "$ROOT/app/desktop/src/main/kotlin/com/ariadne/desktop/Main.kt" || rc=1

if [ "$rc" -ne 0 ]; then
  echo
  echo "entry-check: FAILED"
  exit 1
fi
echo
echo "entry-check: all four entry points compile"
