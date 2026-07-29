#!/usr/bin/env python3
"""Génère les textures PNG + blockstates/modèles de porte du mod Ender Portals.

Style « Obsidienne & vide » : palettes limitées, taches quantifiées façon
stone vanilla, fondus verticaux dithérés, contours sombres.
"""
import json
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
    """Brique ciselée : un cadre de pierre et, gravé au centre, l'œil pâle des
    cadres de portail de l'End — le même motif que les portes du mod. Même
    alpha que le reste de la famille."""
    frame_dark = (40, 42, 50, ENDER_ALPHA)
    frame = (72, 76, 86, ENDER_ALPHA)
    panel = [(84, 88, 98, ENDER_ALPHA), (94, 98, 108, ENDER_ALPHA),
             (104, 108, 118, ENDER_ALPHA)]
    noise = blob_noise(16, 16, seed=3131, scale=4)
    px = canvas(16, 16)
    for y in range(16):
        for x in range(16):
            put(px, x, y, shade(panel, noise[y][x]))
    # Cadre : bord sombre d'1 px, liseré clair juste à l'intérieur.
    outline(px, 0, 0, 15, 15, frame_dark)
    outline(px, 1, 1, 14, 14, frame)
    outline(px, 2, 2, 13, 13, frame_dark)
    # L'œil, gravé : amande pâle sur fond creusé.
    eye_bg = (54, 46, 74, ENDER_ALPHA)
    pale = EYE_PALE[:3] + (ENDER_ALPHA,)
    core = EYE_CORE[:3] + (ENDER_ALPHA,)
    rect(px, 4, 6, 11, 9, eye_bg)
    rect(px, 5, 7, 10, 8, pale)
    rect(px, 7, 7, 8, 8, core)
    put(px, 4, 7, pale)
    put(px, 11, 7, pale)
    put(px, 4, 8, pale)
    put(px, 11, 8, pale)
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

# Voile du passage : or pâle lumineux, presque blanc au cœur.
VEIL_DORMANT = [(46, 44, 52, 255), (56, 53, 62, 255), (66, 62, 72, 255)]
VEIL_WAKING = [(120, 104, 74, 214), (162, 140, 96, 214), (204, 178, 122, 214)]
VEIL_OPEN = [(214, 190, 130, 206), (236, 216, 164, 206), (252, 243, 214, 206)]


def tex_paradise_frame():
    """Le cadre du Passage : quartz taillé, une veine dorée au centre."""
    noise = blob_noise(16, 16, seed=8801, scale=4)
    px = canvas(16, 16)
    for y in range(16):
        for x in range(16):
            put(px, x, y, shade(P_PARADISE, noise[y][x]))
    outline(px, 0, 0, 15, 15, PARADISE_DARK)
    # Veine verticale dorée, dithérée : elle attrape la lumière sans clignoter.
    for y in range(2, 14):
        put(px, 7, y, PARADISE_SEAM if y % 3 else PARADISE_SEAM_LIT)
        put(px, 8, y, PARADISE_SEAM_LIT if y % 3 else PARADISE_SEAM)
    write_png(f"{ASSETS}/textures/block/ally_passage_frame.png", 16, 16, px)


def _veil(name, palette, seed, swirl):
    """Le voile du passage : anneaux concentriques tordus, façon portail de l'End."""
    px = canvas(16, 16)
    noise = blob_noise(16, 16, seed=seed, scale=5)
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            radius = (dx * dx + dy * dy) ** 0.5 / 10.6
            v = (radius * swirl + noise[y][x] * (1.0 - swirl * 0.5)) % 1.0
            put(px, x, y, shade(palette, min(0.999, max(0.0, v))))
    write_png(f"{ASSETS}/textures/block/{name}.png", 16, 16, px)


def tex_paradise_veils():
    _veil("ally_veil_closed", VEIL_DORMANT, 8802, 0.35)
    _veil("ally_veil_opening", VEIL_WAKING, 8803, 0.75)
    _veil("ally_veil_open", VEIL_OPEN, 8804, 0.95)


def tex_friendship_console():
    """Face du panneau : le pavé numérique en miniature, lisible à 16 px."""
    body = (206, 200, 186, 255)
    bezel = (150, 144, 130, 255)
    screen = (96, 74, 150, 255)
    key = (58, 56, 64, 255)
    validate = (74, 158, 82, 255)
    px = canvas(16, 16, body)
    outline(px, 0, 0, 15, 15, bezel)
    # Écran violet en haut à droite, comme sur le panneau réel.
    rect(px, 8, 2, 14, 3, screen)
    # Trois colonnes de touches.
    for row in range(4):
        for col in range(3):
            put(px, 9 + col * 2, 5 + row * 2, key)
    # Liste des amis à gauche, et la barre verte de validation.
    rect(px, 2, 2, 6, 12, key)
    rect(px, 9, 13, 14, 13, validate)
    write_png(f"{ASSETS}/textures/block/friendship_console_front.png", 16, 16, px)

    side = canvas(16, 16, body)
    outline(side, 0, 0, 15, 15, bezel)
    for y in range(4, 13, 4):
        for x in range(3, 13):
            put(side, x, y, bezel)
    write_png(f"{ASSETS}/textures/block/friendship_console_side.png", 16, 16, side)

    top = canvas(16, 16)
    noise = blob_noise(16, 16, seed=8805, scale=4)
    for y in range(16):
        for x in range(16):
            top[y][x] = shade(P_PARADISE, noise[y][x])
    outline(top, 0, 0, 15, 15, bezel)
    rect(top, 6, 6, 9, 9, PARADISE_SEAM)
    write_png(f"{ASSETS}/textures/block/friendship_console_top.png", 16, 16, top)


