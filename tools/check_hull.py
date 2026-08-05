#!/usr/bin/env python3
"""Vérifie que la coque du Téléporteur d'entité n'a aucune face coplanaire.

Pourquoi un script pour ça
--------------------------
Deux caisses qui se touchent pile — un rail posé sur une paroi, deux parois qui
se rejoignent à l'angle — ont des faces exactement à la même profondeur. La
carte graphique n'a alors aucun moyen de choisir laquelle dessiner : le résultat
scintille et bave d'une texture à l'autre selon l'angle. C'est le clipping
signalé sur la 0.20.1, dont la coque comptait 116 paires de faces coplanaires.

Le défaut ne se voit ni dans le code — chaque caisse est correcte prise seule —
ni dans les textures, et il ne casse aucun build : il ne se voit qu'en jeu, en
tournant autour de l'objet. D'où cette vérification, qui lit les coordonnées
telles qu'elles sont écrites dans le renderer.

La règle
--------
Deux caisses s'ignorent, ou s'enfoncent franchement l'une dans l'autre. Jamais
elles ne s'alignent : si deux faces partagent un plan **et** que les caisses se
recouvrent dans les deux autres axes, c'est un défaut.
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RENDERER = ROOT / ("src/main/java/com/maxezify/enderportals/client/"
                   "EntityTeleporterRenderer.java")

# Deux plans distants de moins de ça se disputent le tampon de profondeur.
# Les décrochements voulus font 5 mm, avec dix fois la marge.
TOLERANCE = 0.0005
# En deçà, le recouvrement est un contact tangent et non une surface partagée.
EPS = 1e-6


def parts(source):
    """Les caisses du tableau PARTS, telles qu'écrites dans le renderer."""
    block = re.search(r"float\[\]\[\] PARTS = \{(.*?)\n    \};", source, re.S)
    if block is None:
        sys.exit("check_hull : tableau PARTS introuvable dans le renderer.")
    boxes = []
    for row in re.finditer(r"\{([^{}]*)\}", block.group(1)):
        numbers = re.findall(r"-?\d+\.\d+f", row.group(1))
        if len(numbers) != 6:
            sys.exit("check_hull : caisse à %d coordonnées, il en faut 6 : %s"
                     % (len(numbers), row.group(1).strip()))
        values = [float(n[:-1]) for n in numbers]
        low, high = values[:3], values[3:]
        for axis in range(3):
            if high[axis] - low[axis] <= EPS:
                sys.exit("check_hull : caisse plate sur l'axe %s : %s"
                         % ("xyz"[axis], row.group(1).strip()))
        boxes.append((low, high))
    return boxes


def coplanar(boxes):
    """Les paires (i, j, axe, plan) dont les faces se disputent la profondeur."""
    faults = []
    for i in range(len(boxes)):
        for j in range(i + 1, len(boxes)):
            (a0, a1), (b0, b1) = boxes[i], boxes[j]
            for axis in range(3):
                others = [k for k in range(3) if k != axis]
                shared = [min(a1[k], b1[k]) - max(a0[k], b0[k]) for k in others]
                if min(shared) <= EPS:
                    continue          # pas de surface commune : rien à départager
                for pa in (a0[axis], a1[axis]):
                    for pb in (b0[axis], b1[axis]):
                        if abs(pa - pb) < TOLERANCE:
                            faults.append((i, j, "xyz"[axis], pa))
    return faults


def main():
    boxes = parts(RENDERER.read_text(encoding="utf-8"))
    if not boxes:
        sys.exit("check_hull : aucune caisse lue.")
    faults = coplanar(boxes)
    if faults:
        print("Coque du téléporteur : %d paires de faces coplanaires." % len(faults))
        for i, j, axis, plan in faults[:20]:
            print("  caisses %2d et %2d : %s = %.3f" % (i, j, axis, plan))
        if len(faults) > 20:
            print("  … et %d autres." % (len(faults) - 20))
        print("Deux caisses s'ignorent ou s'enfoncent l'une dans l'autre ;")
        print("alignées, elles scintillent en jeu.")
        sys.exit(1)
    print("Coque du téléporteur : %d caisses, aucune face coplanaire." % len(boxes))


if __name__ == "__main__":
    main()
