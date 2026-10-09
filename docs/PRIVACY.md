# Privacy policy — Moneta

_Last updated: 9 October 2026. Applies to the Android app `it.bbnss.moneta`._

## The short version

Moneta collects nothing. There is no account, no sign-up, no advertising, no
analytics and no crash reporting. Nothing you type or choose in the app leaves
your device. No Google Play Services library is linked into the app.

## What stays on your device

Your favourite currencies, the currencies you last converted, the rate source
you picked, the card markup, the update interval and the downloaded exchange
rates are stored in the app's own private storage on your phone. Uninstalling
the app deletes all of it. There is no cloud copy, because there is nowhere for
it to be copied to.

## What leaves your device

One thing only: a request for exchange rates, sent to the rate source you have
selected. It contains no identifier of any kind — no account, no device id, no
advertising id — and never contains an amount you typed or a currency pair you
looked at. It is a request for a public rate table, identical for every user of
the app.

As with any request over the internet, the operator of that endpoint can see the
IP address it came from, and their own logging policy applies to it. Those
operators are:

| Source | Endpoint |
|---|---|
| Frankfurter (default) | `api.frankfurter.dev` |
| Currency API (fawazahmed0) | `cdn.jsdelivr.net` |
| ExchangeRate-API open | `open.er-api.com` |
| European Central Bank | `www.ecb.europa.eu` |
| Bank of Canada | `www.bankofcanada.ca` |
| Norges Bank | `data.norges-bank.no` |
| InforEuro (European Commission) | `ec.europa.eu` |
| Bank Rossii | `www.cbr.ru` |

If you set a self-hosted endpoint in the settings, requests go to that address
instead, and only you decide what it logs.

Turning off automatic updates, or simply never refreshing, means the app makes
no network request at all. It keeps working, on the rates it already has, and
tells you how old they are.

## Permissions

- `INTERNET` — to download the rate table. That is its only use.
- `ACCESS_NETWORK_STATE` — to tell "there is no connection" apart from "the
  source did not answer", so a failed refresh is never reported as a success.
- `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE` — added by
  WorkManager, the Android library that runs the scheduled rate refresh and
  restores its schedule after a reboot. They are normal permissions, granted at
  install time, and none of them gives access to anything about you.

None of these is a runtime permission: Android never shows you a dialog for
them, because there is nothing to consent to.

Moneta asks for no location permission. The local-currency suggestion is derived
from the mobile network code, the SIM, the time zone and the system locale,
all of which are already available to any app without a permission, and it is
never sent anywhere.

## Children

Moneta contains nothing directed at children, and since it collects no data it
collects no data from children either.

## Changes

Any change to this policy will be published in this file, whose history is
public at <https://github.com/bbnss/moneta/commits/main/docs/PRIVACY.md>.

## Contact

Open an issue at
<https://github.com/bbnss/moneta/issues>.
