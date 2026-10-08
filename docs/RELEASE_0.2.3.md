# Moneta 0.2.3

Android version `0.2.3`, version code `9`, package `it.bbnss.moneta`. A normal stable GitHub release for direct installation and Obtainium.

## Changes

- Centre the rectangular swap button horizontally on the screen, keeping its existing spacing between the two currency fields and its 64×48 dp touch target.
- Change the base currency directly in Your currencies: tap the flag, currency code and arrow in the top amount field to open the currency picker. Adding favourite currencies remains a separate action.
- Save the selected base currency and share it with Convert, preserving entered amounts and existing favourites. If the chosen currency is already the quote currency in Convert, move the previous base to the quote field to keep a useful pair. The previous base remains among favourites, following the existing promotion behaviour.

Includes all the Cash, chart, clipboard, fee and offline improvements from 0.2.2. Italian and English remain available in the app; GitHub release notes and instructions are in English.

## Download and Obtainium

Download `Moneta-0.2.3-9.apk`. In Obtainium, use `https://github.com/bbnss/moneta` as the GitHub source. Prereleases need not be enabled. Leave APK-name and release-title filters empty: this release has one APK.

Download `SHA256SUMS.txt` beside the APK and run:

```sh
shasum -a 256 -c SHA256SUMS.txt
```

The signed AAB from the same commit stays local for Play; it is not a GitHub asset. No Play upload is performed.

## Signing and updates

The existing Moneta keystore and certificate are unchanged. Certificate SHA-256:

`e674d49ab66f68eba081e7a4ffbbd4f4c6a78d7d2e8bc3add7f4517f904c2c02`

Signed 0.2.x GitHub installations with a version code below 9 can update while preserving data. The actual certificate distributed by Play has not been compared directly. The old 0.1.1 GitHub debug APK has a different signature.

## Checks to try

1. In Convert, check the swap is centred on the screen between the fields. Tap it twice and verify the currencies swap and return.
2. In Your currencies, tap the currency in the top amount field and choose USD. Check the list recalculates with USD as base while retaining the entered amount.
3. Open Convert: its first currency should match the chosen base, with a different second currency. Restart and check the selection remains saved.
4. Choose a base outside your favourites, then use Add a currency to add a favourite. The two controls must perform their separate actions.
5. Check both screens with enlarged text. The selector, swap button, fee-adjusted/reference amounts and keypad controls should remain readable.

Report language, currency pair, screen size and reproduction steps with any issue.
