# Moneta 0.2.1

Android version `0.2.1`, version code `7`, package `it.bbnss.moneta`. A normal GitHub release for direct installation and Obtainium.

## Changes

- Give the rectangular swap button its own row on the left between the currency fields. It no longer overlaps the flags or selectors.
- “Copy” on Your currencies exports the base amount followed by every selected conversion, with currency codes, in saved order. Unavailable conversions are shown as “—”.
- Use English for GitHub release descriptions and APK instructions, including older releases.
- Keep the calculator as the default and show both fee-adjusted and reference amounts.

Also includes quotation dates per currency, coverage-aware failover, per-pair cache, shared offline/Wi-Fi/update rules, custom endpoints, saved calculations, reorderable favourites and a banknote counter. Italian and English remain available in the app.

## Download and Obtainium

Download `Moneta-0.2.1-7.apk`. In Obtainium, use `https://github.com/bbnss/moneta` as the GitHub source. This is a normal release: enabling prereleases is unnecessary. Leave APK-name and release-title filters empty to select the single APK automatically.

The AAB from the same commit is kept locally for Play and is not a GitHub asset. No Play Store upload has been performed for this release.

Download `SHA256SUMS.txt` into the same directory as the APK and check it with:

```sh
shasum -a 256 -c SHA256SUMS.txt
```

## Signing and updates

The APK uses the existing Moneta keystore. Signing certificate SHA-256:

`e674d49ab66f68eba081e7a4ffbbd4f4c6a78d7d2e8bc3add7f4517f904c2c02`

This matches the 0.2.0 release candidates, the stable 0.2.0 APK, and the previous local `Moneta-0.1.1-3.aab` prepared for Play. Installations signed with this certificate and a version code lower than 7 can be updated while preserving their data. The certificate of the APK actually distributed by Play has not been compared directly.

The old GitHub `moneta-0.1.1-test.apk` uses the Android Debug certificate and cannot be updated directly with this APK. Uninstalling that old debug build deletes its data; record your favourites and settings before reinstalling. The private key, keystore and passwords are not published. The certificate and its fingerprint are public.

## Checks to try

1. In Convert, check that the swap button sits between the two fields without covering either flag. Tap it and confirm the pair is inverted.
2. Set a cash or card fee and check both the adjusted amount and “Without fee”. Check with larger text as well.
3. In Your currencies, enter an amount, select several favourites, tap Copy and paste into a notes app. The first line should be the base amount (for example, `100 EUR`), followed by all selected conversions with their currency codes.
4. Reorder favourites and copy again: the exported lines should follow the displayed order. Missing rates should remain “—”, never a made-up value.
5. Update the existing signed 0.2.0 installation and check that settings, calculations and favourite order are retained.

When reporting a problem, include the app language, screen size, currency pair, source and reproduction steps.
