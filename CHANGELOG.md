# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] — first release

### Added

- **Offline from the very first launch.** A rate snapshot ships inside the APK, so the
  app converts correctly before it has ever reached the network. The local database is
  the only source of truth for the interface, which therefore never waits on a connection.
- **Explicit data age.** Every screen states when the rates were last updated and which
  source supplied them, with four escalating states up to a red warning past a week.
  The bundled snapshot reports its real generation date rather than pretending to be current.
- **Eight independent rate sources** with automatic fallback: Frankfurter (201 currencies
  aggregated from 84 central banks), fawazahmed0's CDN-hosted dataset, ExchangeRate-API,
  the European Central Bank, Bank of Canada, Norges Bank, InforEuro and Bank Rossii.
  A self-hosted Frankfurter instance can be configured for places where the public
  endpoints are blocked. The source that actually answered is always named in the interface.
- **Cash table** listing the banknotes really in circulation for the local currency, for
  around 75 currencies, with a calibrated estimate elsewhere — clearly labelled as such.
- **Multi-currency board**: one amount converted into all favourite currencies at once.
- **Local currency detection** from the mobile network, SIM, time zone and system locale,
  with no location permission and no GPS. Offered as a suggestion, never applied silently.
- **Historical charts** with grid and axes, ranges from one month to twenty years, cached
  locally so they remain readable offline.
- **Card or bureau markup**, shown as a second line beside the reference amount.
- **Calculator in the amount field**: arithmetic, parentheses, tip percentage and bill splitting.
- Currency search by code, name or **country**.
- Italian and English interface, with per-app language selection on Android 13 and above.

### Notes on correctness

- Money is represented with `BigDecimal` throughout and persisted as text. It is never a
  `Float`, a `Double` or a SQLite `REAL` at any point.
- Rate snapshots keep the source's own pivot currency instead of being re-based on save,
  which avoids an extra rounding step inherited by every later conversion.
- Rounding happens only when formatting for display, following each currency's ISO 4217
  minor units.

[Unreleased]: https://github.com/bbnss/moneta/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/bbnss/moneta/releases/tag/v0.1.0
