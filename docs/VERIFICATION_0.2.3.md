# Moneta 0.2.3 verification

Package `it.bbnss.moneta`, version `0.2.3`, version code `9`.

## Automated checks

- `./gradlew test lint assembleRelease --max-workers=2` passed: 136 distinct JVM tests, no failures; lint has no errors. Existing warnings remain.
- The release uses minification and resource shrinking. Final APK and local AAB are built from the release commit; version, signatures and archive contents are checked before publication.

## Release UI checks

- Installed the signed APK over 0.2.2 without uninstalling or clearing data.
- Measured the actual clickable swap bounds: the horizontal centre is exactly 412 px on an 824 px screen (412×915 dp at density 320); target remains 64×48 dp. Tapping twice swaps the pair and returns to EUR/USD.
- Your currencies shows a flag/code/arrow button in the top amount field, with a localized accessibility description. Picking USD directly keeps the amount `1` and recalculates the displayed currencies.

- Chose USD while Convert had EUR/USD: Convert became USD/EUR, keeping its saved amount `14`. Force-stopped and reopened the app; Your currencies retained USD as base and its own amount `1`.
- Selected EGP outside favourites, then used Add a currency to select CNY: the board base remained EGP. Restored the original pair and favourites after testing.
- Inspected screenshots at 412×915 dp in English and 360×640 dp in Italian with 130% text. At the smaller size, the swap centre is exactly 360 px on a 720 px screen; its touch target remains 64×48 dp. The base selector, amount, clear/keypad actions and both fee-adjusted/reference Convert amounts remain visible.
- Restored the emulator display geometry, text scale and app language after verification.

## Signing and publication

- Existing certificate retained: `e674d49ab66f68eba081e7a4ffbbd4f4c6a78d7d2e8bc3add7f4517f904c2c02`.
- Signing passwords, private keys, common token patterns and personal paths are checked in changed files and release archives. Keystore and private build configuration remain ignored and untracked.
- Normal GitHub release `v0.2.3`, with one APK, checksum, provenance and English instructions. The AAB stays local; no Play upload or branch merge.

Earlier cash, chart, clipboard and persistence checks are recorded in [0.2.2 verification](VERIFICATION_0.2.2.md).
