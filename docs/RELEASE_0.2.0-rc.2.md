# Moneta 0.2.0 — prova firmata rc.2

Versione Android `0.2.0`, codice `5`, pacchetto `it.bbnss.moneta`. L’APK è prodotto con shrinking attivo e firmato con il keystore esistente `moneta-upload-key.jks`. Su GitHub è disponibile l’APK per la prova; l’AAB dello stesso commit è conservato localmente per il successivo caricamento su Play, dopo l’approvazione. Il keystore e le password restano esclusi dal repository e dagli allegati.

Questa build corregge il trascinamento dei preferiti della rc.1. Può aggiornare la rc.1 firmata Moneta senza disinstallazione: usa lo stesso certificato e un codice versione superiore.

## Novità

- Date per singola valuta e data della conversione corretta; “Tasso del…” distinto da “Verificato…”.
- Fallback che controlla la copertura della richiesta, cache per coppia e nessuna mescolanza di fonti nella stessa conversione.
- Offline, Wi-Fi e intervallo applicati da una politica comune; modalità manuale senza aggiornamenti automatici.
- Converti compatto, tastierino numerico con calcolatrice opzionale, copia/incolla e ripristino del calcolo.
- Commissioni “Cambio contanti” e “Pagamento carta” incluse nel risultato, anche modificando il secondo campo.
- Preferiti ordinabili e salvati. Contanti con coppia indipendente e conta-banconote; conteggio per valuta locale e totale nelle due valute.
- Endpoint personale HTTPS: modifica, Salva, Verifica, selezione CUSTOM e fallback.

## Installazione e firma

Scarica l’APK e `SHA256SUMS.txt` dalla prerelease. Verifica con `shasum -a 256 -c SHA256SUMS.txt` nella cartella contenente l’APK e il file dei checksum. Consenti l’installazione dalla fonte usata per scaricarlo e apri il file APK.

Il certificato SHA-256 di questa build è:

`e674d49ab66f68eba081e7a4ffbbd4f4c6a78d7d2e8bc3add7f4517f904c2c02`

Il vecchio `moneta-0.1.1-test.apk` pubblicato su GitHub è firmato con Android Debug, SHA-256:

`50ebbcabee5e06a2de08935519043f4a25a35fcf8cc0a4747ce89e2f6e11bf30`

Le due firme non sono compatibili: questa build non può aggiornare direttamente quel vecchio APK. Se è installato, la prova richiede disinstallazione e nuova installazione; la disinstallazione elimina cache, preferiti e impostazioni. Annota ciò che vuoi ripristinare prima di procedere. La migrazione Room conserva i dati solo quando l’aggiornamento è consentito dalla firma. Il certificato di questo APK coincide con quello del precedente AAB locale `Moneta-0.1.1-3.aab` preparato per Play. La chiave di upload e la chiave di firma dell’app su Play possono coincidere; non è stato modificato alcun keystore né effettuato un caricamento su Play. Se l’app distribuita da Play è firmata con questo stesso certificato, l’APK può aggiornarla quando il codice versione installato è inferiore. Il certificato dell’APK effettivamente distribuito da Play non è stato confrontato qui; il precedente AAB conferma la continuità della firma del bundle. [Firma Android](https://developer.android.com/studio/publish/app-signing).

## Prova consigliata

1. Apri offline: importo iniziale 1 e conversione dai dati inclusi; la prima cifra deve sostituire 1. Controlla il tasso e la data dichiarata.
2. Prova VND/EUR, seleziona BCE e aggiorna con fallback attivo e disattivo. Se manca copertura, la cache utilizzabile deve restare disponibile con avviso.
3. Prova “Solo quando lo chiedo”, offline e Wi-Fi-only, anche per Aggiorna, storico e Verifica endpoint. Una richiesta bloccata non deve cambiare “Verificato”.
4. Imposta una commissione: contanti deve ridurre il risultato, carta aumentarlo. Modifica il secondo campo e controlla l’inverso.
5. Digita un’espressione e chiudi forzatamente l’app. Alla riapertura devono tornare importo, campo attivo e modalità calcolatrice. Azzera e ripeti: deve restare zero.
6. Copia/incolla numeri in italiano e inglese; testo non valido deve dare un messaggio e lasciare invariato il calcolo. Prova importi lunghi e scorrimento orizzontale.
7. Riordina Valute trascinando dopo pressione prolungata o con i comandi su/giù; riapri e controlla l’ordine.
8. In Contanti scegli Casa e Locale; conta i tagli con −/+, verifica i due totali e azzera. Cambia Casa: il conteggio locale deve restare. Cambia Locale e torna: deve tornare il suo conteggio.
9. Salva un server HTTPS Frankfurter v2 con eventuale sottopercorso e premi Verifica. Controlla copertura e errori; Verifica non deve scegliere automaticamente la fonte. Cambia indirizzo e verifica che la vecchia cache CUSTOM non sia usata.
10. Controlla tema chiaro/scuro/nero, italiano/inglese e testo ingrandito. Nella vista numerica normale importi e commissione devono essere visibili senza scorrere.

Comunica eventuali problemi indicando lingua, dimensioni schermo, fonte, coppia e passaggi per riprodurli. La pubblicazione definitiva, i nuovi screenshot store e il caricamento Play seguiranno il tuo riscontro positivo.
