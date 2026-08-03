#!/usr/bin/env python3
"""Mesure la place qu'occupe une page de livre écrit, telle que le jeu la rend.

Une page de livre n'est pas un nombre de caractères : c'est une surface.
{@code BookViewScreen} dispose de **14 lignes** de **114 pixels**, et la police
de Minecraft est à chasse variable — un « i » vaut 2 pixels, un « m » en vaut 6.
Compter les caractères donne donc une réponse fausse dans les deux sens : une
page d'accents et de « l » tient largement, une page de majuscules déborde.

Ce qui déborde est purement et simplement **perdu** : le jeu n'ajoute pas de
page, il tronque au milieu d'une phrase, sans le moindre avertissement.

D'où cette mesure, faite avec les largeurs de la police vanilla et le même
retour à la ligne que le jeu — coupure aux espaces, ligne vide comptée comme
une ligne.
"""

# Largeurs de la police par défaut, en pixels, espacement d'un pixel compris.
_NARROW = {" ": 4, "!": 2, '"': 5, "'": 3, "(": 5, ")": 5, "*": 5, ",": 2,
           ".": 2, ":": 2, ";": 2, "<": 5, ">": 5, "[": 4, "]": 4, "`": 3,
           "f": 5, "i": 2, "k": 5, "l": 3, "t": 4, "{": 5, "|": 2, "}": 5,
           "@": 7, "~": 7}

#: Largeur du cadre de texte d'une page, en pixels.
PAGE_WIDTH = 114
#: Nombre de lignes qu'une page peut afficher.
PAGE_LINES = 14


def char_width(char):
    """Largeur d'un caractère. Les signes hors Latin-1 — tiret cadratin,
    guillemets, points de suspension — viennent de la police de repli, dont les
    glyphes font 8 pixels. On les compte à 8 : surestimer raccourcit les pages,
    sous-estimer les tronque."""
    if char in _NARROW:
        return _NARROW[char]
    if ord(char) > 0xFF:
        return 8
    return 6


def strip_codes(text):
    """Retire les codes de couleur : ils ne s'affichent pas, donc ne prennent
    aucune place."""
    out = []
    skip = False
    for char in text:
        if skip:
            skip = False
            continue
        if char == "§":
            skip = True
            continue
        out.append(char)
    return "".join(out)


def wrap(text):
    """Les lignes telles que le jeu les découpera."""
    lines = []
    for paragraph in strip_codes(text).split("\n"):
        if not paragraph:
            lines.append("")
            continue
        line, width = "", 0
        for word in paragraph.split(" "):
            word_width = sum(char_width(c) for c in word)
            space = char_width(" ") if line else 0
            if line and width + space + word_width > PAGE_WIDTH:
                lines.append(line)
                line, width = word, word_width
            else:
                line = f"{line} {word}" if line else word
                width += space + word_width
        lines.append(line)
    return lines


def overflow(text):
    """Nombre de lignes en trop, ou 0 si la page tient."""
    return max(0, len(wrap(text)) - PAGE_LINES)


def preview(text):
    """Rendu texte d'une page, avec le trait de coupe du jeu."""
    lines = wrap(text)
    out = []
    for index, line in enumerate(lines, 1):
        marker = "  " if index <= PAGE_LINES else "✂ "
        out.append(f"{marker}{index:2} |{line}")
    return "\n".join(out)


if __name__ == "__main__":
    import json
    import pathlib
    import sys

    lang = sys.argv[1] if len(sys.argv) > 1 else "fr_fr"
    root = pathlib.Path(__file__).resolve().parent.parent
    keys = json.loads((root / "src/main/resources/assets/enderportals/lang"
                       / f"{lang}.json").read_text(encoding="utf-8"))
    worst = 0
    for page in range(1, 100):
        text = keys.get(f"enderportals.book.page{page}")
        if text is None:
            break
        lines = len(wrap(text))
        worst = max(worst, lines)
        flag = "  DÉBORDE" if lines > PAGE_LINES else ""
        print(f"page {page:2} : {lines:2} lignes{flag}")
        if lines > PAGE_LINES:
            print(preview(text))
    print(f"\n{lang} : au plus {worst} lignes (capacité {PAGE_LINES})")
