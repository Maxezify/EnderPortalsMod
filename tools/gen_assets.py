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


OPAQUE_SUFFIX = "_opaque"


def opaque_tex(name):
    """Le nom, côté modèle, du jumeau opaque d'une texture de bloc."""
    return f"enderportals:block/{name}{OPAQUE_SUFFIX}"


def write_png_pair(path, w, h, px):
    """Écrit la texture translucide, et son double opaque à côté.

    Le Bloc de l'Ender se voit au travers une fois posé : c'est tout le propos
    du monde, et il n'est pas question d'y toucher. Mais le modèle de l'objet
    hérite de la texture du bloc, si bien qu'on tient dans la main une vitre —
    et qu'on voit le paysage lointain à travers le cube qu'on transporte. Ce
    n'est pas un mur qu'on regarde, c'est un objet ; il doit être plein.

    D'où ce jumeau, identique au pixel près sauf son canal alpha. Les modèles
    d'objet le désignent, les modèles de bloc gardent l'original. On aurait pu
    changer la passe de rendu de l'objet plutôt que sa texture ; l'opacité tient
    alors à la façon dont le moteur traite une passe « solide » sur un objet, là
    où un alpha plein est vrai quelle que soit la passe."""
    write_png(path, w, h, px)
    solid = [[(p[0], p[1], p[2], 255) for p in row] for row in px]
    write_png(path.replace(".png", f"{OPAQUE_SUFFIX}.png"), w, h, solid)


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

# ------------------------------------------------------- le Cristal de l'Ender
#
# La 0.21 le peignait en sarcelle : à seize pixels, un caillou sarcelle clair
# est un diamant, quoi qu'en dise le nom. Le mod avait donc sa pierre fondatrice
# déguisée en matériau vanilla.
#
# Le violet règle deux choses à la fois. C'est la couleur de l'End — enderman,
# chorus, cadres de portail — donc celle que le joueur associe déjà au monde
# d'où vient la pierre ; et c'est celle de la porte, de l'obsidienne et des
# machines du mod, si bien que le cristal a enfin l'air d'appartenir à la
# famille qu'il sert à construire.
#
# Reste l'améthyste, seule gemme violette de vanilla. On s'en écarte par le
# contraste plutôt que par la teinte : l'améthyste est un lavande mat, presque
# sans ombre ; celui-ci est une pierre de verre, ombres indigo froides d'un
# côté, arêtes magenta chaudes de l'autre, et un cœur presque blanc. Sept
# nuances au lieu de trois — c'est ce que font les gemmes de vanilla, et ce qui
# leur donne leur éclat.
#
# La lumière vient d'en haut à gauche, partout, comme dans tout Minecraft.
GEM_OUT = (26, 10, 44, 255)    # contour : lisible sur n'importe quel fond
GEM_DEEP = (58, 24, 96, 255)   # creux, ombre portée dans la pierre
GEM_DARK = (94, 38, 152, 255)  # facette à l'ombre
GEM_MID = (138, 58, 198, 255)  # corps
GEM_LIT = (180, 94, 230, 255)  # facette éclairée
GEM_HI = (220, 148, 246, 255)  # arête vive
GEM_CORE = (248, 216, 255, 255)  # cœur, et l'éclat unique du dessus

# Du plus sombre au plus clair : les fonctions de dessin travaillent par indice
# pour pouvoir assombrir une facette d'un cran sans réécrire une couleur.
GEM_RAMP = [GEM_OUT, GEM_DEEP, GEM_DARK, GEM_MID, GEM_LIT, GEM_HI, GEM_CORE]


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
    write_png_pair(f"{ASSETS}/textures/block/ender_block.png", 16, 16, px)


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
    write_png_pair(f"{ASSETS}/textures/block/ender_bricks.png", 16, 16, px)


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


