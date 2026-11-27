# Recogniser fixtures and platform testing (step by step)

This guide covers how to complete the recogniser work with **real receipt photos**, align expected text / parser assertions, and run platform recogniser tests. It is written for a setup with an **Android phone and (optionally) an Android emulator**, but **no iPhone/Mac**.

> Reality check (current repo state)
> - **Merge conflict markers:** none found by `rg` search for `^(<<<<<<<|=======|>>>>>>>)` in this workspace.
> - **Real receipt photos:** not included/producible automatically here. Files named `synthetic-receipt.png` under `recogniser-fixtures/*/` are **placeholders** and must be replaced with real, redacted receipt photos you capture/scan.
> - **Platform recogniser tests:** Android instrumented harness added; desktop JVM fixture test exists (engine/native-dependent). iOS simulator/device tests require macOS/Xcode (not possible without a Mac). Wasm/browser OCR is optional and needs a host-page `tesseract.js` hook to validate end-to-end in-browser.
> - **Latest Android device run:** `:app:android:connectedDebugAndroidTest` built and ran on the connected CPH2629 (Android 16) successfully; the recogniser fixture test was **skipped** because no image had a matching `.expected.txt` or `.meta.json`. This was not an OCR pass.

---

## 1) Capture and organise **real receipt photos** (all platforms)

### What to capture (aim for 8–10 varied real cases)
Use real receipts/notes where possible (remove/redact personal data: names, phone numbers, addresses, card numbers, emails, order IDs if sensitive):
- thermal receipt (clear)
- faded/low-contrast receipt
- handwritten chit/note
- angled/perspective photo
- total partially off-frame or near edge
- non-Latin script example (where you can test it; Android bundled ML Kit in this repo covers Latin + Devanagari/Japanese/Korean/Chinese fallback, but **not** Thai/Arabic/Cyrillic/Greek)
- different currencies/merchant styles relevant to trips (INR/SGD/PHP/THB/JPY/EUR/USD etc.)

### Where to put them
Add per-platform fixture images and expected outputs under:
- `recogniser-fixtures/android/`
- `recogniser-fixtures/ios/` (can be prepared now; runnable iOS tests need Mac)
- `recogniser-fixtures/desktop/`
- `recogniser-fixtures/wasm/` (only needed if you enable/validate browser OCR)

For each fixture use a stable base name, e.g. `bangalore-cafe-01`:
- `bangalore-cafe-01.jpg` (or `.png/.webp`) — **real photo**
- `bangalore-cafe-01.expected.txt` **or** `bangalore-cafe-01.meta.json`

**Replace placeholders:** delete or replace `synthetic-receipt.png` in each platform folder once you add real photos. Keep expected files aligned with the real image you add.

### Expected output formats
- **`<name>.expected.txt`**: expected OCR text (best-effort). Used for fuzzy/normalised text comparison (similarity target ≥ 0.6 by default in harnesses) and key survival checks (amount/merchant tokens).
- **`<name>.meta.json`** (recommended for parser assertions): machine-readable expectations, e.g.
  ```json
  {
    "expectedText": "CAFE ARIADNE\nTOTAL 45.00",
    "minTextSimilarity": 0.6,
    "expectedAmountMinor": 4500,
    "expectedCurrency": "INR",
    "expectedMerchantContains": "Ariadne",
    "mustContainTokens": ["45", "00"],
    "expectParsedReceipt": true
  }
  ```
  (Fields are optional; include what the platform/test should assert. Keep amounts in minor units consistent with `ReceiptParser`/currency.)

### Optional: README-visible samples
If you want samples shown in docs/README, copy **redacted** images to `docs/` (or reference from there) and update `README.md`. Do not commit sensitive personal data.

---

## 2) Android (you can do this) — real photos + instrumented recogniser tests

### Prerequisites
- JDK 17+ (as required by project)
- Android SDK (API 37 platform/build tools for the project’s current `compileSdk`) + **Platform-Tools** installed
- SDK location configured for Gradle: set `ANDROID_HOME`/`ANDROID_SDK_ROOT` **or** create `local.properties` in the repo root with `sdk.dir=C:\\Path\\To\\Android\\Sdk` (use escaped backslashes or forward slashes)
- **Either** a physical Android phone **or** an Android emulator
- For validating bundled ML Kit (no Play Services runtime model download), prefer a **Play-Services-less emulator image/device** when possible

