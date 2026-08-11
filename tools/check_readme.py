#!/usr/bin/env python3
"""Vérifie que les deux README disent la vérité sur le mod.

Un README dérive sans bruit. On change un coût dans le code, on oublie la
documentation, et rien ne le signale — jusqu'à ce qu'un joueur suive une
instruction fausse. Ce contrôle relie donc chaque chiffre documenté à la
constante Java, à la recette ou au fichier de langue dont il sort, et refuse
le build s'ils ont cessé de s'accorder.

Il vérifie aussi que les deux traductions gardent la même charpente : même
sommaire, mêmes sections, aucun lien de sommaire mort. Deux README qui
divergent en structure finissent par diverger en contenu.
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
JAVA = ROOT / "src/main/java/com/maxezify/enderportals"
DATA = ROOT / "src/main/resources/data/enderportals"
LANG = ROOT / "src/main/resources/assets/enderportals/lang"
FR, EN = ROOT / "README.fr.md", ROOT / "README.md"

problems = []


def fail(where, message):
    problems.append(f"{where} : {message}")


def java_int(relative, name):
    """La valeur d'une constante entière, lue dans sa source."""
    text = (JAVA / relative).read_text(encoding="utf-8")
    match = re.search(rf"\b{name}\s*=\s*(\d+)", text)
    if not match:
        raise SystemExit(f"constante {name} introuvable dans {relative}")
    return int(match.group(1))


def seconds(ticks, decimal):
    """Une durée en ticks, telle que le README l'écrit. 20 ticks = 1 s."""
    value = ticks / 20.0
    text = f"{value:.2f}".rstrip("0").rstrip(".")
    return text.replace(".", decimal)


def flat(path):
    """Le texte d'un README, espaces normalisés.

    La recherche ne doit pas dépendre de l'endroit où une phrase revient à la
    ligne : sans cela, reformater un paragraphe casserait un contrôle qui n'a
    rien à voir, et on finirait par écrire la documentation pour le
    vérificateur plutôt que pour le lecteur."""
    return re.sub(r"\s+", " ", path.read_text(encoding="utf-8"))


def require(label, fr_text, en_text):
    """Cette chaîne doit apparaître dans le README de sa langue."""
    for name, path, needle in (("README.fr.md", FR, fr_text), ("README.md", EN, en_text)):
        if re.sub(r"\s+", " ", needle) not in flat(path):
            fail(name, f"{label} — « {needle} » attendu, absent")


# ------------------------------------------------------------------ chiffres

def check_numbers():
    stack = java_int("tardis/CentralizerLogic.java", "XP_COST_PER_STACK")
    require("prix du Sac", f"**{stack} points d'expérience par pile**",
            f"**{stack} experience points per stack**")

    passenger = java_int("entity/EntityTeleporterLogic.java", "XP_PER_PASSENGER")
    require("prix du Téléporteur", f"**{passenger} XP** par créature", f"**{passenger} XP** per creature")

    fade_in = java_int("block/entity/TardisDoorBlockEntity.java", "FADE_IN_TICKS")
    fade_out = java_int("block/entity/TardisDoorBlockEntity.java", "FADE_OUT_TICKS")
    swing = java_int("block/TardisDoorBlock.java", "SWING_TICKS")
    require("matérialisation", f"**{seconds(fade_in, ',')} s**", f"**{seconds(fade_in, '.')} s**")
    require("dématérialisation", f"**{seconds(fade_out, ',')} s**", f"**{seconds(fade_out, '.')} s**")
    require("battement", f"**{seconds(swing, ',')} s**", f"**{seconds(swing, '.')} s**")

    spacing = java_int("tardis/TardisStateManager.java", "PLOT_SPACING")
    require("taille de parcelle", f"**{spacing} blocs**", f"**{spacing} blocks**")

    timeout = java_int("tardis/AllyLinks.java", "REQUEST_TIMEOUT_TICKS") // 20 // 60
    require("délai de connexion", f"**{timeout} minutes**", f"**{timeout} minutes**")

    fall = java_int("EnderPortalsMod.java", "ACTIVATION_FALL_DISTANCE")
    require("chute du rituel", f"**{fall} blocs**", f"**{fall} blocks**")

    radius = java_int("tardis/TardisHelper.java", "RECALL_RADIUS")
    height = java_int("tardis/TardisHelper.java", "RECALL_HEIGHT")
    require("recherche du rappel", f"**{radius} blocs** autour, **{height}**",
            f"**{radius} blocks** around, **{height}**")

    gallery = java_int("ModAdvancements.java", "GALLERY_TARGET")
    if gallery != 1000:
        fail("README", f"le progrès « galerie » vise {gallery} blocs, les README disent mille")


