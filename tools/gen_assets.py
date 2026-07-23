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

# Bloc de l'Ender : gris froids sombres, translucides, grain « stone ».
P_ENDER = [(64, 66, 76, 210), (76, 79, 89, 210), (89, 92, 102, 210), (101, 104, 115, 210)]

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
    grain en taches et rehaut haut-gauche par pavé."""
    mortar = (46, 48, 56, 255)
    palette = [(88, 92, 102, 255), (99, 103, 113, 255), (110, 114, 124, 255)]
    highlight = (124, 128, 139, 255)
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


def main():
    tex_ender_block()
    tex_ender_bricks()
    tex_inactive_door_sides()
    tex_ender_ore()
    tex_doors()
    tex_door_entity_sheet()
    tex_ender_crystal()
    tex_tardis_key()
    tex_ender_pickaxe()
    tex_inactive_door_item()
    tex_icon()
    door_blockstate()
    door_models()


if __name__ == "__main__":
    main()
