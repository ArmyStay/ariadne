# SDK setup

Everything a machine needs to build Ariadne, per target, with the versions the build
actually asks for. `gradle/libs.versions.toml` is the source of truth; when this file
and that file disagree, that file wins and this one is a bug.

Nothing here is installed by the repository. There is no setup script on purpose: every
line below is one command you can read before running it.

---

## 1. The one requirement for all five targets

| | Version | Why |
| --- | --- | --- |
| **JDK** | 21 | what CI uses (`actions/setup-java`, temurin 21). Bytecode is compiled to **17** for every module, so a JDK 17 also builds. |
| **Gradle** | none | the wrapper is in the repository (9.9.2). `./gradlew` is enough. |

```bash
java -version    # 21.x is what CI proves
./gradlew --version
```

If several JDKs are installed, point Gradle at one rather than changing your shell:

```properties
# gradle.properties, or ~/.gradle/gradle.properties
org.gradle.java.home=/path/to/jdk-21
```

`gradle.properties` already sets `org.gradle.jvmargs=-Xmx3g`. If AGP's configuration
phase is killed on a small machine, raise it — that is the one setting that matters.

---

## 2. Android

| | Value | Where |
| --- | --- | --- |
| `compileSdk` | **37** | `libs.versions.android.compileSdk` |
| `targetSdk` | **36** | `libs.versions.android.targetSdk` |
| `minSdk` | **29** | `libs.versions.android.minSdk` — moved up from 26 for the glasses SDK; that is the whole reason |
| build-tools | whatever AGP 9.5.3 resolves | not pinned in the repository |
| Kotlin | 2.4.21, `android.builtInKotlin=false` | AGP 9's built-in Kotlin is switched off, so the Kotlin plugin owns compilation |

Install **Android SDK Platform 37** and the platform tools through Android Studio's SDK
Manager, or:

```bash
sdkmanager "platforms;android-37" "platform-tools"
```

Then either export `ANDROID_HOME`, or create `local.properties` at the repository root
(**git-ignored — never commit it**):

```properties
sdk.dir=/path/to/Android/sdk
# Windows, escaping the separators:
# sdk.dir=C\:\\Users\\you\\AppData\\Local\\Android\\Sdk
```

That file is the whole of "SDK setup" for most people. If Gradle says *SDK location not
found*, it is this file or `ANDROID_HOME`, nothing else.

```bash
./gradlew :app:android:assembleDebug
./gradlew :app:android:installDebug        # to a connected device
```

### The glasses SDK

The Meta Wearables Device Access Toolkit is resolved from Maven Central and needs no
manual download:

| Artifact | Version | Module |
| --- | --- | --- |
| `com.meta.wearable:mwdat-core` | 1.1.0 | `:app:android` |
| `com.meta.wearable:mwdat-camera` | 1.1.0 | `:app:android` |
| `com.meta.wearable:mwdat-mockdevice` | 1.1.0 | `:app:android`, **debug only** |

Pairing needs an app identity, and the build ships the documented developer-mode pair:
`mwdatApplicationId = 0`, `mwdatClientToken = 0` as manifest placeholders. With both at
`0` the glasses only accept the app while **Developer Mode** is on in the Meta AI app.
A release build replaces both from the Wearables Developer Center.

The mock device is how the path is exercised without a pair:

```bash
adb shell am start -n com.ariadne.app/com.ariadne.android.MainActivity --ez mock_glasses true
```

### Permission and privacy constraints the build encodes

The app manifest declares these permissions (SDK manifests may contribute others):

| Permission | Why |
| --- | --- |
| `CAMERA` | the shutter. `uses-feature android.hardware.camera` is `required="false"`, so the app installs on a device without one and falls back to the plate |
| `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` | the glasses: the Wearables Device Access Toolkit talks to the phone over BLE; scanning declares `neverForLocation`, not location use |
| `INTERNET` | explicitly declared for exchange-rate requests; this is not proof that the wearables SDK makes no network requests |
| `POST_NOTIFICATIONS` | requested only when enabling the evening reminder |

There is **no** storage permission: the photo picker is `PickMultipleVisualMedia`, which
grants exactly the images a person chooses. Receipt photographs are written to
app-private storage, scaled to 1600px on the long edge and re-encoded at 88 before they
are kept. The manifest also carries a `<queries>` entry for
`android.speech.RecognitionService`, which is how Android 11+ lets the microphone find a
recogniser app — a query, not a permission. Voice uses an external recognition activity,
with `EXTRA_PREFER_OFFLINE` as a best-effort hint. Providers can ignore it and upload
audio; the app warns before launch and offers typing instead.

---

## 3. Desktop (JVM)

| | Requirement |
| --- | --- |
| JDK | 17+ (21 in CI) |
| OCR | Tess4J, which needs the native Tesseract libraries |

```bash
./gradlew :app:desktop:run
```

On Debian/Ubuntu (this is what CI installs):

