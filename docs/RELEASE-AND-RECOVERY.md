# Release and recovery: requirements and current limits

**Status: not yet ready for a production release.** The signing workflow exists, but a
signed build and upgrade have not been verified on a device. iOS photo capture/import,
browser persistence, and archive restore are not implemented yet. Do not present the ZIP
export as a restorable backup or ask a person to uninstall their existing app to upgrade.

## Android signing and distribution

1. Generate and retain a production signing key in a secure, backed-up key management
   system. Put its base64-encoded keystore in the GitHub Actions secret
   `ARIADNE_KEYSTORE_BASE64`; configure `ARIADNE_STORE_PASSWORD`, `ARIADNE_KEY_ALIAS`
   and `ARIADNE_KEY_PASSWORD` as secrets too. Never commit the key or passwords.
2. Restrict who can create release tags/run the release workflow. Protect the release
   environment and its secrets. Build an RC, verify its signing certificate and
   non-debuggable manifest independently, and test on supported Android versions.
3. Install a second build signed by the **same production key** over the first. Confirm
   data, photos, settings, closed days and shared traveller entries survive. Test the
   existing legacy schema migration and interrupted upgrade on real devices; the CI
   emulator test covers only selected legacy fixtures, not a complete upgrade matrix.
4. Publish the signed artifact only after these gates pass. Keep the signing certificate
   fingerprint in release records and use the same key for subsequent updates (or use a
   documented, tested platform signing-key rotation).

### Existing debug APK users

A production-signed APK with the same application ID **cannot update** an installation
signed by another key, including the current published debug build. Android's uninstall
removes its private data, and the app has no restore function today. **Do not uninstall**
to make the new APK fit. Keep the existing installation until a tested migration/export
and restore path exists, or explicitly accept that its data will be lost. Exporting a ZIP
provides a readable copy, not an in-app restoration mechanism.

## Interim data copy

Use Settings → Export data and save the resulting ZIP somewhere under your control. It
contains unencrypted JSON, CSV and available photos; a cloud folder may upload it. The
archive now includes traveller figures and the reminder-offer state in JSON, but older
exports do not. Check that the ZIP opens and contains the expected records and photos.
A ZIP is a manual data copy, not a tested backup: there is no import, automatic backup,
or guarantee that all internal retry state is represented. Store an independent copy
securely, and avoid sharing it casually.

## Required restore design before release

- Define a versioned complete format including records, traveller entries, settings,
  rates and photos, with checksums; retain compatibility with previous exports.
- Import into a new staging database and private photo directory, validate schema,
  relationships, bounds, paths, duplicates and integrity before touching existing data.
  Treat all archive content as untrusted and reject zip-slip, oversized or corrupt files.
- Offer an explicit choice between restoring into an empty installation and replacing
  existing data. Never silently merge or overwrite. Make the final switch atomic or
  recoverable after interruption; preserve the old data until success is verified.
- Test export/restore round trips across supported versions and platforms, full disk,
  corrupt/truncated archives, interrupted operations, large datasets and signature
  changes. Verify photos and totals as well as row counts.
- Consider encrypted archives with a well-reviewed format and explicit key-recovery UX.
  Do not invent custom cryptography.

Browser storage can be evicted or cleared even after persistence is implemented; it is
not a substitute for an independently saved, tested backup. iOS permission strings and
the camera/library bridge belong to a real iOS host app and must be device-tested before
any capture claim is made.
