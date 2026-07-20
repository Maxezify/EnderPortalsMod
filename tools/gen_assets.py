#!/usr/bin/env python3
"""Génère les textures PNG + blockstates/modèles de porte du mod Ender Portals."""
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


# ---------------------------------------------------------------- blocs

def tex_ender_block():
    base = (138, 143, 155, 170)
    px = canvas(16, 16)
    for y in range(16):
        for x in range(16):
            put(px, x, y, jitter(base, 7))
    for i in range(16):  # bord légèrement plus sombre, façon bloc de miel
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            r, g, b, a = px[y][x]
            put(px, x, y, (max(0, r - 22), max(0, g - 22), max(0, b - 18), min(255, a + 25)))
    # petits éclats clairs
    for _ in range(6):
        x, y = rng.randint(2, 13), rng.randint(2, 13)
        put(px, x, y, (205, 210, 220, 190))
    write_png(f"{ASSETS}/textures/block/ender_block.png", 16, 16, px)


def tex_ender_bricks():
    mortar = (72, 75, 84, 255)
    brick = (121, 126, 138, 255)
    px = canvas(16, 16, mortar)
    for row in range(4):
        y0 = row * 4
        offset = 0 if row % 2 == 0 else 4
        for col in range(3):
            x0 = (offset + col * 8) % 16
            for y in range(y0 + 1, y0 + 4):
                for dx in range(7):
                    x = (x0 + dx) % 16
                    put(px, x, y, jitter(brick, 6))
    write_png(f"{ASSETS}/textures/block/ender_bricks.png", 16, 16, px)


def tex_ender_ore():
    stone = (221, 223, 165, 255)
    px = canvas(16, 16)
    for y in range(16):
        for x in range(16):
            put(px, x, y, jitter(stone, 8))
    crystal = (64, 224, 205, 255)
    dark = (23, 130, 120, 255)
    for cx, cy in ((3, 4), (11, 3), (6, 10), (12, 11), (8, 6)):
        put(px, cx, cy, crystal)
        put(px, cx + 1, cy, jitter(crystal, 12))
        put(px, cx, cy + 1, jitter(crystal, 12))
        put(px, cx + 1, cy + 1, dark)
        put(px, cx - 1, cy, dark)
        put(px, cx, cy - 1, dark)
    write_png(f"{ASSETS}/textures/block/ender_ore.png", 16, 16, px)


TARDIS_BLUE = (23, 48, 102, 255)
TARDIS_DARK = (13, 28, 66, 255)
TARDIS_LIGHT = (44, 78, 145, 255)
WINDOW = (214, 226, 240, 255)
WINDOW_FRAME = (10, 20, 48, 255)


def paint_door_top(px, ox=0, oy=0):
    rect(px, ox, oy, ox + 15, oy + 15, TARDIS_BLUE)
    outline(px, ox, oy, ox + 15, oy + 15, TARDIS_DARK)
    # fenêtre à 4 carreaux
    rect(px, ox + 3, oy + 2, ox + 12, oy + 8, WINDOW_FRAME)
    rect(px, ox + 4, oy + 3, ox + 7, oy + 5, WINDOW)
    rect(px, ox + 9, oy + 3, ox + 12, oy + 5, WINDOW)
    rect(px, ox + 4, oy + 7, ox + 7, oy + 8, jitter(WINDOW, 6))
    rect(px, ox + 9, oy + 7, ox + 12, oy + 8, jitter(WINDOW, 6))
    put(px, ox + 8, oy + 3, WINDOW_FRAME)
    # panneau sous la fenêtre
    outline(px, ox + 3, oy + 10, ox + 12, oy + 14, TARDIS_DARK)
    rect(px, ox + 4, oy + 11, ox + 11, oy + 13, TARDIS_LIGHT)


