# Moneta 0.2.0

Versione `0.2.0`, codice `6`, pacchetto `it.bbnss.moneta`. Release normale per GitHub e Obtainium.

## Converti

- Calcolatrice disponibile all’apertura, con pulsante per passare al tastierino numerico.
- Scambio delle valute a sinistra, fra i due campi degli importi.
- Con una commissione impostata, il secondo campo mostra sia il risultato con commissione sia “Senza commissione”, riferiti allo stesso importo di partenza. Il confronto resta coerente quando modifichi il secondo campo.
- “Ricevi” identifica l’importo dopo la commissione per cambio contanti; “Costo con carta” identifica il costo maggiorato. Il pulsante separato “Commissione: 5%” apre le impostazioni della percentuale e della modalità.

Sono inclusi anche date per singola quotazione, cache per coppia, fallback con copertura, regole comuni per offline/Wi-Fi/aggiornamenti, endpoint personale, copia/incolla, calcoli salvati, preferiti ordinabili e conta-banconote.

## Download e Obtainium

Scarica `Moneta-0.2.0-6.apk`. Per Obtainium usa la fonte GitHub `https://github.com/bbnss/moneta`: questa release non richiede “Includi prerelease”. Lascia vuoti i filtri sul nome dell’APK o sul titolo della release. L’AAB è conservato localmente per Play e non è un allegato GitHub. Nessun caricamento su Play è stato effettuato.

Scarica anche `SHA256SUMS.txt` e verifica nella stessa cartella:

```sh
shasum -a 256 -c SHA256SUMS.txt
```

## Firma e aggiornamento

L’APK usa il keystore Moneta esistente. Il certificato SHA-256 è:

`e674d49ab66f68eba081e7a4ffbbd4f4c6a78d7d2e8bc3add7f4517f904c2c02`

La firma coincide con rc.1, rc.2 e il precedente AAB locale `Moneta-0.1.1-3.aab` preparato per Play. Le due rc possono essere aggiornate direttamente, conservando i dati. Anche una versione Play firmata con questo stesso certificato può essere aggiornata se il suo codice versione è inferiore a 6; il certificato dell’APK distribuito da Play non è stato confrontato direttamente.

Il vecchio `moneta-0.1.1-test.apk` di GitHub usa invece il certificato Android Debug e non può essere aggiornato direttamente da questo APK. Disinstallare quella vecchia build elimina i suoi dati: annota preferiti e impostazioni prima di un’eventuale reinstallazione. La chiave privata, il keystore e le password non sono pubblicati; il certificato e la sua impronta sono pubblici. [Firma Android](https://developer.android.com/studio/publish/app-signing).
