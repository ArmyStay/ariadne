# Running Ariadne on your own machine

Two routes: the **Gradle-free** one (works today, nothing to install but a JDK or
nothing at all) and the **Gradle** one (the four real targets).

Start with the Gradle-free route if you just want to see it running — it takes
about 40 seconds and needs no Android SDK, no Xcode, no IDE.

---

## 0. Get the code

```bash
git clone https://github.com/ArmyStay/ariadne.git
cd ariadne
```

---

## 1. Gradle-free: run the real UI (recommended first)

This compiles the shared Compose UI directly with `kotlinc` and opens it in a
desktop window. It is the same `AriadneApp` composable that all four targets
call — not a separate build.

It needs a **JDK 21** and, the first time, it downloads nothing: the scripts look
for a toolchain in `~/.cache/tools` and, if it isn't there, print where to put one.

```bash
mkdir -p ~/.cache/tools && cd ~/.cache/tools

# JDK 21 (pick your platform)
# macOS (Apple silicon):  curl -L -o jdk.tgz "https://api.adoptium.net/v3/binary/latest/21/ga/mac/aarch64/jdk/hotspot/normal/eclipse"
# macOS (Intel):          curl -L -o jdk.tgz "https://api.adoptium.net/v3/binary/latest/21/ga/mac/x64/jdk/hotspot/normal/eclipse"
# Linux x64:              curl -L -o jdk.tgz "https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse"
# Windows (Git Bash):     curl -L -o jdk.zip "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse"
tar xzf jdk.tgz && mv jdk-21* jdk21

# Kotlin compiler
curl -L -o kotlinc.zip "https://github.com/JetBrains/kotlin/releases/download/v2.4.21/kotlin-compiler-2.4.21.zip"
unzip -q kotlinc.zip
```

Then the Compose jars (about 25, ~20 MB total):

```bash
mkdir -p libs && cd libs
M=https://repo1.maven.org/maven2
G=https://dl.google.com/dl/android/maven2
C=1.9.0

get() { curl -sSL -o "$2" "$1"; }

# Compose Desktop platform jars
for m in foundation/foundation foundation/foundation-layout ui/ui ui/ui-graphics \
         ui/ui-text ui/ui-unit ui/ui-geometry ui/ui-util \
         animation/animation animation/animation-core \
         runtime/runtime-saveable material/material-ripple; do
  n=$(basename $m); get "$M/org/jetbrains/compose/$m-desktop/$C/$n-desktop-$C.jar" "$n.jar"
done
get "$M/org/jetbrains/compose/material3/material3-desktop/$C/material3-desktop-$C.jar" material3-desktop.jar

# The runtime must match the Compose compiler plugin generation (1.9.x)
get "$G/androidx/compose/runtime/runtime-desktop/1.9.3/runtime-desktop-1.9.3.jar" runtime.jar
get "$G/androidx/collection/collection-jvm/1.5.0/collection-jvm-1.5.0.jar" collection-jvm.jar
get "$G/androidx/annotation/annotation-jvm/1.9.1/annotation-jvm-1.9.1.jar" annotation.jar

# Skiko (Skia binding) — change linux-x64 to macos-arm64 / macos-x64 / windows-x64
get "$M/org/jetbrains/skiko/skiko-awt/0.9.22.2/skiko-awt-0.9.22.2.jar" skiko-awt.jar
get "$M/org/jetbrains/skiko/skiko-awt-runtime-linux-x64/0.9.22.2/skiko-awt-runtime-linux-x64-0.9.22.2.jar" skiko-native.jar

# Coroutines, datetime, serialization
get "$M/org/jetbrains/kotlinx/kotlinx-coroutines-core-jvm/1.10.2/kotlinx-coroutines-core-jvm-1.10.2.jar" coroutines-core.jar
get "$M/org/jetbrains/kotlinx/kotlinx-coroutines-swing/1.10.2/kotlinx-coroutines-swing-1.10.2.jar" coroutines-swing.jar
get "$M/org/jetbrains/kotlinx/kotlinx-datetime-jvm/0.7.1/kotlinx-datetime-jvm-0.7.1.jar" datetime.jar
get "$M/org/jetbrains/kotlinx/kotlinx-serialization-json-jvm/1.9.0/kotlinx-serialization-json-jvm-1.9.0.jar" serialization.jar
get "$M/org/jetbrains/kotlinx/kotlinx-collections-immutable-jvm/0.4.0/kotlinx-collections-immutable-jvm-0.4.0.jar" collection.jar
get "$M/org/jetbrains/kotlinx/atomicfu-jvm/0.29.0/atomicfu-jvm-0.29.0.jar" atomicfu-jvm-0.29.0.jar
```

That's the whole toolchain — 27 jars, about 34 MB.

### Then run it

```bash
cd ariadne

./tools/run-preview.sh              # 1180x820 desktop window
./tools/run-preview.sh --phone      # 420x860, the phone layout
./tools/run-preview.sh --night      # the night paper
```

