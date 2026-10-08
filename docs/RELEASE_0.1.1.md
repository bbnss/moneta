# Moneta 0.1.1 — a refresh without a network is not a refresh

Fixes from testing 0.1.0 on a phone.

## Changes

- Refreshing offline no longer reports success or stamps a new verification time onto old rates. Requests require revalidation; an HTTP response that never reached the network is treated as a network failure.
- The fee line no longer pushes the last keypad row off screen. Zero, decimal separator and equals remain accessible. The keypad is outside the scrolling area; shorter screens use 48 dp keys instead of 56 dp.
- “Max” uses the full twenty-year period rather than a previously viewed shorter interval. History cache must cover both requested endpoints.
- Period filters wrap instead of squeezing the final label into a narrow column.
- Charts show their source and the period actually covered, which may differ from the requested range.
- Your currencies has a collapsible three-column numeric keypad with one fewer row.
- New icon based on the denarius of Juno Moneta.

## Testing

Download `moneta-0.1.1-test.apk`. It can update the 0.1.0 debug build directly.

Enable airplane mode, open the app and try refreshing. It must report that no source was reached and leave the previous verification time unchanged.

## Signing

This historical APK uses the Android Debug key, like the previous test build. Later Moneta release APKs use the release keystore and cannot update these debug-signed installations. Uninstalling a debug build deletes its data; record favourites and settings before reinstalling.
