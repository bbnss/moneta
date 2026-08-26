# Scheda del Play Store — Moneta 0.1.1 (versionCode 2)

Tutto il materiale per creare la scheda dal browser. Generato il 26 agosto 2026
dalla stessa build firmata che si carica.

I testi si incollano da `testi/`, un file per lingua, con le quattro sezioni già
divise e i conteggi dei caratteri verificati contro i limiti di Play.

Le lingue sono le due che l'app parla: italiano (predefinita) e inglese. In
Console si aggiungono da *Gestisci traduzioni*.

## Il pacchetto da caricare

    dist/Moneta-0.1.1-2.aab

Firmato con `moneta-upload-key.jks` (alias `moneta`), verificato con
`jarsigner -verify`. È la **chiave di upload**: Google firma poi l'APK con una
chiave propria, come per ogni app nuova. Password e percorso stanno in
`keystore.properties`, che non è nel repository.

SHA-256 del certificato di upload:

    E6:74:D4:9A:B6:6F:68:EB:A0:81:E7:A4:FF:BB:D4:F4:C6:A7:8D:7D:2E:8B:C3:AD:D7:F4:51:7F:90:4C:2C:02

Per ricostruirlo da zero:

    JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
      ./gradlew :app:bundleRelease

## Dove va ogni cosa

**Cresci › Presenza sul Play Store › Scheda principale dello Store**

| Campo in Console | File |
|---|---|
| Nome dell'app · Descrizione breve · Descrizione completa | `testi/<lingua>.md` |
| Icona dell'app | `grafica/icona_512.png` |
| Immagine in primo piano (italiano) | `grafica/immagine_in_primo_piano_1024x500_it.png` |
| Immagine in primo piano (inglese) | `grafica/immagine_in_primo_piano_1024x500_en.png` |
| Screenshot per telefono (italiano) | `screenshot/telefono_it/` (6 file) |
| Screenshot per telefono (inglese) | `screenshot/telefono_en/` (6 file) |

Le note di versione non stanno qui: si incollano al momento del caricamento, in
**Testa e rilascia › Produzione › Crea nuova release › Note di versione**, una
lingua alla volta. Sono l'ultima sezione di ogni file in `testi/`.

Non ci sono screenshot per tablet né per Android TV. L'app gira sui tablet, ma
il layout è pensato per il telefono e non ha una versione a due colonne: senza
screenshot per tablet Play riusa quelli del telefono, che è il male minore.

## Le altre schede della Console, e come rispondere

Sono i moduli obbligatori senza i quali la release resta bloccata. Le risposte
qui sotto descrivono l'app com'è; se un giorno cambia, cambiano anche loro.

**Norme › Contenuti dell'app › Norme sulla privacy**
`https://github.com/bbnss/moneta/blob/main/docs/PRIVACY.md`
Il testo è in `docs/PRIVACY.md`, versionato insieme al codice.

**Norme › Contenuti dell'app › Annunci**
L'app non contiene annunci.

**Norme › Contenuti dell'app › Accesso all'app**
Tutte le funzionalità sono disponibili senza restrizioni di accesso: non c'è
login, non c'è account.

**Norme › Contenuti dell'app › Sicurezza dei dati**
- L'app raccoglie o condivide dati utente? **No.**
- Nessun tipo di dato da dichiarare, quindi cadono anche le domande su cifratura
  in transito e cancellazione su richiesta.
- ID pubblicità: **non usato.** Nel manifest non c'è `AD_ID` e non è linkata
  nessuna libreria Google Play Services.
La sola cosa che esce dal telefono è la richiesta della tabella dei tassi, che
non contiene identificatori: è spiegata per esteso nelle norme sulla privacy.

**Norme › Contenuti dell'app › Classificazione dei contenuti**
Categoria del questionario: *Utility, produttività, comunicazione o altro*.
Tutte le domande su violenza, sesso, linguaggio, droghe, gioco d'azzardo e
contenuti generati dagli utenti: **no**. Ne esce PEGI 3 / Everyone.

