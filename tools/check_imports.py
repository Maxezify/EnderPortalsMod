#!/usr/bin/env python3
"""Vérifie qu'une classe du mod citée dans un fichier y est bien importée.

Un import manquant vers une classe du mod ne se voit pas d'un coup d'œil, et
il ne se voit pas non plus dans une compilation partielle : hors de la CI,
Minecraft est introuvable et le compilateur crie « cannot find symbol » sur des
centaines de lignes. Une classe du mod oubliée s'y noie exactement, avec le même
message que le bruit — c'est arrivé, et le build a cassé sur le commit qui
prétendait nettoyer le projet.

D'où ce contrôle, qui ne compile rien : il compare, pour chaque fichier, les
noms de classes du mod qu'il cite aux imports qu'il déclare. Le paquet courant
n'a pas besoin d'import, une écriture pleinement qualifiée non plus.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "src/main/java"
PREFIX = "com.maxezify.enderportals"

problems = []


def strip_noise(code):
    """Le code sans commentaires ni chaînes : on n'importe pas une citation."""
    code = re.sub(r"/\*.*?\*/", " ", code, flags=re.S)
    code = re.sub(r"//[^\n]*", " ", code)
    return re.sub(r'"(\\.|[^"\\])*"', '""', code)


def package_of(path):
    return ".".join(path.relative_to(SRC).parts[:-1])


def main():
    files = sorted(SRC.rglob("*.java"))
    if not files:
        raise SystemExit("aucune source Java trouvée")

    # Nom simple -> paquet. Un nom porté par deux fichiers rendrait la
    # vérification ambiguë : on préfère le dire que de deviner.
    home = {}
    for path in files:
        if path.stem in home:
            problems.append(f"deux classes nommées {path.stem} : {home[path.stem]} et {package_of(path)}")
        home[path.stem] = package_of(path)

    for path in files:
        text = path.read_text(encoding="utf-8")
        body = strip_noise(text)
        here = package_of(path)
        imported = set(re.findall(rf"^import\s+(?:static\s+)?{re.escape(PREFIX)}\.[\w.]*?(\w+);",
                                  text, re.M))
        for name, package in home.items():
            if name == path.stem or package == here or name in imported:
                continue
            if not re.search(rf"\b{re.escape(name)}\b", body):
                continue
            # Écriture pleinement qualifiée : l'import est facultatif.
            if re.search(rf"\b{re.escape(package)}\.{re.escape(name)}\b", body):
                continue
            problems.append(f"{path.relative_to(ROOT)} : {name} utilisé sans import "
                            f"(il vit dans {package})")

    if problems:
        for problem in problems:
            print(f"  {problem}", file=sys.stderr)
        raise SystemExit(f"imports : {len(problems)} anomalie(s)")
    print(f"Imports vérifiés : {len(files)} fichiers, chaque classe du mod citée est importée.")


if __name__ == "__main__":
    main()
