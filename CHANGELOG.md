# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.2.0-rc.2]

- Fix favourite dragging: the normal row long-press no longer competes with the reorder gesture. Show a dedicated reorder hint.
- Increment versionCode to 5 so the published rc.1 can be updated with the same Moneta signature.

## [0.2.0-rc.1] — rates, compact conversion and banknotes

- Preserve each quotation date through providers, Room schema 2 and offline seed. Cross-rates use the oldest necessary date; verification time is separate.
- Select complete source snapshots for each pair and continue failover on missing coverage. Preserve cache on failed refresh.
- Share network policy across opening, foreground, worker, manual updates, history and custom endpoint verification. Recompute freshness every minute without network traffic.
- Add explicit HTTPS endpoint Save/Verify, CUSTOM selection and fallback, and invalidate its cache when the address changes.
- Compact numeric keypad and navigation; optional calculator; saved calculations and active field; locale-aware copy/paste and scrollable long amounts.
- Cash exchange and card payment fee modes apply directly to the main result, including inverse editing.
- Saved favourite order with dragging and accessible move controls. Independent cash pair and banknote counter with decimal totals and per-currency counts.
- Update Italian/English copy and store materials; state finite decimal precision and rounding accurately.
- Release version 0.2.0, code 4, signed with the Moneta upload key. The previously published 0.1.1 test APK used a different debug certificate and cannot be updated directly.


## [0.1.1] — honesty fixes

### Fixed

- **An update with no network no longer counts as an update.** In flight mode, refreshing
  reported success and stamped a fresh time on stale rates: the HTTP cache was answering
  from disk without ever reaching the source, and a cached 200 is indistinguishable from a
  real one. Requests now force revalidation, and a response that never touched the network
  is treated as the network failure it is. The rate age is the one claim this app cannot
  get wrong.
- **The last row of the keypad can no longer be pushed off screen.** Turning on the card fee
  added a line and cost the zero, the decimal separator and the equals key. The keypad is
  now measured first and everything above it scrolls; on short screens the keys step down
  from 56dp to 48dp rather than disappearing.
- **"Max" showed six months.** After viewing a shorter range, the cached series was judged
  sufficient by looking only at its most recent point, so a twenty-year chart was served
  from six months of data. Cached series must now cover both ends of the requested range.
- **The range filters no longer get crushed.** Six chips on one row squeezed the last one
  until its label read one letter per line. They wrap now.

### Changed

- **The chart states its source** and the period actually covered, which is not always the
  period requested — not every source has history that far back.
- **The currencies board gets its screen back.** The calculator keypad has been replaced
  there by a three-column numeric pad, one row shorter, that can also be collapsed: on that
  screen you type an amount, you do not do arithmetic.
- **New app icon**, drawn from the denarius of Juno Moneta, on a dark slate ground so the
  silver reads as metal. The previous vector drawing stays as the monochrome layer for
  Android 13 themed icons.

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
- **Country flags** beside every currency, derived from the country code rather than bundled
  images: no assets to maintain, no licences to check, and they cover currencies added later.
- Favourites start with the four most compared currencies (EUR, USD, CNY, GBP) instead of an
  empty screen, and can be added straight from the currencies board.
- Italian and English interface, with per-app language selection on Android 13 and above.
- App icon drawn as the denarius of Juno Moneta — the temple on the Capitoline where Rome
  struck coins, and the origin of the word "money" itself.

### Notes on correctness

- Money is represented with `BigDecimal` throughout and persisted as text. It is never a
  `Float`, a `Double` or a SQLite `REAL` at any point.
- Rate snapshots keep the source's own pivot currency instead of being re-based on save,
  which avoids an extra rounding step inherited by every later conversion.
- Rounding happens only when formatting for display, following each currency's ISO 4217
  minor units.

[Unreleased]: https://github.com/bbnss/moneta/compare/v0.1.1...HEAD
[0.1.1]: https://github.com/bbnss/moneta/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/bbnss/moneta/releases/tag/v0.1.0
