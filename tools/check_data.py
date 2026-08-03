#!/usr/bin/env python3
"""Vérifie les invariants des fichiers de données que le compilateur ne voit pas.

Un datapack n'est lu qu'au chargement du monde. Une erreur de forme n'y fait
pas échouer la compilation : elle fait rejeter silencieusement l'entrée fautive,
et le joueur découvre en jeu qu'une recette n'existe pas. Ce script rattrape en
CI ce que `./gradlew build` laisse passer.

Chaque contrôle ici correspond à un défaut réellement rencontré. On n'y ajoute
pas de règle par précaution : une règle sans bug derrière elle finit par gêner
plus qu'elle n'aide.
"""
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
DATA = ROOT / "src" / "main" / "resources" / "data"
ASSETS = ROOT / "src" / "main" / "resources" / "assets"

errors = []


def fail(path, message):
    errors.append(f"{path.relative_to(ROOT)} : {message}")


def check_json_parses():
    """Tout fichier JSON doit être analysable — la panne la plus bête."""
    for path in sorted(list(DATA.rglob("*.json")) + list(ASSETS.rglob("*.json"))):
        try:
            json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as e:
            fail(path, f"JSON invalide — {e}")


def check_written_books():
    """Les pages d'un livre écrit sont des CHAÎNES contenant du JSON.

    {@code WrittenBookContent} décode ses pages avec
    {@code ComponentSerialization.flatCodec(1024)} : le codec lit d'abord une
    chaîne, puis analyse le contenu de cette chaîne comme un composant de texte.
    Écrire une page en objet JSON — ce qui semble pourtant naturel — fait échouer
    le décodage, et la recette entière est rejetée : la grille reste vide sans
    aucun message en jeu.

    Le piège est d'autant plus vicieux que {@code custom_name}, juste à côté dans
    le même bloc, utilise {@code ComponentSerialization.CODEC} et veut un objet.
    Deux composants de texte voisins, deux formes contraires. Ce défaut a été
    introduit trois fois entre juillet et la 0.16.2.
    """
    for path in sorted(DATA.rglob("recipe/*.json")):
        recipe = json.loads(path.read_text(encoding="utf-8"))
        components = recipe.get("result", {}).get("components", {})
        content = components.get("minecraft:written_book_content")
        if content is None:
            continue
        for index, page in enumerate(content.get("pages", []), 1):
            if not isinstance(page, str):
                fail(path, f"page {index} écrite en {type(page).__name__} ; "
                           "written_book_content.pages attend des chaînes "
                           "contenant du JSON (flatCodec)")
            else:
                try:
                    json.loads(page)
                except json.JSONDecodeError:
                    fail(path, f"page {index} : la chaîne ne contient pas de JSON valide")
                if len(page) > 1024:
                    fail(path, f"page {index} : {len(page)} caractères, limite 1024")
        title = content.get("title", "")
        if len(title) > 32:
            fail(path, f"titre de {len(title)} caractères, limite 32")


def check_translations_exist():
    """Toute clé de traduction citée par un datapack doit exister en anglais.

    L'anglais est la langue de repli : une clé qui y manque s'affiche telle
    quelle à l'écran, sous sa forme brute.
    """
    en = json.loads((ASSETS / "enderportals" / "lang" / "en_us.json").read_text(encoding="utf-8"))
    for path in sorted(DATA.rglob("*.json")):
        tree = json.loads(path.read_text(encoding="utf-8"))
        for key in sorted(set(_translation_keys(tree))):
            if key.startswith("enderportals.") and key not in en:
                fail(path, f"clé de traduction absente de en_us.json : {key}")


def _translation_keys(node):
    """Toutes les valeurs de « translate » d'un arbre JSON, y compris celles
    enfouies dans une chaîne qui contient elle-même du JSON (les pages de
    livre)."""
    if isinstance(node, dict):
        value = node.get("translate")
        if isinstance(value, str):
            yield value
        for child in node.values():
            yield from _translation_keys(child)
    elif isinstance(node, list):
        for child in node:
            yield from _translation_keys(child)
    elif isinstance(node, str) and node.startswith("{") and "translate" in node:
        try:
            yield from _translation_keys(json.loads(node))
        except json.JSONDecodeError:
            pass


def check_guide_book_pages():
    """Le livre-guide déclare son nombre de pages en Java ; les textes vivent
    dans les fichiers de langue. Rien ne relie les deux à la compilation : une
    page ajoutée au code sans sa traduction s'ouvre sur sa clé brute."""
    import re
    source = (ROOT / "src" / "main" / "java" / "com" / "maxezify" / "enderportals"
              / "item" / "GuideBook.java")
    match = re.search(r"PAGE_COUNT\s*=\s*(\d+)", source.read_text(encoding="utf-8"))
    if match is None:
        fail(source, "PAGE_COUNT introuvable")
        return
    count = int(match.group(1))
    for lang in ("en_us", "fr_fr", "fr_ca"):
        path = ASSETS / "enderportals" / "lang" / f"{lang}.json"
        keys = json.loads(path.read_text(encoding="utf-8"))
        for page in range(1, count + 1):
            key = f"enderportals.book.page{page}"
            if key not in keys:
                fail(path, f"{key} manquante ({count} pages déclarées dans GuideBook.java)")


def main():
    check_json_parses()
    if errors:
        # Inutile d'aller plus loin : les contrôles suivants relisent ces mêmes
        # fichiers et ne feraient que répéter la même panne.
        report()
    check_written_books()
    check_guide_book_pages()
    check_translations_exist()
    report()
    print("Données vérifiées : aucune anomalie.")


def report():
    if errors:
        print("Anomalies dans les fichiers de données :\n", file=sys.stderr)
        for error in errors:
            print("  •", error, file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
