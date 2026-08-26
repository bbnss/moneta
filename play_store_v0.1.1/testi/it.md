# Italiano (it-IT) — lingua predefinita della scheda

## Nome dell'app · max 30 caratteri

Moneta - Cambio valuta

## Descrizione breve · max 80 caratteri

Convertitore di valuta che funziona offline e dichiara l'età dei tassi

## Descrizione completa · max 4000 caratteri

Moneta risponde a "quanto sono 45.000 dong in euro?" in due secondi — in aereo, in un mercato senza campo, o in un paese dove metà internet è irraggiungibile.

Quasi tutte le app di cambio falliscono su almeno uno di tre punti. Moneta è costruita attorno a tutti e tre.

FUNZIONA PRIMA ANCORA DI AVER VISTO INTERNET
Una fotografia dei tassi è inclusa nell'app, quindi converte correttamente già al primo avvio, con la rete spenta. Da lì in poi conserva una copia locale di tutto, e l'interfaccia non aspetta mai una connessione.

NON NASCONDE MAI QUANTO SONO VECCHI I TASSI
Ogni schermata dice quando i tassi sono stati aggiornati e da quale fonte arrivano. Oltre una settimana lo scrive in rosso. Un tasso senza età dichiarata è un numero di cui non ci si può fidare, e mostrarlo in silenzio è il difetto comune a quasi tutti i convertitori.

NON DIPENDE DA UN SOLO ENDPOINT
Otto fonti indipendenti: l'aggregato Frankfurter di 84 banche centrali, un archivio servito da CDN che resta raggiungibile dove le API dirette sono bloccate, e diverse banche centrali fra cui BCE, Bank of Canada, Norges Bank e Bank Rossii. Se una non risponde si passa alla successiva, e l'app dice quale ha usato. Si può anche puntare a una propria istanza self-hosted.

PENSATA PER VIAGGIARE
- Tabella dei contanti: quanto vale ogni banconota vera della valuta locale, con i tagli davvero in circolazione — da 1.000 a 500.000 dong in Vietnam, non un generico elenco 1-2-5
- Board multi-valuta: un importo, tutte le tue valute convertite insieme
- Rilevamento della valuta locale senza permesso di posizione e senza GPS
- Grafici storici con griglia e assi, da un mese a vent'anni
- Maggiorazione di carta o cambiavalute, per vedere la cifra che ti daranno davvero
- Calcolatrice dentro il campo importo: dividi il conto, aggiungi la mancia
- Ricerca delle valute per paese: scrivi "Vietnam", non "VND"
- Bandiere dei paesi accanto a ogni valuta

ARITMETICA ESATTA
Il denaro non viene mai salvato né calcolato in virgola mobile. Gli importi restano esatti attraverso ogni conversione, cosa che conta soprattutto per le valute ad alta denominazione, dove l'errore sarebbe visibile.

PRIVACY
Nessuna pubblicità, nessun tracciatore, nessuna analisi, nessun servizio Google Play. L'unico permesso che conta è l'accesso a internet, usato solo per scaricare i tassi. Moneta non ha account, non chiede registrazione e non raccoglie nulla.

Software libero e open source, GPL-3.0-or-later. Il codice è su github.com/bbnss/moneta

I tassi sono di riferimento. Non sono quelli che darà davvero una banca o un cambiavalute: è a questo che serve la maggiorazione configurabile.

## Note di questa versione · max 500 caratteri

Prima versione su Google Play.

- Converte offline fin dal primo avvio, con i tassi inclusi nell'app
- Dichiara sempre l'età dei tassi e la fonte da cui arrivano
- Otto fonti indipendenti con ricaduta automatica
- Tabella dei contanti con le banconote reali della valuta locale
- Board multi-valuta, grafici storici, calcolatrice integrata
- Aritmetica decimale esatta: niente virgola mobile sul denaro
- Nessuna pubblicità, nessun tracciatore, nessuna analisi
