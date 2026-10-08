# Moneta

**A currency converter for travellers. Works offline, never hides how old the rates are, and survives any single data source going down.**

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

Moneta answers "how much is 45,000 dong in euros?" in two seconds — on a plane, in a market with no signal, or in a country where half the internet is unreachable.

## Why another converter?

Every currency app fails on at least one of these:

- **It needs a connection.** Moneta ships with a rate snapshot inside the APK, so it can convert using dated reference rates the very first time you open it, even with the network off. From then on it keeps a local copy of everything.
- **It shows stale rates without telling you.** Moneta always displays how old the rates are and which source they came from. Past a week, it says so in red.
- **It depends on one endpoint.** Moneta talks to several independent providers and falls back automatically when one is unreachable — including a CDN-hosted source that stays reachable where direct APIs are blocked, and your own self-hosted endpoint if you want full control.

## Features

- Offline-first: the local database is the only source of truth, the UI never waits on the network
- Explicit freshness indicator with four states, always visible
- Multiple free providers, no API keys, automatic failover, manual override
- Multi-currency board: one amount, favourites in your saved order, with drag and accessible move controls; copy the base amount and every selected conversion together
- Cash table and banknote counter: independent Home/Local pair, counts saved per local currency, totals in both currencies
- Automatic local-currency detection with no location permission
- Historical charts; cash exchange fees reduce what you receive, card fees increase the cost; calculator by default with an optional numeric keypad; amounts shown with and without fees
- Decimal arithmetic: BigDecimal with 16 significant digits and HALF_UP for intermediate operations, currency-aware display rounding
- No ads, no trackers, no analytics, no Google Play Services. `INTERNET` is the only meaningful permission.

## Rates and updates in 0.2.0

Each quotation keeps its own date. A cross-rate uses the oldest date among the quotations it needs; the pivot adds no artificial date. “Rate from” and “Verified” are separate. Today is green, 1–3 days neutral, 4–7 days amber, and more than 7 days red. Old cache entries whose dates were lost by 0.1.x remain unknown until refreshed. Age updates every minute while visible and on foreground return.

Updates require coverage of the requested currencies. Incomplete responses continue through failover and never replace usable cache. Every conversion uses a complete snapshot from one provider. Offline blocks requests; Wi-Fi-only also applies to manual updates, history and endpoint verification. Manual-only disables automatic updates; the selected interval also governs opening and foreground checks.

A custom endpoint accepts a complete HTTPS server URL with an optional deployment path. Moneta appends `/v2/rates`; Save and Verify are separate actions. Verification reports coverage without selecting a source or writing rate cache. Changing the address invalidates the previous CUSTOM cache.

Amounts start at 1 and the first digit replaces that initial value. Amount, expression, active field, explicit clear and favourite order persist across process restarts. Paste accepts numbers in the active app language and rejects malformed text without changing the calculation.

## Data sources

All free, all key-less. See [`docs/SPEC.md`](docs/SPEC.md) for the full list and endpoint contracts.

| Provider | Currencies | Notes |
|---|---|---|
| [Frankfurter](https://frankfurter.dev/) v2 | varies | Default. Aggregates official sources, supports history and self-hosting |
| [fawazahmed0/exchange-api](https://github.com/fawazahmed0/exchange-api) | 338 | CDN-hosted, includes crypto and precious metals |
| [ExchangeRate-API open](https://www.exchangerate-api.com/docs/free) | ~160 | Independent infrastructure |
| European Central Bank | 29 | Static XML, official, survives API outages |
| Bank of Canada, Norges Bank, InforEuro, Bank Rossii | varies | Regional official sources |

Rates are reference rates. They are not what a bank or a bureau will actually give you — that is what the configurable markup is for.

## Install with Obtainium

Use `https://github.com/bbnss/moneta` as the GitHub source. Version 0.2.1 is a normal release and does not require enabling prereleases. Leave APK and release-title filters empty to select the single APK automatically. The AAB is kept locally for Play; it is not a GitHub download.

## Build

Requires JDK 21 and the Android SDK (compileSdk 36).

```sh
./gradlew assembleDebug     # build
./gradlew test              # unit tests
./gradlew lint              # static analysis
./gradlew :core:data:connectedDebugAndroidTest  # Room/DataStore on an emulator
```

Download the signed APK from the [latest release](https://github.com/bbnss/moneta/releases/latest). Installation and signature notes: [0.2.1](docs/RELEASE_0.2.1.md). [Verification record](docs/VERIFICATION_0.2.1.md).

## Contributing

Translations and bug reports are especially welcome — see [CONTRIBUTING.md](CONTRIBUTING.md).

## License

GPL-3.0-or-later. See [LICENSE](LICENSE).