### A. Add real Android fixtures (required for meaningful coverage)
1. Take clear photos of real receipts on your Android phone (or scan) and transfer them to your PC.
2. Put images in `recogniser-fixtures/android/` and add matching `.expected.txt` or `.meta.json`.
3. Mirror the same files into **instrumented test assets** so they are packaged on-device:
   - `app/android/src/androidTest/assets/recogniser-fixtures/android/`
4. Remove/replace `synthetic-receipt.png` placeholders in both locations once real fixtures exist.
   The current placeholder image and expected text have different base names, so they are intentionally not treated as a fixture. For example, add `cafe-01.jpg` and `cafe-01.expected.txt` to both locations.

### B. Run on a physical Android phone (recommended)
1. Enable **Developer options** and **USB debugging** on the phone.
2. Connect via USB and confirm your device is visible:
   ```powershell
   adb devices
   ```
   If `adb` is not recognized, install **Android SDK Platform-Tools** (Android Studio: *SDK Manager → SDK Tools → Android SDK Platform-Tools*, or download the standalone Platform-Tools zip), add the `platform-tools` folder to your `PATH`, restart the terminal, then run `adb devices` again. On the phone, accept the **“Allow USB debugging?”** prompt for this computer (and ensure the USB mode allows data/ADB).
3. Build/install and run the Android instrumented tests:
   ```powershell
   .\gradlew.bat :app:android:connectedDebugAndroidTest
   ```
   The `connectedDebugAndroidTest` task runs all instrumented tests in the app module. It does not accept Gradle’s JVM `--tests` option; the test-class project-property form was also rejected by this project’s Gradle invocation, so use the unfiltered task.
4. The fixture test exercises **image file → `MlKitTextRecognizer` → `ReceiptParser`** with fuzzy/normalised checks based on matching expected files. Synthetic images without a matching `.expected.txt` or `.meta.json` are ignored; if no matching image fixtures are packaged, the fixture test is skipped. On a phone without Google Play Services, this validates bundled ML Kit behavior as long as dependencies remain bundled-only.
5. Gradle may remove the app/test APKs after the connected test task finishes. To keep the app installed for a manual smoke test, install it separately after testing:
   ```powershell
   .\gradlew.bat :app:android:installDebug
   adb shell monkey -p com.ariadne.app 1
   ```
   The test runner package is temporary; the `installDebug` task installs the regular app APK.

### C. Run on an Android emulator (if no device / for CI-like checks)
1. Install Android Studio (SDK Manager + AVD Manager) or command-line tools.
2. Create an AVD. To check bundled behavior without Play Services, choose a system image **without Google Play** (e.g. “Google APIs”/AOSP image as available for your target; exact naming varies by SDK). Start the emulator.
3. Confirm it’s connected (requires `adb` / Platform-Tools on PATH):
   ```powershell
   adb devices
   ```
4. Run all connected instrumented tests:
   ```powershell
   .\gradlew.bat :app:android:connectedDebugAndroidTest
   ```

### D. What to verify (Android OCR behaviour)
- OCR returns `OcrResult.Text`/parsed output for readable receipts; failures/blank fields degrade gracefully (no crash).
- Amount/merchant survival matches expected fixture assertions (fuzzy/normalised).
- Large/imported photos are handled (recognizer downscales to ~1600px long edge with EXIF correction).
- Bundled recognizers work without Play Services runtime model downloads (best validated on Play-Services-less AVD/device).
- Script coverage note: current Android recognizer tries bundled Latin + Devanagari/Japanese/Korean/Chinese; **Thai/Arabic/Cyrillic/Greek** may require manual entry—capture such cases explicitly if relevant.

