# Moneta — convertitore di valuta open source per viaggiatori

## Context

**Il problema.** In viaggio serve uno strumento che risponda in due secondi a "quanto sono 45.000 dong in euro?" — anche senza rete, anche in paesi dove metà internet è irraggiungibile. Gli strumenti esistenti falliscono su almeno uno di questi tre punti: o richiedono connessione, o mostrano tassi vecchi senza dirlo, o dipendono da un singolo endpoint che può essere bloccato o spegnersi.

**Perché ora.** L'analisi dei competitor (sotto) mostra una lacuna precisa: nessuna app open source combina multi-provider con failover, offline reale al primo avvio e UX pensata per il viaggio. Il leader di categoria (sal0max/currencies, 340 stelle) ha 20 issue aperte che descrivono esattamente ciò che gli manca, e ha già perso 2 provider per chiusura delle API — una fragilità architetturale che possiamo evitare per costruzione.

**Risultato atteso.** Un'app Android che al primo avvio funziona già offline, che non mostra mai un tasso senza dire quanto è vecchio e da chi arriva, che sopravvive alla morte di qualunque singolo provider, e che parla la lingua di chi viaggia.

**Stato di partenza.** `/Users/bbnss/kDrive2/app/moneta` è vuota. Si parte da zero, nessun vincolo di codice esistente.

---

## Decisioni prese

| Ambito | Scelta |
|---|---|
| Stack | Kotlin + Jetpack Compose + Material 3 (Android nativo) |
| Licenza | GPL-3.0-or-later |
| Scope v1.0 | Core + board multi-valuta + rate card + rilevamento valuta locale + grafico storico + fee bancaria + calcolatrice |
| Rimandato a v1.1 | Widget home + Quick Settings tile |
| Distribuzione | F-Droid + GitHub Releases; Google Play dopo la stabilizzazione della v1 |

---

## Analisi competitor

Dati raccolti dalle API GitHub e da F-Droid il 2026-08-09.

| Progetto | ★ | Licenza | Stack | Provider | Ultimo push |
|---|---|---|---|---|---|
| **sal0max/currencies** | 340 | GPL-3.0 | Kotlin, XML Views, LiveData, Fuel+Moshi | 6 (Frankfurter v1, OpenExchangerates, InforEuro, Norges, Bank Rossii, BoC) | 2025-07-20 |
| **DavidNeurieder/offline-currency-converter** | 13 | AGPL-3.0 | Compose, Hilt, Room, WorkManager, Retrofit | 1 (Frankfurter) | 2026-07-26 |
| **billthefarmer/currency** | 91 | GPL-3.0 | Java, Views | 1 (BCE) | 2026-03-15 |
| **ferraridamiano/ConverterNOW** | 611 | GPL-3.0 | Flutter/Dart | generico, valuta secondaria | 2026-08-08 |

### Cosa copiamo (idee buone dei competitor)

- **Astrazione provider di sal0max** (`model/ApiProvider.kt`): enum con `id` numerico stabile e mai riordinabile, ogni provider una classe con parser dedicato, fallback se l'id salvato non esiste più. Impianto corretto, lo riprendiamo e lo estendiamo.
- **Descrizione del provider in-app**: nome, cadenza di aggiornamento, numero valute, note. Rende trasparente da dove arrivano i numeri.
- **Stack moderno di DavidNeurieder**: Compose + Room + WorkManager + intervallo di sync configurabile, sync in background e non a ogni avvio.
- **Valute preferite con stella**, ricerca nel selettore valuta, tema chiaro/scuro/OLED.
- **Fee/markup configurabile** per simulare la commissione reale della carta.

### Cosa facciamo meglio (lacune verificate)

