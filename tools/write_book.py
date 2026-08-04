#!/usr/bin/env python3
"""Réécrit les pages du livre-guide, mesurées pour tenir dans le cadre.\n\nLe texte du livre vit ici, en un seul endroit et dans les deux langues, plutôt\nqu'éparpillé dans les fichiers de langue. Chaque page est mesurée avant\nd'être écrite : le script refuse d'en produire une qui déborderait.\n"""
import json
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import book as measure  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent
LANG = ROOT / "src/main/resources/assets/enderportals/lang"

FR = [
    # 1
    "§5L'Offrande§0\n\nLe joueur a l'Overworld, "
    "et sous l'Overworld, le Nether.\n\n"
    "L'Enderman a l'End, et sous l'End, l'§5Ender§0.",
    # 2
    "§5L'Offrande§0\n\nQuand un Enderman "
    "soulève un bloc, il ne le vole pas.\n\n"
    "Il l'emporte pour le sacrifier — et ce qui est "
    "sacrifié rejoint l'Ender.",
    # 3
    "§5Le Paradis des Cubes§0\n\nLà dort la matière "
    "défaite, prise dans une masse grise et "
    "translucide.\n\nVotre porte n'ouvre pas sur un "
    "monde neuf. Elle ouvre sur le leur.",
    # 4
    "§5La Masse§0\n\nPas une caverne, pas un tunnel : "
    "l'Ender est plein d'un bout à l'autre.\n\n"
    "On n'y avance qu'en taillant sa galerie.",
    # 5
    "§5Les Reliques§0\n\nAu travers du gris, on "
    "entrevoit les blocs venus mourir ici, groupés "
    "en nuées diffuses.\n\nDe rares filons violets "
    "sont les seuls repères — et la seule lumière.",
    # 6
    "§5Le Minerai§0\n\nDans la pierre de l'End "
    "affleure un minerai ancien.\n\nUne pioche de "
    "fer suffit. La Fortune en tire davantage.",
    # 7
    "§5La Porte dormante§0\n\nCraft (établi) :\n"
    "§5O O O§0\n§5O §bC §6★§0\n§5O O O§0\n\n"
    "§5O§0 = Obsidienne pleureuse\n"
    "§bC§0 = Cristal de l'Ender\n"
    "§6★§0 = Étoile du Nether",
    # 8
    "§5Le Rituel§0\n\nSeul un impact colossal peut "
    "l'éveiller.\n\nPosez la porte. Munissez-vous "
    "d'une §cMasse§0, montez à §c20 blocs§0 "
    "au-dessus, et sautez.",
    # 9
    "§5L'Impact§0\n\nFrappez la porte en pleine "
    "chute.\n\nL'impact absorbera votre chute. La "
    "foudre fera le reste : la porte s'éveille.",
    # 10
    "§5La Clé§0\n\nCraft (vertical) :\n"
    "§bCristal de l'Ender§0\n§3Perle de l'Ender§0\n"
    "§6Lingot d'or§0\n\nClic droit dans le vide : "
    "elle se lie à §5votre§0 porte.",
    # 11
    "§5La Clé, au sol§0\n\nClic droit par terre : la "
    "porte se matérialise.\n\nSur la porte : elle "
    "s'ouvre. Une fois encore : elle se referme et "
    "s'efface.",
    # 12
    "§5La Clé perdue§0\n\nElle n'obéit qu'à vous. "
    "Ramassée par un autre, elle est inerte.\n\n"
    "Perdue, reforgez-en une : elle retrouve votre "
    "porte. Rien n'est perdu.",
    # 13
    "§5La Pioche de l'Ender§0\n\nCraft : 3 cristaux "
    "en ligne, 2 bâtons.\n\nElle seule entame les "
    "§7Blocs de l'Ender§0, et les brise comme du "
    "verre.",
    # 14
    "§5Les Briques§0\n\n4 cristaux = 1 Bloc\n"
    "4 Blocs = 4 Briques\n\nTranslucides comme lui : "
    "un mur qu'on voit au travers.\n\nEscaliers, "
    "dalles, muret et brique ciselée suivent.",
    # 15
    "§5Votre base de poche§0\n\nDerrière la porte : "
    "votre parcelle, taillée dans la caverne "
    "infinie.\n\nUne seule porte par personne.",
    # 16
    "§5Dormir ici§0\n\nLes §alits§0 y fonctionnent, "
    "et y fixent votre réapparition.\n\nLes "
    "§cancres§0, non : elles y explosent. Nul "
    "portail du Nether n'y prend feu.",
    # 17
    "§5La Sortie§0\n\nOn a bâti à l'endroit exact où "
    "dormait votre porte ?\n\nVous n'êtes pas "
    "prisonnier pour autant.",
    # 18
    "§5Le Rappel§0\n\nCliquez la porte intérieure. "
    "Elle se repose à côté, ou à défaut à votre "
    "point de réapparition.\n\nElle vous en donne "
    "les coordonnées.",
    # 19
    "§5Le Transmetteur§0\n\nCraft (établi) :\n"
    "§7I §4R §7I§0\n§7I §5E §7I§0\n§7I I I§0\n\n"
    "§7I§0 = Bloc de fer\n§4R§0 = Bloc de redstone\n"
    "§5E§0 = Coffre de l'Ender",
    # 20
    "§5Le Transmetteur§0\n\nPosez-le dans votre "
    "base, et collez-lui vos rangements.\n\nDe bloc "
    "en bloc, le réseau grandit.",
    # 21
    "§5Le Sac de l'Ender§0\n\nCraft : un sac et un "
    "coffre de l'Ender.\n\nEn §bseconde main§0, clic "
    "droit : la ligne du haut de votre inventaire "
    "file dans votre base.",
    # 22
    "§5Le Prix§0\n\nChaque case rangée coûte un peu "
    "d'§aexpérience§0.\n\nRéseau plein, ou trop peu "
    "d'XP : rien ne part, et on vous le dit.",
    # 23
    "§5Le Grand Réseau§0\n\nLe Transmetteur parle la "
    "langue commune du rangement.\n\nTonneaux, sacs "
    "à dos, Sophisticated, Tom's : tous répondent, "
    "et les filtres trient seuls.",
    # 24
    "§5Le Passage§0\n\nCraft (établi) :\n"
    "§7Q §bC §7Q§0\n§bC §5E §bC§0\n§7Q §bC §7Q§0\n\n"
    "§7Q§0 = Bloc de quartz\n§bC§0 = Cristal\n"
    "§5E§0 = Coffre de l'Ender",
    # 25
    "§5Le Passage§0\n\nÀ poser dans l'Ender, un "
    "§6Contrôle de l'amitié§0 collé contre lui.\n\n"
    "§lUn passage par ami§r : posez-en autant que vous "
    "voulez, chacun avec son panneau.",
    # 26
    "§5Le Code d'ami§0\n\nVotre clé porte un code de "
    "huit chiffres.\n\nDictez-le. Tapez celui de "
    "l'autre au panneau, et qu'il tape le vôtre : "
    "vous voilà amis.",
    # 27
    "§5Ouvrir§0\n\nCliquez le nom de votre allié, "
    "qu'il clique le vôtre.\n\nLes deux passages "
    "s'ouvrent, et l'on va librement chez l'autre.",
    # 28
    "§5Toujours ouvert§0\n\nLe lien tient tant que "
    "vous le voulez, même quand l'autre est "
    "déconnecté.\n\nPour le rompre, recliquez son "
    "nom.\n\nBon voyage.",
]