```bash
sudo apt-get install -y tesseract-ocr libtesseract-dev libleptonica-dev
```

On macOS: `brew install tesseract`. On Windows the DLLs come with the Tess4J
distribution — `RUN-WINDOWS.md` covers the classpath if Gradle cannot find them.

The database lives at `~/.ariadne/ariadne.db`. Photos live beside it.

---

## 4. Browser (Wasm)

| | Requirement |
| --- | --- |
| JDK | 21 |
| Node | **none to install** — the Kotlin plugin downloads its own Node the first time |
| Browser | a current Chrome, Firefox or Safari (Wasm GC) |

```bash
./gradlew :app:wasm:wasmJsBrowserDevelopmentRun      # dev server
./gradlew :app:wasm:wasmJsBrowserDistribution        # what the demo deploys
```

The demo at [armystay.github.io/ariadne](https://armystay.github.io/ariadne/) is the
`productionExecutable` directory from that second command, uploaded as a Pages artifact
by `.github/workflows/pages.yml` — with three guards, because this is a *project* page
served from `/ariadne/` and it was once published green while the root URL answered 404.
The page itself is **committed**, not generated: `index.html` lives at
`app/wasm/src/wasmJsMain/resources/index.html` and the distribution task copies it beside
`ariadne.js`. Kotlin 2.4 writes no page of its own, so the artifact this workflow used to
upload held a bundle and no way to open it.

1. `tools/demo-check.sh` **finds** the page wherever `build/dist/wasmJs` put it instead of
   assuming the directory name, and copies its folder into a stage directory whose root
   is the site;
2. the same script **serves that stage under `/ariadne/`** (a static server, `python3`,
   no JDK) and asserts every `src`/`href` in `index.html` resolves — an absolute
   `/asset` path works on a laptop and 404s here. `ci.yml` runs it on every pull
   request too, so a demo that would 404 is caught before it is merged;
3. after deployment a `verify` job performs an **unauthenticated `curl` of the public
   URL**, requiring HTTP 200 and `ariadne.js` in the HTML, so a README render or an
   empty artifact fails the run instead of shipping.

One more thing had to go for the link to work: the starter `static.yml` that the Pages
UI writes when a repository is switched to "GitHub Actions" uploaded `path: '.'` — the
whole repository, which has no `index.html` — and shared the `pages` concurrency group
with `pages.yml`. The two raced on every push, and the starter one served a 404 with a
green tick. It is deleted; `pages.yml` is the only writer to Pages.

What the browser build does *not* have: a camera, OCR, persistent storage, or rate
fetching. It has the shutter's plate, the file chooser (**Library**), manual entry, and
the whole shared UI.

---

## 5. iOS (macOS only)

| | Requirement |
| --- | --- |
| Xcode | a current Xcode with the iOS SDK |
| Xcode CLT | selected: `sudo xcode-select -s /Applications/Xcode.app` |
| JDK | 21 |

`:app:ios` is only included in the build when the host is a Mac (`settings.gradle.kts`
checks `os.name`), so Linux and Windows CI legs never see it.

```bash
./gradlew :app:ios:linkDebugFrameworkIosSimulatorArm64
./gradlew :app:ios:compileKotlinIosSimulatorArm64 :app:ios:compileKotlinIosArm64
```

The second pair of tasks is what CI runs — the only place iOS compiler errors can be
read without a Mac in front of you. Compiling is not running: this target has no camera
and no photo picker.

---

## 6. CI, as the reference setup

`.github/workflows/ci.yml` is a working, version-pinned install of everything above:

| Job | Runner | What it proves |
| --- | --- | --- |
| `shared tests` | ubuntu, JDK 21 | `jvmTest`, `:app:android:assembleDebug`, `:app:wasm:compileKotlinWasmJs`, `:app:wasm:wasmJsBrowserDistribution` |
| `web demo` | ubuntu, JDK 21 | builds the wasm distribution and publishes it to Pages as an artifact |
| `desktop` | ubuntu, JDK 21, tesseract | `:app:desktop:compileKotlinJvm` |
| `ios` | macos, JDK 21 | `:app:ios:compileKotlinIosSimulatorArm64`, `:app:ios:compileKotlinIosArm64` |

If your machine fails where CI passes, diff your setup against those steps — they are
the shortest correct answer to "what does this need?".

---

## 7. When it does not work

| Symptom | Cause | Fix |
| --- | --- | --- |
| `SDK location not found` | no `local.properties` / `ANDROID_HOME` | §2 |
| `Unsupported class file major version` | JDK older than the tooling | JDK 21 (§1) |
| AGP configuration phase killed | low memory | raise `org.gradle.jvmargs` |
| `Task 'linkDebugFrameworkIosSimulatorArm64' not found` | not on a Mac, or `:app:ios` excluded | §5 — it is a macOS-only module by design |
| Tess4J `UnsatisfiedLinkError` | native Tesseract missing | §3 |
| Wasm build hangs on first run | it is downloading Node | wait; it happens once |
