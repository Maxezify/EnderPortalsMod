# World of Ender

*[English version](README.md)* · Mod **Minecraft 1.21.1 / NeoForge** · version **0.30.0**

Vous forgez une porte d'obsidienne, vous l'éveillez au prix d'un rituel, et elle
s'ouvre sur un monde qui n'existait pas. Derrière elle, une parcelle entière vous
appartient — et vous l'emportez partout : la porte se dématérialise dans votre
poche et se repose où vous voulez.

---

## Sommaire

**Découvrir** — [En bref](#en-bref) · [Le lore](#le-lore)
**Jouer** — [Prise en main](#prise-en-main) · [La porte au quotidien](#la-porte-au-quotidien) · [Le monde de l'Ender](#le-monde-de-lender) · [Bâtir](#bâtir)
**Les machines** — [Le stockage à distance](#le-stockage-à-distance) · [Le Téléporteur d'entité](#le-téléporteur-dentité)
**À plusieurs** — [Le Passage des Alliés](#le-passage-des-alliés) · [Les degrés de confiance](#les-degrés-de-confiance)
**Référence** — [Tous les crafts](#tous-les-crafts) · [Coûts et durées](#coûts-et-durées) · [Les progrès](#les-progrès) · [Les infobulles](#les-infobulles)
**Technique** — [Serveur et compatibilité](#serveur-et-compatibilité) · [Shaders](#shaders) · [Installer et compiler](#installer-et-compiler)

---

## En bref

* **Une base transportable.** Une porte de deux blocs, posée d'un clic, qui
  contient une parcelle de 8192 blocs dans une dimension à part.
* **Un monde à creuser.** L'Ender est plein d'un bout à l'autre : pas une
  caverne, pas un tunnel. On s'y déplace en taillant sa galerie.
* **Un stockage à distance.** Où que vous soyez, videz une pile dans les coffres
  de votre base d'un clic droit.
* **Un partage choisi.** Le Passage des Alliés relie deux bases, si les deux
  joueurs le veulent — et vous décidez, ami par ami, de ce qu'il peut y faire.
* **Des matériaux translucides.** Le Bloc de l'Ender et toute sa famille de
  briques : on bâtit des murs qu'on voit au travers.

---

## Le lore

Le joueur a l'Overworld, et sous l'Overworld, le Nether.
L'Enderman a l'End, et sous l'End, **l'Ender**.

Les Endermen ne ramassent pas des blocs au hasard. Chaque bloc soulevé est une
offrande : de la matière soustraite au monde, emportée pour être sacrifiée — et
ce qui est sacrifié rejoint l'Ender, là où les choses détruites viennent se
reposer. Le paradis des cubes.

Votre porte n'ouvre pas sur un monde neuf. Elle ouvre sur le leur.

---

## Prise en main

### 1. Le Cristal de l'Ender

Le **Minerai de l'Ender** se génère **dans la pierre de l'End**, entre **Y 10 et
Y 70**, en petits amas. Il n'apparaît nulle part ailleurs : le mod commence donc
par un voyage dans l'End.

Une **pioche en fer** suffit. Le minerai rend des **Cristaux de l'Ender** —
Fortune augmente le rendement, Silk Touch ramasse le bloc intact. Tout le reste
du mod se construit à partir de ces cristaux.

### 2. Forger la porte

**7 obsidiennes pleureuses + 1 cristal + 1 nether star** donnent une **Porte de
l'Ender inactive**. À ce stade elle ne fait rien : c'est un caisson de deux blocs
qui se pose et se récupère à la pioche. Elle dort.

### 3. Le rituel de la Mace

Pour l'éveiller, il faut la frapper assez fort pour fêler l'espace.

Posez la porte, munissez-vous d'une **Mace** vanilla, montez à **20 blocs au
moins** au-dessus d'elle, sautez, et **frappez-la en pleine chute** (clic
gauche). C'est l'attaque écrasante de la Mace, appliquée à une porte.

L'impact absorbe vos dégâts de chute, la foudre tombe, la porte s'éveille. La
Mace n'est pas consommée, seulement un peu usée.

> **Une seule porte par joueur.** Le rituel refuse d'en éveiller une seconde.

### 4. Forger la clé

**1 cristal + 1 perle de l'Ender + 1 lingot d'or**, en colonne, donnent la **Clé
de l'Ender**. **Clic droit dans le vide** pour la lier à votre porte : elle
n'obéira qu'à vous.

### 5. La pioche

**3 cristaux + 2 bâtons** donnent la **Pioche de l'Ender**. Elle est la seule à
récolter les Blocs de l'Ender, et elle les casse presque instantanément.

À défaut, l'enchantement **« Brisure d'Espace-Temps »** permet à n'importe quelle
pioche de les casser avec butin, à vitesse correcte. C'est un enchantement de
**trésor**, comme Raccommodage : on le trouve en butin ou en troc avec un
bibliothécaire, jamais à la table d'enchantement.

---

## La porte au quotidien

| Vous faites… | Il se passe… |
| --- | --- |
| Clic droit sur la porte, **à la main** | Elle s'ouvre, ou se referme. |
| Clic droit sur la porte, **la clé en main** | Pareil — la clé ne vous oblige pas à la ranger. |
| **Clé** + **accroupi** + clic droit **par terre** | La porte se matérialise à l'endroit visé, et disparaît de son ancien emplacement. |
| **Clé** + **accroupi** + clic droit sur la porte | Elle se dématérialise : votre base repart dans votre poche. |
| **Clé** + clic droit **dans le vide** | Lie la clé à votre porte. |
| Clic droit sur la **porte intérieure**, porte rangée | Elle se rematérialise dehors, ouverte : c'est votre sortie. |

Ce que la clé seule peut faire, c'est **faire apparaître et disparaître** la
porte, et se lier à elle. Ouvrir et fermer se fait à la main.

**Tout cela n'obéit qu'au propriétaire.** Un autre joueur qui clique votre porte,
à la main comme à la clé, lit « verrouillée ».

**Un geste à la fois.** Tant que le fondu dure, la porte refuse l'ordre suivant
et répond « La porte n'a pas fini son passage. » Elle obéit de nouveau dès
qu'elle a fini de réapparaître.

### Ce que vous voyez pendant le fondu

Une **onde de lumière** blanche à bords violets remonte les quatre faces du
caisson, du seuil au linteau — et redescend à la disparition. Les **grains de
portail convergent** depuis une couronne d'un bon mètre et se resserrent à mesure
que la porte prend ; quand elle s'en va, tout repart vers l'extérieur. La porte
**tremble** tant qu'elle n'est pas tout à fait là, puis se stabilise : elle ne
claque pas à l'opacité pleine, elle se pose. Elle **atterrit** sur une couronne
d'étincelles, un choc grave et une résonance d'améthyste.

### Les trois sécurités

**Perdre sa clé ne coûte pas sa base.** Reforgez-en une et faites un clic droit
dans le vide : elle se relie à votre porte, où qu'elle soit. Sans cela, une porte
dématérialisée n'offrirait plus rien à cliquer, et le rituel refuse d'en éveiller
une seconde.

**La clé n'obéit qu'à vous.** Perdue, jetée ou volée, elle est inerte dans les
mains d'un autre : elle ne matérialise rien, n'ouvre rien, et ne se relie pas à
votre porte.

**Le rappel ne peut pas échouer.** Si l'on a bâti à l'endroit exact où dormait
votre porte pendant que vous étiez à l'intérieur, le rappel cherche, dans
l'ordre : l'emplacement mémorisé, puis un espace libre à proximité (**8 blocs**
autour, **4** au-dessus ou en dessous), puis votre point de réapparition. Il vous
donne les coordonnées s'il a dû la déplacer.

> **Poser** la porte au sol depuis l'extérieur, en revanche, peut être refusé :
> là, c'est vous qui avez désigné l'endroit, et vous n'êtes enfermé nulle part.

---

## Le monde de l'Ender

Un monde **entièrement souterrain**, taillé dans une masse de Blocs de l'Ender :
gris, translucides comme le bloc de miel, doucement luminescents.

Au travers de cette matière, on **entrevoit les blocs-reliques** venus mourir
ici — pierre, troncs, minerais jusqu'au diamant, bibliothèques, éponges. Ils sont
groupés en **nuées** d'une même matière, si diffuses qu'aucun bloc n'en touche un
autre.

De rares **filons de froglight perlescent** la traversent en longs semis de
lueurs violettes. Ce sont les seuls repères — et la seule lumière : partout
ailleurs, l'obscurité est totale. Une cendre claire tombe en continu.

**La masse est pleine.** Pas une caverne, pas un vide. On ne s'y déplace qu'à la
Pioche de l'Ender, en creusant sa propre galerie.

### Votre parcelle

| | |
| --- | --- |
| Taille | **8192 blocs** de côté |
| Hauteur du monde | −64 à 320, fermé en haut et en bas par du bedrock |
| Salle d'arrivée | Y 64 |
| Lits | ✔ on y dort, on y fixe son point de réapparition |
| Ancres de réapparition | ✘ elles explosent, comme dans l'Overworld |
| Portails du Nether | ✘ ils ne s'allument pas |

Les parcelles sont séparées par un **quadrillage de murs de bedrock**, du
plancher au plafond du monde. Vous pouvez creuser dans toutes les directions sans
jamais tomber sur la base de quelqu'un d'autre, ni qu'on tombe sur la vôtre. Le
Passage des Alliés est la porte que l'on ouvre volontairement dans ce mur.

Le pseudo du propriétaire s'affiche sur un petit panneau à l'avant de la porte.

---

## Bâtir

**4 cristaux** donnent un **Bloc de l'Ender** ; **4 Blocs de l'Ender** donnent
**4 Briques de l'Ender**. La famille se décline en **escaliers, dalles, muret**
et une variante **ciselée**, gravée de l'œil des cadres de portail de l'End —
tout se taille aussi au tailleur de pierre.

Les briques sont **translucides comme le bloc dont elles sortent** : un mur
laisse deviner ce qu'il y a derrière, et un mur épais ne s'assombrit pas couche
après couche. En main et dans l'inventaire, en revanche, elles sont **pleines** :
voir le paysage au travers du cube qu'on tient ne ressemblait à rien.

---

## Le stockage à distance

Deux objets transforment votre base en entrepôt joignable de n'importe où.

**Le Transmetteur d'objet** (7 blocs de fer, 1 bloc de redstone, 1 coffre de
l'Ender) se pose **dans l'Ender**, contre vos rangements. Il fédère tout le
réseau accolé, de bloc en bloc.

**Le Sac de l'Ender** (un bundle + un coffre de l'Ender) se garde dans
l'inventaire. Le geste marche **dans les deux sens** :

* prenez une pile au curseur, puis **clic droit sur le sac** ;
* ou gardez le sac au curseur, et **clic droit sur une pile**.

La pile part dans le réseau, où que vous soyez. **3 points d'expérience par
pile** — le prix du voyage, pas du poids : soixante-quatre blocs coûtent autant
qu'un objet seul. Réseau plein ou expérience insuffisante : rien ne part, la pile
reste au curseur, un son sec le dit et le message donne la raison.

> **Attention au second sens.** On déplace un sac dans son inventaire plus
> souvent qu'on ne range, et pendant ce déplacement un clic droit sur une pile
> l'expédie.

### Il se branche sur votre mod de rangement

Le Transmetteur ne remplace pas votre système de stockage, il **s'y raccorde** :
il reconnaît tout rangement qui expose un inventaire au standard NeoForge, donc
en pratique la quasi-totalité des mods.

* **Sophisticated Storage** — coffres, tonneaux, shulkers de tous tiers, et le
  Storage Controller.
* **Sophisticated Backpacks** — sacs à dos posés au sol.
* **Tom's Simple Storage** — collez le Transmetteur à un Inventory Connector : le
  Sac déverse dans tout le réseau, et le Storage Terminal affiche vos objets,
  comptés une seule fois.
* Coffres et tonneaux vanilla, drawers, et le reste.

Le dépôt passant par l'insertion standard, il **respecte les filtres et les
upgrades** de chaque rangement : un tonneau Sophisticated filtré fait le tri
automatique de ce que vous déversez.

---

## Le Téléporteur d'entité

La clé ne prend que vous. Pour emmener une bête dans l'Ender, il faut deux
pièces :

* **L'Atterrisseur d'entité** (6 obsidiennes pleureuses, 2 cristaux, 1 bloc de
  slime) se pose **dans l'Ender**, là où la bête doit arriver.
* **Le Téléporteur d'entité** (5 obsidiennes pleureuses, 1 perle de l'Ender) est
  une coque qui se pose et se charge **comme une barque**.

**Le geste tient en trois temps.** Le téléporteur en main, **clic droit sur un
Atterrisseur** : la coque retient ce point. **Posez-la** où vous voulez et
**attirez la bête dessus**. **Clic droit sur la coque** : elle part.

| | |
| --- | --- |
| **Témoin vert** | destination *et* passager : la coque peut partir |
| **Témoin rouge** | il manque l'un ou l'autre |
| **Prix** | **20 XP** par créature, prélevés à l'arrivée |
| **Clic droit à vide** | reprend la coque en main, avec son lien |
| **Accroupi + clic droit** | reprend la coque **et libère** ce qu'elle transporte |

Le témoin parle de la machine, pas du terrain : savoir si l'Atterrisseur est
toujours en place demanderait de charger son chunk en permanence. Cette
vérification-là se fait au départ, et vous êtes prévenu s'il a disparu.

### La charge, et le départ

Le voyage ne part pas au clic : les chunks autour de l'Atterrisseur sont
**demandés, puis attendus**, ce qui prend de zéro à quelques secondes. La machine
met cette attente en scène.

Un **anneau de lumière** remonte la coque en boucle — la même lumière que l'onde
de la porte. La coque **aspire** : les grains convergent, et les quatre cristaux
crachent chacun leur tour une étincelle. Une **note monte**, de plus en plus
haut, tant que le terrain n'est pas prêt.

Au départ, la coque s'en va sur une **colonne d'étincelles** et une gerbe violette
qui s'engouffre à l'endroit qu'elle vient de quitter — c'est ce que vous voyez,
puisque vous restez de ce côté-ci. Si le voyage est refusé après le début de la
charge, l'anneau s'éteint sur un **déclic sec**. Recliquer pendant la charge ne
relance rien.

Une fois posée, la coque et sa passagère ne font qu'**une seule fiche** dans la
sauvegarde : le chunk peut se décharger derrière elles sans que rien ne se perde,
exactement comme un cochon en barque à l'autre bout du monde.

---

## Le Passage des Alliés

Le **Passage des Alliés** (4 blocs de quartz, 4 cristaux, 1 coffre de l'Ender)
est une arche qui se pose **dans votre parcelle**. Le **Contrôle de l'amitié**
(6 quartz, 2 redstone, 1 cristal) se pose **contre elle** : c'est lui qui la
commande.

**Posez autant de passages que vous avez d'amis à relier.** Chaque arche porte le
nom de l'allié auquel elle mène, et chaque Contrôle commande l'arche qu'il
touche. Une arche close est une arche liée à personne, et elle ne porte aucun
nom.

### 1. Devenir amis

Chaque porte éveillée reçoit un **code de huit chiffres**, tous compris entre 1
et 9 — c'est pour cela que le pavé n'a pas de touche zéro. Vous le lisez sur
l'infobulle de votre clé et sous le pavé du Contrôle.

Dictez le vôtre, tapez celui de l'autre, **et qu'il fasse de même** : l'amitié
est la conjonction de deux déclarations. Tant qu'un seul a tapé, le nom apparaît
avec un **sablier**.

### 2. Ouvrir le passage

**Cliquez le nom** d'un ami dans le carnet : une demande part, valable **2
minutes**. Son nom clignote en orange chez lui ; il clique à son tour, et les
deux arches s'ouvrent l'une sur l'autre. Le liseré passe au **vert**.

Pour refermer, cliquez le nom entouré de vert — depuis n'importe lequel de vos
Contrôles. **Maj + clic** retire l'ami du carnet.

### 3. Le terminal

Le bas du panneau porte le journal de l'appareil : demandes, ouvertures,
fermetures, refus. Un **témoin** dans son en-tête dit si ce Contrôle commande
bien un passage, et une **ligne d'état** en bas en donne la cause quand il ne
fonctionne pas — panneau posé loin de toute arche, arche libre, arche cassée à
l'autre bout.

Les messages reçus pendant votre absence vous attendent à la prochaine ouverture.

---

## Les degrés de confiance

Ouvrir un passage n'est pas tout donner. À droite de chaque nom, dès que le lien
est **vert**, une pastille dit ce que cet allié peut faire **chez vous**. Cliquez
dessus : elle passe au degré suivant, et revient au premier après le dernier.

| Degré | Entrer | Actionner vos blocs | Casser et poser |
| --- | :---: | :---: | :---: |
| **Visiteur** | ✔ | | |
| **Invité** | ✔ | ✔ | |
| **Associé** | ✔ | ✔ | ✔ |

« Actionner » couvre **tout ce qui s'ouvre ou se déclenche** : coffres, fours,
établis, leviers. Un visiteur ne les touche pas — mais il **circule** : portes,
trappes, portillons, boutons et plaques de pression lui restent ouverts.

Trois choses à savoir :

* **Le degré est à sens unique.** Vous ouvrir mes coffres ne m'oblige pas à
  ouvrir les vôtres. Chacun règle sa parcelle, sur son propre panneau.
* **Il survit à la fermeture du passage.** Le redemander à chaque réouverture
  aurait fait d'un réglage une corvée — et poussé tout le monde à laisser
  l'associé par défaut.
* **Tout le monde commence visiteur**, et **il n'y a pas d'exception**, pas même
  pour les opérateurs. Un administrateur garde le mode créatif et les commandes,
  qui ne passent pas par là.

L'allié est prévenu au terminal à chaque changement. Oublier un allié remet son
degré à zéro dans les deux sens.

> **Ce qui est gardé, et ce qui ne l'est pas.** Les trois voies directes sont
> tenues : casser, poser, actionner. Une créature apprivoisée tuée, un cadre
> d'objet vidé ou un TNT allumé depuis l'extérieur de la parcelle restent
> possibles. C'est une portée annoncée, pas une étanchéité prétendue.
>
> **Si vous refermez le passage pendant qu'un ami est chez vous**, il sort par
> votre porte intérieure — si elle est ouverte. Fermée, il ne peut pas l'ouvrir,
> et votre parcelle est cloisonnée de bedrock. Refermez quand il est rentré.

La liste de ce qu'un visiteur peut actionner est le tag
`enderportals:visitor_usable`, fait de tags de vanilla : les portes des mods qui
s'inscrivent dans `#minecraft:doors` en profitent, et un pack qui veut y ajouter
les leviers n'a qu'une ligne à écrire.

### Avec Immersive Portals

S'il est installé, la porte comme le passage deviennent des portails **« voir au
travers »** : on aperçoit l'autre côté avant de franchir le seuil. Sans lui, le
mod bascule seul sur sa téléportation au contact.

---

## Référence

### Tous les crafts

```
Porte inactive       Clé            Pioche de l'Ender    Sac de l'Ender
O O O                C              C C C                (sans forme)
O C N                P              . S .                Bundle
O O O                G              . S .                + Coffre de l'Ender

Transmetteur d'objet  Passage des Alliés   Contrôle de l'amitié   Guide (livre)
I R I                 Q C Q                q q q                  C C C
I E I                 C E C                r C r                  C L C
I I I                 Q C Q                q q q                  C C C

Téléporteur d'entité   Atterrisseur         Bloc de l'Ender    Briques (×4)
. . .                  O S O                C C                B B
O p O                  O C O                C C                B B
O O O                  O C O

O = Obsidienne pleureuse   C = Cristal de l'Ender    N = Nether Star
p = Perle de l'Ender       G = Lingot d'or           S = Bâton / Bloc de slime
I = Bloc de fer            E = Coffre de l'Ender     R = Bloc de redstone
Q = Bloc de quartz         q = Quartz                r = Redstone
L = Livre                  B = Bloc de l'Ender
```

Escaliers, dalles, murets et briques ciselées se taillent depuis les Briques de
l'Ender, à l'établi comme au tailleur de pierre. Le rituel d'éveil demande en
plus une **Mace** vanilla, qui n'est pas consommée.

**Le Guide de World of Ender** (8 cristaux autour d'un livre) est un livre écrit
de **32 pages** : le lore, chaque machine et chaque craft, dans la langue du jeu.

### Coûts et durées

| | |
| --- | --- |
| Sac de l'Ender | **3 XP** par pile expédiée |
| Téléporteur d'entité | **20 XP** par créature, prélevés à l'arrivée |
| Rituel d'éveil | chute de **20 blocs** minimum |
| Matérialisation de la porte | **1,75 s** |
| Dématérialisation | **1,5 s** |
| Ouvrir ou fermer la porte | **0,5 s** avant le geste suivant |
| Demande de connexion | valable **2 minutes** |
| Recherche du rappel | **8 blocs** autour, **4** au-dessus ou en dessous |
| Minerai de l'Ender | Y 10 à 70, dans la pierre de l'End |

### Les progrès

Un arbre complet, **« Le paradis des cubes »** :

*Une pierre qui n'est pas d'ici* → *Le seuil* → *Frapper le ciel* → *Le paradis
des cubes*, puis cinq branches — *Tailler sa galerie* (mille Blocs de l'Ender),
*Le fond du monde*, *L'entrepôt* → *Tout ranger d'un geste*, *Bâtir dans le
translucide*, et *Ami de passage*.

### Les infobulles

Chaque machine a de quoi remplir cinq ou six lignes. Le mode d'emploi ne se
déroule donc que si vous **maintenez MAJ**, comme dans les mods de rangement.

Ce qui reste visible sans rien appuyer, c'est **l'état de cet exemplaire-là** :
la porte à laquelle cette clé est liée et son code d'ami, l'Atterrisseur que vise
ce téléporteur. C'est ce qui distingue deux objets dans un coffre.

---

## Serveur et compatibilité

Le mod est **serveur-autoritaire** : toute la logique — rituel, clé, passages,
stockage, permissions — s'exécute côté serveur, le client ne fait qu'afficher. Il
fonctionne tel quel en serveur dédié.

Il est aussi conçu pour cohabiter : **aucun mixin**, des tags vanilla purement
additifs, des écouteurs d'événements limités à ses propres blocs et à sa propre
dimension, et un générateur de dimension autonome. La clé et le rituel respectent
la **spawn protection**, le **mode aventure** et les **mods de protection de
terrain**.

**Immersive Portals** est optionnel : présent, il donne des portails « voir au
travers » ; absent, ou si son API change, le mod bascule seul sur sa
téléportation classique et le signale dans les logs.

---

## Shaders

L'Ender se déclare auprès des shaders **comme le Nether** : c'est ce qui lui
donne son rendu souterrain, sans horizon ni ciel, avec son brouillard et sa
tempête de cendres. **Il n'y a rien à configurer.** Testé avec Complementary
Reimagined et Photon.

Le regard porte au travers de la masse translucide, donc très loin. Un fondu au
noir ferme la vue avant la limite des chunks — vous ne verrez jamais le bord du
monde chargé. De loin en loin, ce lointain s'embrase une fraction de seconde : de
silencieuses lueurs d'orage.

**Un réglage de Photon vaut d'être connu** : *Light Sources →
**NETHER_USE_BIOME_COLOR***. Activé — c'est son défaut — Photon prend la couleur
que le monde lui donne, donc la nôtre. Désactivé, il retombe sur son orange du
Nether et l'Ender vire au feu. Laissez-le tel quel.

**Envie d'un flou de profondeur ?** Complementary le fournit, désactivé par
défaut : *Camera Settings → World Blur → World Blur → **Distance Blur***, puis
l'intensité sur le curseur *« Dis. Blur — The Nether »*.

### Forcer une autre passe de rendu

Rien ne vous oblige à garder celle du Nether. Une ligne dans le fichier
`dimension.properties` du shaderpack (dans son dossier `shaders/`, à créer s'il
n'existe pas) redirige le monde où vous voulez — Photon a par exemple un
brouillard volumétrique dédié à l'End :

```properties
dimension.world1=minecraft:the_end enderportals:ender_world
dimension.world0=*
```

Cette correspondance explicite l'emporte sur tout le reste : c'est le seul
endroit d'où l'on puisse changer d'avis, un mod ne pouvant pas écrire dans un
shaderpack.

### Une pause au premier passage

À la toute première ouverture de la porte dans une session, l'écran se fige deux
à quatre secondes : c'est **Iris qui compile sa passe de rendu** pour une
dimension jamais encore apparue, sur le fil d'affichage, et aucun mod n'a la main
dessus. Immersive Portals la rend visible en créant le monde d'arrivée pendant
que vous êtes encore dehors — la même compilation existe pour un portail du
Nether, mais elle s'y cache derrière l'écran de chargement.

Elle ne se paie **qu'une fois par session**, et pas du tout sans shaders.

---

## Installer et compiler

Placez le jar dans `mods/`, avec **NeoForge 21.1.x** pour **Minecraft 1.21.1**.
Chaque build est publié dans la pré-release
[`dev-latest`](../../releases/tag/dev-latest).

Pour compiler — prérequis **Java 21** :

```bash
./gradlew build
# → build/libs/enderportals-0.30.0.jar
```

Le build est géré par **ModDevGradle** ; NeoForge et les mappings officiels sont
téléchargés automatiquement, les versions épinglées dans `gradle.properties`.

> Le mod s'appelle *World of Ender*, mais son identifiant interne reste
> `enderportals` — il est inscrit dans chaque bloc, objet, recette et sauvegarde
> existante. En changer casserait tous les mondes déjà créés.
