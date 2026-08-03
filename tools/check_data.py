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

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

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


def check_text_components():
    """Un composant de texte se persiste en CHAÎNE contenant du JSON.

    C'est la forme qu'on lit dans la syntaxe des commandes, où les apostrophes
    délimitent bien une chaîne :

        /give @s written_book[custom_name='{"text":"Guide"}']

    Écrire un composant de texte en objet JSON — ce qui semble pourtant naturel,
    puisque tout le JSON alentour est structuré — fait échouer le décodage avec
    « Not a string », et c'est la recette ENTIÈRE qui est rejetée. Sans aucun
    message en jeu : le joueur voit une case de résultat vide devant une grille
    correcte, et il faut ouvrir les journaux pour le comprendre.

    Ce défaut a été introduit quatre fois entre juillet et la 0.16.3, et a
    survécu douze versions. La correction de la 0.16.3 n'en avait redressé que
    la moitié — les pages du livre — en laissant custom_name en objet.
    """
    # Composants dont la valeur est un texte, ou une liste de textes.
    TEXT = {"minecraft:custom_name", "minecraft:item_name"}
    TEXT_LIST = {"minecraft:lore"}

    def check_text(path, label, value, limit=None):
        if not isinstance(value, str):
            fail(path, f"{label} écrit en {type(value).__name__} ; un composant de "
                       "texte attend une CHAÎNE contenant du JSON")
            return
        try:
            json.loads(value)
        except json.JSONDecodeError:
            fail(path, f"{label} : la chaîne ne contient pas de JSON valide")
        if limit is not None and len(value) > limit:
            fail(path, f"{label} : {len(value)} caractères, limite {limit}")

    for path in sorted(DATA.rglob("recipe/*.json")):
        components = json.loads(path.read_text(encoding="utf-8")) \
            .get("result", {}).get("components", {})
        for name, value in components.items():
            if name in TEXT:
                check_text(path, name, value)
            elif name in TEXT_LIST:
                for index, line in enumerate(value, 1):
                    check_text(path, f"{name}[{index}]", line)
            elif name == "minecraft:written_book_content":
                for index, page in enumerate(value.get("pages", []), 1):
                    check_text(path, f"page {index}", page, limit=1024)
                title = value.get("title", "")
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
    """Chaque page du livre doit exister dans les trois langues, et tenir dans
    le cadre.

    Deux pannes distinctes, toutes deux muettes. Une page déclarée par
    {@code PAGE_COUNT} mais absente d'une langue s'ouvre sur sa clé brute. Une
    page trop longue est simplement <b>tronquée</b> — le jeu n'ajoute pas de
    page, il coupe au milieu d'une phrase, et rien ne le signale. C'est ce qui
    est arrivé à la moitié des pages de la 0.16.4.

    La place se mesure en pixels, pas en caractères : voir {@code tools/book.py}.
    """
    import re
    import book

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
            text = keys.get(key)
            if text is None:
                fail(path, f"{key} manquante ({count} pages déclarées dans GuideBook.java)")
                continue
            lines = book.wrap(text)
            if len(lines) > book.PAGE_LINES:
                perdu = " / ".join(lines[book.PAGE_LINES:])
                fail(path, f"{key} tient sur {len(lines)} lignes, la page en affiche "
                           f"{book.PAGE_LINES} — texte perdu : « {perdu} »")


def main():
    check_json_parses()
    if errors:
        # Inutile d'aller plus loin : les contrôles suivants relisent ces mêmes
        # fichiers et ne feraient que répéter la même panne.
        report()
    check_text_components()
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