def paint_door_bottom(px, ox=0, oy=0):
    rect(px, ox, oy, ox + 15, oy + 15, TARDIS_BLUE)
    outline(px, ox, oy, ox + 15, oy + 15, TARDIS_DARK)
    for y0 in (1, 9):
        outline(px, ox + 3, oy + y0, ox + 12, oy + y0 + 5, TARDIS_DARK)
        rect(px, ox + 4, oy + y0 + 1, ox + 11, oy + y0 + 4, TARDIS_LIGHT)
    # poignée dorée
    put(px, ox + 13, oy + 7, (222, 177, 45, 255))
    put(px, ox + 13, oy + 8, (176, 135, 26, 255))


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
    # arrière : copie assombrie en (16..31, 0..31)
    for y in range(32):
        for x in range(16):
            r, g, b, a = px[y][x]
            put(px, 16 + x, y, (max(0, r - 18), max(0, g - 18), max(0, b - 14), a))
    # chants : bande bleu sombre (32..35, 0..31)
    rect(px, 32, 0, 35, 31, TARDIS_DARK)
    for y in range(0, 32, 3):
        put(px, 33, y, jitter(TARDIS_BLUE, 6))
    # voile de vortex (48..63, 48..63)
    for y in range(48, 64):
        for x in range(48, 64):
            put(px, x, y, jitter((6, 8, 24, 235), 4))
    for _ in range(9):  # étoiles du vortex
        x, y = rng.randint(49, 62), rng.randint(49, 62)
        put(px, x, y, (170, 190, 235, 255))
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
    px = canvas(16, 16)
    gold = (222, 177, 45, 255)
    gold_d = (160, 122, 22, 255)
    gold_l = (250, 224, 130, 255)
    # anneau
    outline(px, 2, 5, 6, 9, gold)
    put(px, 2, 5, gold_d)
    put(px, 6, 9, gold_d)
    put(px, 3, 6, gold_l)
    # tige
    for x in range(7, 14):
        put(px, x, 7, gold)
        put(px, x, 8, gold_d)
    # dents
    put(px, 11, 9, gold)
    put(px, 13, 9, gold)
    put(px, 13, 10, gold_d)
    # éclat "vortex"
    put(px, 4, 7, (64, 224, 205, 255))
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
    # tête en arc
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
    px = canvas(16, 16)
    rect(px, 4, 1, 11, 14, TARDIS_BLUE)
    outline(px, 4, 1, 11, 14, TARDIS_DARK)
    rect(px, 6, 3, 9, 5, WINDOW)
    outline(px, 6, 3, 9, 5, WINDOW_FRAME)
    outline(px, 6, 8, 9, 12, TARDIS_DARK)
    rect(px, 7, 9, 8, 11, TARDIS_LIGHT)
    put(px, 10, 7, (222, 177, 45, 255))
    write_png(f"{ASSETS}/textures/item/inactive_tardis_door.png", 16, 16, px)


def tex_icon():
    s = 128
    px = canvas(s, s, (10, 12, 26, 255))
    for _ in range(70):  # étoiles
        x, y = rng.randint(0, s - 1), rng.randint(0, s - 1)
        put(px, x, y, (170, 190, 235, 255))
    # halo turquoise
    for y in range(s):
        for x in range(s):
            d = ((x - 64) ** 2 + (y - 66) ** 2) ** 0.5
            if 34 < d < 46:
                r, g, b, a = px[y][x]
                put(px, x, y, (min(255, r + 18), min(255, g + 60), min(255, b + 55), 255))
    # porte
    rect(px, 40, 22, 88, 110, TARDIS_BLUE)
    outline(px, 40, 22, 88, 110, TARDIS_DARK)
    outline(px, 41, 23, 87, 109, TARDIS_DARK)
    # fenêtre
    rect(px, 48, 30, 80, 50, WINDOW_FRAME)
    rect(px, 50, 32, 63, 48, WINDOW)
    rect(px, 66, 32, 78, 48, WINDOW)
    # panneaux
    for y0 in (56, 84):
        outline(px, 48, y0, 80, y0 + 22, TARDIS_DARK)
        rect(px, 50, y0 + 2, 78, y0 + 20, TARDIS_LIGHT)
    put(px, 84, 68, (222, 177, 45, 255))
    put(px, 84, 69, (222, 177, 45, 255))
    write_png(f"{ASSETS}/icon.png", s, s, px)


# ---------------------------------------------------------------- JSON portes

def door_blockstate():
    """Blockstate vanilla-style pour la porte inactive (32 variantes)."""
    base_y = {"east": 0, "south": 90, "west": 180, "north": 270}
    variants = {}
    for facing, y in base_y.items():
        for half in ("lower", "upper"):
            part = "bottom" if half == "lower" else "top"
            for hinge in ("left", "right"):
                for is_open in ("false", "true"):
                    key = f"facing={facing},half={half},hinge={hinge},open={is_open}"
                    model = f"enderportals:block/inactive_door_{part}_{hinge}"
                    rot = y
                    if is_open == "true":
                        model += "_open"
                        rot = (y + 90) % 360 if hinge == "left" else (y - 90) % 360
                    entry = {"model": model}
                    if rot:
                        entry["y"] = rot
                    variants[key] = entry
    path = f"{ASSETS}/blockstates/inactive_tardis_door.json"
    with open(path, "w") as f:
        json.dump({"variants": variants}, f, indent=2, sort_keys=True)
    print("json", path)


def door_models():
    for part in ("bottom", "top"):
        for hinge in ("left", "right"):
            for suffix in ("", "_open"):
                name = f"inactive_door_{part}_{hinge}{suffix}"
                parent = f"minecraft:block/door_{part}_{hinge}{suffix}"
                path = f"{ASSETS}/models/block/{name}.json"
                with open(path, "w") as f:
                    json.dump({
                        "parent": parent,
                        "textures": {
                            "bottom": "enderportals:block/tardis_door_bottom",
                            "top": "enderportals:block/tardis_door_top",
                        },
                    }, f, indent=2)
                print("json", path)


def main():
    tex_ender_block()
    tex_ender_bricks()
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
