# Moneta 0.2.2

Android version `0.2.2`, version code `8`, package `it.bbnss.moneta`. A normal stable GitHub release for direct installation and Obtainium.

## Changes

- Refine the first screen: align the rectangular swap button and both currency selectors around the middle row; give the lower field more space. Calculator remains the default, and both adjusted and reference amounts stay visible.
- Compact historical chart values to at most two decimals, using `k` and `m` for thousands and millions. Measure the axis labels to prevent clipping. Tiny nonzero rates use scientific notation; conversion precision is unchanged.
- Copy Your currencies with a title, quotation date, verification date/time, the base amount and all selected conversions in saved order, followed by the app link.
- Replace the confusing Cash Home/Local controls with **Banknotes** on the left and **Value in** on the right, matching the table columns. Change Banknotes to change denominations; change Value in to change only the conversion. Swap exchanges the two currencies.
- Keep the counted total visible in both currencies while the banknote list scrolls. Counts belong to the banknote currency and survive changing the comparison currency, switching currencies and restarting.
- Document banknote catalogs for 32 currencies, with a source and check date available in the app. Correct missing denominations and remove the invalid DKK 1000 note. Some older or commemorative series may be omitted; denominations do not identify individual note designs.
- For other currencies, show explicitly labelled example amounts rather than claiming they are banknotes. New counting is disabled without a documented catalog. Existing saved counts are retained and included in totals; undocumented saved amounts remain removable.

The cash table and counter use reference rates without Convert fees. Italian and English remain available in the app. Repository documentation and GitHub release instructions are in English.

## Download and Obtainium

Download `Moneta-0.2.2-8.apk`. In Obtainium, use `https://github.com/bbnss/moneta` as the GitHub source. Prereleases need not be enabled. Leave APK-name and release-title filters empty: the release contains one APK.

Download `SHA256SUMS.txt` beside the APK, then run:

```sh
shasum -a 256 -c SHA256SUMS.txt
```

The signed AAB from the same commit is kept locally for Play; it is not a GitHub asset. This release does not upload to Play.

## Signing and updates

The existing Moneta keystore is unchanged. Signing certificate SHA-256:

`e674d49ab66f68eba081e7a4ffbbd4f4c6a78d7d2e8bc3add7f4517f904c2c02`

This matches the signed 0.2.x GitHub APKs and the previous local AAB prepared for Play. Installations with this certificate and a version code below 8 can update while preserving data. The certificate actually distributed by Play has not been directly compared. The old GitHub 0.1.1 test APK has a different debug signature and cannot be updated directly with this APK.

## Checks to try

1. Check Convert at your normal and enlarged text sizes: selectors, swap button, both amounts and fees should be readable. Switch currencies in both directions.
2. Open an EUR/VND history chart and check complete labels such as `30k`. Tiny reciprocal rates should stay nonzero.
3. Copy Your currencies into a notes app: check title, dates, currency codes, favourite order and the app link.
4. In Cash, choose EGP under Banknotes and EUR under Value in. The left column must list EGP denominations; the right must show their EUR values. Open the catalog information.
5. Count two EGP 100 notes. Change Value in to USD: the count and EGP 200 total must stay unchanged. Change Banknotes to EUR and back to EGP: the saved EGP count should return.
6. Scroll the count list and check that both totals stay visible. Reset or remove notes and check the result.
7. Choose a currency without a documented catalog: check the indicative warning and disabled new counting. Previous saved counts, if present, should be retained with an explicit warning.
8. Update your existing signed installation and check saved calculations, fees, favourite order, cash pair and counts.

Report the app language, screen size, currency pair, rate source and reproduction steps with any issue.
