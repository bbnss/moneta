#!/usr/bin/env bash
# Rigenera i tassi di partenza inclusi nell'APK.
#
# Servono perché l'app deve saper convertire già al primo avvio, anche installata
# senza rete — in aereo, o appena atterrati con la SIM ancora da attivare. Senza
# questo file la prima schermata sarebbe vuota, che è il comportamento di tutti
# i concorrenti.
#
# Il file è **committato nel repository** e non scaricato durante la build: le
# build di F-Droid devono essere riproducibili, e una build che chiama la rete
# non lo è. Va rilanciato a mano prima di ogni release.
#
#   ./scripts/refresh_seed.sh

set -euo pipefail

OUT="core/data/src/main/assets/seed_rates.json"
SRC="https://api.frankfurter.dev/v2/rates?base=EUR"

echo "Scarico i tassi da $SRC"
curl -sS --fail --max-time 30 "$SRC" \
  | python3 -c '
import json, sys, datetime
from decimal import Decimal

rows = json.load(sys.stdin, parse_float=Decimal, parse_int=Decimal)
if not rows:
    sys.exit("Risposta vuota: seed non aggiornato")

rates = {}
dates = {}
latest = ""
for row in rows:
    quote, rate, date = row.get("quote"), row.get("rate"), row.get("date", "")
    if not quote or rate is None:
        continue
    # Il tasso viene tenuto come stringa: il denaro non passa mai da un float,
    # nemmeno qui.
    rates[quote] = str(rate)
    if date: dates[quote] = date
    latest = max(latest, date)

if len(rates) < 50:
    sys.exit(f"Solo {len(rates)} valute: risposta sospetta, seed non aggiornato")

json.dump(
    {
        "providerId": 1,
        "pivot": "EUR",
        "rateDate": latest,
        "generatedAt": datetime.datetime.now(datetime.timezone.utc)
            .replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        "rates": dict(sorted(rates.items())),
        "rateDates": dict(sorted(dates.items())),
    },
    sys.stdout,
    indent=1,
    ensure_ascii=False,
)
print()
' > "$OUT.tmp"

mv "$OUT.tmp" "$OUT"
python3 -c "
import json
d = json.load(open('$OUT'))
print(f\"Scritto $OUT: {len(d['rates'])} valute, tassi al {d['rateDate']}\")
"
