# Recogniser fixtures

Real receipt images with expected OCR text, organised per platform. These fixtures validate recogniser bindings and platform-specific **bytes → text → parser** flows (complementing shared parser fixtures in `core/extract`).

## Structure
- `android/`, `ios/`, `desktop/`, `wasm/`: platform-specific receipt images and expected outputs.
- Each fixture should include:
  - `<name>.jpg|png|webp`: **real receipt photo/image** used by the recogniser (replace/remove `synthetic-receipt.png` placeholders)
  - `<name>.expected.txt`: expected OCR text for fuzzy/normalised comparison (key amount/merchant tokens should survive), or
  - `<name>.meta.json` (recommended): expected text and/or parser assertions (`expectedAmountMinor`, `expectedCurrency`, `expectedMerchant`/`expectedMerchantContains`, `minTextSimilarity`, `mustContainTokens`, `expectParsedReceipt`, etc.)

## Notes
- Add real, device-captured or scanned receipt photos where possible. **Do not commit sensitive personal data** (redact names, card numbers, phones, addresses, IDs).
- `synthetic-receipt.png` files currently present are placeholders, not real receipts; replace them with real photos per platform.
- Android instrumented tests also need fixtures mirrored into `app/android/src/androidTest/assets/recogniser-fixtures/android/` so they are packaged on-device.
- Platform tests should skip with a clear message or degrade gracefully when fixtures or required OCR runtime assets (Tesseract natives/traineddata, browser OCR bundle, Apple frameworks) are unavailable.
- See [docs/RECOGNISER-TESTING.md](../docs/RECOGNISER-TESTING.md) for step-by-step testing instructions (Android phone/emulator without Mac/iPhone).