# Les gemmes du minerai : la même taille que l'objet, en miniature, comme le
# minerai de diamant montre des diamants. Deux tailles, pour qu'aucun amas ne
# soit le jumeau d'un autre.
ORE_GEM_BIG = [
    ".hh..",
    "chhmm",
    "chmmd",
    "kmmdd",
    ".kkd.",
]
ORE_GEM_MID = [
    ".hh.",
    "chmm",
    "kmmd",
    ".kd.",
]
ORE_GEM_SMALL = [
    ".h.",
    "cmd",
    ".k.",
]
ORE_GEM_TINY = [
    "ch",
    "kd",
]

# Placement des amas. Trois règles, toutes tirées des minerais de vanilla :
# une trentaine de pixels de gemme en tout — au-delà, le bloc devient une grappe
# et non une pierre où dort un filon ; au moins un pixel de pierre entre deux
# liserés, sinon les amas se soudent en chaîne ; et quatre tailles différentes
# jetées hors d'axe, faute de quoi un mur de minerai dessine une grille.
ORE_CLUSTERS = [
    (ORE_GEM_BIG, 2, 2),
    (ORE_GEM_SMALL, 11, 2),
    (ORE_GEM_MID, 10, 8),
    (ORE_GEM_TINY, 4, 11),
]


def tex_ender_ore():
    """Surcouche du minerai : gemmes violettes sur fond transparent.

    Le bloc lui-même est du grès de l'End vanilla, référencé par le modèle : on
    ne peint ici que ce qui pousse dedans."""
    px = canvas(16, 16)
    for rows, ox, oy in ORE_CLUSTERS:
        stamp(px, ox, oy, rows, GEM_LETTERS)
    outline_around(px, GEM_OUT)
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

GEM_LETTERS = {"o": GEM_OUT, "d": GEM_DEEP, "k": GEM_DARK, "m": GEM_MID,
               "l": GEM_LIT, "h": GEM_HI, "c": GEM_CORE}


