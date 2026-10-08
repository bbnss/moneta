# Moneta 0.2.0

Android version `0.2.0`, version code `6`, package `it.bbnss.moneta`. A normal GitHub release for direct installation and Obtainium.

## Changes

- Open Convert with the calculator and switch to the numeric keypad when preferred.
- Place currency swapping on the left between the two amount fields.
- Show both the fee-adjusted result and “Without fee” for the same base amount, including when editing the second field.
- Label cash results “You receive” and card results “Card cost”. Keep the separate “Fee: 5%” control for choosing the percentage and fee mode.

Also includes quotation dates per currency, coverage-aware failover, per-pair cache, shared offline/Wi-Fi/update rules, custom endpoints, saved calculations, reorderable favourites and a banknote counter. Italian and English remain available in the app.

## Download and Obtainium

Download `Moneta-0.2.0-6.apk`. In Obtainium, use `https://github.com/bbnss/moneta` as the GitHub source. This is a normal release: enabling prereleases is unnecessary. Leave APK-name and release-title filters empty to select the single APK automatically.

The AAB from the same commit is kept locally for Play and is not a GitHub asset. No Play Store upload has been performed for this release.

Download `SHA256SUMS.txt` into the same directory as the APK and check it with:

```sh
shasum -a 256 -c SHA256SUMS.txt
```

## Signing and updates

The APK uses the existing Moneta keystore. Signing certificate SHA-256:

`e674d49ab66f68eba081e7a4ffbbd4f4c6a78d7d2e8bc3add7f4517f904c2c02`

This matches the 0.2.0 release candidates, the stable 0.2.0 APK, and the previous local `Moneta-0.1.1-3.aab` prepared for Play. Installations signed with this certificate and a version code lower than 6 can be updated while preserving their data. The certificate of the APK actually distributed by Play has not been compared directly.

The old GitHub `moneta-0.1.1-test.apk` uses the Android Debug certificate and cannot be updated directly with this APK. Uninstalling that old debug build deletes its data; record your favourites and settings before reinstalling. The private key, keystore and passwords are not published. The certificate and its fingerprint are public.

## Checks to try

- Compare cash and card fees, then edit the second amount and check the inverse conversion.
- Switch between calculator and numeric keypad; reopen the app and check saved calculations.
- Check both amounts and fee controls with large text on a small screen.

When reporting a problem, include the app language, screen size, currency pair, source and reproduction steps.