**Norme › Contenuti dell'app › Pubblico di destinazione**
Fascia d'età **18 e oltre**. L'app non è rivolta ai bambini e questa scelta la
tiene fuori dalle Norme per le famiglie, che non aggiungerebbero nulla a
un'app che non raccoglie dati.

**Norme › Contenuti dell'app › Funzionalità finanziarie**
**L'app non offre alcuna funzionalità finanziaria.** È la risposta giusta anche
scegliendo la categoria Finanza: Moneta non muove denaro, non è un exchange,
non offre credito e non si collega a nessun conto. Converte numeri.

**Norme › Contenuti dell'app › App di attualità, App per il COVID-19, App
governative**: no a tutte e tre.

**Se la Console chiede dei servizi in primo piano**: l'app non ne avvia
nessuno. `FOREGROUND_SERVICE`, `WAKE_LOCK` e `RECEIVE_BOOT_COMPLETED` compaiono
nel manifest unito perché li dichiara WorkManager, la libreria che manda avanti
l'aggiornamento programmato dei tassi. Nessun `foregroundServiceType` è
dichiarato, quindi normalmente il modulo non viene nemmeno chiesto.

**Cresci › Impostazioni dello Store**
- Tipo di app: App
- Categoria: **Finanza** (in alternativa *Viaggi e info locali*: la categoria si
  cambia quando si vuole, il nome dell'app no)
- Tag: convertitore di valuta, cambio, viaggi
- Email di contatto: quella dell'account sviluppatore — Play la mostra in
  chiaro sulla scheda, quindi va scelta sapendolo
- Sito web (facoltativo): `https://github.com/bbnss/moneta`

Se l'account sviluppatore è personale ed è stato aperto dopo il 13 novembre
2023, Play chiede prima un test chiuso con almeno 12 tester per 14 giorni
consecutivi. Sugli account più vecchi non si applica.

## Come sono stati fatti gli screenshot

Emulatore Android 16 (`Medium_Phone_API_36.1`), 1080x2400, APK di **release
firmato** — lo stesso binario che si carica, non una build di sviluppo.

Barra di stato in modalità demo: orologio fermo sulle 10:00, batteria piena,
niente notifiche, niente rete mobile.

    adb shell settings put global sysui_demo_allowed 1
    adb shell am broadcast -a com.android.systemui.demo -e command enter
    adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1000
    adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
    adb shell am broadcast -a com.android.systemui.demo -e command network -e mobile hide -e wifi show -e level 4
    adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false

La lingua dell'app si cambia senza toccare quella del sistema:

    adb shell cmd locale set-app-locales it.bbnss.moneta --locales it-IT

Lo scudo di Safety Center che l'emulatore pianta nella barra di stato non
c'entra niente con l'app e viene coperto in fase di elaborazione. Quello e la
grafica li rifà:

    python3 scripts/make_play_assets.py <cartella_degli_screenshot_grezzi>

Lo scenario è sempre lo stesso, il Vietnam dell'esempio nella descrizione:
45.000 VND in euro, preferite CNY EUR GBP JPY THB USD, commissione 3% dove
serve mostrarla. Fonte Frankfurter, tassi aggiornati in giornata — il pallino
verde accanto all'ora è lo stato "fresco", ed è il punto di quelle schermate.

## Due cose che si vedono negli screenshot

**Il grafico lascia vuota la metà bassa dello schermo.** Su un telefono da
2400px il contenuto finisce a poco più di metà. Si nota nello screenshot 04.

**Le date sugli assi del grafico restano in formato italiano anche in inglese**
(`26/08/21` invece di `Aug 26, 2021`), mentre la riga della fonte sotto è
tradotta. Si vede in `screenshot/telefono_en/04_chart.png`.

Nessuna delle due blocca la pubblicazione. Vanno sistemate nel codice, non
ritoccando gli screenshot.
