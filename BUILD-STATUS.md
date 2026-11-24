# Build status

Last updated: 2026-11-23.

## Verified — actually run, in this session

| Check | Command | Result |
| --- | --- | --- |
| Shared UI compiles | `tools/verify-ui.sh …` | **0 errors** |
| Domain logic | `tools/test-domain.sh` | **OK (70 tests)** |
| Real UI renders | `tools/run-preview.sh --shots docs/shots` | **14 frames, PNG** |
| Real UI window | `tools/run-preview.sh` | launches under Xvfb |

The compile covers `:core:model`, `:core:time`, `:core:queue`, `:core:extract`,
`:core:fx`, `:core:search`, `:core:design` (Ink palette, components, plate,
motion), `:core:store` (`InMemoryStore`, `PhotoPaths`), the four tab destinations,
the five stacked pages, the tab bar with its notch, and `AppModel`, `Route`,
`AppGraph`, `AriadneApp` — against real Compose jars with the real Compose
compiler plugin, no Gradle.

The screenshots in `docs/shots/` are produced by `ImageComposeScene` rendering
that same `AriadneApp`, so the layout, type scale and spacing on screen are the
ones the four app targets compile.

## Written, not executed

| Area | Why not | Risk |
| --- | --- | --- |
| Gradle four-target build | sandbox is 2 GB, no swap; AGP 9 configuration is OOM-killed | medium — one `./gradlew build` settles it |
| `AriadneStore` (SQLDelight) | needs codegen from Gradle | low — the UI only depends on the `Store` interface, and `InMemoryStore` implements it identically |
| Platform entry points (android/ios/desktop/wasm) | need the Gradle build | low — each is ~40 lines over the same `AriadneApp` |
| iOS target | no macOS, no Xcode | unknown |

## Design pass — Ink

The palette is the reference's (cool paper, one blue, slate) and the bones are the
second reference's (big italic serif, white panels, ink-filled controls, a bottom
bar with the shutter in a notch). Type is Instrument Serif + Inter Tight, both
vendored under OFL in `core/design/src/commonMain/resources/fonts`.

## Known next steps

1. `./gradlew build` on a machine with memory; fix whatever resolution errors appear.
2. Point `AppGraph` at `AriadneStore(driver)` per target and delete `InMemoryStore`
   from the shipping paths (keep it for previews and tests).
3. Bind OCR per target behind `TextRecognizer`: ML Kit (Android), Vision (iOS),
   Tesseract (desktop), none (Wasm — by design).
4. Wasm: replace `InMemoryStore` with the IndexedDB driver before the Pages deploy.
5. A real camera/photo picker behind `PlatformPhotos`.