def tex_console_gui():
    """Fond d'interface 256x256, façon panneau vanilla : biseau clair en haut à
    gauche, ombre en bas à droite, encarts creusés pour la liste et l'écran."""
    W, H = 220, 192
    px = canvas(256, 256, (0, 0, 0, 0))
    face = (198, 198, 198, 255)
    light = (255, 255, 255, 255)
    shadow = (85, 85, 85, 255)
    inset_dark = (24, 22, 30, 255)
    inset_edge = (58, 56, 66, 255)
    display = (86, 66, 138, 255)
    display_edge = (44, 34, 72, 255)

    rect(px, 0, 0, W - 1, H - 1, face)
    # Biseau : deux pixels clairs en haut/gauche, deux sombres en bas/droite.
    for i in range(2):
        for x in range(i, W - i):
            put(px, x, i, light)
            put(px, x, H - 1 - i, shadow)
        for y in range(i, H - i):
            put(px, i, y, light)
            put(px, W - 1 - i, y, shadow)

    def inset(x0, y0, x1, y1, fill, edge):
        rect(px, x0, y0, x1, y1, fill)
        outline(px, x0, y0, x1, y1, edge)
        for x in range(x0, x1 + 1):
            put(px, x, y0, shadow)
        for y in range(y0, y1 + 1):
            put(px, x0, y, shadow)

    inset(8, 8, 8 + 95, 8 + 159, inset_dark, inset_edge)      # le carnet
    inset(112, 10, 112 + 95, 10 + 19, display, display_edge)  # l'écran
    write_png(f"{ASSETS}/textures/gui/friendship_console.png", 256, 256, px)


# ---------------------------------------------------- modèles et blockstates

FRAME = "enderportals:block/ally_passage_frame"
_VEIL_TEX = {"closed": "enderportals:block/ally_veil_closed",
             "opening": "enderportals:block/ally_veil_opening",
             "open": "enderportals:block/ally_veil_open"}


def _all_faces(texture):
    """Les six faces d'un élément. Aucun cullface : les éléments de l'arche ne
    touchent pas tous le bord du bloc, et un cullface mal placé escamote une
    face au lieu d'en économiser une."""
    return {face: {"texture": texture}
            for face in ("north", "south", "east", "west", "up", "down")}


def passage_models():
    """Une arche : deux montants, un voile au centre, un linteau en haut.

    Les montants font 3 px, le voile 2 px d'épaisseur au milieu du bloc — c'est
    ce plan-là qu'on traverse. Le repère local a l'axe X en travers du passage ;
    la rotation du blockstate s'occupe de l'orientation.
    """
    for phase, veil in _VEIL_TEX.items():
        for half in ("lower", "upper"):
            elements = [
                {"from": [0, 0, 0], "to": [3, 16, 16],
                 "faces": _all_faces("#frame")},
                {"from": [13, 0, 0], "to": [16, 16, 16],
                 "faces": _all_faces("#frame")},
            ]
            if half == "upper":
                elements.append({"from": [3, 13, 0], "to": [13, 16, 16],
                                 "faces": _all_faces("#frame")})
                veil_top = 13
            else:
                veil_top = 16
            elements.append({"from": [3, 0, 7], "to": [13, veil_top, 9],
                             "faces": _all_faces("#veil")})
            body = {
                "parent": "minecraft:block/block",
                "render_type": "minecraft:translucent",
                "textures": {"particle": FRAME, "frame": FRAME, "veil": veil},
                "elements": elements,
            }
            path = f"{ASSETS}/models/block/ally_passage_{half}_{phase}.json"
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, "w", encoding="utf-8") as f:
                json.dump(body, f, indent=2)
                f.write("\n")
            print("json", path)

    # Objet : l'arche basse vue en perspective d'inventaire.
    _item_model("ally_passage", "enderportals:block/ally_passage_lower_closed")
    _item_model("friendship_console", "enderportals:block/friendship_console")


def passage_blockstates():
    variants = {}
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for half in ("lower", "upper"):
            for phase in _VEIL_TEX:
                entry = {"model": f"enderportals:block/ally_passage_{half}_{phase}"}
                if y:
                    entry["y"] = y
                variants[f"facing={facing},half={half},phase={phase}"] = entry
    _blockstate("ally_passage", {"variants": variants})

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
    tex_friendship_console()
    tex_console_gui()
    passage_models()
    passage_blockstates()


if __name__ == "__main__":
    main()
