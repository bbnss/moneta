# Moneta

**A currency converter for travellers. Works offline, never hides how old the rates are, and survives any single data source going down.**

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

Moneta answers "how much is 45,000 dong in euros?" in two seconds — on a plane, in a market with no signal, or in a country where half the internet is unreachable.

## Why another converter?

Every currency app fails on at least one of these:

- **It needs a connection.** Moneta ships with a rate snapshot inside the APK, so it converts correctly the very first time you open it, even with the network off. From then on it keeps a local copy of everything.
- **It shows stale rates without telling you.** Moneta always displays how old the rates are and which source they came from. Past a week, it says so in red.
- **It depends on one endpoint.** Moneta talks to several independent providers and falls back automatically when one is unreachable — including a CDN-hosted source that stays reachable where direct APIs are blocked, and your own self-hosted endpoint if you want full control.

## Features

- Offline-first: the local database is the only source of truth, the UI never waits on the network
- Explicit freshness indicator with four states, always visible
- Multiple free providers, no API keys, automatic failover, manual override
- Multi-currency board: one amount, all your favourite currencies at once
- Travel rate card: real banknote denominations of the local currency converted at a glance
- Automatic local-currency detection with no location permission
- Historical charts, configurable card/exchange markup, built-in calculator with tip and bill split
- Exact decimal maths — no floating-point rounding on your money
- No ads, no trackers, no analytics, no Google Play Services. `INTERNET` is the only meaningful permission.

## Data sources

All free, all key-less. See [`docs/SPEC.md`](docs/SPEC.md) for the full list and endpoint contracts.

| Provider | Currencies | Notes |
|---|---|---|
| [Frankfurter](https://frankfurter.dev/) v2 | 201 | Default. 84 central banks, history back to 1948, self-hostable |
| [fawazahmed0/exchange-api](https://github.com/fawazahmed0/exchange-api) | 338 | CDN-hosted, includes crypto and precious metals |
| [ExchangeRate-API open](https://www.exchangerate-api.com/docs/free) | ~160 | Independent infrastructure |
| European Central Bank | 29 | Static XML, official, survives API outages |
| Bank of Canada, Norges Bank, InforEuro, Bank Rossii | varies | Regional official sources |

Rates are reference rates. They are not what a bank or a bureau will actually give you — that is what the configurable markup is for.

## Build

Requires JDK 21 and the Android SDK (compileSdk 36).

```sh
./gradlew assembleDebug     # build
./gradlew test              # unit tests
./gradlew lint              # static analysis
```

## Contributing

Translations and bug reports are especially welcome — see [CONTRIBUTING.md](CONTRIBUTING.md).

## License

GPL-3.0-or-later. See [LICENSE](LICENSE).
