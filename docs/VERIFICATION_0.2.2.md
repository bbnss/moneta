# Moneta 0.2.2 verification

Package `it.bbnss.moneta`, version `0.2.2`, version code `8`.

## Automated checks

- `./gradlew test lint assembleRelease --max-workers=2` passed: 136 distinct JVM tests, no failures; lint without errors. Existing dependency/deprecation/R8 warnings remain.
- `./gradlew :core:data:connectedDebugAndroidTest --max-workers=2` passed all four tests on Android 16 / API 36.1: Room migration preserving cache/history and unknown dates, actual DataStore restart, seed quotation dates and cash-count preservation/decimal-key normalization.
- New cash tests cover exact fractional totals, merging differently scaled denominations, preserving withdrawn/undocumented saved amounts, removal and preventing examples from becoming countable banknotes.
- Chart tests cover thousands, millions, rounded unit boundaries, locale separators and tiny reciprocal rates. Board clipboard tests cover dates, time zone, missing values, cleared input and favourite ordering.
- Release uses minification and resource shrinking. Build/signature provenance is attached to the GitHub release.

## Catalog and persistence

- Primary-source catalog coverage is documented for 32 currencies in [Banknote catalog](BANKNOTE_CATALOG.md), with issuer/source and check date in the app. Scope is denominations, not identification of every older or commemorative design.
- Unverified currencies explicitly show examples and cannot create new counts. Positive saved counts outside the documented catalog remain visible, removable and included in totals.
- Existing cash pair keys and per-currency count keys are preserved. Counts do not depend on the comparison currency. Decimal denominations are normalized before adjusting old counts.

## Release Cash checks

- On the signed release at 412×915 dp, Banknotes EGP / Value in EUR shows EGP denominations (including fractional notes) on the left and EUR values on the right. Catalog dialog shows the central-bank source and check date.
- Counted two EGP 100 notes: total EGP 200.00 / EUR 3.40 using the cached reference rate. Changing Value in to USD retained both notes and EGP 200.00, changing only the conversion to USD 3.82.
- Switched Banknotes to EUR and saw the existing two EUR 5 notes and EUR 10.00 total, then returned to EGP. Force-stopped and reopened the app: EGP counts and pair were restored.
- Verified Cash swap changes both actual selectors. ARS, outside the documented catalog, displays an indicative warning and disables new counting.
- Inspected release screenshots in English/light theme and Italian/AMOLED. Restored the original theme and physical emulator display geometry afterwards.
- At 360×640 dp with 130% Italian text, the first banknote and both totals are visible immediately. Scrolling changes the visible notes while leaving the totals fixed. Count view keeps rate/date visible and moves explanatory/catalog details to Table. Convert still shows both amounts and the fee control without scrolling.
- While scrolling the list, the total stays outside the list and remains visible in both currencies. Temporary EGP test additions were removed afterwards; the prior EUR count was preserved.

## Earlier visual checks included in this release

The [initial UI review](UI_REVIEW_2026-10-08.md) records screenshot and clipboard checks for the conversion layout, EUR/VND chart and dated clipboard text before the Cash redesign. It includes 412×915 and 360×640 dp, Italian/English and 130% text. Million-label formatting was checked by unit tests rather than an actual million-valued chart.

## Signing and publication

- The existing keystore and package are retained. APK certificate SHA-256: `e674d49ab66f68eba081e7a4ffbbd4f4c6a78d7d2e8bc3add7f4517f904c2c02`.
- Installed the signed release over version code 7 on the emulator without uninstalling or clearing data.
- Changed files and release archive contents scanned for actual local signing passwords, private keys, common token patterns and personal filesystem paths. Private build configuration remains ignored and untracked.
- Normal GitHub release `v0.2.2`: one signed APK plus checksums, provenance and English testing instructions. AAB retained locally; no Play upload, merge or pull request.
