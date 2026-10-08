# Moneta 0.2.1 verification

Package `it.bbnss.moneta`, version `0.2.1`, version code `7`.

## Build and automated checks

- `./gradlew test lint assembleRelease bundleRelease --max-workers=2` passed. 123 distinct JVM tests, no failures; lint without errors. Existing dependency/deprecation/R8 asynchronous-parsing warnings remain.
- Three new board clipboard tests cover the base plus every selected conversion in displayed order, missing rates shown as “—”, and a cleared amount without favourites.
- APK release with minification and resource shrinking enabled; AAB from the same source. APK certificate verified with apksigner, AAB signature with jarsigner, and version/code with bundletool.
- Certificate SHA-256 remains `e674d49ab66f68eba081e7a4ffbbd4f4c6a78d7d2e8bc3add7f4517f904c2c02`. Updated the existing signed 0.2.0 emulator installation without uninstalling; calculations, fee settings and favourites were retained.

## Release emulator checks

- Android 16 / API 36.1; 412×915 and 360×640 dp; English/Italian; normal and 130% text.
- Rectangular swap button occupies its own row between the fields. Tap inverts the actual pair. Amount selectors and flags are uncovered; touch target is 64×48 dp.
- At 360×640 dp with 130% text, both adjusted and reference amounts remain fully visible alongside the fee control in the default compact calculator. The shorter Italian “Numerico” toggle prevents wrapping from taking space from the amounts. Numeric mode remains available.
- Read the actual release APK's clipboard using a temporary local foreground reader app. `100 EUR` exports `752.80 CNY`, `112.35 USD`, `84.74 GBP` in the displayed order. Moving the first favourite down exports USD, CNY, GBP instead.
- Switched to Italian and entered `1.000,5 EUR`: copied `1.124,06 USD`, `7.531,76 CNY`, `847,79 GBP`, with all codes and the saved favourite order.
- Restored the emulator display geometry and text scale, restored favourite order, and removed the temporary reader. Test screenshots/logs remain local in the temporary directory.

## Publication and privacy

- Changed files and all APK/AAB archive entries scanned for actual local signing passwords, private keys, common token patterns and personal paths: no findings. Keystore, keystore.properties and local.properties remain ignored and untracked.
- Normal GitHub release `v0.2.1`, APK-only downloads with checksum, build provenance and English testing instructions. The AAB is kept locally for Play; no Play upload.
- English descriptions for all existing GitHub releases and English testing attachments for the three 0.2.0 releases. Historical APKs and their checksums are preserved.

Earlier rate, cache and persistence checks are recorded in [0.2.0 verification](VERIFICATION_0.2.0.md).
