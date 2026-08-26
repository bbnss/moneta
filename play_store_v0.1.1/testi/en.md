# English (en-US)

## App name · max 30 characters

Moneta - Currency Converter

## Short description · max 80 characters

Currency converter that works offline and never hides how old the rates are

## Full description · max 4000 characters

Moneta answers "how much is 45,000 dong in euros?" in two seconds — on a plane, in a market with no signal, or in a country where half the internet is unreachable.

Nearly every currency app fails on at least one of three points. Moneta is built around all three.

IT WORKS BEFORE IT HAS EVER SEEN THE INTERNET
A rate snapshot ships inside the app, so it converts correctly the very first time you open it, with the network off. From then on it keeps a local copy of everything, and the interface never waits on a connection.

IT NEVER HIDES HOW OLD THE RATES ARE
Every screen states when the rates were last updated and which source they came from. Past a week, it says so in red. A rate with no stated age is a number you cannot trust, and showing one in silence is the flaw common to nearly every converter.

IT DOES NOT DEPEND ON ONE ENDPOINT
Eight independent sources: the Frankfurter aggregate of 84 central banks, a CDN-hosted dataset that stays reachable where direct APIs are blocked, and several central banks including the ECB, Bank of Canada, Norges Bank and Bank Rossii. If one is unreachable the next one is used, and the app tells you which. You can also point it at your own self-hosted instance.

MADE FOR TRAVELLING
- Cash table: what each real banknote of the local currency is worth, for the denominations actually in circulation — 1,000 to 500,000 dong in Vietnam, not a generic 1-2-5 list
- Multi-currency board: one amount, all your currencies converted at once
- Local currency detection with no location permission and no GPS
- Historical charts with grid and axes, from one month to twenty years
- Card or bureau markup, so you see the figure you will actually be handed
- Calculator built into the amount field: split the bill, add the tip
- Search currencies by country: type "Vietnam", not "VND"
- Country flags beside every currency

EXACT MATHS
Money is never stored or calculated as a floating-point number. Amounts stay exact through every conversion, which matters most for high-denomination currencies where the error would be visible.

PRIVACY
No ads, no trackers, no analytics, no Google Play Services. The only permission that matters is internet access, used solely to download rates. Moneta has no account, asks for no sign-up and collects nothing.

Free and open source software, GPL-3.0-or-later. The code is at github.com/bbnss/moneta

Rates are reference rates. They are not what a bank or a bureau will actually give you — that is what the configurable markup is for.

## Release notes for this version · max 500 characters

First release on Google Play.

- Converts offline from the very first launch, using rates bundled in the app
- Always states how old the rates are and which source they came from
- Eight independent sources with automatic fallback
- Cash table with the real banknotes of the local currency
- Multi-currency board, historical charts, built-in calculator
- Exact decimal maths, no floating point on your money
- No ads, no trackers, no analytics