def outline_around(px, color):
    """Ajoute un liseré sur les pixels vides qui touchent la forme.

    Dessiner le contour dans la carte elle-même oblige à le recompter à chaque
    retouche ; le laisser au code garantit qu'il reste fermé quoi qu'on change
    à la silhouette."""
    ring = []
    for y in range(len(px)):
        for x in range(len(px[0])):
            if px[y][x][3] != 0:
                continue
            if any(0 <= x + dx < len(px[0]) and 0 <= y + dy < len(px)
                   and px[y + dy][x + dx][3] != 0
                   for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                ring.append((x, y))
    for x, y in ring:
        put(px, x, y, color)


def stamp(px, ox, oy, rows, letters):
    """Peint une carte de caractères. Le point est transparent.

    Les formes anguleuses se dessinent au pixel près, pas par formule : une
    gemme sortie d'une équation a des bords ronds et l'air d'un galet. C'est
    ainsi que sont faites les textures de vanilla, et ça se voit."""
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                put(px, ox + x, oy + y, letters[ch])


# Le cristal : une taille en gemme — table plate en haut, pointe en bas, la
# coupe que l'œil lit comme « pierre précieuse » avant d'avoir lu le nom.
#
# Ce qui fait la taille, ce sont les bords francs entre facettes : la colonne
# claire qui court le long de l'arête gauche, la table vive en haut, et la
# diagonale qui sépare le corps de son ombre en descendant vers la pointe. Un
# dégradé, même bien fait, donne un galet.
CRYSTAL = [
    "................",
    "................",
    ".....oooooo.....",
    "...olccchhkko...",
    "..ollccchhmkko..",
    "..ollcchhmmkko..",
    "..ollchhmmmkko..",
    "..olmmmmmkkkdo..",
    "..oddddddddddo..",
    "...olmmkkkddo...",
    "....olmkkddo....",
    ".....olkkdo.....",
    "......okdo......",
    ".......oo.......",
    "................",
    "................",
]


def tex_ender_crystal():
    """Le Cristal de l'Ender : une gemme de verre violet, taillée à facettes."""
    px = canvas(16, 16)
    stamp(px, 0, 0, CRYSTAL, GEM_LETTERS)
    write_png(f"{ASSETS}/textures/item/ender_crystal.png", 16, 16, px)


def tex_tardis_key():
    """Clé dorée vanilla-style, à 45°, cristal serti dans l'anneau."""
    GOLD_L = (252, 225, 112, 255)
    GOLD = (233, 177, 45, 255)
    GOLD_D = (180, 126, 20, 255)
    GOLD_DD = (122, 83, 12, 255)
    # La gemme de l'anneau est un éclat du Cristal : même palette, sans quoi
    # la clé raconterait qu'elle est faite d'autre chose.
    GEM = GEM_HI
    GEM_D = GEM_DARK
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


# Bois du manche : les trois valeurs d'un bâton vanilla.
WOOD_LIT = (150, 106, 62, 255)
WOOD = (117, 79, 42, 255)
WOOD_DARK = (82, 54, 28, 255)

PICK_LETTERS = dict(GEM_LETTERS, W=WOOD_LIT, w=WOOD, v=WOOD_DARK)

# La pioche : tête en barre à deux dents et bossage central, manche en diagonale
# vers le bas-gauche. C'est la silhouette de toutes les pioches du jeu, et il n'y
# avait aucune raison d'en inventer une autre : ce qui doit distinguer celle-ci,
# c'est la matière, pas la forme.
PICKAXE = [
    "................",
    "...hhh....hhh...",
    "..chhhhhhmmmkk..",
    "..chm..mm..mkd..",
    "..mk...mk...kd..",
    ".......Ww.......",
    "......Ww........",
    ".....Ww.........",
    "....Ww..........",
    "...Ww...........",
    "..Ww............",
    ".Ww.............",
    ".vv.............",
    "................",
    "................",
    "................",
]


def tex_ender_pickaxe():
    """La Pioche de l'Ender : tête de cristal, manche de bois."""
    px = canvas(16, 16)
    stamp(px, 0, 0, PICKAXE, PICK_LETTERS)
    outline_around(px, GEM_OUT)
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


# Cuir de l'Ender : quatre valeurs, du creux du pli à l'arête qui prend la
# lumière. Assez sombre pour qu'on y reconnaisse l'obsidienne des machines,
# assez clair en haut à gauche pour que le sac ne soit pas une tache noire dans
# un inventaire mal éclairé.
BAG_OUT = (14, 10, 22, 255)
BAG_DARK = (30, 24, 44, 255)
BAG_MID = (58, 48, 82, 255)
BAG_LIT = (92, 80, 124, 255)
BAG_HI = (126, 114, 158, 255)

BAG_LETTERS = {
    "o": BAG_OUT, "d": BAG_DARK, "m": BAG_MID, "l": BAG_LIT, "h": BAG_HI,
    # L'or des machines, recopié ici : la palette du Téléporteur est
    # définie plus bas dans le fichier, et un sac ne devrait pas
    # dépendre de l'ordre des déclarations.
    "G": (238, 206, 124, 255), "g": (196, 158, 74, 255), "k": (132, 100, 40, 255),
    "C": GEM_CORE, "H": GEM_HI, "M": GEM_MID, "D": GEM_DARK,
}

# Le Sac de l'Ender.
#
# L'ancien était une boule sombre à goulot brun : la silhouette ne disait pas
# « sac », le bruit de la toile brouillait les valeurs, et l'emblème tenait en
# trois pixels sarcelle qu'on lisait comme une salissure.
#
# Celui-ci reprend la silhouette que tout joueur reconnaît — celle de la bourse
# de vanilla : col resserré, épaules qui s'ouvrent, fond lourd. Ce qui change,
# c'est la matière : cuir violet sombre au lieu du cuir fauve, cordon d'or comme
# les rails des machines, et un cristal serti dans son logement au milieu du
# rabat. Le cristal plutôt que l'œil sarcelle des versions précédentes : c'est
# lui la signature du mod depuis la 0.22, et un emblème qui ne renvoie à rien
# n'est qu'un ornement.
#
# Le sertissage compte autant que la gemme : sans son anneau sombre, la pierre
# flotte sur le cuir au lieu d'y être enchâssée — c'était exactement le défaut
# de l'ancien emblème.
ENDER_BAG = [
    "......oooo......",
    ".....ohmmdo.....",
    "....ohlmmmdo....",
    "...oGGgggkkko...",
    "....olmggmdo....",
    "...ohldmmldmo...",
    "..ohlldmmldmdo..",
    "..ohlmdmmmdmdo..",
    ".ohlmmmCHmmmmdo.",
    ".ohlmmCHMDmmmdo.",
    ".ohlmmmMDdmmmdo.",
    ".olmmmmmdmmmmdo.",
    ".olmmmmmmmmmmdo.",
    "..olmmmmmmmddo..",
    "...omdddddddo...",
    "....oooooooo....",
]


def tex_ender_bag():
    """Le Sac de l'Ender : bourse de cuir violet, cordon d'or, cristal serti."""
    px = canvas(16, 16)
    stamp(px, 0, 0, ENDER_BAG, BAG_LETTERS)
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
    write_png_pair(f"{ASSETS}/textures/block/chiseled_ender_bricks.png", 16, 16, px)


# ------------------------------------------------- famille des briques

TRANSLUCENT = "minecraft:translucent"
BRICK_TEX = "enderportals:block/ender_bricks"
BRICK_TEX_OPAQUE = opaque_tex("ender_bricks")


def _model(name, parent, textures):
    path = f"{ASSETS}/models/block/{name}.json"
    body = {"parent": parent, "render_type": TRANSLUCENT, "textures": textures}
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(body, f, indent=2)
        f.write("\n")
    print("json", path)


def _item_model(name, parent, textures=None):
    """Modèle d'objet : la forme du bloc, éventuellement repeinte.

    Les familles translucides passent ici leurs textures opaques : même
    géométrie, même dessin, mais un objet qu'on ne traverse pas du regard."""
    path = f"{ASSETS}/models/item/{name}.json"
    body = {"parent": parent}
    if textures:
        body["textures"] = textures
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(body, f, indent=2)
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

    solid = {k: BRICK_TEX_OPAQUE for k in ("bottom", "top", "side")}
    _item_model("ender_block", "enderportals:block/ender_block",
                {"all": opaque_tex("ender_block")})
    _item_model("ender_bricks", "enderportals:block/ender_bricks",
                {"all": BRICK_TEX_OPAQUE})
    _item_model("chiseled_ender_bricks", "enderportals:block/chiseled_ender_bricks",
                {"all": opaque_tex("chiseled_ender_bricks")})
    _item_model("ender_brick_stairs", "enderportals:block/ender_brick_stairs", solid)
    _item_model("ender_brick_slab", "enderportals:block/ender_brick_slab", solid)
    _item_model("ender_brick_wall", "enderportals:block/ender_brick_wall_inventory",
                {"wall": BRICK_TEX_OPAQUE})


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


# ----------------------------------------------------------------------
# Le Téléporteur d'entité et son Atterrisseur
# ----------------------------------------------------------------------

# Métal froid sur noir d'obsidienne : la palette des machines de l'Ender, mais
# assombrie. Le Téléporteur se pose dehors, souvent en plein jour, et devait se
# distinguer d'un bateau au premier coup d'œil.
TELE_DARK = (18, 14, 26, 255)
TELE_HULL = (46, 42, 62, 255)
TELE_HULL_HI = (78, 74, 98, 255)
TELE_STEEL = (128, 126, 142, 255)
TELE_GOLD = (196, 158, 74, 255)
# Le témoin de proue ne s'éteint jamais : il dit si le départ est possible.
# Rouge, il manque une destination ou un passager ; vert, la coque peut partir.
TELE_LAMP_RED = (236, 84, 76, 255)
TELE_LAMP_ON = (96, 236, 128, 255)


def tex_entity_teleporter():
    """Planche 32x32 de la coque, découpée comme l'attend
    EntityTeleporterRenderer : bordé, plancher, rail d'or, témoin rouge, témoin
    vert, montant d'acier. Les six régions doivent rester à leur place — le
    renderer les y lit par coordonnées.

    Le seul rouge de la planche est celui du témoin, et il tient dans huit
    pixels : la coque rouge de la 0.20.0 ne venait pas d'ici mais de l'overlay
    de vertex, laissé à zéro — c'est-à-dire sur la ligne du flash de dégâts."""
    px = canvas(32, 32)

    # Bordé (0,0)-(16,8) : plaques d'obsidienne rivetées d'acier, arête claire
    # en haut, ombre portée en bas — c'est ce dégradé qui donne du relief à une
    # paroi de deux pixels d'épaisseur.
    rect(px, 0, 0, 15, 7, TELE_HULL)
    rect(px, 0, 0, 15, 0, TELE_HULL_HI)
    rect(px, 0, 6, 15, 7, TELE_DARK)
    for x in (1, 6, 11):
        rect(px, x, 2, x + 3, 4, TELE_DARK)
        rect(px, x, 2, x + 3, 2, (60, 56, 78, 255))
    for x in (0, 5, 10, 15):
        put(px, x, 1, TELE_STEEL)
        put(px, x, 5, TELE_STEEL)

    # Plancher et quille (16,0)-(32,8) : tôle rivetée, pas un trou noir.
    #
    # La version précédente était de l'obsidienne mate presque noire : vue de
    # dessus — c'est-à-dire la vue qu'on a d'une machine posée par terre — la
    # coque s'ouvrait sur un vide. Une tôle claire donne un fond au bateau, et
    # fait ressortir la plaque de départ qui s'y incruste.
    rect(px, 16, 0, 31, 7, TELE_HULL)
    rect(px, 16, 0, 31, 0, TELE_HULL_HI)
    rect(px, 16, 7, 31, 7, TELE_DARK)
    for x in (19, 24, 29):
        rect(px, x, 1, x, 6, TELE_DARK)
    for y in (2, 5):
        rect(px, 16, y, 31, y, (54, 50, 70, 255))
    for x, y in ((17, 1), (22, 1), (27, 1), (17, 6), (22, 6), (27, 6)):
        put(px, x, y, TELE_STEEL)

    # Rail (0,8)-(16,12) : or brossé, lumière en haut.
    rect(px, 0, 8, 15, 11, TELE_GOLD)
    rect(px, 0, 8, 15, 8, (238, 206, 124, 255))
    rect(px, 0, 11, 15, 11, (132, 100, 40, 255))
    for x in range(2, 15, 4):
        put(px, x, 9, (250, 230, 170, 255))

    # Témoin éteint (16,8)-(24,12), allumé (24,8)-(32,12).
    for x0, glass, spark in ((16, TELE_LAMP_RED, (255, 176, 160, 255)),
                             (24, TELE_LAMP_ON, (190, 255, 210, 255))):
        rect(px, x0, 8, x0 + 7, 11, TELE_DARK)
        rect(px, x0 + 1, 9, x0 + 6, 10, glass)
        rect(px, x0 + 2, 9, x0 + 5, 9, spark)

    # Montant (0,12)-(16,16) : acier strié, plus clair que le bordé pour que les
    # angles se détachent de la masse.
    rect(px, 0, 12, 15, 15, TELE_STEEL)
    rect(px, 0, 12, 15, 12, (170, 168, 184, 255))
    rect(px, 0, 15, 15, 15, (74, 72, 88, 255))
    for x in range(1, 15, 3):
        rect(px, x, 13, x, 14, (96, 94, 110, 255))

    # Cristal d'émetteur (0,16)-(16,24) : la gemme du mod, taillée à facettes,
    # posée en haut des quatre montants. C'est elle qui dit « téléporteur »
    # plutôt que « caisse » — quatre pylônes coiffés de la même pierre que la
    # clé et le sac.
    rect(px, 0, 16, 15, 23, GEM_DARK)
    rect(px, 0, 16, 15, 16, GEM_HI)
    rect(px, 0, 17, 15, 19, GEM_MID)
    for x in range(1, 15, 4):
        rect(px, x, 17, x + 1, 18, GEM_HI)
        put(px, x, 17, GEM_CORE)
    rect(px, 0, 22, 15, 23, GEM_DEEP)

    # Plaque de départ (16,16)-(32,24) : le disque violet incrusté dans le
    # plancher, sur lequel la bête se tient. Vu de dessus, c'est la pièce la
    # plus visible de la machine, et la seule qui explique ce qu'elle fait.
    rect(px, 16, 16, 31, 23, GEM_DEEP)
    outline(px, 16, 16, 31, 23, TELE_DARK)
    rect(px, 18, 17, 29, 22, GEM_DARK)
    outline(px, 19, 18, 28, 21, GEM_MID)
    rect(px, 21, 19, 26, 20, GEM_HI)
    rect(px, 23, 19, 24, 20, GEM_CORE)
    for x, y in ((17, 17), (30, 17), (17, 22), (30, 22)):
        put(px, x, y, GEM_HI)

    write_png(f"{ASSETS}/textures/entity/entity_teleporter.png", 32, 32, px)


def tex_entity_lander():
    """Les deux faces de l'Atterrisseur : un dessus de piste marqué d'une croix
    d'or, et un flanc de machine sombre."""
    top = canvas(16, 16)
    rect(top, 0, 0, 15, 15, TELE_DARK)
    outline(top, 0, 0, 15, 15, TELE_HULL)
    outline(top, 2, 2, 13, 13, TELE_HULL_HI)
    # La croix d'atterrissage, creusée puis dorée.
    rect(top, 7, 4, 8, 11, TELE_GOLD)
    rect(top, 4, 7, 11, 8, TELE_GOLD)
    rect(top, 7, 7, 8, 8, (240, 214, 140, 255))
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        put(top, x, y, TELE_STEEL)
    write_png(f"{ASSETS}/textures/block/entity_lander_top.png", 16, 16, top)

    side = canvas(16, 16)
    rect(side, 0, 0, 15, 15, TELE_HULL)
    rect(side, 0, 0, 15, 1, TELE_HULL_HI)
    rect(side, 0, 14, 15, 15, TELE_DARK)
    for x in range(2, 15, 5):
        rect(side, x, 4, x + 2, 11, TELE_DARK)
        put(side, x + 1, 7, TELE_LAMP_ON)
    write_png(f"{ASSETS}/textures/block/entity_lander_side.png", 16, 16, side)


TELEPORTER_ITEM_LETTERS = {
    "o": (12, 9, 18, 255),
    "d": TELE_DARK, "m": TELE_HULL, "h": TELE_HULL_HI, "s": TELE_STEEL,
    "G": (238, 206, 124, 255), "g": (196, 158, 74, 255), "k": (132, 100, 40, 255),
    "V": GEM_DARK, "H": GEM_HI, "C": GEM_CORE,
    "L": (96, 236, 128, 255), "l": (36, 128, 64, 255),
}

# Le Téléporteur en main : la machine vue de face, un rien de haut.
#
# Quatre marques suffisent à la reconnaître, et ce sont celles qu'on voit en
# jeu : les cristaux des pylônes, le rail d'or, la lueur violette de la plaque
# de départ dans l'ouverture, et le témoin vert du tableau de proue. Le reste
# n'est que coque.
TELEPORTER_ITEM = [
    "................",
    "................",
    "....oooooooo....",
    "....okggggko....",
    "...odVVVVVVdo...",
    "..odVHHHHHHVdo..",
    "..oCggggggggCo..",
    "..odmmhhhhmmdo..",
    "..odmhLLLLhmdo..",
    "..odmmhhhhmmdo..",
    "..oddmmmmmmddo..",
    "..osssssssssso..",
    "..oooooooooooo..",
    "................",
    "................",
    "................",
]


def tex_entity_teleporter_item():
    """L'objet en main : la machine de face — cristaux, rail d'or, plaque
    violette dans l'ouverture, témoin vert."""
    px = canvas(16, 16)
    stamp(px, 0, 0, TELEPORTER_ITEM, TELEPORTER_ITEM_LETTERS)
    write_png(f"{ASSETS}/textures/item/entity_teleporter.png", 16, 16, px)


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
    tex_entity_teleporter()
    tex_entity_lander()
    tex_entity_teleporter_item()
    passage_models()
    passage_blockstates()


if __name__ == "__main__":
    main()
