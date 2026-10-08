# Moneta 0.1.0 — first test release

The first complete version for field testing.

## Features

- Works before its first network connection: a rate snapshot ships inside the APK and the UI reads the local database.
- Shows the age and source of its data; rates older than a week have a red warning.
- Eight independent providers with automatic failover, including Frankfurter, a CDN-hosted dataset, ECB, Bank of Canada, Norges Bank, InforEuro and Bank Rossii, plus a self-hosted endpoint. The responding source is shown.
- Cash table using the local currency's banknote denominations.
- One amount converted into all favourite currencies, with flags.
- Local currency detection without location permission or GPS.
- Historical charts with grid and axes.
- Configurable card/bureau markup and a calculator in the amount field.
- Currency search by country, such as “Vietnam” for VND.
- Decimal arithmetic using BigDecimal with finite precision and rounding, without binary floating-point money calculations.
- No ads, trackers, analytics or Google Play Services. 95 automated tests at this release.

## Testing

Enable airplane mode and reopen the app. Conversion must continue while clearly showing how old the rates are.

## Signing

This historical APK is signed with the Android Debug key for testing. Later release APKs use the Moneta release keystore and cannot update this debug-signed installation directly. Uninstalling deletes its data; record favourites and settings before reinstalling.