| Lacuna del leader | Evidenza | Nostra risposta |
|---|---|---|
| Tassi salvati come `Float` | issue "Do not store money in float" | `BigDecimal` end-to-end, persistito come stringa |
| Nessun failover: se il provider cade, l'app non aggiorna | 2 provider già persi (exchangerate.host chiuso, fer.ee rotto) | Catena di fallback automatica + endpoint self-hosted |
| Nessun widget | feature request #1, +9 voti | v1.1 (Glance) |
| Nessuna schermata multi-valuta | 2 issue aperte, +3 voti | Board multi-valuta in v1 |
| Nessuna tabella di riferimento | issue "Show example conversions for 1x/5x/10x/20x" | Rate card da viaggio in v1 |
| Default provider sbagliato per geografia | issue "Bank Rossii as default outside of Russia", +3 | Default = Frankfurter, scelta esplicita al primo avvio |
| Frankfurter v1: solo 30 valute | usa ancora `api.frankfurter.app` | Frankfurter **v2**: 201 valute |
| App vuota al primo avvio offline | nessuno spedisce tassi nell'APK | Seed rates inclusi nell'APK |
| Nomi valuta hardcoded in inglese | `values/strings.xml` | `java.util.Currency.getDisplayName(locale)` da ICU |
| Bug RTL | issue "Overlapping for ar-EG locale" | RTL testato dall'inizio |

---

## Provider dei tassi

Tutti verificati funzionanti con `curl` il 2026-08-09, tutti gratuiti e **senza API key** (requisito F-Droid: nessuna dipendenza da credenziali).

### Primario — Frankfurter v2

Il salto di qualità rispetto a tutti i competitor: **201 valute** aggregate da **84 banche centrali**, storico fino al **1948**, nessuna chiave, nessuna quota, self-hostabile via Docker.

```
GET https://api.frankfurter.dev/v2/rates?base=EUR                       → tutti i tassi correnti
GET https://api.frankfurter.dev/v2/rates?base=EUR&quotes=JPY,USD        → filtro valute
GET https://api.frankfurter.dev/v2/rates?base=EUR&date=2026-07-01       → storico puntuale
GET https://api.frankfurter.dev/v2/rates?base=EUR&from=…&to=…&quotes=…  → serie storica (grafico)
GET https://api.frankfurter.dev/v2/rates?base=EUR&providers=ECB         → filtro per banca centrale
GET https://api.frankfurter.dev/v2/currencies                           → metadati (nome, simbolo, ISO numerico)
GET https://api.frankfurter.dev/v2/providers                            → 84 fonti con copertura valute
```

Attenzione ai nomi esatti dei parametri: sono `quotes`, `from`, `to`, `providers`, `date`. Le varianti `quote`/`start_date`/`end_date` restituiscono **422**. Risposta = array piatto di `{date, base, quote, rate}`. Header utili: `ETag` (→ `If-None-Match` per refresh a costo quasi zero) e `stale-if-error=86400`.

### Catena di fallback

| # | Provider | Endpoint | Valute | Perché è in lista |
|---|---|---|---|---|
| 1 | **Frankfurter v2** | `api.frankfurter.dev/v2/…` | 201 | Default: copertura + storico + serie |
| 2 | **fawazahmed0 currency-api** | `cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/{base}.min.json` | 338 (incl. crypto, XAU, XAG) | CC0, su CDN jsDelivr → raggiungibile dove le API dirette sono bloccate. Mirror: `latest.currency-api.pages.dev` |
| 3 | **ExchangeRate-API open** | `open.er-api.com/v6/latest/{base}` | ~160 | Infrastruttura indipendente, espone `time_next_update_unix` |
| 4 | **BCE diretta** | `ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml` | 29 | Fonte ufficiale, XML statico: sopravvive quando cadono le API |
| 5 | **Bank of Canada Valet** | `bankofcanada.ca/valet/observations/group/FX_RATES_DAILY/json?recent=1` | ~23 | Fonte nordamericana indipendente |
| 6 | **Norges Bank** | `data.norges-bank.no/api/data/EXR/B..NOK.SP?lastNObservations=1&format=sdmx-json` | ~40 | Fonte nordica indipendente |
| 7 | **InforEuro** | `ec.europa.eu/budg/inforeuro/api/public/monthly-rates?lang=EN` | ~150 | Tassi ufficiali UE mensili, utili per rendicontazione |
| 8 | **Bank Rossii** | `cbr.ru/scripts/XML_daily.asp` | ~44 | Copertura per la Russia — **endpoint da verificare in Fase 1** |
| 9 | **Endpoint personalizzato** | URL configurabile dall'utente | — | Istanza Frankfurter self-hosted: la vera risposta alla censura |

