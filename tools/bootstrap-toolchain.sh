#!/usr/bin/env bash
# A Kotlin toolchain, without Gradle and without Maven.
#
#   tools/bootstrap-toolchain.sh
#
# Why this exists: `tools/test-domain.sh` expects a compiler and a JDK under
# `~/.cache/tools`, which is fine on a developer machine and impossible in a sandbox
# that can reach npm and PyPI but not `repo.maven.apache.org`. This script gets the
# same toolchain from the two registries that *are* reachable:
#
#   * the Kotlin compiler is published as an npm package (`kotlin-compiler`), and the
#     tarball is the ordinary compiler distribution — `bin/`, `lib/kotlin-compiler.jar`,
#     the stdlib, the Compose compiler plugin;
#   * the JDK comes from PyPI as `jdk4py`, a wheel with a Temurin runtime inside.
#
# It installs both into $ARIADNE_TOOLS (default ~/.cache/tools) in the layout
# `tools/test-domain.sh` expects, then compiles and runs the domain suites with a
# small `kotlin.test` shim so no JUnit jar is needed.
#
# This is a fallback for environments that cannot use Gradle. It is not a
# replacement: it compiles the pure-Kotlin cores only. Nothing here can build or
# render Compose.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
T="${ARIADNE_TOOLS:-$HOME/.cache/tools}"
KOTLIN_VERSION="${KOTLIN_VERSION:-2.4.21}"
WORK="${WORK:-/tmp/ariadne-bootstrap}"

mkdir -p "$T" "$WORK"

# ── the compiler ─────────────────────────────────────────────────────────────
if [ ! -x "$T/kotlinc/bin/kotlinc" ]; then
  echo "== kotlin $KOTLIN_VERSION from the npm registry =="
  curl -fsSL -o "$WORK/kotlin-compiler.tgz" \
    "https://registry.npmjs.org/kotlin-compiler/-/kotlin-compiler-${KOTLIN_VERSION}.tgz"
  tar xzf "$WORK/kotlin-compiler.tgz" -C "$WORK"
  rm -rf "$T/kotlinc"
  mv "$WORK/package" "$T/kotlinc"
fi

# ── the JDK ──────────────────────────────────────────────────────────────────
if [ ! -x "$T/jdk21/bin/java" ]; then
  echo "== jdk from PyPI (jdk4py) =="
  python3 -m pip install --quiet --target "$WORK/pyjdk" jdk4py
  rm -rf "$T/jdk21"
  cp -R "$WORK/pyjdk/jdk4py/java-runtime" "$T/jdk21"
fi

# ── the shim and the runner ──────────────────────────────────────────────────
# `kotlin.test` on the JVM normally resolves to JUnit. A shim with the handful of
# assertions these suites use keeps the run to one jar: the stdlib.
SHIM="$T/harness"
mkdir -p "$SHIM"
cat > "$SHIM/KotlinTestShim.kt" <<'SHIM_KT'
@file:Suppress("unused")
package kotlin.test

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
public annotation class Test

public fun fail(message: String? = null): Nothing = throw AssertionError(message ?: "failed")
public fun <T> assertEquals(expected: T, actual: T, message: String? = null) {
    if (expected != actual) fail((message?.plus(": ") ?: "") + "expected <$expected> but was <$actual>")
}
public fun assertEquals(expected: Long, actual: Long, message: String? = null) {
    if (expected != actual) fail((message?.plus(": ") ?: "") + "expected <$expected> but was <$actual>")
}
public fun assertEquals(expected: Double, actual: Double, absoluteTolerance: Double, message: String? = null) {
    if (kotlin.math.abs(expected - actual) > absoluteTolerance) fail(message ?: "doubles differ")
}
public fun <T> assertNotEquals(unexpected: T, actual: T, message: String? = null) {
    if (unexpected == actual) fail((message?.plus(": ") ?: "") + "expected anything but <$actual>")
}
public fun assertTrue(actual: Boolean, message: String? = null) { if (!actual) fail(message ?: "expected true") }
public fun assertFalse(actual: Boolean, message: String? = null) { if (actual) fail(message ?: "expected false") }
public fun <T> assertNull(actual: T?, message: String? = null) {
    if (actual != null) fail((message?.plus(": ") ?: "") + "expected null but was <$actual>")
}
public fun <T : Any> assertNotNull(actual: T?, message: String? = null): T =
    actual ?: fail(message ?: "expected not null")
