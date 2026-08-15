# Bug trovati durante il test

Quaderno di lavoro: qui si annotano i problemi trovati usando l'app, si sistemano
tutti insieme in una tornata sola e poi si spostano in fondo, fra quelli risolti.

Non serve essere precisi né ordinati. Bastano poche righe buttate giù: se manca
qualcosa la si chiede dopo. L'unica cosa che conta davvero è **cosa hai fatto
prima che succedesse**, perché è quello che permette di riprodurlo.

Da copiare per ogni nuovo bug:

```
### N. Titolo in una riga

- **Dove:** schermata o sezione
- **Cosa succede:**
- **Cosa dovrebbe succedere:**
- **Come riprodurlo:** 1. … 2. … 3. …
- **Sempre o a volte:**
- **Note:** versione, telefono, rete, orario
```

---

## Aperti

_(nessuno)_

---

## Risolti

### 1. Offline, l'aggiornamento riesce comunque e scrive un orario nuovo

Risolto in **0.1.1**.

- **Dove:** aggiornamento dei tassi
- **Cosa succedeva:** con il telefono senza rete l'aggiornamento andava a buon
  fine e scriveva un nuovo orario di ultimo aggiornamento su dati vecchi.
- **Causa:** la cache HTTP di OkHttp considera *fresca* la copia locale finché
  dura il `max-age` dichiarato dalla fonte — ore, su una CDN — e la restituisce
  **senza toccare la rete**. All'app arrivava un 200 identico a quello di una
  richiesta riuscita.
- **Correzione:** due regole complementari. Ogni richiesta porta ora
  `Cache-Control: max-age=0`, che obbliga a rivalidare con la fonte; e un
  interceptor scarta le risposte mai passate dalla rete, trasformandole in un
  errore che la catena di ricaduta già sa gestire. La cache resta utile: se i
  tassi non sono cambiati la fonte risponde 304 senza corpo.
- **Verifica:** in modalità aereo l'aggiornamento ora dichiara "non è stato
  possibile raggiungere nessuna fonte" e l'orario **non** cambia.

### 2. La riga della commissione mangiava l'ultima riga della calcolatrice

Risolto in **0.1.1**.

- **Dove:** schermata Converti, e tastierino della schermata Valute
- **Cosa succedeva:** attivando la commissione compariva una riga in più e
  l'ultima fila di tasti — zero, virgola, uguale — finiva fuori dallo schermo.
- **Causa:** la schermata era una colonna rigida. Tutto ciò che compariva in
  cima spingeva fuori ciò che stava in fondo, invece di scorrere.
- **Correzione:** il tastierino è uscito dall'area che scorre e viene misurato
  per primo, quindi c'è sempre per intero; banner, commissione e avvisi ora
  scorrono. Su schermi bassi i tasti passano da 56dp a 48dp, che resta sopra il
  minimo tattile.
- **Nella schermata Valute** il tastierino da calcolatrice è stato sostituito da
  un tastierino numerico a tre colonne, una riga in meno e tasti più bassi — lì
  si scrive un importo, non si fanno conti — e si può chiudere del tutto con la
  freccia accanto all'importo, liberando lo schermo per l'elenco.

### 3. Il pulsante "Max" tagliato a metà, e dati senza provenienza

Risolto in **0.1.1**.

- **Dove:** schermata dello storico
- **Cosa succedeva:** i sei filtri di periodo stavano su una riga sola che li
  schiacciava, e all'ultimo restava così poco spazio da scriverne l'etichetta
  una lettera per riga. Inoltre nulla diceva da dove venissero i numeri.
- **Correzione:** i filtri vanno a capo quando non ci stanno, restando tutti
  leggibili senza scorrimenti nascosti. Sotto il grafico compare ora la fonte
  con il periodo davvero coperto — che non è sempre quello chiesto, perché non
  tutte le fonti hanno storico così indietro.

### 4. "Max" mostrava sei mesi

Trovato verificando il punto 3, risolto in **0.1.1**.

- **Dove:** schermata dello storico
- **Cosa succedeva:** dopo aver guardato sei mesi, scegliendo "Max" il grafico
  restava di sei mesi pur dichiarando vent'anni.
- **Causa:** la cache locale veniva giudicata sufficiente guardando solo il
  punto più recente. I sei mesi già scaricati arrivavano a oggi, quindi
  passavano il controllo anche per un intervallo molto più ampio.
- **Correzione:** ora la cache basta solo se copre l'intervallo da entrambe le
  parti, con una settimana di tolleranza sull'inizio per fine settimana e
  festività.

### 5. Icona dell'app

Fatto in **0.1.1**: adottato il disegno fornito, ritagliato dal fondo a
scacchiera e composto sui cinque formati richiesti da Android più l'icona per gli
store. Il fondo è blu ardesia scuro: l'argento del conio si legge come metallo
solo se ha qualcosa di scuro dietro. Il vecchio disegno vettoriale resta come
livello monocromatico per le icone a tema di Android 13, dove una fotografia
ridotta a tinta piatta diventerebbe un cerchio e basta.

Lo script `scripts/make_icon.py` rigenera tutto dal file in `design/moneta.jpeg`.