**Regola di failover.** L'utente sceglie il provider preferito; se fallisce (timeout 8s, HTTP≠2xx, parsing fallito) si scende nella catena dei provider abilitati. Il provider che ha effettivamente fornito i dati è **sempre visibile in UI** — un cambio di fonte cambia i numeri e non va mai nascosto. Il fallback automatico è attivabile/disattivabile (default: attivo).

**Nota di sostenibilità.** Il rischio numero uno di questa categoria di app è la morte silenziosa delle API. Mitigazione: job CI settimanale che interroga davvero tutti gli endpoint e apre una issue automatica se cambia forma della risposta o cade la disponibilità.

---

## Architettura

Moduli Gradle — separazione pensata per rendere i provider testabili su JVM pura, senza emulatore:

```
:app              UI Compose, ViewModel, navigazione, DI
:core:model       Currency, Rate, RateSnapshot, ProviderId — Kotlin puro, zero Android
:core:providers   implementazioni RateProvider + parser — Kotlin puro + Ktor, test JVM
:core:data        Room, DataStore, RateRepository, motore di failover, WorkManager
:core:ui          design system M3, componenti condivisi
```

### Contratto provider

```kotlin
interface RateProvider {
    val id: ProviderId                    // ordinale stabile, MAI riordinare
    val displayName: String
    val infoUrl: String
    val capabilities: ProviderCapabilities // historical, timeSeries, crypto, metals, currencyCount, cadence

    suspend fun fetchLatest(): ProviderResult<RateSnapshot>
    suspend fun fetchAt(date: LocalDate): ProviderResult<RateSnapshot>
    suspend fun fetchSeries(base: Currency, quote: Currency, from: LocalDate, to: LocalDate): ProviderResult<RateSeries>
}
```

### Modello dati e precisione

Punto critico, ed è dove il competitor leader sbaglia.

- Ogni `RateSnapshot` conserva il **proprio pivot nativo** (EUR per Frankfurter/BCE, CAD per Bank of Canada, NOK per Norges…). Non si ri-basa mai lo snapshot: si evita un arrotondamento in più.
- Cross-rate calcolato a runtime: `rate(A→B) = rate(pivot→B) / rate(pivot→A)`.
- `BigDecimal` ovunque, `MathContext(16, HALF_UP)` nei calcoli intermedi.
- Persistenza in Room come **stringa** (`toPlainString()`), mai `Float`/`Double`/`REAL`.
- Arrotondamento solo in fase di visualizzazione, secondo i minor units ISO 4217 (JPY 0 decimali, KWD 3, BTC 8).

### Offline-first

1. **Seed rates nell'APK**: `app/src/main/assets/seed_rates.json` con uno snapshot Frankfurter, precaricato in Room al primo avvio. L'app non è **mai** vuota, nemmeno installata in aereo.
   *Vincolo build riproducibile F-Droid: il seed è un file committato nel repo e aggiornato da uno script prima di ogni release. La build non deve mai fare chiamate di rete.*
2. **Room è l'unica fonte di verità della UI.** La rete aggiorna Room, la UI non aspetta mai la rete.
3. **WorkManager** periodico, intervallo configurabile (6/12/24/48h, settimanale, manuale), opzione solo-Wi-Fi, `If-None-Match` per non riscaricare dati identici.
4. **Modalità offline totale**: interruttore che disabilita ogni traffico.

### Indicatore di freschezza (elemento identitario)

