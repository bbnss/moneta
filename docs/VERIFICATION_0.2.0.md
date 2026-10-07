# Verifica Moneta 0.2.0

## Build finale — codice 6

- `./gradlew test lint assembleRelease bundleRelease --max-workers=2`: completato. 120 test JVM distinti, nessun fallimento, lint senza errori; rimangono avvisi già descritti sotto.
- Nuovo test del confronto commissioni: per 100 con tasso 1,2, riferimento 120, contanti al 5% 114 e carta al 5% 126; il confronto mantiene lo stesso importo di partenza anche modificando il secondo campo.
- APK release con shrinking installato e verificato prima della pubblicazione. Italiano e inglese, 360×640 e 412×915 dp, testo 130%. Importi con e senza commissione e controllo percentuale visibili insieme. Sulla calcolatrice degli schermi piccoli tutti gli operatori sono mantenuti in quattro righe, con bersagli tattili di almeno 48 dp.
- Verificati passaggio calcolatrice/tastierino, scambio delle valute a sinistra tra i campi, importi contanti/carta e formula inversa nella build reale. Calcolatrice compatta: `2×(3+4)=14`, espressione conservata dopo force-stop. Dialogo commissioni scorrevole, con Salva accessibile anche a 360×640 dp e testo 130%. Geometria e scala del testo dell’emulatore ripristinate.
- Certificato APK verificato con apksigner; firma e versione dell’AAB controllate con jarsigner, keytool e bundletool. Stesso certificato Moneta, pacchetto `it.bbnss.moneta`, versione `0.2.0`, codice `6`.
- Controllati file modificati e contenuto degli archivi APK/AAB: nessuna password locale di firma, chiave privata, token o percorso personale trovato. Keystore e proprietà locali sono ignorati da Git. I log e le schermate di prova restano temporanei e non sono allegati alla release.
- Release GitHub normale `v0.2.0`, APK pubblico con checksum e provenienza. AAB conservato localmente per Play; nessun caricamento su Play.

## Verifiche delle precedenti rc

## Verifiche automatiche

- `./gradlew test lint assembleRelease bundleRelease`: test JVM per modello, parser e repository, incluse entrambe le varianti Android; lint senza errori.
- 119 test JVM distinti (60 modello, 50 provider, 9 repository/persistenza) e 3 test su emulatore Android 16 / API 36.1.
- Fixture ALL/USD con date diverse, data più vecchia per cross-rate, pivot senza data artificiale e date mancanti sconosciute.
- Tassi vecchi appena verificati, soglie 0/1–3/4–7/oltre 7 giorni, avanzamento dell’orologio senza mutazioni del database.
- VND/EUR con risposta incompleta BCE, fallback verso fonte compatibile, failover disabilitato e cache conservata.
- Blocchi offline/Wi-Fi per apertura/worker, manuale, storico e Verifica endpoint; nessuna chiamata né aggiornamento dell’ora di verifica quando bloccati. Intervallo automatico e modalità manuale.
- Endpoint con sottopercorso, verifica separata da scelta fonte/cache e invalidazione di snapshot e storico CUSTOM al cambio indirizzo.
- Formule dirette/inverse contanti e carta, totali con tagli decimali, parsing italiano/inglese e testo non valido.
- Preferiti esistenti in ordine alfabetico, nuovo ordine persistente e spostamento che salta la base nascosta.
- Coppia contanti indipendente e conteggi separati per valuta locale.
- Room: database reale costruito dallo schema 1, con cache e storico preesistenti, migrato e validato da Room 2. Valori decimali e timestamp preservati; nuove date NULL.
- DataStore reale chiuso e riaperto: espressione, campo attivo, azzeramento esplicito, ordine, modalità commissione e conteggio recuperati.
- Seed offline con almeno 50 valute e date per tutte le quotazioni, senza passaggi da float nello script.

## Prove della release su emulatore

APK release installato con minificazione e resource shrinking attivi. Controllati italiano/inglese, schermi 360×640 e 412×915 dp, testo 130%, tastierino numerico/calcolatrice, ripristino dell’espressione dopo force-stop, temi, avviso offline, riordino e conteggio dei tagli. Nella vista numerica standard i due importi e la commissione restano visibili senza scorrere, anche a 360×640 con testo 130%. Le schermate temporanee di verifica non sostituiscono i materiali store, che saranno rifatti dopo il riscontro positivo.

Prova di regressione del trascinamento: `python3 scripts/check_reorder.py` su emulatore di prova con almeno tre preferiti visibili. Controlla che il gesto cambi l’ordine, conservi le valute e la base, e che l’ordine torni dopo force-stop. La rc.2 elimina il conflitto fra il gesto di riordino e la normale pressione prolungata della riga.

## Build e firma

Pacchetto `it.bbnss.moneta`, versione `0.2.0`, codice `5`. APK e AAB firmati dalla chiave locale Moneta. Certificati confrontati con apksigner e keytool; AAB verificato con jarsigner. Le password non vengono pubblicate. Provenienza del commit e checksum sono negli allegati della prerelease.

R8 è fissato a 9.1.29, versione compatibile con Kotlin 2.4, tramite il repository ufficiale. I precedenti avvisi sui metadati Kotlin sono risolti. Rimangono avvisi R8 relativi al parsing asincrono non supportato dal provider AGP e avvisi lint sulle versioni delle dipendenze: non sono errori di compilazione. [Compatibilità Kotlin/R8](https://developer.android.com/build/kotlin-support), [repository R8](https://r8.googlesource.com/r8/+/refs/heads/main/README.md).

La firma del vecchio APK GitHub 0.1.1 è Android Debug e differisce da questa chiave Moneta. La compatibilità con installazioni Play non è stata verificata. Le istruzioni complete sono in [RELEASE_0.2.0-rc.2.md](RELEASE_0.2.0-rc.2.md).