def check_ore():
    placed = json.loads((DATA / "worldgen/placed_feature/ender_ore.json").read_text(encoding="utf-8"))
    height = next(p for p in placed["placement"] if p["type"] == "minecraft:height_range")["height"]
    low = height["min_inclusive"]["absolute"]
    high = height["max_inclusive"]["absolute"]
    require("hauteur du minerai", f"**Y {low} et Y {high}**", f"**Y {low} and Y {high}**")

    configured = json.loads((DATA / "worldgen/configured_feature/ender_ore.json").read_text(encoding="utf-8"))
    target = configured["config"]["targets"][0]["target"]["block"]
    if target != "minecraft:end_stone":
        fail("README", f"le minerai se génère dans {target}, les README disent la pierre de l'End")


def check_book_pages():
    pages = len([k for k in json.loads((LANG / "fr_fr.json").read_text(encoding="utf-8"))
                 if k.startswith("enderportals.book.page")])
    require("pages du livre", f"**{pages} pages**", f"**{pages}-page**")


def check_neoforge_floor():
    """La version minimale de NeoForge, telle que le chargeur l'exige.

    C'est le seul chiffre du README qu'un joueur applique avant même d'avoir
    lancé le jeu. S'il est trop bas, le mod refuse de se charger et le README a
    menti ; s'il est trop haut, on écarte des installations qui marcheraient.
    La source de vérité est la plage déclarée au chargeur, pas la prose.
    """
    toml = (ROOT / "src/main/resources/META-INF/neoforge.mods.toml").read_text(encoding="utf-8")
    section = toml.split('modId = "neoforge"', 1)
    if len(section) < 2:
        raise SystemExit("dépendance neoforge introuvable dans neoforge.mods.toml")
    match = re.search(r'versionRange = "\[([0-9.]+),', section[1])
    if not match:
        raise SystemExit("plancher de version NeoForge illisible dans neoforge.mods.toml")
    floor = match.group(1)
    require("plancher NeoForge", f"**NeoForge {floor}**", f"**NeoForge {floor}**")

    compiled = re.search(r"^neo_version=(.+)$", (ROOT / "gradle.properties")
                         .read_text(encoding="utf-8"), re.M).group(1).strip()
    if tuple(int(p) for p in compiled.split(".")) < tuple(int(p) for p in floor.split(".")):
        fail("build", f"compilé contre NeoForge {compiled}, mais le mod en exige {floor} : "
                      "le build ne prouve rien de ce qu'il promet")


def check_settings():
    """Les réglages documentés, et ceux que le code déclare.

    Un nom d'option se renomme en une ligne de Java. Le tableau du README, lui,
    ne bouge pas tout seul — et une option mal recopiée ne proteste pas : le
    fichier de configuration accepte la ligne, l'ignore, et le joueur croit
    avoir réglé quelque chose. La comparaison porte donc dans les deux sens :
    rien de déclaré ne doit manquer au README, rien de documenté ne doit être
    absent du code.
    """
    text = (JAVA / "ModSettings.java").read_text(encoding="utf-8")
    declared = dict(re.findall(r'\.define\("(\w+)",\s*(true|false)\)', text))
    if not declared:
        raise SystemExit("aucun réglage trouvé dans ModSettings.java")
    for name, default in declared.items():
        row = f"`{name}` | `{default}`"
        require(f"réglage {name}", row, row)
    # Le tableau se relit ligne par ligne, sur le texte brut — pas sur la
    # version aplatie. Une ligne tronquée reste un tableau valide en markdown :
    # elle s'affiche avec une colonne vide, et personne ne le remarque. C'est
    # arrivé, et deux versions sont sorties avec un réglage documenté sans son
    # effet. Sur le texte aplati, la cellule manquante se prolongeait dans le
    # paragraphe suivant jusqu'à la barre verticale d'après, et le contrôle
    # trouvait une cellule bien remplie là où il n'y avait plus de cellule.
    row = re.compile(r"\|\s*`(\w+)`\s*\|\s*`(true|false)`\s*\|(.*)\|\s*$")
    for name, path in (("README.fr.md", FR), ("README.md", EN)):
        documented = {}
        for line in path.read_text(encoding="utf-8").splitlines():
            match = row.match(line)
            if not match:
                continue
            key, default, effect = match.groups()
            documented[key] = default
            if not effect.strip():
                fail(name, f"réglage {key} : documenté sans dire ce qu'il fait")
        if documented != declared:
            fail(name, f"réglages documentés {documented}, déclarés {declared}")


def check_version():
    text = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    version = re.search(r"^mod_version=(.+)$", text, re.M).group(1).strip()
    require("version", f"version **{version}**", f"version **{version}**")
    require("jar compilé", f"enderportals-{version}.jar", f"enderportals-{version}.jar")


# ------------------------------------------------------------------ recettes