Sempre visibile sotto il risultato, mai nascosto in un menu:

| Età dei dati | Aspetto | Testo |
|---|---|---|
| < 24h | verde | "Aggiornato oggi, 14:30" |
| 1–3 giorni | neutro | "Aggiornato 2 giorni fa" |
| 3–7 giorni | ambra | "Aggiornato 5 giorni fa" |
| > 7 giorni | rosso | "Tassi di 12 giorni fa — potrebbero non essere accurati" |

Tap → dettaglio: provider usato, timestamp locale esatto, pivot, prossimo aggiornamento previsto, esito dell'ultimo tentativo.

---

## Funzionalità v1.0

**Schermata Converti.** Due valute, tastierino grande a una mano, conversione bidirezionale istantanea. Campo importo con **parser di espressioni** (shunting-yard su `BigDecimal`, nessuna dipendenza esterna: `12+8*3`, parentesi, `%`), mancia in percentuale, divisione del conto per N persone. Scambio valute con animazione. Selettore valuta con ricerca per codice, nome e paese.

**Board multi-valuta.** Un importo in cima, tutte le valute preferite convertite simultaneamente, riordinabili con drag, long-press per promuovere a valuta base.

**Rate card da viaggio.** Tabella 1 / 2 / 5 / 10 / 20 / 50 / 100 / 200 / 500 / 1000 / 5000 di valuta locale → valuta di casa. Dove disponibile usa i **tagli di banconota reali** della valuta (tabella inclusa per le ~60 valute più comuni), altrimenti la serie 1-2-5. Condivisibile come testo, pienamente offline. È lo strumento per contrattare al mercato senza tirare fuori la calcolatrice.

**Rilevamento automatico valuta locale.** Nessun permesso, nessun GPS, funziona offline. Cascata: `TelephonyManager.networkCountryIso` (la rete a cui sei agganciato = dove sei davvero) → `simCountryIso` → fuso orario → locale di sistema. Mapping ISO 3166 → ISO 4217. Suggerimento non invasivo e ignorabile ("Sembra tu sia in Vietnam. Imposto VND?"), **mai** un cambio silenzioso.

**Grafico storico.** Andamento della coppia con intervalli 1M / 3M / 6M / 1A / 5A / Max sfruttando lo storico Frankfurter dal 1948. Libreria Vico (Apache-2.0, su Maven Central). Serie in cache su Room per la consultazione offline. Griglia e assi presenti fin da subito — sono una issue aperta del competitor.

**Fee / markup bancario.** Percentuale globale più override per singola valuta, con riga secondaria "con commissione carta 2,5%".

**Impostazioni.** Scelta provider con schede descrittive, ordine della catena di fallback, endpoint personalizzato, intervallo di sync, tema chiaro/scuro/OLED, lingua, decimali, feedback aptico.

**Privacy.** Solo permesso `INTERNET`. Zero analytics, zero tracker, zero pubblicità, nessun Google Play Services. Registro in chiaro nelle impostazioni di quali host sono stati contattati e quando.

---

## Internazionalizzazione

Non è una feature accessoria: è un'app da viaggio.

- **Nomi delle valute da ICU**, non hardcoded: `java.util.Currency.getInstance(code).getDisplayName(locale)` traduce gratuitamente ~180 codici ISO in ogni locale Android. Stringhe manuali solo per crypto e metalli. Nessun competitor lo sfrutta.
- **Formattazione numerica** locale-aware via `NumberFormat`/ICU, incluse cifre arabo-indiane e persiane.
- **RTL** dall'inizio: `supportsRtl`, padding `start`/`end`, test su `ar` e `fa`.
- **Selettore lingua in-app**: `AppCompatDelegate.setApplicationLocales` + `res/xml/locales_config.xml` (per-app language di Android 13).
- **Weblate** su hosted.weblate.org (gratuito per progetti liberi) fin dal primo giorno, con `CONTRIBUTING` che spiega come tradurre.
- Lingue di lancio: it, en, es, fr, de, pt-BR, ru, zh-CN, ar, hi, ja, tr, id, vi, pl, uk, nl.
- Descrizioni store tradotte in `fastlane/metadata/android/<locale>/` (formato condiviso F-Droid + Play).