EN = [
    "§5The Offering§0\n\nThe player has the "
    "Overworld, and beneath it, the Nether.\n\n"
    "The Enderman has the End, and beneath it, the "
    "§5Ender§0.",

    "§5The Offering§0\n\nWhen an Enderman lifts a "
    "block, it is not stealing it.\n\nIt carries it "
    "away to be given up — and what is given up "
    "reaches the Ender.",

    "§5The Paradise of Cubes§0\n\nThere sleeps "
    "undone matter, caught in a grey translucent "
    "mass.\n\nYour door does not open onto a new "
    "world. It opens onto theirs.",

    "§5The Mass§0\n\nNo cave, no tunnel: the Ender "
    "is solid from end to end.\n\nYou only move "
    "through it by carving your own gallery.",

    "§5The Relics§0\n\nThrough the grey you glimpse "
    "the blocks that came here to die, gathered in "
    "diffuse clouds.\n\nRare violet veins are the "
    "only landmarks — and the only light.",

    "§5The Ore§0\n\nAn ancient ore surfaces in the "
    "End's stone.\n\nAn iron pickaxe is enough. "
    "Fortune yields more.",

    "§5The Dormant Door§0\n\nCraft (bench):\n"
    "§5O O O§0\n§5O §bC §6★§0\n§5O O O§0\n\n"
    "§5O§0 = Crying Obsidian\n§bC§0 = Ender Crystal\n"
    "§6★§0 = Nether Star",

    "§5The Ritual§0\n\nOnly a colossal impact can "
    "awaken it.\n\nPlace the door. Take a §cMace§0, "
    "climb §c20 blocks§0 above it, and jump.",

    "§5The Impact§0\n\nStrike the door mid-fall.\n\n"
    "The impact will absorb your fall. Lightning "
    "does the rest: the door wakes.",

    "§5The Key§0\n\nCraft (vertical):\n"
    "§bEnder Crystal§0\n§3Ender Pearl§0\n"
    "§6Gold Ingot§0\n\nRight-click the air: it binds "
    "to §5your§0 door.",

    "§5The Key, in use§0\n\nRight-click the ground: "
    "the door materialises.\n\nOn the door: it "
    "opens. Once more: it closes and fades away.",

    "§5A Lost Key§0\n\nIt answers to you alone. "
    "Picked up by another, it is inert.\n\nLost it? "
    "Forge another: it finds your door again.",

    "§5The Ender Pickaxe§0\n\nCraft: 3 crystals in a "
    "row, 2 sticks.\n\nIt alone can carve §7Ender "
    "Blocks§0, shattering them like glass.",

    "§5The Bricks§0\n\n4 crystals = 1 Block\n"
    "4 Blocks = 4 Bricks\n\nTranslucent like it: a "
    "wall you can see through.\n\nStairs, slabs, "
    "wall and a chiselled brick follow.",

    "§5Your pocket base§0\n\nBehind the door: your "
    "plot, carved out of the endless cavern.\n\nOne "
    "door per person.",

    "§5Sleeping here§0\n\n§aBeds§0 work, and set "
    "your respawn.\n\n§cAnchors§0 do not: they "
    "explode. No Nether portal will light.",

    "§5The Way Out§0\n\nSomeone built on the very "
    "spot where your door slept?\n\nYou are not "
    "trapped.",

    "§5The Recall§0\n\nClick the inner door. It "
    "settles nearby, or failing that at your respawn "
    "point.\n\nIt gives you the coordinates.",

    "§5The Transmitter§0\n\nCraft (bench):\n"
    "§7I §4R §7I§0\n§7I §5E §7I§0\n§7I I I§0\n\n"
    "§7I§0 = Iron Block\n§4R§0 = Redstone Block\n"
    "§5E§0 = Ender Chest",

    "§5The Transmitter§0\n\nPlace it in your base, "
    "and attach your storage to it.\n\nBlock to "
    "block, the network grows.",

    "§5The Ender Bag§0\n\nCraft: a bundle and an "
    "ender chest.\n\nIn your §boffhand§0, "
    "right-click: the top row of your inventory "
    "flies home.",

    "§5The Price§0\n\nEach stored slot costs a "
    "little §aXP§0.\n\nNetwork full, or too little "
    "XP: nothing leaves, and you are told so.",

    "§5The Great Network§0\n\nThe Transmitter speaks "
    "the common tongue of storage.\n\nBarrels, "
    "backpacks, Sophisticated, Tom's: all answer, "
    "and filters sort on their own.",

    "§5The Passage§0\n\nCraft (bench):\n"
    "§7Q §bC §7Q§0\n§bC §5E §bC§0\n§7Q §bC §7Q§0\n\n"
    "§7Q§0 = Quartz Block\n§bC§0 = Crystal\n"
    "§5E§0 = Ender Chest",

    "§5The Passage§0\n\nPlace it in the Ender, with "
    "a §6Friendship Control§0 flush against it.\n\n"
    "§lOne passage per friend§r: place as many as you "
    "like, each with its own panel.",

    "§5The Friend Code§0\n\nYour key carries an "
    "eight-digit code.\n\nRead it out. Type the "
    "other's on the panel, and let them type yours: "
    "you are friends.",

    "§5Opening§0\n\nClick your ally's name, and let "
    "them click yours.\n\nBoth passages open, and "
    "each walks freely into the other's base.",

    "§5Always open§0\n\nThe link holds as long as "
    "you want, even while the other is offline.\n\n"
    "To break it, click their name again.\n\nSafe "
    "travels.",
]