# Ce que chaque README annonce en toutes lettres, et la recette qui en décide.
# La comparaison porte sur le décompte des ingrédients, pas sur la disposition :
# c'est ce qu'un joueur lit, et c'est ce qui se périme.
DOCUMENTED = {
    "inactive_tardis_door": {"minecraft:crying_obsidian": 7, "enderportals:ender_crystal": 1,
                             "minecraft:nether_star": 1},
    "tardis_key": {"enderportals:ender_crystal": 1, "minecraft:ender_pearl": 1,
                   "minecraft:gold_ingot": 1},
    "ender_pickaxe": {"enderportals:ender_crystal": 3, "minecraft:stick": 2},
    "centralizer": {"minecraft:iron_block": 7, "minecraft:redstone_block": 1,
                    "minecraft:ender_chest": 1},
    "ally_passage": {"minecraft:quartz_block": 4, "enderportals:ender_crystal": 4,
                     "minecraft:ender_chest": 1},
    "friendship_console": {"minecraft:quartz": 6, "minecraft:redstone": 2,
                           "enderportals:ender_crystal": 1},
    "entity_teleporter": {"minecraft:crying_obsidian": 5, "minecraft:ender_pearl": 1},
    "entity_lander": {"minecraft:crying_obsidian": 6, "enderportals:ender_crystal": 2,
                      "minecraft:slime_block": 1},
    "guide_book": {"enderportals:ender_crystal": 8, "minecraft:book": 1},
    "ender_block": {"enderportals:ender_crystal": 4},
    "ender_bricks": {"enderportals:ender_block": 4},
}

# Le titre au-dessus de la grille des crafts, dans chaque langue.
GRID_HEADING = {"README.fr.md": "### Tous les crafts", "README.md": "### Every recipe"}


def ingredients(recipe):
    """Décompte réel des ingrédients d'une recette en grille."""
    data = json.loads((DATA / "recipe" / f"{recipe}.json").read_text(encoding="utf-8"))
    counts = {}
    for row in data["pattern"]:
        for symbol in row:
            if symbol == " ":
                continue
            item = data["key"][symbol]["item"]
            counts[item] = counts.get(item, 0) + 1
    return counts


def check_recipes():
    for recipe, expected in DOCUMENTED.items():
        actual = ingredients(recipe)
        if actual != expected:
            fail("README", f"recette {recipe} : documentée {expected}, réelle {actual}")


def yield_of(recipe):
    """Combien d'objets sort une recette. Sans mention, elle en sort un."""
    data = json.loads((DATA / "recipe" / f"{recipe}.json").read_text(encoding="utf-8"))
    result = data.get("result")
    return int(result.get("count", 1)) if isinstance(result, dict) else 1


def grid(name, path):
    """Le bloc de code qui dessine les crafts, sous son titre."""
    text = path.read_text(encoding="utf-8")
    heading = GRID_HEADING[name]
    if heading not in text:
        fail(name, f"section des crafts introuvable : « {heading} »")
        return ""
    parts = text[text.index(heading):].split("```")
    if len(parts) < 3:
        fail(name, "la section des crafts n'a plus de bloc de code")
        return ""
    return parts[1]


def check_yields():
    """La grille annonce « (×N) » ; la recette décide de N.

    Un rendement se change en une ligne de JSON et se documente à trois
    endroits. Celui de l'Atterrisseur est passé de deux à un : sans ce
    contrôle, la grille aurait continué d'annoncer « ×2 » pour toujours, et
    un joueur aurait cru avoir raté son craft.
    """
    expected = sorted(n for recipe in DOCUMENTED if (n := yield_of(recipe)) > 1)
    for name, path in (("README.fr.md", FR), ("README.md", EN)):
        found = sorted(int(n) for n in re.findall(r"\(×(\d+)\)", grid(name, path)))
        if found != expected:
            fail(name, f"la grille des crafts annonce les rendements {found}, "
                       f"les recettes donnent {expected}")


# ------------------------------------------------------------------ charpente

def anchor(title):
    return re.sub(r"\s+", "-", re.sub(r"[^\w\s-]", "", title.lower(), flags=re.UNICODE).strip())


def check_structure():
    shapes = {}
    for name, path in (("README.fr.md", FR), ("README.md", EN)):
        text = path.read_text(encoding="utf-8")
        heads = re.findall(r"^(#{2,3}) (.+)$", text, re.M)
        anchors = {anchor(title) for _, title in heads}
        for link in set(re.findall(r"\]\(#([^)]+)\)", text)):
            if link not in anchors:
                fail(name, f"lien de sommaire mort : #{link}")
        shapes[name] = [level for level, _ in heads]
    if shapes["README.fr.md"] != shapes["README.md"]:
        fail("README", "les deux traductions n'ont plus la même charpente de titres")


def main():
    check_numbers()
    check_ore()
    check_book_pages()
    check_neoforge_floor()
    check_settings()
    check_version()
    check_recipes()
    check_yields()
    check_structure()
    if problems:
        for problem in problems:
            print(f"  {problem}", file=sys.stderr)
        raise SystemExit(f"README : {len(problems)} anomalie(s)")
    print("README vérifiés : chiffres, recettes et charpente d'accord avec le mod.")


if __name__ == "__main__":
    main()