---

## Piano di implementazione

**Fase 0 — Ricognizione e scaffolding.**
Clonare i competitor in `reference/` (in `.gitignore`) per consultazione durante l'implementazione: `sal0max/currencies` (astrazione provider e parser delle banche centrali), `DavidNeurieder/offline-currency-converter` (Room/WorkManager/Compose), `billthefarmer/currency` (parsing XML BCE).
Poi: progetto Gradle multi-modulo con version catalog, `LICENSE` GPL-3.0, `README`, `CONTRIBUTING`, CI GitHub Actions (build + test + lint su PR), definizione del package id.

**Fase 1 — Provider (`:core:model`, `:core:providers`).**
`RateProvider` e modello dati. Implementare Frankfurter v2, fawazahmed0, open.er-api, BCE XML. Un file JSON/XML di fixture registrato per provider, test JVM su tutti i parser, zero rete nei test. Verificare l'endpoint Bank Rossii. Aggiungere il job CI settimanale di liveness.

**Fase 2 — Dati e offline (`:core:data`).**
Room + DataStore, `RateRepository`, motore di failover, generazione e caricamento del seed, WorkManager con `If-None-Match`, calcolo dell'età dei dati. Test di migrazione Room e property test sui cross-rate (identità A→B→A, invarianza rispetto al pivot).

**Fase 3 — Schermata Converti.** Design system M3 in `:core:ui`, tastierino, parser di espressioni, mancia e conto diviso, badge di freschezza, selettore valuta con ricerca.

**Fase 4 — Board multi-valuta e rate card**, incluse le tabelle dei tagli di banconota.

**Fase 5 — Rilevamento valuta locale**, con la cascata di segnali e il suggerimento ignorabile.

**Fase 6 — Grafico storico e fee/markup.**

**Fase 7 — Impostazioni complete**, selettore provider, ordinamento fallback, endpoint personalizzato.

**Fase 8 — i18n**: estrazione stringhe, integrazione Weblate, metadati fastlane, verifica RTL.

**Fase 9 — Rilascio**: accessibilità (TalkBack, contrasto, dimensioni testo), build riproducibile, merge request F-Droid, GitHub Release, poi Play.

---

## Verifica

**Test automatici**
- `./gradlew test` — parser di ogni provider su fixture registrate, matematica `BigDecimal`, parser di espressioni, cross-rate.
- `./gradlew connectedAndroidTest` — migrazioni Room, schermata Converti, badge di freschezza in Compose.
- Job CI settimanale separato: interroga gli endpoint reali e apre una issue se la forma della risposta cambia.

**Verifica manuale sul dispositivo**
1. **Primo avvio in aereo**: installare con la rete disattivata → l'app deve convertire subito con i seed rates e mostrare l'età reale del seed.
2. **Failover**: bloccare `api.frankfurter.dev` via DNS/hosts → l'app scende sul provider successivo e dichiara in UI quale ha usato.
3. **Freschezza**: spostare l'orologio di sistema avanti di 10 giorni → il badge deve diventare rosso con il testo di avviso.
4. **Precisione**: convertire 999.999.999 VND → EUR e riconvertire; il round-trip deve tornare al valore iniziale entro i decimali visualizzati (è il test che il competitor fallisce con `Float`).
5. **Valuta locale**: con una SIM o un fuso orario estero, verificare il suggerimento e che sia ignorabile.
6. **i18n**: passare ad `ar` e a `ja` → nomi valuta tradotti da ICU, layout RTL corretto, separatori numerici locali.
7. **Rate card**: offline, verificare i tagli reali per THB, VND, JPY.

---

## Roadmap post-v1

