#!/usr/bin/env bash
# Compiles the shared Compose UI against real Compose Desktop jars with the real
# Compose compiler plugin, on a JVM, without Gradle. This is how the UI is
# verified inside the sandbox; Gradle remains the source of truth for the four
# real targets.
#
# Usage: tools/verify-ui.sh <outDir> <sources...>
set -uo pipefail

TOOLS="${ARIADNE_TOOLS:-$HOME/.tools}"
JAVA_HOME="$TOOLS/jdk21"
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

CP=""
for j in "$TOOLS"/libs/*.jar; do
  size=$(stat -c%s "$j")
  [ "$size" -gt 12000 ] || continue
  case "$(basename "$j")" in
    compose-compiler.jar|cc-full.jar|coroutines.jar) continue ;;
  esac
  CP="$CP$j:"
done

COMPILER_CP=""
for j in "$TOOLS"/kotlinc/lib/*.jar; do
  case "$j" in *sources*) continue ;; esac
  COMPILER_CP="$COMPILER_CP$j:"
done

OUT="$1"; shift
rm -rf "$OUT"; mkdir -p "$OUT"

"$JAVA_HOME/bin/java" -cp "$TOOLS/psi-shim.jar:$COMPILER_CP" \
  org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
  -nowarn -jvm-target 17 \
  -Xplugin="$TOOLS/kotlinc/lib/compose-compiler-plugin.jar" \
  -cp "$CP" \
  -d "$OUT" \
  "$@" 2>&1 | grep -v "^warning:" 