### Or just look at it

```bash
./tools/run-preview.sh --shots docs/shots   # render all 44 pages to PNG, headless
open docs/shots/contact-sheet.png           # macOS  (xdg-open on Linux, start on Windows)
# pre-2026-11-26 renders live in docs/shots/archive-2026-11-26-pre-third-review/
```

### And check the logic

```bash
./tools/test-domain.sh              # -> OK (70 tests)
./tools/store-check.sh              # -> ALL STORE CHECKS PASSED (30)
./tools/flow-check.sh               # -> ALL FLOW CHECKS PASSED (32)
./tools/entry-check.sh              # -> all four entry points compile
```

The middle two need six extra jars in `~/.cache/tools/sdl`:

```bash
mkdir -p ~/.cache/tools/sdl && cd ~/.cache/tools/sdl
M=https://repo1.maven.org/maven2/app/cash/sqldelight
for a in runtime-jvm sqlite-driver jdbc-driver; do
  curl -sSLO "$M/$a/2.4.1/$a-2.4.1.jar"; done
curl -sSLO "$M/coroutines-extensions-jvm/2.4.1/coroutines-ext.jar" 
mv coroutines-ext.jar coroutines-ext.jar 2>/dev/null
curl -sSL -o sqlite-jdbc.jar "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.50.1.0/sqlite-jdbc-3.50.1.0.jar"
```

`store-check.sh` runs SQLDelight's code generator in `tools/storecheck` — a tiny
JVM-only Gradle project, so it needs no Android SDK and only about 700 MB.

---

## 2. Gradle: the four real targets

**Requires:** JDK 21, and for Android an SDK with **compileSdk 36**.
Gradle wrapper 9.9.2 is included, so no Gradle install needed.

```bash
# Desktop — by far the fastest real target
./gradlew :app:desktop:run

# Shared domain tests
./gradlew jvmTest

# Android (needs ANDROID_HOME / local.properties with sdk.dir)
./gradlew :app:android:installDebug      # to a connected device
./gradlew :app:android:assembleDebug     # or just build the APK

# Wasm — the judge's path, a browser and nothing installed
./gradlew :app:wasm:wasmJsBrowserDevelopmentRun
```

For Android, create `local.properties` (git-ignored) with:

```properties
sdk.dir=/path/to/Android/sdk
```

**iOS** (macOS only — the module is only included on a Darwin host):

```bash
./gradlew :app:ios:linkDebugFrameworkIosSimulatorArm64
```

### One honest warning

The four-target Gradle build runs on every push in CI, so if it is broken, main is
red — check the checks on the commit before blaming your machine. It also runs in a
sandbox with 2 GB of RAM, where AGP 9's configuration phase can be OOM-killed;
`gradle.properties` already asks for `-Xmx3g`, so if you hit an OOM, raise it.

What CI has and has not executed is in the check list on the pull request, and the
history of it is in [`docs/TIMELINE.md`](docs/TIMELINE.md).

---

## 3. What to try once it's running

1. **Begin with the demo roll** → Home, with five photographs waiting.
2. **Finish 5 photographs** (the blue block) → Nightfall. Type over the amount,
   press **Confirm**. Both decisions are above the keypad, so nothing scrolls.
3. **No amount** on the next card — the record keeps things you didn't price.
4. Keep going; the last card's second button becomes **Close the day**.
5. **Timeline → a day** opens the record: the figures match what you confirmed.
6. **Prefs → Close the trip into a card.**

Keyboard, at Nightfall only: `0–9` and `.` type, `Backspace` deletes, `Enter`
confirms, `J`/`K` move through the stack, `Esc` leaves.

---

## Troubleshooting

| Symptom | Cause and fix |
| --- | --- |
| `HeadlessException` on launch | No display. Desktop needs one; use `--shots` for headless, or `xvfb-run` on a server. |
| `NoClassDefFound kotlin/jvm/internal/Intrinsics` | The stdlib isn't on the *runtime* classpath — the scripts add it; check `~/.cache/tools/kotlinc/lib/`. |
| `ClassNotFound androidx.collection.MutableObjectList` | Missing `collection-jvm.jar` — see the jar list above. |
| `NoSuchMethodError getCurrentCompositeKeyHashCode` | Compose runtime and compiler plugin are different generations. Use runtime **1.9.3** with the 1.9.0 platform jars. |
| Fonts render as a default serif | The resource dirs aren't on the runtime classpath. `run-preview.sh` adds `core/*/src/commonMain/resources` — check that your shell isn't overriding it. |
| `Permission denied: ./tools/...` | `chmod +x tools/*.sh` |
| `NoClassDefFound com/intellij/psi/...` | Harmless if it appears as a classpath warning; the compiler finds its own. If the build fails outright, re-download `kotlin-compiler-2.4.21.zip`. |
| Gradle OOM | Raise `org.gradle.jvmargs` in `gradle.properties`. |
