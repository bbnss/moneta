# Contributing to Moneta

## Translations

Moneta is a travel app, so it should speak your language. Translations are the single most valuable contribution.

Currency *names* are not translated by hand — they come from ICU via `java.util.Currency.getDisplayName(locale)`, which already covers ~180 ISO codes in every Android locale. Only the app's own interface strings need translating, which keeps the workload small.

Add a `app/src/main/res/values-<lang>/strings.xml` and translate the entries from `values/strings.xml`. If a string is missing, the English original is used, so partial translations are fine and useful.

Please also add the language to `app/src/main/res/xml/locales_config.xml` so it shows up in the in-app language picker.

## Reporting a broken provider

Third-party rate APIs change shape or disappear without warning — this project has been designed around that fact. A scheduled CI job checks every endpoint weekly and opens an issue automatically, but if you notice a provider failing before we do, please open an issue with the provider name and the error shown in the app.

## Code

### Architecture

```
:app              Compose UI, ViewModels, navigation, manual DI container
:core:model       Currency, Rate, RateSnapshot — pure Kotlin, no Android
:core:providers   RateProvider implementations and parsers — pure Kotlin, tested on the JVM
:core:data        Room, DataStore, repository, failover engine, WorkManager
:core:ui          Material 3 design system and shared components
```

`:core:model` and `:core:providers` are deliberately Android-free so their tests run on the JVM in milliseconds. Provider parsers are the most fragile part of the app and must stay cheap to test.

### Rules that are not negotiable

- **Money is never a `Float` or a `Double`.** Use `BigDecimal` everywhere and persist it as a string. This is the single most common bug in currency apps.
- **Never round in the middle of a calculation.** Round only when formatting for display, using the currency's ISO 4217 minor units.
- **Never show a rate without its age and its source.** The UI must not silently present stale or substituted data.
- **No network calls during the build.** F-Droid builds must be reproducible, so the bundled seed rates are a committed file, refreshed by a script before a release.
- **No proprietary dependencies**, no analytics, no crash reporters.

### Tests

Provider parsers are tested against recorded fixtures, never against the live network:

```sh
./gradlew test
```

If you add a provider, add a recorded response fixture with it.

## License

By contributing you agree that your work is licensed under GPL-3.0-or-later.
