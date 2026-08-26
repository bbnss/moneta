"""Genera la grafica della scheda del Play Store e ripulisce gli screenshot.

Tre cose, tutte derivate da materiale che sta già nel repository:

  icona 512      la stessa composizione di make_icon.py, ma salvata con canale
                 alfa, che è quello che Play si aspetta
  immagine in    1024x500, moneta a sinistra e nome inciso a destra, sullo
  primo piano    stesso blu ardesia del fondo dell'icona adattiva
  screenshot     l'emulatore disegna nella barra di stato lo scudo di Safety
                 Center, che nell'app non c'entra nulla: va coperto

Uso:
    python3 scripts/make_play_assets.py <cartella_screenshot_grezzi>

Gli screenshot grezzi vengono letti da quella cartella e riscritti puliti in
play_store_v<versione>/screenshot/telefono/.
"""

import os
import sys

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "design", "moneta.jpeg")
OUT = os.path.join(ROOT, "play_store_v0.1.1")

# Misurati sul file, gli stessi di make_icon.py.
CX, CY, R = 511.5, 517.0, 377.0
SS = 4
BACKGROUND = (0x2B, 0x36, 0x44)
SILVER = (0xD8, 0xD9, 0xDE)
RULE = (0x4A, 0x56, 0x66)
DIM = (0x9A, 0xA6, 0xB6)

BASKERVILLE = "/System/Library/Fonts/Supplemental/Baskerville.ttc"
HELVETICA = "/System/Library/Fonts/HelveticaNeue.ttc"

TAGLINE = {
    "it-IT": ["Funziona offline dal primo avvio.",
              "Dice sempre quanto sono vecchi i tassi."],
    "en-US": ["Works offline from the very first launch.",
              "Always states how old the rates are."],
}


def coin():
    """La moneta ritagliata a cerchio, con l'alfa che elimina la scacchiera."""
    src = Image.open(SRC).convert("L")
    box = (int(CX - R) - 1, int(CY - R) - 1, int(CX + R) + 2, int(CY + R) + 2)
    disc = src.crop(box)
    size = disc.size[0]

    mask = Image.new("L", (size * SS, size * SS), 0)
    ImageDraw.Draw(mask).ellipse(
        [(CX - R - box[0]) * SS, (CY - R - box[1]) * SS,
         (CX + R - box[0]) * SS, (CY + R - box[1]) * SS],
        fill=255,
    )
    mask = mask.resize((size, size), Image.LANCZOS)
    return Image.merge("LA", (disc, mask)).convert("RGBA")


def store_icon(disc, path):
    """512x512 con alfa: il fondo dell'icona adattiva, ritagliato ai 72dp visibili."""
    full = 768
    fraction = 68.0 / 108.0
    img = Image.new("RGBA", (full, full), BACKGROUND + (255,))
    diameter = round(full * fraction)
    scaled = disc.resize((diameter, diameter), Image.LANCZOS)
    offset = (full - diameter) // 2
    img.paste(scaled, (offset, offset), scaled)
    crop = (full - 512) // 2
    img.crop((crop, crop, full - crop, full - crop)).save(path, optimize=True)
    print(path, os.path.getsize(path) // 1024, "kB")


def tracked(draw, xy, text, font, fill, tracking):
    """PIL non fa spaziatura fra le lettere: si disegna un carattere per volta."""
    x, y = xy
    for ch in text:
        draw.text((x, y), ch, font=font, fill=fill)
        x += draw.textlength(ch, font=font) + tracking
    return x - tracking


def feature_graphic(disc, locale, path):
    img = Image.new("RGB", (1024, 500), BACKGROUND)
    draw = ImageDraw.Draw(img)

    diameter = 304
    scaled = disc.resize((diameter, diameter), Image.LANCZOS)
    img.paste(scaled, (84, (500 - diameter) // 2), scaled)

    # Il nome in un serif classico e ben spaziato: è il conio di un denario,
    # non il logo di una fintech.
    name = ImageFont.truetype(BASKERVILLE, 96, index=0)
    body = ImageFont.truetype(HELVETICA, 28, index=0)
    tracking = 9

    # Play ritaglia i bordi dell'immagine in alcune superfici: il blocco di
    # testo si centra in una colonna che sta lontana dal margine destro.
    left, right = 436, 972
    widths = [sum(draw.textlength(c, font=name) + tracking for c in "MONETA") - tracking]
    widths += [draw.textlength(line, font=body) for line in TAGLINE[locale]]
    block = max(widths)
    x0 = left + (right - left - block) / 2

    tracked(draw, (x0, 136), "MONETA", name, SILVER, tracking)
    draw.line([(x0, 268), (x0 + block, 268)], fill=RULE, width=2)
    for i, line in enumerate(TAGLINE[locale]):
        draw.text((x0, 302 + i * 42), line, font=body, fill=DIM)

    img.save(path, optimize=True)
    print(path, os.path.getsize(path) // 1024, "kB")


def clean_screenshot(src_path, dst_path):
    """Copre lo scudo di Safety Center che l'emulatore mette nella barra di stato.

    Sta sempre in x 152..200, y 2..60. Il fondo della barra è una tinta piatta,
    quindi si campiona a destra dell'icona e si riempie: la barra torna com'è
    su un telefono senza quella notifica.
    """
    img = Image.open(src_path).convert("RGB")
    px = img.load()
    bg = px[620, 30]
    for x in (300, 480, 700):
        if max(abs(px[x, 30][i] - bg[i]) for i in range(3)) > 8:
            raise SystemExit(f"{src_path}: la barra di stato non è a tinta piatta")
    ImageDraw.Draw(img).rectangle([146, 0, 208, 66], fill=bg)
    img.save(dst_path, optimize=True)
    print(dst_path, img.size, os.path.getsize(dst_path) // 1024, "kB")


def main():
    disc = coin()

    grafica = os.path.join(OUT, "grafica")
    os.makedirs(grafica, exist_ok=True)
    store_icon(disc, os.path.join(grafica, "icona_512.png"))
    feature_graphic(disc, "it-IT",
                    os.path.join(grafica, "immagine_in_primo_piano_1024x500_it.png"))
    feature_graphic(disc, "en-US",
                    os.path.join(grafica, "immagine_in_primo_piano_1024x500_en.png"))

    if len(sys.argv) > 1:
        raw = sys.argv[1]
        # Play tiene screenshot separati per lingua: due cartelle, non una.
        for prefix, folder in (("it_", "telefono_it"), ("en_", "telefono_en")):
            shots = os.path.join(OUT, "screenshot", folder)
            os.makedirs(shots, exist_ok=True)
            for name in sorted(os.listdir(raw)):
                if name.startswith(prefix) and name.endswith(".png"):
                    clean_screenshot(os.path.join(raw, name),
                                     os.path.join(shots, name[len(prefix):]))


if __name__ == "__main__":
    main()
