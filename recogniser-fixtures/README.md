# Recogniser fixtures

Real receipt images with expected OCR text, organised per platform. These fixtures are intended to validate recogniser bindings and platform-specific test runs (not just the shared parser).

## Structure
- `android/`, `ios/`, `desktop/`, `wasm/`: platform-specific receipt images and expected outputs.
- Each fixture should include:
  - `<name>.jpg|png|webp`: receipt photo/image used by the recogniser
  - `<name>.expected.txt`: expected normalized text lines (one line per OCR line) or key extracted fields documented at top
  - `<name>.meta.json` (optional): locale/currency hints and expected parser outcome

## Notes
- Add real, device-captured or scanned receipt photos where possible. Do not commit sensitive personal data.
- Platform tests should skip with a clear message if fixtures or required OCR runtime assets (e.g. Tesseract traineddata, browser OCR bundle) are unavailable.
- Shared parser correctness remains covered by `core/extract` fixtures; recogniser fixtures prove bytes?text?parser integration per target.