public fun <T> assertContentEquals(expected: List<T>, actual: List<T>, message: String? = null) {
    if (expected != actual) fail(message ?: "lists differ")
}
public fun assertContentEquals(expected: ByteArray?, actual: ByteArray?, message: String? = null) {
    val a: ByteArray = expected ?: ByteArray(0)
    val b: ByteArray = actual ?: ByteArray(0)
    if (!a.contentEquals(b)) fail(message ?: "byte arrays differ")
}
SHIM_KT

cat > "$SHIM/Runner.kt" <<'RUNNER_KT'
fun main(args: Array<String>) {
    var passed = 0
    var failed = 0
    for (name in args) {
        val cls = Class.forName(name)
        val instance = cls.getDeclaredConstructor().newInstance()
        val tests = cls.methods
            .filter { it.isAnnotationPresent(kotlin.test.Test::class.java) && it.parameterCount == 0 }
            .sortedBy { it.name }
        for (m in tests) {
            try {
                m.invoke(instance)
                passed++
            } catch (e: Throwable) {
                failed++
                val cause = (e as? java.lang.reflect.InvocationTargetException)?.targetException ?: e
                println("FAIL ${cls.simpleName}.${m.name}: ${cause.message}")
            }
        }
    }
    println("passed=$passed failed=$failed")
    if (failed > 0) kotlin.system.exitProcess(1)
}
RUNNER_KT

# ── run the two suites this environment can compile ──────────────────────────
export JAVA_HOME="$T/jdk21"
export PATH="$JAVA_HOME/bin:$T/kotlinc/bin:$PATH"
STDLIB="$T/kotlinc/lib/kotlin-stdlib.jar"

echo "== core:model =="
kotlinc -nowarn -jvm-target 17 -Xallow-kotlin-package \
  $(find "$ROOT/core/model/src/commonMain/kotlin" "$ROOT/core/model/src/commonTest/kotlin" -name '*.kt') \
  "$SHIM/KotlinTestShim.kt" "$SHIM/Runner.kt" -d "$WORK/out-model"
java -cp "$WORK/out-model:$STDLIB" RunnerKt \
  com.ariadne.model.MoneyTest com.ariadne.model.LocalDayTest \
  com.ariadne.model.MerchantNameTest com.ariadne.model.EntryStatusTest \
  com.ariadne.model.BudgetPlanTest com.ariadne.model.SpokenExpenseTest \
  com.ariadne.model.ShareCodeTest com.ariadne.model.ShareCodeFuzzTest

echo "== core:fx =="
kotlinc -nowarn -jvm-target 17 -Xallow-kotlin-package \
  $(find "$ROOT/core/model/src/commonMain/kotlin" "$ROOT/core/fx/src/commonMain/kotlin" \
    "$ROOT/core/fx/src/commonTest/kotlin" -name '*.kt') \
  "$SHIM/KotlinTestShim.kt" "$SHIM/Runner.kt" -d "$WORK/out-fx"
java -cp "$WORK/out-fx:$STDLIB" RunnerKt \
  com.ariadne.fx.FxTest com.ariadne.fx.CategoryTotalsTest \
  com.ariadne.fx.RatesTest com.ariadne.fx.FxHonestyTest

echo
echo "toolchain: $T (kotlinc $KOTLIN_VERSION, JDK from jdk4py)"
echo "for the Gradle legs of the real suite: tools/test-domain.sh"
