"""Ritaglia la moneta dall'immagine fornita e genera le icone dell'app.

Il file di partenza è un JPEG: la scacchiera che sembra trasparenza è fatta di
pixel veri, quindi va rimossa. La moneta è un disco, e ritagliarla come cerchio
elimina in un colpo solo scacchiera e ombra portata — l'ombra la mette il
launcher, non ce la deve mettere l'icona.
"""

import os

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "design", "moneta.jpeg")
RES = os.path.join(ROOT, "app", "src", "main", "res")
STORE = os.path.join(ROOT, "fastlane", "metadata", "android")

# Misurati sul file: l'anello scuro del bordo sta in 135..888 x 142..892.
CX, CY, R = 511.5, 517.0, 377.0
SS = 4  # supersampling del bordo, per non avere una circonferenza a scalini

src = Image.open(SRC).convert("L")

# Ritaglio quadrato attorno al disco, con un pixel di margine.
box = (int(CX - R) - 1, int(CY - R) - 1, int(CX + R) + 2, int(CY + R) + 2)
coin = src.crop(box)
size = coin.size[0]

mask = Image.new("L", (size * SS, size * SS), 0)
ImageDraw.Draw(mask).ellipse(
    [(CX - R - box[0]) * SS, (CY - R - box[1]) * SS,
     (CX + R - box[0]) * SS, (CY + R - box[1]) * SS],
    fill=255,
)
mask = mask.resize((size, size), Image.LANCZOS)

coin_rgba = Image.merge("LA", (coin, mask))
print("moneta ritagliata:", coin_rgba.size)

# --- livello foreground dell'icona adattiva -------------------------------
# Tela di 108dp; l'area che i launcher mostrano davvero è quella centrale di
# 72dp. La moneta sta a 68dp: entra in qualunque maschera senza toccarne il
# bordo, e resta comunque grande.
DENSITIES = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}
COIN_FRACTION = 68.0 / 108.0

for suffix, canvas in DENSITIES.items():
    diameter = round(canvas * COIN_FRACTION)
    layer = Image.new("LA", (canvas, canvas), (0, 0))
    scaled = coin_rgba.resize((diameter, diameter), Image.LANCZOS)
    offset = (canvas - diameter) // 2
    layer.paste(scaled, (offset, offset))

    directory = os.path.join(RES, "mipmap-" + suffix)
    os.makedirs(directory, exist_ok=True)
    path = os.path.join(directory, "ic_launcher_foreground.png")
    layer.save(path, optimize=True)
    print(path, canvas, os.path.getsize(path) // 1024, "kB")

# --- icona per gli store ---------------------------------------------------
# Play chiede 512x512 e mostra l'icona come apparirà: si compone lo stesso
# fondo dell'icona adattiva e si ritaglia l'area visibile di 72dp su 108.
BACKGROUND = (0x2B, 0x36, 0x44)
full = 768              # 108dp a questa scala; i 72dp visibili sono 512px
store = Image.new("RGB", (full, full), BACKGROUND)
diameter = round(full * COIN_FRACTION)
scaled = coin_rgba.resize((diameter, diameter), Image.LANCZOS).convert("RGBA")
offset = (full - diameter) // 2
store.paste(scaled, (offset, offset), scaled)
crop = (full - 512) // 2
store = store.crop((crop, crop, full - crop, full - crop))

for locale in ("en-US", "it-IT"):
    directory = os.path.join(STORE, locale, "images")
    os.makedirs(directory, exist_ok=True)
    path = os.path.join(directory, "icon.png")
    store.save(path, optimize=True)
    print(path, os.path.getsize(path) // 1024, "kB")
