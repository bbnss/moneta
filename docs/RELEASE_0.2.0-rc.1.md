# Moneta 0.2.0 — signed test build rc.1

Android version `0.2.0`, version code `4`, package `it.bbnss.moneta`. Release shrinking is enabled and the APK is signed with the existing Moneta keystore. Only the test APK is published on GitHub. The AAB from the same commit is kept locally for a later Play upload after approval. The keystore and passwords are excluded from the repository and release assets.

This historical build has a favourite-dragging bug fixed in rc.2. Prefer the latest stable release for installation.

## Changes

- Keep quotation dates per currency and use the correct conversion date; distinguish “Rate from” from “Verified”.
- Check requested currency coverage during failover, keep per-pair cache, and use one source per conversion.
- Apply offline, Wi-Fi and interval rules through one policy; manual-only mode disables automatic updates.
- Compact Convert, numeric keypad with optional calculator, copy/paste and calculation restore.
- Cash exchange and card payment fees are included in the result, including when editing the second field.
- Save favourite order. Cash uses an independent pair and a banknote counter with counts per local currency and totals in both currencies.
- Custom HTTPS endpoint: edit, Save, Verify, CUSTOM selection and fallback.

## Installation and signing

Download the APK and `SHA256SUMS.txt`. Run `shasum -a 256 -c SHA256SUMS.txt` in their directory, allow installation from the downloading app, and open the APK.

Signing certificate SHA-256:

`e674d49ab66f68eba081e7a4ffbbd4f4c6a78d7d2e8bc3add7f4517f904c2c02`

The old GitHub `moneta-0.1.1-test.apk` is signed with Android Debug, SHA-256:

`50ebbcabee5e06a2de08935519043f4a25a35fcf8cc0a4747ce89e2f6e11bf30`

These certificates are incompatible: this APK cannot update that old debug build. Uninstalling the debug build deletes cache, favourites and settings; record what you want to restore first. Room migration preserves data when signature compatibility permits an update.

This APK's certificate matches the previous local `Moneta-0.1.1-3.aab` prepared for Play. The upload key and Play app signing key can be the same. No keystore was changed and no Play upload was performed for this release. A Play installation using this same certificate can be updated when its version code is lower. The actual Play-distributed APK certificate was not compared here; the earlier AAB establishes bundle signing continuity.

## Suggested checks

1. Open offline: the initial amount is 1 and conversion uses bundled data; the first digit replaces 1. Check the stated rate and date.
2. Try VND/EUR with ECB selected and failover on/off. Missing coverage must keep usable cache and show a warning.
3. Try manual-only, offline and Wi-Fi-only modes for refresh, history and endpoint verification. A blocked request must not change “Verified”.
4. Set a fee: cash reduces the result, card increases it. Edit the second amount and check the inverse.
5. Enter an expression and force-stop the app. Reopening restores the amount, active field and calculator. Clear and repeat: zero must remain saved.
6. Copy/paste Italian and English numbers. Invalid text must leave the calculation unchanged and show a message. Check long amounts and horizontal scrolling.
7. Reorder currencies with a long-press drag or up/down controls; reopen and check the saved order.
8. In Cash, select Home and Local, count notes using minus/plus, check both totals and reset. Changing Home must preserve local counts. Changing Local and returning must restore its counts.
9. Save a Frankfurter v2 HTTPS server, including an optional deployment path, and tap Verify. Check coverage and errors. Verification must not change the source. Changing the URL must invalidate the old CUSTOM cache.
10. Check light, dark and black themes, Italian/English and larger text. In standard numeric mode, amounts and fee must be visible without scrolling.

Report issues with app language, screen size, source, currency pair and reproduction steps. Store screenshots and the Play upload follow positive user feedback.
