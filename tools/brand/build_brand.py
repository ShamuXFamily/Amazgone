#!/usr/bin/env python3
"""Build Amazgone brand assets from tools/brand/amazgone-logo-source.png.

Outputs: in-app logos (composeResources/drawable/brand_{logo,wordmark}_{light,dark}.png), the iOS app icon
(default/dark/tinted) and launch logo, and Android launcher mipmaps + adaptive foreground.
Re-run after changing the source:  python3 tools/brand/build_brand.py   (needs Pillow + numpy)
"""
from PIL import Image, ImageDraw
import numpy as np, os, json
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SRC = os.path.join(ROOT, "tools/brand/amazgone-logo-source.png")

def load_transparent():
    """White (and already-transparent) background -> alpha, with colours un-premultiplied."""
    source = Image.open(SRC).convert("RGBA")
    flat = Image.new("RGBA", source.size, (255, 255, 255, 255)); flat.alpha_composite(source)
    img = np.asarray(flat.convert("RGB")).astype(float)
    a = (255 - img.min(axis=2)) / 255.0
    a[a < 0.04] = 0
    safe = np.where(a > 0, a, 1)[..., None]
    rgb = np.clip((img - 255 * (1 - a[..., None])) / safe, 0, 255)
    return np.dstack([rgb, a * 255]).astype(np.uint8)

rgba = load_transparent()
TAG_X, TAG_Y = 770, 632          # tagline box (source pixels)
LIGHT_INK = np.array([243, 243, 243])
NAVY = (30, 27, 58)

def crop(arr, pad=8):
    ys, xs = np.nonzero(arr[..., 3] > 10)
    return arr[max(ys.min() - pad, 0):ys.max() + pad + 1, max(xs.min() - pad, 0):xs.max() + pad + 1]

def darkened(arr):
    """Black ink -> light ink for dark backgrounds; orange stays."""
    out = arr.copy()
    rgb = out[..., :3].astype(int)
    ink = (rgb.max(axis=2) - rgb.min(axis=2) < 60) & (rgb.max(axis=2) < 150)
    out[ink, :3] = LIGHT_INK
    return out

full = crop(rgba)
word = rgba.copy(); word[TAG_Y:, TAG_X:, 3] = 0; word = crop(word)
img = lambda a: Image.fromarray(np.ascontiguousarray(a))

def save_width(image, path, width):
    h = round(image.height * width / image.width)
    image.resize((width, h), Image.LANCZOS).save(path, optimize=True)

DRAW = f"{ROOT}/shared/src/commonMain/composeResources/drawable"
save_width(img(full), f"{DRAW}/brand_logo_light.png", 900)
save_width(img(darkened(full)), f"{DRAW}/brand_logo_dark.png", 900)
save_width(img(word), f"{DRAW}/brand_wordmark_light.png", 600)
save_width(img(darkened(word)), f"{DRAW}/brand_wordmark_dark.png", 600)

def icon(size, bg, logo, width_frac, dy_frac=0.0):
    canvas = Image.new("RGBA", (size, size), bg)
    w = round(size * width_frac); h = round(logo.height * w / logo.width)
    mark = logo.resize((w, h), Image.LANCZOS)
    canvas.alpha_composite(mark, ((size - w) // 2, (size - h) // 2 + round(size * dy_frac)))
    return canvas

wl, wd = img(word), img(darkened(word))
ios = f"{ROOT}/iosApp/iosApp/Assets.xcassets/AppIcon.appiconset"
icon(1024, (255, 255, 255, 255), wl, 0.86).convert("RGB").save(f"{ios}/app-icon-1024.png")
icon(1024, NAVY + (255,), wd, 0.86).convert("RGB").save(f"{ios}/app-icon-1024-dark.png")
gray = img(darkened(word)).convert("LA").convert("RGBA")
icon(1024, (0, 0, 0, 255), gray, 0.86).convert("RGB").save(f"{ios}/app-icon-1024-tinted.png")

# Android: legacy square/round mipmaps + adaptive foreground (108dp canvas, logo inside the 66dp safe zone)
RES = f"{ROOT}/androidApp/src/main/res"
for folder, px in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
    square = icon(px * 4, (255, 255, 255, 255), wl, 0.78).resize((px, px), Image.LANCZOS)
    square.save(f"{RES}/mipmap-{folder}/ic_launcher.png")
    mask = Image.new("L", (px * 4, px * 4), 0); ImageDraw.Draw(mask).ellipse((0, 0, px * 4 - 1, px * 4 - 1), fill=255)
    round_icon = icon(px * 4, (255, 255, 255, 255), wl, 0.72); round_icon.putalpha(mask)
    round_icon.resize((px, px), Image.LANCZOS).save(f"{RES}/mipmap-{folder}/ic_launcher_round.png")
    fg = px * 108 // 48
    icon(fg * 2, (0, 0, 0, 0), wl, 0.58).resize((fg, fg), Image.LANCZOS).save(f"{RES}/mipmap-{folder}/ic_launcher_foreground.png")

# iOS launch screen logo (260pt wide, light + dark appearance)
launch_dir = f"{ROOT}/iosApp/iosApp/Assets.xcassets/LaunchLogo.imageset"
os.makedirs(launch_dir, exist_ok=True)
entries = []
for variant, art in (("light", img(full)), ("dark", img(darkened(full)))):
    for scale in (1, 2, 3):
        name = f"launch-logo-{variant}@{scale}x.png"
        save_width(art, f"{launch_dir}/{name}", 260 * scale)
        entry = {"filename": name, "idiom": "universal", "scale": f"{scale}x"}
        if variant == "dark":
            entry["appearances"] = [{"appearance": "luminosity", "value": "dark"}]
        entries.append(entry)
json.dump({"images": entries, "info": {"author": "xcode", "version": 1}}, open(f"{launch_dir}/Contents.json", "w"), indent=2)
print("full", full.shape, "word", word.shape)
