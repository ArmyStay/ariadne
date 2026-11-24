#!/usr/bin/env bash
# Compiles and launches the desktop preview of the real shared UI.
#
#   tools/run-preview.sh            # wide window (desktop layout)
#   tools/run-preview.sh --phone    # 420x860 window (phone layout)
#   tools/run-preview.sh --night    # night paper
#   tools/run-preview.sh --phone --night
#   tools/run-preview.sh --shots docs/shots   # render every page to PNG, no window
#
# No Gradle, no database, no emulator: the window runs the same commonMain UI the
# four real targets compile, against the in-memory store.
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TOOLS="${ARIADNE_TOOLS:-$HOME/.cache/tools}"
export JAVA_HOME="$TOOLS/jdk21"
export PATH="$JAVA_HOME/bin:$PATH"
OUT="$ROOT/build/preview"

CP=""
for j in "$TOOLS"/libs/*.jar; do
  [ "$(stat -c%s "$j")" -gt 12000 ] || continue
  case "$(basename "$j")" in compose-compiler.jar|cc-full.jar|coroutines.jar) continue ;; esac
  CP="$CP$j:"
done

COMPILER_CP=""
for j in "$TOOLS"/kotlinc/lib/*.jar; do
  case "$j" in *sources*) continue ;; esac
  COMPILER_CP="$COMPILER_CP$j:"
done

SOURCES=(
  $(find "$ROOT/core/model" "$ROOT/core/queue" "$ROOT/core/extract" "$ROOT/core/fx" \
         "$ROOT/core/search" "$ROOT/core/time" "$ROOT/core/design" \
         "$ROOT/core/store" "$ROOT/ui" -name '*.kt' -path '*commonMain*' \
         ! -name 'AriadneStore.kt' ! -name 'PhotoStore.kt')
  "$ROOT/preview/src/main/kotlin/com/ariadne/preview/Main.kt"
  "$ROOT/preview/src/main/kotlin/com/ariadne/preview/Shots.kt"
)

echo "compiling ${#SOURCES[@]} files…"
rm -rf "$OUT"; mkdir -p "$OUT"
"$JAVA_HOME/bin/java" -cp "$TOOLS/psi-shim.jar:$COMPILER_CP" \
  org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
  -nowarn -jvm-target 17 \
  -Xplugin="$TOOLS/kotlinc/lib/compose-compiler-plugin.jar" \
  -cp "$CP" -d "$OUT" "${SOURCES[@]}" 2>&1 | grep -E "error:" && {
    echo "compilation failed"; exit 1; }

# Runtime adds the standard library and the reflection/serialization helpers the
# stdlib drags in; the compile classpath above already has everything else.
# Fonts and other resources: kotlinc only compiles sources, so the module's
# resource directories have to be put on the runtime classpath by hand.
RES=""
for d in "$ROOT"/core/*/src/commonMain/resources; do
  [ -d "$d" ] && RES="$RES$d:"
done
RTP="$CP$OUT:$RES"
for j in "$TOOLS"/kotlinc/lib/kotlin-stdlib.jar "$TOOLS"/kotlinc/lib/kotlin-stdlib-jdk8.jar \
         "$TOOLS"/kotlinc/lib/kotlin-stdlib-jdk7.jar "$TOOLS"/kotlinc/lib/annotations-13.0.jar \
         "$TOOLS"/kotlinc/lib/kotlin-reflect.jar; do
  [ -f "$j" ] && RTP="$RTP$j:"
done

if [ "${1:-}" = "--shots" ]; then
  OUTDIR="${2:-$ROOT/docs/shots}"
  echo "rendering pages to $OUTDIR…"
  "$JAVA_HOME/bin/java" -Djava.awt.headless=true -cp "$RTP" com.ariadne.preview.ShotsKt "$OUTDIR"
  exit $?
fi

echo "launching (close the window to exit)…"
"$JAVA_HOME/bin/java" -cp "$RTP" com.ariadne.preview.MainKt "$@"
