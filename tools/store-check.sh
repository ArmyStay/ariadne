#!/usr/bin/env bash
# The proof that :core:store is real: generate the SQLDelight interface from the
# app's own schema, compile AriadneStore against it, then drive it over an actual
# SQLite file.
#
# This exists because the four-target Gradle build does not fit in every sandbox,
# and "the store has never been compiled" is a risk worth removing permanently.
#
#   tools/store-check.sh
#
# Needs: a JDK 21 in $ARIADNE_TOOLS (default ~/.cache/tools) and a few SQLDelight
# jars in $ARIADNE_TOOLS/sdl — see RUN.md, or just run it and read what's missing.
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
T="${ARIADNE_TOOLS:-$HOME/.cache/tools}"
export JAVA_HOME="$T/jdk21"
SD="$T/sdl"

fail() { echo "store-check: $*" >&2; exit 1; }

[ -d "$T/jdk21" ] || fail "no JDK at $T/jdk21"
[ -d "$T/kotlinc" ] || fail "no kotlinc at $T/kotlinc"
for j in runtime-jvm sqlite-driver jdbc-driver coroutines-ext sqlite-jdbc; do
  [ -f "$SD/$j.jar" ] || fail "missing $SD/$j.jar (see RUN.md, 'store jars')"
done

CP=""
for j in "$T"/libs/*.jar; do [ -s "$j" ] && CP="$CP$j:"; done
# The stdlib and reflect are not in libs/: they live with the compiler, and a
# compile classpath without them fails on `kotlin.Unit` of all things.
CP="$CP$T/kotlinc/lib/kotlin-stdlib.jar:$T/kotlinc/lib/kotlin-stdlib-jdk8.jar:$T/kotlinc/lib/kotlin-reflect.jar:"
CP="$CP$SD/runtime-jvm.jar:$SD/sqlite-driver.jar:$SD/jdbc-driver.jar:$SD/coroutines-ext.jar:$SD/sqlite-jdbc.jar"
# The narrow classpath, not the whole distribution: kotlinc/lib also ships plugin
# jars built against unrelocated IntelliJ classes, and loading them next to the
# embeddable compiler makes the frontend die with AbstractMethodError.
COMPILER_CP="$T/kotlinc/lib/kotlin-compiler-embeddable-2.4.21.jar:$T/kotlinc/lib/kotlin-stdlib.jar:$T/kotlinc/lib/kotlin-reflect.jar:$T/kotlinc/lib/annotations-13.0.jar:$T/kotlinc/lib/kotlinx-coroutines-core-jvm.jar:"

# 1. Generate the interface from the app's schema. Codegen runs in its own
#    JVM-only Gradle project so it does not need AGP's memory.
echo "1/3  generating the SQLDelight interface from core/store's schema…"
( cd "$ROOT/tools/storecheck" && ./gradlew --no-daemon -q \
    -Dorg.gradle.jvmargs=-Xmx700m generateMainAriadneDbInterface ) \
  || fail "code generation failed"
GEN="$(find "$ROOT/tools/storecheck/build/generated" -name '*.kt')"
[ -n "$GEN" ] || fail "no generated sources"

# 2. Compile the store and the smoke test against the generated code.
echo "2/3  compiling AriadneStore against it…"
OUT="$ROOT/build/store-check"
rm -rf "$OUT"; mkdir -p "$OUT"
SRCS="$(find "$ROOT"/core/model "$ROOT"/core/time "$ROOT"/core/queue "$ROOT"/core/fx "$ROOT"/core/store \
        -name '*.kt' -path '*commonMain*')"
"$JAVA_HOME/bin/java" -cp "${T}/psi-shim.jar:$COMPILER_CP" \
  org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
  -nowarn -jvm-target 17 -cp "$CP" -d "$OUT" \
  $SRCS $GEN "$ROOT/tools/storecheck/StoreSmoke.kt" 2>&1 | grep -E "error:" && fail "compilation failed"
[ -f "$OUT/StoreSmokeKt.class" ] || fail "smoke test did not compile"

# 3. Run it against a real SQLite file.
echo "3/3  running the store against real SQLite…"
RTP="$CP:$OUT:$T/kotlinc/lib/kotlin-stdlib.jar:$T/kotlinc/lib/kotlin-stdlib-jdk8.jar:$T/kotlinc/lib/annotations-13.0.jar"
"$JAVA_HOME/bin/java" -cp "$RTP" StoreSmokeKt
