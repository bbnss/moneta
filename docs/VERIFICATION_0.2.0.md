# Moneta 0.2.0 verification

## Final build — version code 6

- `./gradlew test lint assembleRelease bundleRelease --max-workers=2` passed. 120 distinct JVM tests, no failures, lint without errors; existing warnings remain as described below.
- Added fee-comparison tests: base 100 and rate 1.2 give reference 120, cash at 5% gives 114, card at 5% gives 126. Comparison uses the same base when editing the second field.
- Installed the shrunk release APK before publication. Checked Italian and English, 360×640 and 412×915 dp, and 130% text. Fee-adjusted and reference amounts and percentage control are visible together. Compact calculator retains all operators in four rows with touch targets of at least 48 dp.
- Checked calculator/numeric switching, swapping on the left between fields, cash/card fees and inverse conversion in the actual release. Compact expression `2×(3+4)=14` survives force-stop. Fee dialog scrolls and Save remains accessible at 360×640 dp with 130% text. Restored emulator display geometry and font scale.
- Verified APK certificate with apksigner, AAB signature with jarsigner, and AAB certificate/version with keytool and bundletool. Existing Moneta certificate, package `it.bbnss.moneta`, version `0.2.0`, code `6`.
- Scanned changed files and APK/AAB contents: no actual local signing passwords, private keys, tokens or personal paths found. Local keystore/properties are ignored by Git. Test logs and screenshots remain temporary and are not release assets.
- Published normal GitHub release `v0.2.0`, public APK with checksum and provenance. AAB kept locally; no Play upload.

## Earlier release-candidate checks

### Automated verification

- `./gradlew test lint assembleRelease bundleRelease` passed model, parser and repository JVM tests, including both Android variants; lint without errors.
- 119 distinct JVM tests (60 model, 50 providers, 9 repository/persistence) and 3 instrumented tests on Android 16 / API 36.1.
- ALL/USD fixture with different dates, oldest necessary date for cross-rates, pivot without an artificial date, and unknown missing dates.
- Old rates just verified, freshness boundaries at 0/1–3/4–7/over 7 days, and clock advancement without database mutations.
- VND/EUR with incomplete ECB coverage, fallback to a compatible provider, disabled failover, and preserved cache.
- Offline/Wi-Fi blocking for opening, worker, manual updates, history and endpoint verification: no calls or verification timestamp changes for blocked requests. Automatic interval and manual-only mode.
- Custom endpoint deployment path, verification independent of source/cache, and invalidation of CUSTOM snapshots/history on URL change.
- Direct/inverse cash and card formulas, decimal-denomination totals, Italian/English parsing and invalid text.
- Alphabetical migration of existing favourites, persisted order, and movement skipping the hidden base.
- Independent cash pair and counts per local currency.
- Room migration from a real schema-1 database containing cache/history, validated against schema 2. Decimal values and timestamps preserved; new dates NULL.
- Real DataStore close/reopen: expression, active field, explicit clear, favourite order, fee mode and banknote counts recovered.
- Offline seed with at least 50 currencies and dates for every quotation; no float conversions in the seed script.

### Release emulator checks

Installed the release APK with minification and resource shrinking. Checked Italian/English, 360×640 and 412×915 dp, 130% text, numeric/calculator modes, expression restore after force-stop, themes, offline warning, reorder and banknote counting. In standard numeric mode, both amounts and fee remain visible without scrolling, including 360×640 dp with 130% text. Temporary screenshots do not replace store materials, which follow positive user feedback.

Drag regression: `python3 scripts/check_reorder.py` on a test emulator with at least three visible favourites. The gesture changes order, preserves currencies/base, and persists across force-stop. rc.2 removes the conflict between the reorder gesture and the normal row long-press.

### Build and signing

Release-candidate package `it.bbnss.moneta`, version `0.2.0`, code `5`. APK and AAB signed with the local Moneta key. Certificates compared using apksigner/keytool; AAB verified with jarsigner. Passwords are not published. Commit provenance and checksums are prerelease assets.

R8 is pinned to 9.1.29, compatible with Kotlin 2.4, through its official repository. Earlier Kotlin metadata warnings are resolved. Existing warnings concern asynchronous R8 parsing unsupported by the AGP provider and lint dependency versions; these are not build errors. [Kotlin/R8 compatibility](https://developer.android.com/build/kotlin-support), [R8 repository](https://r8.googlesource.com/r8/+/refs/heads/main/README.md).

The old GitHub 0.1.1 APK uses Android Debug signing and differs from the Moneta key. Compatibility with an actual Play installation was not verified. See [release-candidate instructions](RELEASE_0.2.0-rc.2.md).