def main():
    assert len(FR) == len(EN), f"{len(FR)} pages FR, {len(EN)} pages EN"
    worst = 0
    for label, pages in (("fr", FR), ("en", EN)):
        for index, text in enumerate(pages, 1):
            lines = len(measure.wrap(text))
            worst = max(worst, lines)
            if lines > measure.PAGE_LINES:
                print(f"!! {label} page {index} : {lines} lignes")
                print(measure.preview(text))
    if worst > measure.PAGE_LINES:
        raise SystemExit("des pages débordent")

    for lang, pages in (("fr_fr", FR), ("fr_ca", FR), ("en_us", EN)):
        path = LANG / f"{lang}.json"
        keys = json.loads(path.read_text(encoding="utf-8"))
        for old in range(1, 100):
            keys.pop(f"enderportals.book.page{old}", None)
        for index, text in enumerate(pages, 1):
            keys[f"enderportals.book.page{index}"] = text
        path.write_text(json.dumps(keys, ensure_ascii=False, indent=2) + "\n",
                        encoding="utf-8")
        print(f"{lang} : {len(pages)} pages écrites")

    source = ROOT / "src/main/java/com/maxezify/enderportals/item/GuideBook.java"
    text = source.read_text(encoding="utf-8")
    import re
    text = re.sub(r"PAGE_COUNT = \d+", f"PAGE_COUNT = {len(FR)}", text)
    source.write_text(text, encoding="utf-8")
    print(f"PAGE_COUNT = {len(FR)}")
    print(f"page la plus haute : {worst} lignes (capacité {measure.PAGE_LINES})")


if __name__ == "__main__":
    main()
