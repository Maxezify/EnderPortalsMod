#!/usr/bin/env python3
"""Génère les textures PNG + blockstates/modèles de porte du mod Ender Portals.

Style « Obsidienne & vide » : palettes limitées, taches quantifiées façon
stone vanilla, fondus verticaux dithérés, contours sombres.
"""
import json
import math
import os
import random
import struct
import zlib

import pathlib
ROOT = str(pathlib.Path(__file__).resolve().parent.parent / "src" / "main" / "resources")
ASSETS = os.path.join(ROOT, "assets", "enderportals")

rng = random.Random(421)


def write_png(path, w, h, px):
    raw = b"".join(b"\x00" + b"".join(bytes(p) for p in row) for row in px)

    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(
            ">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9))
    png += chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(png)
    print("png ", path)


def canvas(w, h, color=(0, 0, 0, 0)):
    return [[tuple(color) for _ in range(w)] for _ in range(h)]


def put(px, x, y, c):
    if 0 <= y < len(px) and 0 <= x < len(px[0]):
        px[y][x] = tuple(c)


def rect(px, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put(px, x, y, c)


def outline(px, x0, y0, x1, y1, c):
    for x in range(x0, x1 + 1):
        put(px, x, y0, c)
        put(px, x, y1, c)
    for y in range(y0, y1 + 1):
        put(px, x0, y, c)
        put(px, x1, y, c)


def jitter(c, amount):
    return tuple(max(0, min(255, v + rng.randint(-amount, amount))) for v in c[:3]) + (c[3],)


def blob_noise(w, h, seed, scale=3):
    """Bruit en taches (valeur lissée), dans [0,1) — le grain « stone »."""
    r = random.Random(seed)
    gw, gh = w // scale + 2, h // scale + 2
    grid = [[r.random() for _ in range(gw)] for _ in range(gh)]
    out = []
    for y in range(h):
        gy, fy = divmod(y, scale)
        fy /= scale
        row = []
        for x in range(w):
            gx, fx = divmod(x, scale)
            fx /= scale
            v = (grid[gy][gx] * (1 - fx) * (1 - fy)
                 + grid[gy][gx + 1] * fx * (1 - fy)
                 + grid[gy + 1][gx] * (1 - fx) * fy
                 + grid[gy + 1][gx + 1] * fx * fy)
            row.append(min(0.999, max(0.0, v)))
        out.append(row)
    return out


def shade(palette, v):
    return palette[int(v * len(palette))]



# ---------------------------------------------------------------- cartes

def from_map(rows, palette, alpha=255):
    """Peint une texture depuis une carte de caractères.

    Le bruit procédural convient à de la matière brute — pierre, minerai — mais
    pas à un motif taillé : une gravure demande que chaque pixel soit voulu.
    Écrire la texture en caractères la rend relisible et modifiable à l'œil, ce
    qu'une pile de boucles n'est pas.

    Les couleurs de la palette sont des triplets RVB ; l'alpha est commun.
    """
    height = len(rows)
    width = len(rows[0])
    for row in rows:
        if len(row) != width:
            raise ValueError(f"ligne de {len(row)} caractères, attendu {width} : {row!r}")
    px = canvas(width, height)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            rgb = palette[ch]
            put(px, x, y, rgb if len(rgb) == 4 else rgb + (alpha,))
    return px

# ---------------------------------------------------------------- palettes

# Opacité commune au Bloc de l'Ender et à toute sa famille de briques : c'est
# elle qui fait qu'on voit au travers de la matière du monde.
ENDER_ALPHA = 210

# Bloc de l'Ender : gris froids sombres, translucides, grain « stone ».
P_ENDER = [(64, 66, 76, ENDER_ALPHA), (76, 79, 89, ENDER_ALPHA),
           (89, 92, 102, ENDER_ALPHA), (101, 104, 115, ENDER_ALPHA)]

# Obsidienne : noirs violacés, rehauts visibles comme la texture vanilla.
OBS = [(24, 19, 41, 255), (37, 29, 62, 255), (52, 41, 87, 255), (70, 55, 113, 255)]
OBS_DARKEST = (10, 8, 18, 255)

# Le vide : fondu du noir profond vers le violet sombre, étoiles discrètes.
VOID_BOTTOM = (8, 5, 16)
VOID_TOP = (27, 17, 48)
SPECK_TEAL = (22, 74, 66, 255)
SPECK_VIOLET = (63, 42, 99, 255)

# Œil : violet pâle, clin d'œil aux yeux des cadres de portail de l'End.
EYE_PALE = (172, 144, 214, 255)
EYE_CORE = (208, 184, 240, 255)

HANDLE = (82, 205, 184, 255)
HANDLE_D = (30, 108, 95, 255)


# ---------------------------------------------------------------- blocs

def tex_ender_block():
    """Façon stone vanilla : taches irrégulières, fondu léger, pas d'éclats."""
    noise = blob_noise(16, 16, seed=777, scale=3)
    px = canvas(16, 16)
    for y in range(16):
        for x in range(16):
            v = noise[y][x] - 0.10 * (y / 15.0)  # fondu léger, plus sombre en bas
            v = min(0.999, max(0.0, v))
            put(px, x, y, shade(P_ENDER, v))
    write_png(f"{ASSETS}/textures/block/ender_block.png", 16, 16, px)


def tex_ender_bricks():
    """Façon stone bricks vanilla : 2 rangées de gros pavés, joints sombres,
    grain en taches et rehaut haut-gauche par pavé.

    Translucides au même titre que le Bloc de l'Ender : même alpha (210), pour
    qu'un mur de briques laisse deviner ce qu'il y a derrière exactement comme
    la masse dans laquelle on le bâtit."""
    mortar = (46, 48, 56, ENDER_ALPHA)
    palette = [(88, 92, 102, ENDER_ALPHA), (99, 103, 113, ENDER_ALPHA),
               (110, 114, 124, ENDER_ALPHA)]
    highlight = (124, 128, 139, ENDER_ALPHA)
    noise = blob_noise(16, 16, seed=1212, scale=3)
    px = canvas(16, 16, mortar)
    # (x0, y0, x1, y1) intérieurs des pavés ; joints d'1 px autour.
    bricks = [(0, 0, 6, 6), (8, 0, 15, 6), (0, 8, 2, 14), (4, 8, 12, 14), (14, 8, 15, 14)]
    for (bx0, by0, bx1, by1) in bricks:
        for y in range(by0, by1 + 1):
            for x in range(bx0, bx1 + 1):
                put(px, x, y, shade(palette, noise[y][x]))
        # rehaut sur l'arête haute, ombre sur l'arête basse
        for x in range(bx0, bx1 + 1):
            put(px, x, by0, highlight if noise[by0][x] > 0.35 else palette[2])
            put(px, x, by1, palette[0])
    write_png(f"{ASSETS}/textures/block/ender_bricks.png", 16, 16, px)


def tex_inactive_door_sides():
    """Flancs/dos et chants du caisson inactif : obsidienne pleine."""
    noise = blob_noise(16, 16, seed=2727, scale=3)
    side = canvas(16, 16)
    for y in range(16):
        for x in range(16):
            put(side, x, y, shade(OBS, 0.25 + noise[y][x] * 0.74))
    outline(side, 0, 0, 15, 15, OBS_DARKEST)
    write_png(f"{ASSETS}/textures/block/inactive_door_side.png", 16, 16, side)

    top = canvas(16, 16)
    for y in range(16):
        for x in range(16):
            put(top, x, y, shade(OBS[:2], noise[15 - y][x]))
    outline(top, 0, 0, 15, 15, OBS_DARKEST)
    write_png(f"{ASSETS}/textures/block/inactive_door_top.png", 16, 16, top)


def tex_ender_ore():
    """Surcouche seule : cristaux sarcelle façon ores vanilla, fond transparent.
    La base du bloc est la texture end_stone vanilla, référencée par le modèle."""
    crystal = (64, 224, 205, 255)
    light = (170, 248, 238, 255)
    dark = (23, 130, 120, 255)
    edge = (14, 78, 70, 255)
    px = canvas(16, 16)
    # amas en croix, ombrés bas-droite comme les minerais 1.17+
    for cx, cy in ((3, 4), (11, 3), (6, 10), (12, 12), (8, 6)):
        put(px, cx, cy, crystal)
        put(px, cx - 1, cy, dark)
        put(px, cx, cy - 1, light)
        put(px, cx + 1, cy, crystal)
        put(px, cx, cy + 1, dark)
        put(px, cx + 1, cy + 1, edge)
        put(px, cx - 1, cy - 1, edge)
    write_png(f"{ASSETS}/textures/block/ender_ore_overlay.png", 16, 16, px)


# ---------------------------------------------------------------- la porte

DOOR_NOISE = blob_noise(16, 32, seed=4242, scale=3)


def void_color(g, x):
    """Couleur du panneau de vide à la ligne globale g (0 = haut, 31 = bas)."""
    t = 1.0 - g / 31.0  # 1 en haut, 0 en bas
    base = tuple(int(VOID_BOTTOM[i] + (VOID_TOP[i] - VOID_BOTTOM[i]) * t) for i in range(3))
    # dithering léger pour le fondu
    d = DOOR_NOISE[g][x]
    base = tuple(max(0, min(255, c + int((d - 0.5) * 10))) for c in base)
    return base + (255,)


def paint_door_half(px, ox, oy, top_half):
    """Une moitié de porte 16×16 : cadre obsidienne + panneau de vide."""
    row0 = 0 if top_half else 16
    for ly in range(16):
        g = row0 + ly
        for x in range(16):
            frame = x < 2 or x > 13 or g < 2 or g > 29
            if frame:
                # biais vers les nuances claires : le cadre doit se détacher du vide
                put(px, ox + x, oy + ly, shade(OBS, 0.25 + DOOR_NOISE[g][x] * 0.74))
            else:
                put(px, ox + x, oy + ly, void_color(g, x))
    # liseré intérieur sombre du cadre
    for ly in range(16):
        g = row0 + ly
        if 2 <= g <= 29:
            put(px, ox + 2, oy + ly, OBS_DARKEST)
            put(px, ox + 13, oy + ly, OBS_DARKEST)
    if top_half:
        for x in range(2, 14):
            put(px, ox + x, oy + 2, OBS_DARKEST)
        # étoiles discrètes du vide
        for (sx, sy) in ((5, 7), (10, 11), (7, 13)):
            put(px, ox + sx, oy + sy, SPECK_TEAL)
        put(px, ox + 11, oy + 6, SPECK_VIOLET)
        # l'œil, en haut au centre
        put(px, ox + 7, oy + 4, EYE_PALE)
        put(px, ox + 8, oy + 4, EYE_PALE)
        put(px, ox + 7, oy + 5, EYE_CORE)
        put(px, ox + 8, oy + 5, EYE_CORE)
        put(px, ox + 6, oy + 5, EYE_PALE)
        put(px, ox + 9, oy + 5, EYE_PALE)
        put(px, ox + 7, oy + 6, EYE_PALE)
        put(px, ox + 8, oy + 6, EYE_PALE)
    else:
        for x in range(2, 14):
            put(px, ox + x, oy + 13, OBS_DARKEST)
        # poignée sarcelle à droite
        put(px, ox + 12, oy + 1, HANDLE)
        put(px, ox + 12, oy + 2, HANDLE_D)
        # étoiles discrètes
        put(px, ox + 5, oy + 5, SPECK_VIOLET)
        put(px, ox + 9, oy + 8, SPECK_TEAL)


def paint_door_top(px, ox=0, oy=0):
    paint_door_half(px, ox, oy, True)


def paint_door_bottom(px, ox=0, oy=0):
    paint_door_half(px, ox, oy, False)


def tex_doors():
    top = canvas(16, 16)
    paint_door_top(top)
    write_png(f"{ASSETS}/textures/block/tardis_door_top.png", 16, 16, top)

    bottom = canvas(16, 16)
    paint_door_bottom(bottom)
    write_png(f"{ASSETS}/textures/block/tardis_door_bottom.png", 16, 16, bottom)


def tex_door_entity_sheet():
    px = canvas(64, 64)
    # avant : haut (0..15,0..15) + bas (0..15,16..31)
    paint_door_top(px, 0, 0)
    paint_door_bottom(px, 0, 16)
    # dos et flancs : panneau d'obsidienne plein (16..31, 0..31)
    back_noise = blob_noise(16, 32, seed=515, scale=3)
    for y in range(32):
        for x in range(16):
            c = shade(OBS, back_noise[y][x] * 0.75)  # un peu plus sombre
            put(px, 16 + x, y, c)
    outline(px, 16, 0, 31, 31, OBS_DARKEST)
    # chants : bande obsidienne sombre (32..35, 0..31)
    for y in range(32):
        for x in range(32, 36):
            put(px, x, y, shade(OBS[:2] + OBS[:1], DOOR_NOISE[y][x - 32]))
    # voile de vide (48..63, 48..63) : surface de portail de l'End
    veil_noise = blob_noise(16, 16, seed=909, scale=4)
    for y in range(16):
        for x in range(16):
            v = veil_noise[y][x]
            base = (10 + int(v * 12), 6 + int(v * 8), 20 + int(v * 20), 240)
            put(px, 48 + x, 48 + y, base)
    for (sx, sy) in ((51, 50), (58, 53), (54, 57), (60, 60), (50, 60), (56, 51)):
        put(px, sx, sy, SPECK_TEAL if (sx + sy) % 2 == 0 else SPECK_VIOLET)
    write_png(f"{ASSETS}/textures/entity/tardis_door.png", 64, 64, px)


# ---------------------------------------------------------------- objets

def tex_ender_crystal():
    px = canvas(16, 16)
    dark = (20, 120, 112, 255)
    mid = (64, 224, 205, 255)
    light = (180, 250, 240, 255)
    shard = [
        "......X.........",
        ".....XMX........",
        "....XMLMX.......",
        "....XMLLMX......",
        "...XMLLLMX......",
        "...XMLLMMX......",
        "..XMLLMMX.......",
        "..XMLMMX........",
        ".XMLMMX.........",
        ".XMMMX..........",
        ".XMMX...........",
        ".XMX............",
        ".XX.............",
        "................",
        "................",
        "................",
    ]
    colors = {"X": dark, "M": mid, "L": light}
    for y, row in enumerate(shard):
        for x, ch in enumerate(row):
            if ch in colors:
                put(px, x + 2, y + 1, colors[ch])
    put(px, 12, 3, light)
    put(px, 13, 12, mid)
    put(px, 4, 13, light)
    write_png(f"{ASSETS}/textures/item/ender_crystal.png", 16, 16, px)


def tex_tardis_key():
    """Clé dorée vanilla-style, à 45°, gemme sarcelle sertie dans l'anneau."""
    GOLD_L = (252, 225, 112, 255)
    GOLD = (233, 177, 45, 255)
    GOLD_D = (180, 126, 20, 255)
    GOLD_DD = (122, 83, 12, 255)
    GEM = (93, 206, 186, 255)
    GEM_D = (27, 124, 108, 255)
    OUT = (43, 32, 12, 255)

    fill = {}

    def p(x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            fill[(x, y)] = c

    # tige diagonale (2 px de large), de l'anneau vers la pointe en haut à droite
    for i in range(9):
        x, y = 5 + i, 10 - i
        p(x, y, GOLD)
        p(x + 1, y, GOLD_D)
    # éclats sur l'arête haut-gauche de la tige
    p(7, 7, GOLD_L)
    p(10, 4, GOLD_L)
    p(13, 2, GOLD_L)
    # panneton : deux dents vers le bas-droite, près de la pointe
    p(13, 4, GOLD_D)
    p(14, 5, GOLD_DD)
    p(11, 6, GOLD_D)
    p(12, 7, GOLD_DD)
    # anneau autour de (4,11)
    for dx in range(-3, 4):
        for dy in range(-3, 4):
            d2 = dx * dx + dy * dy
            if 3 <= d2 <= 8:
                shine = (dx + dy) < 0
                if d2 >= 7:
                    c = GOLD_D if shine else GOLD_DD
                else:
                    c = GOLD if shine else GOLD_D
                p(4 + dx, 11 + dy, c)
    p(2, 9, GOLD_L)
    # gemme sertie au centre de l'anneau
    p(4, 11, GEM)
    p(5, 12, GEM_D)

    px = canvas(16, 16)
    for (x, y), c in fill.items():
        put(px, x, y, c)
    # contour sombre automatique, façon items vanilla
    for y in range(16):
        for x in range(16):
            if px[y][x][3] == 0 and any(
                    (x + dx, y + dy) in fill
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                put(px, x, y, OUT)
    write_png(f"{ASSETS}/textures/item/tardis_key.png", 16, 16, px)


def tex_ender_pickaxe():
    px = canvas(16, 16)
    handle = (124, 84, 44, 255)
    handle_d = (86, 56, 28, 255)
    mid = (64, 224, 205, 255)
    dark = (23, 130, 120, 255)
    light = (180, 250, 240, 255)
    for i in range(9):  # manche
        put(px, 3 + i, 13 - i, handle)
        put(px, 4 + i, 13 - i, handle_d)
    head = [(2, 4), (3, 3), (4, 2), (5, 2), (6, 1), (7, 1), (8, 1), (9, 1),
            (10, 2), (11, 2), (12, 3), (13, 4), (2, 5), (13, 5)]
    for (x, y) in head:
        put(px, x, y, mid)
    for (x, y) in ((2, 6), (13, 6), (2, 4), (13, 4)):
        put(px, x, y, dark)
    for (x, y) in ((6, 2), (7, 2), (8, 2)):
        put(px, x, y, light)
    write_png(f"{ASSETS}/textures/item/ender_pickaxe.png", 16, 16, px)


def tex_inactive_door_item():
    """Icône : porte obsidienne miniature au panneau de vide."""
    px = canvas(16, 16)
    noise = blob_noise(16, 16, seed=333, scale=3)
    for y in range(1, 15):
        for x in range(4, 12):
            frame = x < 5 or x > 10 or y < 2 or y > 13
            if frame:
                put(px, x, y, shade(OBS, 0.25 + noise[y][x] * 0.74))
            else:
                t = 1.0 - y / 15.0
                base = tuple(int(VOID_BOTTOM[i] + (VOID_TOP[i] - VOID_BOTTOM[i]) * t)
                             for i in range(3))
                put(px, x, y, base + (255,))
    outline(px, 4, 1, 11, 14, OBS_DARKEST)
    put(px, 7, 4, EYE_PALE)
    put(px, 8, 4, EYE_PALE)
    put(px, 7, 5, EYE_CORE)
    put(px, 8, 5, EYE_PALE)
    put(px, 10, 8, HANDLE)
    put(px, 6, 10, SPECK_TEAL)
    write_png(f"{ASSETS}/textures/item/inactive_tardis_door.png", 16, 16, px)


def tex_centralizer():
    """Cabinet d'ordinateur obsidienne : panneau de voyants sarcelle/violet
    sur les 4 côtés, grille métal sur le dessus/dessous."""
    metal = [(52, 55, 63, 255), (66, 70, 79, 255), (82, 86, 96, 255)]
    lamp_off = (26, 30, 38, 255)
    teal = (64, 224, 205, 255)
    violet = (150, 110, 210, 255)
    amber = (240, 176, 64, 255)
    lamps = [teal, violet, amber]

    # --- côté : panneau sombre + rangées de voyants ---
    side = canvas(16, 16)
    noise = blob_noise(16, 16, seed=5150, scale=4)
    for y in range(16):
        for x in range(16):
            put(side, x, y, shade(OBS, 0.2 + noise[y][x] * 0.5))
    outline(side, 0, 0, 15, 15, OBS_DARKEST)
    # bandeau de voyants (grille 2px) + quelques allumés
    for ry, gy in enumerate(range(3, 13, 3)):
        for rx, gx in enumerate(range(3, 14, 2)):
            on = (rx * 3 + ry * 5) % 4 == 0
            c = lamps[(rx + ry) % 3] if on else lamp_off
            put(side, gx, gy, c)
            put(side, gx, gy + 1, OBS_DARKEST)
    # rail lumineux bas
    for x in range(2, 14):
        put(side, x, 14, shade(metal, 0.5))
    write_png(f"{ASSETS}/textures/block/centralizer_side.png", 16, 16, side)

    # --- dessus/dessous : grille d'aération métal ---
    top = canvas(16, 16)
    for y in range(16):
        for x in range(16):
            shine = 0.35 + 0.35 * ((x // 2 + y // 2) % 2)
            put(top, x, y, shade(metal, shine))
    outline(top, 0, 0, 15, 15, OBS_DARKEST)
    for y in range(2, 15, 3):
        for x in range(2, 14):
            put(top, x, y, lamp_off)
    put(top, 8, 8, teal)
    write_png(f"{ASSETS}/textures/block/centralizer_top.png", 16, 16, top)


def tex_ender_bag():
    """Sac sombre bombé, cordon serré, œil de l'Ender sarcelle sur le rabat."""
    cloth = [(30, 26, 46, 255), (42, 35, 62, 255), (55, 46, 82, 255)]
    tie = (70, 58, 40, 255)
    teal = (72, 214, 196, 255)
    teal_d = (26, 120, 108, 255)
    pale = (170, 248, 238, 255)
    out = (12, 10, 20, 255)
    px = canvas(16, 16)
    noise = blob_noise(16, 16, seed=3690, scale=3)
    # corps du sac : ovale bombé (lignes 5..15)
    body = [
        (5, 4, 10), (4, 5, 11), (3, 6, 12), (3, 7, 12), (2, 8, 13),
        (2, 9, 13), (2, 10, 13), (2, 11, 13), (3, 12, 12), (3, 13, 12), (4, 14, 11),
    ]
    for (x0, y, x1) in body:
        for x in range(x0, x1 + 1):
            put(px, x, y, shade(cloth, noise[y][x]))
    # goulot / cordon (lignes 2..4)
    for x in range(6, 10):
        put(px, x, 2, tie)
        put(px, x, 3, shade(cloth, 0.7))
    put(px, 5, 3, tie)
    put(px, 10, 3, tie)
    # ombre bas
    for x in range(4, 12):
        put(px, x, 14, shade(cloth, 0.1))
    # œil de l'Ender sur le rabat
    put(px, 7, 8, teal); put(px, 8, 8, teal)
    put(px, 7, 9, teal); put(px, 8, 9, teal)
    put(px, 6, 8, teal_d); put(px, 9, 9, teal_d)
    put(px, 7, 8, pale)
    # contour sombre auto
    fill = {(x, y) for y in range(16) for x in range(16) if px[y][x][3] > 0}
    for (x, y) in list(fill):
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + dx, y + dy) not in fill and 0 <= x + dx < 16 and 0 <= y + dy < 16:
                put(px, x + dx, y + dy, out)
    write_png(f"{ASSETS}/textures/item/ender_bag.png", 16, 16, px)


def tex_icon():
    s = 128
    px = canvas(s, s, (10, 8, 20, 255))
    for _ in range(70):  # étoiles violettes discrètes
        x, y = rng.randint(0, s - 1), rng.randint(0, s - 1)
        put(px, x, y, (98, 76, 150, 255))
    # halo sarcelle autour de la porte
    for y in range(s):
        for x in range(s):
            d = ((x - 64) ** 2 + (y - 66) ** 2) ** 0.5
            if 34 < d < 46:
                r, g, b, a = px[y][x]
                put(px, x, y, (min(255, r + 10), min(255, g + 52), min(255, b + 46), 255))
    # porte obsidienne
    noise = blob_noise(64, 96, seed=808, scale=8)
    for y in range(22, 111):
        for x in range(40, 89):
            frame = x < 48 or x > 80 or y < 30 or y > 102
            if frame:
                put(px, x, y, shade(OBS, noise[y - 22][x - 40]))
            else:
                t = 1.0 - (y - 30) / 72.0
                base = tuple(int(VOID_BOTTOM[i] + (VOID_TOP[i] - VOID_BOTTOM[i]) * t)
                             for i in range(3))
                put(px, x, y, base + (255,))
    outline(px, 40, 22, 88, 110, OBS_DARKEST)
    outline(px, 47, 29, 81, 103, OBS_DARKEST)
    # l'œil
    rect(px, 60, 38, 68, 42, EYE_PALE)
    rect(px, 62, 39, 66, 41, EYE_CORE)
    # étoiles dans le vide
    for (sx, sy) in ((55, 60), (72, 52), (64, 80), (52, 92), (76, 88)):
        rect(px, sx, sy, sx + 1, sy + 1, SPECK_TEAL)
    put(px, 78, 66, HANDLE)
    put(px, 78, 67, HANDLE)
    write_png(f"{ASSETS}/icon.png", s, s, px)


# ---------------------------------------------------------------- JSON portes

def door_blockstate():
    """Blockstate du caisson inactif : 4 orientations × 2 moitiés."""
    base_y = {"north": 0, "east": 90, "south": 180, "west": 270}
    variants = {}
    for facing, y in base_y.items():
        for half, model in (("lower", "inactive_door_lower"), ("upper", "inactive_door_upper")):
            entry = {"model": f"enderportals:block/{model}"}
            if y:
                entry["y"] = y
            variants[f"facing={facing},half={half}"] = entry
    path = f"{ASSETS}/blockstates/inactive_tardis_door.json"
    with open(path, "w") as f:
        json.dump({"variants": variants}, f, indent=2, sort_keys=True)
    print("json", path)


def door_models():
    """Deux cubes pleins : face nord = porte, reste = obsidienne."""
    for half, front in (("lower", "tardis_door_bottom"), ("upper", "tardis_door_top")):
        path = f"{ASSETS}/models/block/inactive_door_{half}.json"
        with open(path, "w") as f:
            json.dump({
                "parent": "minecraft:block/cube",
                "textures": {
                    "particle": "enderportals:block/inactive_door_side",
                    "north": f"enderportals:block/{front}",
                    "south": "enderportals:block/inactive_door_side",
                    "east": "enderportals:block/inactive_door_side",
                    "west": "enderportals:block/inactive_door_side",
                    "up": "enderportals:block/inactive_door_top",
                    "down": "enderportals:block/inactive_door_top",
                },
            }, f, indent=2)
        print("json", path)


def tex_chiseled_ender_bricks():
    """Brique ciselée : deux cadres gigognes, un logement creusé et l'œil des
    cadres de portail gravé au centre, quatre clous d'or aux angles.

    Écrite pixel par pixel plutôt que tirée d'un bruit : c'est une gravure, et
    une gravure ne se laisse pas générer au hasard."""
    palette = {
        "#": (40, 42, 50),      # arête sombre
        "=": (72, 76, 86),      # cadre
        ",": (74, 78, 88),      # creux
        ".": (88, 92, 102),     # fond du panneau
        "o": (54, 46, 74),      # ombre de l'œil
        "O": (172, 144, 214),   # amande pâle
        "X": (208, 184, 240),   # cœur
        "*": (206, 172, 96),    # clou d'or
    }
    rows = [
        "################",
        "#*============*#",
        "#=.,,,,,,,,,,.=#",
        "#=,..........,=#",
        "#=,.,======,.,=#",
        "#=,.=......=.,=#",
        "#=,.=.oOOo.=.,=#",
        "#=,.=.OXXO.=.,=#",
        "#=,.=.OXXO.=.,=#",
        "#=,.=.oOOo.=.,=#",
        "#=,.=......=.,=#",
        "#=,.,======,.,=#",
        "#=,..........,=#",
        "#=.,,,,,,,,,,.=#",
        "#*============*#",
        "################",
    ]
    px = from_map(rows, palette, ENDER_ALPHA)
    write_png(f"{ASSETS}/textures/block/chiseled_ender_bricks.png", 16, 16, px)


# ------------------------------------------------- famille des briques

TRANSLUCENT = "minecraft:translucent"
BRICK_TEX = "enderportals:block/ender_bricks"


def _model(name, parent, textures):
    path = f"{ASSETS}/models/block/{name}.json"
    body = {"parent": parent, "render_type": TRANSLUCENT, "textures": textures}
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(body, f, indent=2)
        f.write("\n")
    print("json", path)


def _item_model(name, parent):
    path = f"{ASSETS}/models/item/{name}.json"
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump({"parent": parent}, f, indent=2)
        f.write("\n")
    print("json", path)


def _blockstate(name, body):
    path = f"{ASSETS}/blockstates/{name}.json"
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(body, f, indent=2)
        f.write("\n")
    print("json", path)


def brick_family_models():
    """Modèles de la famille : on hérite des gabarits vanilla, en n'ajoutant
    que la texture et la couche de rendu translucide."""
    faces = {"bottom": BRICK_TEX, "top": BRICK_TEX, "side": BRICK_TEX}
    _model("ender_bricks", "minecraft:block/cube_all", {"all": BRICK_TEX})
    _model("chiseled_ender_bricks", "minecraft:block/cube_all",
           {"all": "enderportals:block/chiseled_ender_bricks"})
    _model("ender_brick_stairs", "minecraft:block/stairs", faces)
    _model("ender_brick_stairs_inner", "minecraft:block/inner_stairs", faces)
    _model("ender_brick_stairs_outer", "minecraft:block/outer_stairs", faces)
    _model("ender_brick_slab", "minecraft:block/slab", faces)
    _model("ender_brick_slab_top", "minecraft:block/slab_top", faces)
    _model("ender_brick_wall_post", "minecraft:block/template_wall_post", {"wall": BRICK_TEX})
    _model("ender_brick_wall_side", "minecraft:block/template_wall_side", {"wall": BRICK_TEX})
    _model("ender_brick_wall_side_tall", "minecraft:block/template_wall_side_tall",
           {"wall": BRICK_TEX})
    _model("ender_brick_wall_inventory", "minecraft:block/wall_inventory", {"wall": BRICK_TEX})

    _item_model("ender_bricks", "enderportals:block/ender_bricks")
    _item_model("chiseled_ender_bricks", "enderportals:block/chiseled_ender_bricks")
    _item_model("ender_brick_stairs", "enderportals:block/ender_brick_stairs")
    _item_model("ender_brick_slab", "enderportals:block/ender_brick_slab")
    _item_model("ender_brick_wall", "enderportals:block/ender_brick_wall_inventory")


# Rotation de base des escaliers, par orientation.
_STAIR_Y = {"east": 0, "south": 90, "west": 180, "north": 270}
# Modèle et décalage de rotation par forme, pour la moitié basse puis haute.
_STAIR_SHAPES = {
    "straight": ("ender_brick_stairs", 0, 0),
    "inner_right": ("ender_brick_stairs_inner", 0, 90),
    "inner_left": ("ender_brick_stairs_inner", -90, 0),
    "outer_right": ("ender_brick_stairs_outer", 0, 90),
    "outer_left": ("ender_brick_stairs_outer", -90, 0),
}


def brick_family_blockstates():
    # --- escaliers : 4 orientations x 2 moitiés x 5 formes.
    variants = {}
    for half in ("bottom", "top"):
        for shape, (model, off_bottom, off_top) in _STAIR_SHAPES.items():
            offset = off_bottom if half == "bottom" else off_top
            for facing, base in _STAIR_Y.items():
                entry = {"model": f"enderportals:block/{model}"}
                if half == "top":
                    entry["x"] = 180
                y = (base + offset) % 360
                if y:
                    entry["y"] = y
                if len(entry) > 1:
                    entry["uvlock"] = True
                variants[f"facing={facing},half={half},shape={shape}"] = entry
    _blockstate("ender_brick_stairs", {"variants": variants})

    # --- dalles : la double reprend le bloc plein.
    _blockstate("ender_brick_slab", {"variants": {
        "type=bottom": {"model": "enderportals:block/ender_brick_slab"},
        "type=top": {"model": "enderportals:block/ender_brick_slab_top"},
        "type=double": {"model": "enderportals:block/ender_bricks"},
    }})

    # --- murs : poteau central plus une branche par côté, basse ou haute.
    parts = [{"when": {"up": "true"},
              "apply": {"model": "enderportals:block/ender_brick_wall_post"}}]
    for suffix, model in (("low", "ender_brick_wall_side"),
                          ("tall", "ender_brick_wall_side_tall")):
        for side, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            apply = {"model": f"enderportals:block/{model}"}
            if y:
                apply["y"] = y
            apply["uvlock"] = True
            parts.append({"when": {side: suffix}, "apply": apply})
    _blockstate("ender_brick_wall", {"multipart": parts})

    _blockstate("chiseled_ender_bricks", {"variants": {
        "": {"model": "enderportals:block/chiseled_ender_bricks"}}})


# ============================================================ Passage des Alliés

# Palette « Paradis » : quartz laiteux, veines dorées. Volontairement à
# l'opposé de l'obsidienne de la Porte de l'Ender — deux portes, deux mondes.
P_PARADISE = [(196, 190, 176, 255), (214, 208, 194, 255), (231, 226, 213, 255),
              (243, 239, 229, 255)]
PARADISE_DARK = (138, 132, 118, 255)
PARADISE_SEAM = (206, 172, 96, 255)
PARADISE_SEAM_LIT = (240, 214, 138, 255)

# Voile du passage : or pâle lumineux, presque blanc au cœur. Six valeurs et
# non trois — trois marches donnaient des anneaux en escalier.
VEIL_DORMANT = [(38, 36, 44, 255), (46, 44, 52, 255), (54, 51, 60, 255),
                (62, 58, 68, 255), (70, 66, 76, 255), (80, 75, 88, 255)]
VEIL_WAKING = [(92, 78, 56, 214), (120, 104, 74, 214), (148, 128, 90, 214),
               (176, 152, 106, 214), (204, 178, 122, 214), (230, 206, 152, 214)]
VEIL_OPEN = [(176, 152, 100, 206), (206, 180, 122, 206), (226, 202, 148, 206),
             (240, 220, 172, 206), (250, 236, 200, 206), (255, 250, 232, 206)]


def tex_paradise_frame():
    """Cadre du Passage : quartz chanfreiné, quatre logements creusés et une
    incrustation d'or en croix — la croix se raccorde d'un bloc à l'autre, si
    bien qu'une arche montée sur plusieurs blocs dessine un réseau continu."""
    palette = {
        "#": (110, 104, 92),    # arête
        "+": (231, 226, 213),   # rehaut
        ".": (214, 208, 194),   # quartz
        "=": (196, 190, 176),   # quartz moyen
        ",": (170, 164, 150),   # creux
        "g": (206, 172, 96),    # or
        "G": (240, 214, 138),   # or éclairé
    }
    rows = [
        "################",
        "#++++++++++++++#",
        "#+.....gG.....+#",
        "#+.,,,.gG.,,,.+#",
        "#+.,=,.GG.,=,.+#",
        "#+.,,,.gG.,,,.+#",
        "#+.....gG.....+#",
        "#ggggggGGgggggg#",
        "#GGGGGGGGGGGGGG#",
        "#+.....Gg.....+#",
        "#+.,,,.Gg.,,,.+#",
        "#+.,=,.GG.,=,.+#",
        "#+.,,,.Gg.,,,.+#",
        "#+.....Gg.....+#",
        "#++++++++++++++#",
        "################",
    ]
    write_png(f"{ASSETS}/textures/block/ally_passage_frame.png", 16, 16,
              from_map(rows, palette))


def _veil_tile(name, palette, rings, sparks=0, core_radius=0.0):
    """Le voile du passage : des anneaux concentriques, comme le portail de l'End.

    Le bruit ne pèse ici que 0,08 : à la moitié du poids qu'il avait, il
    ondulait les anneaux au point de les dissoudre, et la texture se lisait comme
    du sable. Une ondulation sinusoïdale franche déforme les cercles sans les
    effacer — c'est elle qui les empêche de ressembler à une cible.

    Rend la tuile 16×16 sans l'écrire : elle sert deux fois, une fois comme
    texture de bloc (modèle d'inventaire) et une fois recopiée dans la planche
    d'entité du caisson.
    """
    px = canvas(16, 16)
    noise = blob_noise(16, 16, seed=seed_of(name), scale=6)
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            distance = (dx * dx + dy * dy) ** 0.5 / 10.6
            if distance <= core_radius:
                put(px, x, y, palette[-1])
                continue
            angle = math.atan2(dy, dx)
            # L'ondulation dépend de l'angle : les anneaux se tordent au lieu de
            # rester des cercles parfaits.
            ripple = 0.07 * math.sin(angle * 3.0) + 0.05 * math.sin(distance * 12.0)
            v = (distance * rings + ripple + noise[y][x] * 0.08) % 1.0
            put(px, x, y, shade(palette, min(0.999, max(0.0, v))))
    if sparks:
        spark = random.Random(seed_of(name) ^ 0x5EED)
        for _ in range(sparks):
            put(px, spark.randrange(1, 15), spark.randrange(1, 15), palette[-1])
    return px


def _veil(name, palette, rings, sparks=0, core_radius=0.0):
    write_png(f"{ASSETS}/textures/block/{name}.png", 16, 16,
              _veil_tile(name, palette, rings, sparks, core_radius))


def seed_of(name):
    """Graine stable déduite du nom : la texture est la même à chaque exécution."""
    return sum((i + 1) * ord(c) for i, c in enumerate(name))


def tex_paradise_veils():
    """Seul le voile dormant est écrit en texture de bloc : c'est le seul qu'un
    modèle référence encore, celui de l'arche miniature d'inventaire. Les deux
    autres ne vivent que dans la planche du caisson, où ils sont recopiés depuis
    la même définition (voir tex_passage_entity_sheet)."""
    _veil("ally_veil_closed", VEIL_DORMANT, rings=2.5)


# ------------------------------------------------ planche du caisson du Passage

# Le caisson du Passage est dessiné par un BlockEntityRenderer, comme celui de
# la Porte de l'Ender : une seule planche 64×64 porte toutes ses faces.
#
#   (0,0)-(16,32)   façade close : moitié haute puis moitié basse
#   (16,0)-(32,32)  dos et flancs : panneau de quartz veiné d'or
#   (32,0)-(36,32)  chants : bande étroite
#   (36,0)-(52,16)  voile d'ouverture
#   (36,16)-(52,32) voile ouvert
#   (48,48)-(64,64) plaque sombre du panneau de pseudo

PASSAGE_NOISE = blob_noise(16, 32, seed=7331, scale=3)

P_QUARTZ = [(148, 142, 128, 255), (178, 172, 158, 255),
            (206, 200, 186, 255), (231, 226, 213, 255)]
PANEL_TOP = (250, 246, 236)
PANEL_BOTTOM = (206, 198, 180)
GOLD = (206, 172, 96, 255)
GOLD_LIT = (240, 214, 138, 255)
GOLD_CORE = (255, 250, 232, 255)
PLATE_DARK = (46, 42, 52, 255)


def passage_panel_color(g, x):
    """Couleur du panneau de la façade à la ligne globale g (0 = haut, 31 = bas).

    Un dégradé du haut vers le bas plutôt qu'un aplat : c'est ce qui donne au
    quartz sa profondeur, et c'est le pendant clair du dégradé de vide de la
    Porte de l'Ender."""
    t = 1.0 - g / 31.0
    base = tuple(int(PANEL_BOTTOM[i] + (PANEL_TOP[i] - PANEL_BOTTOM[i]) * t) for i in range(3))
    d = PASSAGE_NOISE[g][x]
    base = tuple(max(0, min(255, c + int((d - 0.5) * 12))) for c in base)
    return base + (255,)


def paint_passage_half(px, ox, oy, top_half):
    """Une moitié de façade 16×16 : cadre de quartz, liseré d'or, panneau clair.

    La façade ne se voit que passage clos — ouvert, l'embrasure est vide. Elle
    porte donc ce qui identifie l'arche au repos : la clé de voûte en haut, les
    deux anneaux d'alliance en bas."""
    row0 = 0 if top_half else 16
    for ly in range(16):
        g = row0 + ly
        for x in range(16):
            frame = x < 2 or x > 13 or g < 2 or g > 29
            if frame:
                put(px, ox + x, oy + ly, shade(P_QUARTZ, 0.22 + PASSAGE_NOISE[g][x] * 0.77))
            else:
                put(px, ox + x, oy + ly, passage_panel_color(g, x))
    # Liseré d'or intérieur : il court sur les deux moitiés sans rupture.
    for ly in range(16):
        g = row0 + ly
        if 2 <= g <= 29:
            put(px, ox + 2, oy + ly, GOLD)
            put(px, ox + 13, oy + ly, GOLD)
    if top_half:
        for x in range(2, 14):
            put(px, ox + x, oy + 2, GOLD)
        # Clé de voûte : un soleil d'or au sommet de l'arche.
        for x in (7, 8):
            put(px, ox + x, oy + 4, GOLD_LIT)
            put(px, ox + x, oy + 5, GOLD_CORE)
            put(px, ox + x, oy + 6, GOLD_LIT)
        for x, y in ((6, 5), (9, 5), (6, 4), (9, 6), (7, 3), (8, 7)):
            put(px, ox + x, oy + y, GOLD)
        # Rayons courts, en diagonale.
        for x, y in ((5, 3), (10, 3), (5, 7), (10, 7)):
            put(px, ox + x, oy + y, PARADISE_SEAM)
    else:
        for x in range(2, 14):
            put(px, ox + x, oy + 13, GOLD)
        # Deux anneaux entrelacés : l'alliance, au bas de la façade.
        for x, y in ((4, 5), (5, 5), (3, 6), (6, 6), (3, 7), (6, 7), (4, 8), (5, 8)):
            put(px, ox + x, oy + y, GOLD_LIT)
        for x, y in ((9, 5), (10, 5), (8, 6), (11, 6), (8, 7), (11, 7), (9, 8), (10, 8)):
            put(px, ox + x, oy + y, GOLD)


def tex_passage_entity_sheet():
    px = canvas(64, 64)
    # Façade : haut (0..15, 0..15) puis bas (0..15, 16..31).
    paint_passage_half(px, 0, 0, True)
    paint_passage_half(px, 0, 16, False)

    # Dos et flancs : quartz plein, un peu plus sourd que la façade, deux
    # coutures d'or horizontales pour que le mur ne soit pas un aplat.
    back_noise = blob_noise(16, 32, seed=1717, scale=3)
    for y in range(32):
        for x in range(16):
            put(px, 16 + x, y, shade(P_QUARTZ, 0.30 + back_noise[y][x] * 0.65))
    for y in (7, 23):
        for x in range(16):
            put(px, 16 + x, y, GOLD if x % 4 else GOLD_LIT)
    outline(px, 16, 0, 31, 31, (110, 104, 92, 255))

    # Chants : bande étroite, filet d'or au milieu.
    for y in range(32):
        for x in range(32, 36):
            put(px, x, y, shade(P_QUARTZ[:3], back_noise[y][x - 32]))
        put(px, 33, y, GOLD if y % 3 else GOLD_LIT)

    # Les deux voiles, recopiés depuis les tuiles de bloc : une seule définition
    # du motif, deux emplois.
    for uy, tile in ((0, _veil_tile("ally_veil_opening", VEIL_WAKING, 3.0, 4, 0.12)),
                     (16, _veil_tile("ally_veil_open", VEIL_OPEN, 3.5, 9, 0.20))):
        for y in range(16):
            for x in range(16):
                put(px, 36 + x, uy + y, tile[y][x])

    # Plaque sombre du panneau de pseudo, avec son liseré d'or.
    for y in range(16):
        for x in range(16):
            put(px, 48 + x, 48 + y, PLATE_DARK)
    outline(px, 48, 48, 63, 63, GOLD)

    write_png(f"{ASSETS}/textures/entity/ally_passage.png", 64, 64, px)


def tex_friendship_console():
    """Les trois faces du panneau, dessinées comme un appareil et non comme un
    aplat : biseau clair en haut à gauche, ombre en bas à droite, vis d'or aux
    angles, et sur la face avant un carnet creusé, un écran violet et un pavé de
    neuf touches à liseré."""
    edge = (52, 50, 58, 255)
    body = (196, 190, 176, 255)
    body_light = (231, 226, 213, 255)
    body_shade = (162, 156, 142, 255)
    gold = (206, 172, 96, 255)
    gold_lit = (240, 214, 138, 255)
    inset = (28, 24, 42, 255)
    inset_edge = (58, 52, 78, 255)
    line = (150, 140, 180, 255)
    screen = (58, 44, 104, 255)
    screen_lit = (138, 112, 208, 255)
    key = (56, 54, 62, 255)
    key_edge = (108, 104, 116, 255)
    green = (74, 158, 82, 255)
    green_lit = (128, 214, 132, 255)

    def shell():
        px = canvas(16, 16, body)
        outline(px, 0, 0, 15, 15, edge)
        for i in range(1, 15):
            put(px, i, 1, body_light)
            put(px, 1, i, body_light)
            put(px, i, 14, body_shade)
            put(px, 14, i, body_shade)
        return px

    # ------------------------------------------------------------ avant
    px = shell()
    for (sx, sy) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        put(px, sx, sy, gold)
    # Le carnet, creusé, avec ses lignes de pseudos.
    rect(px, 3, 4, 6, 12, inset)
    outline(px, 3, 4, 6, 12, inset_edge)
    for y in (5, 8, 11):
        rect(px, 4, y, 6, y, line)
    # L'écran, avec sa ligne éclairée en haut.
    rect(px, 8, 4, 12, 5, screen)
    rect(px, 8, 4, 12, 4, screen_lit)
    # Trois colonnes de touches, chacune avec son liseré à droite.
    for kx in (8, 10, 12):
        for ky in (7, 9, 11):
            put(px, kx, ky, key)
            put(px, kx + 1, ky, key_edge)
    # La barre de validation.
    rect(px, 8, 13, 12, 13, green)
    put(px, 8, 13, green_lit)
    write_png(f"{ASSETS}/textures/block/friendship_console_front.png", 16, 16, px)

    # ------------------------------------------------------------ flancs
    side = shell()
    # Coutures d'or en haut et en bas, grille d'aération au centre.
    rect(side, 3, 3, 12, 3, gold)
    rect(side, 3, 12, 12, 12, gold)
    for y in (6, 8, 10):
        for x in range(4, 12):
            put(side, x, y, inset if x % 2 == 0 else inset_edge)
    for (sx, sy) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        put(side, sx, sy, body_shade)
    write_png(f"{ASSETS}/textures/block/friendship_console_side.png", 16, 16, side)

    # ------------------------------------------------------------ dessus
    top = shell()
    outline(top, 4, 4, 11, 11, gold)
    put(top, 4, 4, gold_lit)
    put(top, 11, 11, gold_lit)
    rect(top, 6, 6, 9, 9, screen)
    rect(top, 6, 6, 7, 7, screen_lit)
    write_png(f"{ASSETS}/textures/block/friendship_console_top.png", 16, 16, top)


# Géométrie de l'interface. Doit rester d'accord avec FriendshipConsoleScreen :
# c'est ici que sont creusés les logements des touches, et une touche posée
# ailleurs que son logement se verrait immédiatement.
GUI_W, GUI_H = 220, 234
GUI_PAD_X, GUI_KEYS_Y = 112, 36
GUI_KEY_W, GUI_KEY_H, GUI_KEY_GAP = 30, 24, 3
GUI_ACTION_W, GUI_ACTION_H = 46, 20
# Neuf touches, trois rangées pleines : les codes ne contiennent aucun zéro,
# donc le pavé n'a pas de dixième touche.
GUI_KEY_CELLS = [(i % 3, i // 3) for i in range(9)]
GUI_ACTIONS_Y = GUI_KEYS_Y + 3 * (GUI_KEY_H + GUI_KEY_GAP) + 1

# Le terminal, en bas, sur toute la largeur utile.
GUI_TERM_X, GUI_TERM_W = 8, 204
GUI_TERM_Y, GUI_TERM_H = 162, 64
GUI_TERM_HEADER_H = 12
# Bande d'état, en bas de l'encart : la cause s'y lit en permanence.
GUI_TERM_STATUS_H = 10

# La planche de sprites tient dans les marges laissées libres par le panneau :
# la colonne à sa droite (x ≥ GUI_W) porte le sablier et les deux touches, la
# bande sous lui (y ≥ GUI_H) les quatre boutons d'action, trop larges pour la
# colonne. Le panneau ayant grandi pour loger le terminal, la planche ne peut
# plus occuper la bande y = 192 : elle y serait recouverte par le panneau.
GUI_HOURGLASS_X, GUI_HOURGLASS_Y = 224, 0
GUI_KEY_SPRITE_X = 222
GUI_KEY_SPRITE_Y, GUI_KEY_HOVER_SPRITE_Y = 20, 48
GUI_ACTION_SPRITE_Y = 234
GUI_ACTION_SPRITE_X = (0, 48, 96, 144)


def _outside_panel(x, y, w, h, what):
    """Un sprite posé sous le panneau serait simplement écrasé par lui, et ce
    sont les boutons de l'interface qui disparaîtraient — sans rien signaler.
    Le défaut est arrivé en agrandissant le panneau pour loger le terminal ;
    il tombe désormais à la génération."""
    if x < GUI_W and y < GUI_H:
        raise SystemExit(f"{what} en ({x},{y}) recouvert par le panneau "
                         f"{GUI_W}x{GUI_H} — déplacer la planche de sprites")
    if x + w > 256 or y + h > 256:
        raise SystemExit(f"{what} en ({x},{y}) déborde de la texture 256x256")


def tex_console_gui():
    """Fond d'interface et planche de sprites du Contrôle de l'amitié.

    Le panneau n'emprunte pas le gris de vanilla : il reprend la palette du
    bloc — quartz laiteux, or, violet sombre — pour qu'on reconnaisse l'appareil
    qu'on vient d'ouvrir. Les touches et les boutons sont dessinés ici plutôt que
    laissés aux widgets vanilla, dont le gris passe-partout est précisément ce
    qui faisait générique.

    Le terminal, en bas, occupe toute la largeur : c'est là que s'affiche ce que
    le panneau a à dire, et ce qui partait auparavant dans le chat. Son encart
    est plus sombre que celui du carnet, avec un bandeau de titre et de fines
    lignes de balayage — de quoi le lire comme un écran et non comme du papier.

    La planche de sprites occupe les marges que le panneau laisse libres : la
    colonne à sa droite pour le sablier, la bande sous lui pour les touches et
    les boutons.
    """
    edge = (44, 42, 52, 255)
    body = (178, 176, 186, 255)          # quartz froid, pour que l'or tranche
    body_light = (226, 226, 236, 255)
    body_shade = (126, 124, 136, 255)
    gold = (212, 172, 84, 255)
    gold_lit = (250, 224, 146, 255)
    inset = (18, 15, 28, 255)
    inset_title = (46, 38, 70, 255)
    inset_line = (30, 26, 44, 255)
    inset_edge = (74, 64, 104, 255)
    screen_edge = (30, 22, 56, 255)
    well = (96, 94, 104, 255)
    well_edge = (66, 64, 74, 255)

    px = canvas(256, 256, (0, 0, 0, 0))

    # ---- coque : corps, biseau, arête
    rect(px, 0, 0, GUI_W - 1, GUI_H - 1, body)
    outline(px, 0, 0, GUI_W - 1, GUI_H - 1, edge)
    for i in (1, 2):
        for x in range(i, GUI_W - i):
            put(px, x, i, body_light)
            put(px, x, GUI_H - 1 - i, body_shade)
        for y in range(i, GUI_H - i):
            put(px, i, y, body_light)
            put(px, GUI_W - 1 - i, y, body_shade)

    # ---- liseré d'or sur deux pixels, équerres franches aux quatre angles
    outline(px, 4, 4, GUI_W - 5, GUI_H - 5, gold)
    outline(px, 5, 5, GUI_W - 6, GUI_H - 6, gold_lit)
    for (cx, cy, dx, dy) in ((4, 4, 1, 1), (GUI_W - 5, 4, -1, 1),
                             (4, GUI_H - 5, 1, -1), (GUI_W - 5, GUI_H - 5, -1, -1)):
        for k in range(9):
            for t in range(3):
                put(px, cx + dx * k, cy + dy * t, gold_lit)
                put(px, cx + dx * t, cy + dy * k, gold_lit)

    def sunken(x0, y0, x1, y1, fill, border):
        rect(px, x0, y0, x1, y1, fill)
        outline(px, x0, y0, x1, y1, border)
        for x in range(x0, x1 + 1):
            put(px, x, y0, edge)
        for y in range(y0, y1 + 1):
            put(px, x0, y, edge)
        put(px, x1, y1, body_light)

    # ---- le carnet : encart sombre, bandeau de titre souligné d'or, rayures
    sunken(8, 8, 103, 157, inset, inset_edge)
    rect(px, 9, 9, 102, 21, inset_title)
    rect(px, 9, 22, 102, 22, gold)
    rect(px, 9, 23, 102, 23, inset_edge)
    for y in range(28, 156, 6):
        for x in range(11, 101, 3):
            put(px, x, y, inset_line)

    # ---- l'écran : bezel épais, dégradé vertical, grille fine
    sunken(110, 8, 209, 31, screen_edge, screen_edge)
    for y in range(10, 30):
        # Dégradé sur toute la hauteur : une bande nette en haut se lisait comme
        # un défaut plutôt que comme une lueur.
        t = (y - 10) / 19.0
        shade_row = (int(112 - 60 * t), int(88 - 46 * t), int(190 - 92 * t), 255)
        for x in range(112, 208):
            put(px, x, y, shade_row)
    for x in range(112, 208, 8):
        for y in range(11, 29):
            r, g, b, _ = px[y][x]
            put(px, x, y, (max(0, r - 14), max(0, g - 12), max(0, b - 18), 255))
    rect(px, 112, 10, 207, 10, (168, 146, 236, 255))

    # ---- logements des touches et des boutons
    for (col, row) in GUI_KEY_CELLS:
        x = GUI_PAD_X + col * (GUI_KEY_W + GUI_KEY_GAP)
        y = GUI_KEYS_Y + row * (GUI_KEY_H + GUI_KEY_GAP)
        sunken(x - 1, y - 1, x + GUI_KEY_W, y + GUI_KEY_H, well, well_edge)
    for offset in (0, 50):
        sunken(GUI_PAD_X + offset - 1, GUI_ACTIONS_Y - 1,
               GUI_PAD_X + offset + GUI_ACTION_W, GUI_ACTIONS_Y + GUI_ACTION_H, well, well_edge)

    # ---- filet d'or au-dessus du code du joueur, et rivets de part et d'autre
    hairline = GUI_ACTIONS_Y + GUI_ACTION_H + 2
    rect(px, GUI_PAD_X, hairline, GUI_PAD_X + 95, hairline, gold)
    for x in (GUI_PAD_X, GUI_PAD_X + 95):
        put(px, x, hairline - 1, body_shade)
        put(px, x, hairline + 1, body_light)

    # ---- le terminal : encart très sombre, bandeau de titre, lignes de balayage
    term_bg = (14, 12, 24, 255)
    term_scan = (19, 17, 32, 255)
    tx0, ty0 = GUI_TERM_X, GUI_TERM_Y
    tx1, ty1 = GUI_TERM_X + GUI_TERM_W - 1, GUI_TERM_Y + GUI_TERM_H - 1
    sunken(tx0, ty0, tx1, ty1, term_bg, inset_edge)
    rect(px, tx0 + 1, ty0 + 1, tx1 - 1, ty0 + GUI_TERM_HEADER_H, inset_title)
    rect(px, tx0 + 1, ty0 + GUI_TERM_HEADER_H + 1, tx1 - 1, ty0 + GUI_TERM_HEADER_H + 1, gold)
    rect(px, tx0 + 1, ty0 + GUI_TERM_HEADER_H + 2, tx1 - 1, ty0 + GUI_TERM_HEADER_H + 2, inset_edge)
    # Une ligne sur trois est à peine éclaircie : de près on voit un écran, de
    # loin une surface unie. Une trame plus marquée gênerait la lecture.
    status_top = ty1 - GUI_TERM_STATUS_H
    for y in range(ty0 + GUI_TERM_HEADER_H + 4, status_top - 1, 3):
        rect(px, tx0 + 1, y, tx1 - 1, y, term_scan)
    # La bande d'état est détachée du journal : ce qu'elle porte n'est pas un
    # événement de plus, c'est l'état courant, et il doit rester lisible pendant
    # que les lignes défilent au-dessus.
    rect(px, tx0 + 1, status_top - 1, tx1 - 1, status_top - 1, inset_edge)
    rect(px, tx0 + 1, status_top, tx1 - 1, ty1 - 1, (24, 21, 40, 255))

    # ---------------------------------------------------------- sprites
    def cap(x0, y0, w, h, face, light, shade, border):
        rect(px, x0, y0, x0 + w - 1, y0 + h - 1, face)
        outline(px, x0, y0, x0 + w - 1, y0 + h - 1, border)
        for x in range(x0 + 1, x0 + w - 1):
            put(px, x, y0 + 1, light)
            put(px, x, y0 + h - 2, shade)
        for y in range(y0 + 1, y0 + h - 1):
            put(px, x0 + 1, y, light)
            put(px, x0 + w - 2, y, shade)

    # Touches accordées au corps : un beige chaud sur un gris froid se voyait.
    key_face = (196, 196, 208, 255)
    key_hover = (232, 232, 244, 255)
    _outside_panel(GUI_KEY_SPRITE_X, GUI_KEY_SPRITE_Y, GUI_KEY_W, GUI_KEY_H, "touche")
    _outside_panel(GUI_KEY_SPRITE_X, GUI_KEY_HOVER_SPRITE_Y, GUI_KEY_W, GUI_KEY_H, "touche survolée")
    cap(GUI_KEY_SPRITE_X, GUI_KEY_SPRITE_Y, GUI_KEY_W, GUI_KEY_H,
        key_face, body_light, body_shade, edge)
    cap(GUI_KEY_SPRITE_X, GUI_KEY_HOVER_SPRITE_Y, GUI_KEY_W, GUI_KEY_H,
        key_hover, (255, 255, 255, 255), gold, gold)

    green = (66, 142, 76, 255)
    green_hi = (108, 196, 116, 255)
    green_lo = (38, 96, 46, 255)
    amber = (150, 96, 52, 255)
    amber_hi = (206, 148, 88, 255)
    amber_lo = (96, 58, 30, 255)
    actions = ((green, green_hi, green_lo, edge),
               (green_hi, (168, 236, 172, 255), green, gold_lit),
               (amber, amber_hi, amber_lo, edge),
               (amber_hi, (232, 190, 140, 255), amber, gold_lit))
    for x, (face, light, shade, border) in zip(GUI_ACTION_SPRITE_X, actions):
        _outside_panel(x, GUI_ACTION_SPRITE_Y, GUI_ACTION_W, GUI_ACTION_H, "bouton d'action")
        cap(x, GUI_ACTION_SPRITE_Y, GUI_ACTION_W, GUI_ACTION_H, face, light, shade, border)

    # Le sablier : deux cônes, un col, du sable qui a déjà coulé.
    hg = {
        " ": (0, 0, 0, 0),
        "#": (72, 62, 44, 255),
        "g": (198, 164, 92, 255),
        "s": (232, 206, 148, 255),
        ".": (150, 200, 214, 120),
    }
    hourglass = [
        "                ",
        "   gggggggggg   ",
        "   g########g   ",
        "    #ssssss#    ",
        "    #.ssss.#    ",
        "     #.ss.#     ",
        "      #ss#      ",
        "       ##       ",
        "       ##       ",
        "      #..#      ",
        "     #.ss.#     ",
        "    #.ssss.#    ",
        "    #ssssss#    ",
        "   g########g   ",
        "   gggggggggg   ",
        "                ",
    ]
    _outside_panel(GUI_HOURGLASS_X, GUI_HOURGLASS_Y, 16, 16, "sablier")
    for y, row in enumerate(hourglass):
        for x, ch in enumerate(row):
            if ch != " ":
                put(px, GUI_HOURGLASS_X + x, GUI_HOURGLASS_Y + y, hg[ch])

    write_png(f"{ASSETS}/textures/gui/friendship_console.png", 256, 256, px)


# ---------------------------------------------------- modèles et blockstates

FRAME = "enderportals:block/ally_passage_frame"


def _all_faces(texture):
    """Les six faces d'un élément. Aucun cullface : les éléments de l'arche ne
    touchent pas tous le bord du bloc, et un cullface mal placé escamote une
    face au lieu d'en économiser une."""
    return {face: {"texture": texture}
            for face in ("north", "south", "east", "west", "up", "down")}


def passage_models():
    """Le Passage n'a plus de modèle de bloc : son caisson est dessiné par
    {@code AllyPassageRenderer}, comme celui de la Porte de l'Ender.

    Un modèle de bloc ne sait pas laisser une embrasure vide sans laisser aussi
    passer la collision — et un portail d'Immersive Portals exige exactement
    ça : rien dans le plan qu'il occupe. L'arche de la 0.15.0, dessinée en
    modèle, y plaçait un voile de 2 px et faisait courir ses montants de part en
    part ; le portail se voyait découpé par la géométrie censée le doubler.

    Il reste donc deux modèles seulement : un modèle vide, pour la texture de
    particule que le blockstate doit bien nommer, et une arche miniature pour
    l'inventaire — celle-là ne subit aucune contrainte de portail.
    """
    _raw_block_model("ally_passage_invisible", {"textures": {"particle": FRAME}})

    _raw_block_model("ally_passage_inventory", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:translucent",
        "textures": {"particle": FRAME, "frame": FRAME,
                     "veil": "enderportals:block/ally_veil_closed"},
        "elements": [
            {"from": [0, 0, 4], "to": [3, 16, 12], "faces": _all_faces("#frame")},
            {"from": [13, 0, 4], "to": [16, 16, 12], "faces": _all_faces("#frame")},
            {"from": [3, 13, 4], "to": [13, 16, 12], "faces": _all_faces("#frame")},
            {"from": [3, 0, 7], "to": [13, 13, 9], "faces": _all_faces("#veil")},
        ],
    })

    _item_model("ally_passage", "enderportals:block/ally_passage_inventory")
    _item_model("friendship_console", "enderportals:block/friendship_console")


def _raw_block_model(name, body):
    """Un modèle de bloc écrit tel quel — sans parent ni couche imposés."""
    path = f"{ASSETS}/models/block/{name}.json"
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(body, f, indent=2)
        f.write("\n")
    print("json", path)


def passage_blockstates():
    # Un seul variant, comme la Porte de l'Ender : le bloc est INVISIBLE, et
    # seule sa texture de particule sort d'ici.
    _blockstate("ally_passage", {"variants": {
        "": {"model": "enderportals:block/ally_passage_invisible"}}})

    console = {}
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        entry = {"model": "enderportals:block/friendship_console"}
        if y:
            entry["y"] = y
        console[f"facing={facing}"] = entry
    _blockstate("friendship_console", {"variants": console})

    path = f"{ASSETS}/models/block/friendship_console.json"
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump({
            "parent": "minecraft:block/orientable",
            "textures": {
                "front": "enderportals:block/friendship_console_front",
                "side": "enderportals:block/friendship_console_side",
                "top": "enderportals:block/friendship_console_top",
            },
        }, f, indent=2)
        f.write("\n")
    print("json", path)


def main():
    tex_ender_block()
    tex_ender_bricks()
    tex_chiseled_ender_bricks()
    tex_inactive_door_sides()
    tex_ender_ore()
    tex_doors()
    tex_door_entity_sheet()
    tex_ender_crystal()
    tex_tardis_key()
    tex_ender_pickaxe()
    tex_inactive_door_item()
    tex_centralizer()
    tex_ender_bag()
    tex_icon()
    door_blockstate()
    door_models()
    brick_family_models()
    brick_family_blockstates()
    tex_paradise_frame()
    tex_paradise_veils()
    tex_passage_entity_sheet()
    tex_friendship_console()
    tex_console_gui()
    passage_models()
    passage_blockstates()


if __name__ == "__main__":
    main()