### E. App-level manual smoke test (phone/emulator)
```powershell
.\gradlew.bat :app:android:installDebug
adb shell monkey -p com.ariadne.app 1
```
If Ariadne disappears after `connectedDebugAndroidTest`, that is test-task cleanup rather than an app crash; run `installDebug` again after the test. Then open Ariadne, capture/import a real receipt, confirm extraction completes or fails gracefully, and that Nightfall/edit flows remain usable.

---

## 3) Desktop JVM (runnable without Android/iOS devices, engine-dependent)

### Prerequisites
- JDK 17+
- Tesseract native libraries available on the host OS for real OCR results
  - Linux: `sudo apt install tesseract-ocr libtesseract-dev libleptonica-dev`
  - Windows/macOS: install compatible Tesseract natives/runtime as needed for Tess4J
- Bundled English traineddata is included at `core/extract/src/jvmMain/resources/tessdata/eng.traineddata`; you can also set `TESSDATA_PREFIX`.

### Add real desktop fixtures
1. Put real receipt photos in `recogniser-fixtures/desktop/` with `<name>.expected.txt` or `<name>.meta.json`.
2. Replace/remove `synthetic-receipt.png`.

### Run tests
```powershell
.\gradlew.bat :core:extract:jvmTest :app:desktop:compileKotlinJvm :app:desktop:jvmTest --console=plain
```
- `:core:extract:jvmTest` covers parser/trap fixtures (no OCR engine/device).
- `:app:desktop:jvmTest` exercises fixture discovery and bytes→text→parser when a real image and usable OCR runtime are available; on hosts missing natives it may degrade gracefully (assertions should remain non-crashing).

---

## 4) iOS (requires macOS + Xcode) — cannot be completed on Windows-only setup
1. Add real receipt photos to `recogniser-fixtures/ios/` with expected text/meta now (so fixtures are ready).
2. On a Mac: build/run with Xcode (`xcodebuild` / Simulator / device) using the iOS app target and Kotlin framework integration already in the project.
3. Add/run an iOS recogniser test in the appropriate Apple test source set that performs **image bytes → `VisionTextRecognizer` → `ReceiptParser`** with fuzzy/normalised assertions (similar to Android/Desktop intent). Validate on Simulator and at least once on a real iPhone with real photos.
4. Keep Vision configured with `.accurate` and `usesLanguageCorrection = false` (receipt-safe).

---

## 5) Wasm/browser OCR (optional, only if needed for demo timing)
- Web demo can remain usable with manual/typed entry if OCR hook is not enabled.
- To enable OCR, host page must lazy-load `tesseract.js` and define `window.ariadneRecognize(imagePathOrUrl)` (pinned CDN/version recommended; note CDN fetch/privacy tradeoff). Current Kotlin hook expects a **synchronous** string/line-array result when invoked; adapt hook/host contract if you need true async Promise resolution.
- Add real fixtures in `recogniser-fixtures/wasm/` and validate in a browser harness (manual or automated) that bytes/image input → browser OCR text → `ReceiptParser` behaves with fuzzy assertions.
- Build: `.\gradlew.bat :app:wasm:wasmJsBrowserDistribution` (or dev run task as configured).

---

## 6) Suggested order (practical)
1. Add **real Android** receipt photos + expected files (phone is easiest) and run `connectedDebugAndroidTest` on phone or Play-Services-less emulator.
2. Add **real desktop** fixtures and run desktop JVM tests on a machine with Tesseract natives.
3. Prepare **iOS/Wasm** fixture images/expected files now; run iOS on Mac when available; enable/validate Wasm OCR only if needed.
4. Update `README.md` and `TESTING.md` with exact commands run, devices/emulators used, results, and any known script/OCR gaps.

## 7) Assertion/alignment checklist per fixture
- [ ] Real photo (redacted), not synthetic placeholder
- [ ] `.expected.txt` or `.meta.json` present and matches the same image
- [ ] OCR text fuzzy/normalised similarity acceptable (≥ ~0.6 unless justified)
- [ ] Key amount and merchant survive into OCR text and/or parsed result
- [ ] Currency/amount-minor expectations (if specified) match `ReceiptParser`
- [ ] Failure/empty cases are intentional and asserted (graceful, non-crashing)