v1.1 widget Glance + Quick Settings tile (la richiesta più votata in assoluto) · crypto e metalli preziosi via fawazahmed0 · avvisi su soglie di cambio · scansione OCR dei prezzi da fotocamera · Wear OS · esportazione CSV dello storico.

---

## Toolchain fissata

| Componente | Versione | Nota |
|---|---|---|
| JDK | 21 (JBR di Android Studio) | nessun JDK autonomo installato sulla macchina |
| Gradle | 8.14.5 | |
| AGP | 8.13.2 | **non** 9.x: vedi sotto |
| Kotlin | 2.4.10 | |
| KSP | 2.3.11 | nuovo schema di versionamento disaccoppiato dal compilatore |
| Compose BOM | 2026.06.01 | |
| compileSdk / targetSdk / minSdk | 36 / 36 / 26 | minSdk 26 dà `java.time` nativo, senza desugaring |

**Perché AGP 8 e non 9.** AGP 9 impone il proprio Kotlin integrato (2.2.10) e la nuova DSL, che rifiuta il plugin `kotlin.android` applicato esplicitamente; la configurazione di Compose e KSP sopra il Kotlin integrato non è ancora documentata. Le uniche due dipendenze che ci obbligherebbero ad AGP 9.1 sono `core-ktx 1.19` e `lifecycle 2.11`, che pretendono compileSdk 37: restano alla release precedente. Da rivalutare quando la documentazione del built-in Kotlin sarà completa.

**Niente Hilt.** Per un'app di questa dimensione un contenitore di dipendenze scritto a mano costa meno di un processore di annotazioni in più, riduce il tempo di build e toglie una variabile alla riproducibilità della build F-Droid. KSP resta, ma solo per Room.

## Punti chiusi

- **Package id**: `io.github.bbnss.moneta` (account GitHub `bbnss`).
- **Bank Rossii**: `https://www.cbr.ru/scripts/XML_daily.asp` risponde 200. Tre insidie da gestire nel parser:
  1. la codifica è **windows-1251**, non UTF-8;
  2. i decimali usano la **virgola** (`57,7548`);
  3. il campo `Nominal` indica a quante unità si riferisce il valore (es. 100 JPY), quindi il tasso va diviso per `Nominal`; inoltre CBR pubblica *rubli per valuta estera*, cioè l'inverso della nostra convenzione `rates[c] = quanti c per 1 pivot`.

## Lezione da non ripetere: R8 e i booleani con valore predefinito

Il grafico storico funzionava in debug e restava vuoto **solo in release**. La causa: la
selezione della fonte passava da `firstOrNull { it.capabilities.timeSeries }`, e R8 aveva
ridotto a costante `false` quel booleano — dichiarato con valore predefinito nella data
class e assegnato a `true` in un solo punto.

Regola adottata: **i campi di `ProviderCapabilities` descrivono, non decidono.** Il flusso
del codice non dipende mai da uno di quei booleani. Chi sa fare una cosa lo dimostra
facendola; chi non sa risponde `UNSUPPORTED`, che è già un esito di prima classe.

Conseguenza operativa: la build di release va **installata e provata**, non solo compilata.
Un `assembleRelease` che riesce non dice nulla su cosa R8 ha rimosso.

## Stato

Fasi 0-9 completate. 95 test JVM, lint pulito (restano solo gli avvisi deliberati sulle
versioni bloccate). APK di release 2,7 MB, verificato sul dispositivo dopo lo shrinking.

## Punti ancora aperti

- **Nome "Moneta"**: verificare che non collida con app esistenti su F-Droid e Play prima di congelarlo.
- **Migrazione ad AGP 9** quando il Kotlin integrato sarà documentato per Compose e KSP.
- **Widget e Quick Settings tile** (v1.1): è la richiesta più votata sul progetto concorrente.
- **Traduzioni**: al momento inglese e italiano. L'infrastruttura Weblate è pronta, mancano
  le lingue di lancio previste dalla spec.
- **Keystore di rilascio**: da generare e conservare fuori dal repository.
