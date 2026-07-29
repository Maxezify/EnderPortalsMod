# Ender Portals — la Porte de l'Ender, une base de poche

Mod **Minecraft 1.21.1 / NeoForge** : une porte d'obsidienne plus grande à
l'intérieur qu'à l'extérieur, qui s'ouvre sur **le monde de l'Ender** — le
paradis des cubes, un monde-caverne où viennent se reposer les blocs
détruits. Version courante : **0.11.0**.

## La progression

1. **Le Minerai de l'Ender** se génère dans la pierre de l'End (Y 10–70).
   Minez-le (pioche en fer minimum) pour obtenir des **Cristaux de l'Ender**
   (Fortune fonctionne, Silk Touch ramasse le minerai).
2. **La Porte inactive** se crafte avec 8 **obsidiennes pleureuses**, un
   cristal et une **nether star**.
   C'est une porte normale : elle se pose et se récupère à la pioche.
3. **Le rituel de la Mace** : munissez-vous d'une **Mace vanilla** (heavy
   core + breeze rod), posez la porte où vous voulez, sautez d'au moins
   **20 blocs** et frappez-la (clic gauche) **pendant la chute** — l'attaque
   écrasante de la Mace, appliquée à la porte. L'impact annule vos dégâts de
   chute, la foudre tombe : la porte s'éveille et sa salle intérieure est
   taillée dans le monde de l'Ender.
4. **La Clé de l'Ender** se crafte avec 1 cristal, 1 perle d'Ender et
   1 lingot d'or. Clic droit sur la porte éveillée pour **lier** la clé.
5. **La Pioche de l'Ender** (3 cristaux, 2 bâtons) récolte les Blocs de l'Ender
   — et les casse presque instantanément. À défaut, l'enchantement rare
   **« Brisure d'Espace-Temps »** (trésor, comme Raccommodage : troc, butin,
   pas la table d'enchantement) permet à **n'importe quelle pioche** de les
   casser avec butin, à vitesse correcte.

## La clé, au quotidien

| Action | Effet |
| --- | --- |
| Clic droit **par terre** | La porte se **matérialise en fondu** à l'endroit visé (et se dématérialise de son ancien emplacement si besoin). |
| Clic droit sur la **porte fermée** | La porte **s'ouvre**. Traversez l'embrasure : vous voilà dans votre base de poche. |
| Clic droit sur la **porte ouverte** | Elle se referme et **disparaît en fondu** — votre base est en sécurité dans votre poche. |
| Clic droit sur la **porte intérieure** | Rappelle la porte dans le monde extérieur (ou la dématérialise si elle y est encore). |

## Le monde de l'Ender

Un monde entièrement souterrain de **Blocs de l'Ender** — gris,
semi-transparents comme le bloc de miel, doucement luminescents. Au travers
de la masse translucide, on **entrevoit les blocs-reliques** venus mourir
ici : pierre, troncs, minerais (jusqu'au diamant), bibliothèques, éponges…
Ils ne sont pas éparpillés un par un mais **groupés en nuées** d'une même
matière, si ténues qu'aucun bloc n'en touche un autre : la nuée s'effiloche
dans la masse au lieu de s'arrêter net. De rares **filons
lumineux** de froglight perlescent traversent la masse en longs semis de
lueurs violettes : les seuls repères d'un monde qui se ressemble partout, et
la seule lumière — l'obscurité y est totale partout ailleurs. Une **cendre
claire** tombe en continu.
La masse est **pleine d'un bout à l'autre** : pas la moindre caverne, pas un
tunnel. On ne s'y déplace qu'à la Pioche de l'Ender, en taillant sa propre
galerie — et le brouillard ne se dissipe jamais dans un vide.

Chaque porte éveillée reçoit sa parcelle — une case de 8192 blocs de côté,
attribuée en spirale autour de l'origine : construisez-y base, fermes et
stockage, les lits et ancres de réapparition y fonctionnent. Les cases sont **cloisonnées par un quadrillage
de murs de bedrock de 2 blocs d'épaisseur**, montant d'une calotte de bedrock
à l'autre : on ne peut pas marcher jusque chez le voisin, ni passer
par-dessus ou par-dessous. Le monde s'étend de **-64 à 320**, comme
l'Overworld, et il est fermé en haut et en bas par 2 couches de bedrock.

Cette hauteur est une contrainte optique, pas un confort. Le Bloc de l'Ender
masque ses faces internes : la masse ne s'assombrit pas avec la profondeur, on
la traverse du regard comme une seule vitre. Seul le brouillard arrête l'œil,
et il ne dépend que de la distance — une calotte de bedrock trop proche se
lit donc en clair au travers de la matière. Les 384 blocs les repoussent à 127
en dessous de la base et 254 au-dessus, bien au-delà des **96 blocs** où le
brouillard sature. La base, elle, reste à y = 64, dans la bande d'altitude où
les brouillards des shaders existent.

> La hauteur du monde de l'Ender a changé en 0.9.5 (elle valait 0 → 128).
> Minecraft ne sait pas redimensionner une dimension existante : une
> sauvegarde antérieure doit repartir d'un monde neuf.

Le pseudo du propriétaire s'affiche sur un petit panneau à l'avant de la
porte, et tout fonctionne en multijoueur (logique côté serveur, données
synchronisées).

## Le Passage des Alliés

Deux bases de poche, un seul seuil. Le **Passage des Alliés** est une arche
claire — quartz et veines dorées, à l'opposé de l'obsidienne de la Porte de
l'Ender — posable dans le monde de l'Ender. Seule, elle reste scellée : il faut
lui **accoler un Contrôle de l'amitié**, le pavé numérique qui commande tout.

### Le code d'ami

Chaque porte éveillée reçoit un **code de huit chiffres**, inscrit sur sa clé
(infobulle) et rappelé en bas du panneau. C'est ce code qu'on se dicte.

### Devenir amis de passage

Tapez le code de l'autre au pavé, chiffre par chiffre, puis **VALIDER**
(`EFFACER` remet à zéro, `*` corrige le dernier chiffre, `#` valide). Le nom
apparaît dans le carnet, à gauche, avec un **sablier** : un seul des deux codes
a été tapé. L'autre reçoit un message et doit taper le vôtre de son côté.

Quand les deux l'ont fait, le sablier laisse place à la **tête du joueur**.
L'amitié n'est jamais stockée comme telle : elle est la conjonction des deux
déclarations, ce qui rend impossible l'état où l'un se croirait ami et l'autre
non.

### Ouvrir le passage

Cliquez le nom de votre allié : sa bordure passe au **jaune** (vous attendez).
Il reçoit un message et a **deux minutes** pour cliquer le vôtre — sa bordure
clignote alors en **orange** chez lui, le seul état qui réclame un geste. Passé
ce délai, la demande est abandonnée.

Dès que les deux ont cliqué, l'arche s'anime **trois secondes** puis s'ouvre,
bordure **verte** de chaque côté. Le lien reste ouvert aussi longtemps que vous
le voulez ; traversez dans un sens ou dans l'autre. Pour le refermer, cliquez le
nom entouré de vert — il faudra refaire la poignée de main pour rouvrir.

Maj + clic sur un nom le retire du carnet.

Le quadrillage de bedrock reste intact : on ne se rencontre que par un passage
mutuellement consenti.

## Le stockage : Transmetteur d'objet & Sac de l'Ender

Deux objets transforment votre base de poche en **entrepôt distant** :

* **Le Transmetteur d'objet** (posable uniquement dans le monde de l'Ender)
  fédère tout un **réseau de rangements** accolés — de bloc en bloc, le réseau
  grandit.
* **Le Sac de l'Ender**, porté en **seconde main**, envoie d'un clic droit la
  **rangée du haut** de votre inventaire dans ce réseau, où que vous soyez
  dans le monde. Chaque case rangée coûte un peu d'**expérience** (3 points) ;
  réseau plein ou XP insuffisante, rien ne part.

Le Transmetteur reconnaît **tout rangement exposant un inventaire** (voir
[Compatibilité rangement](#compatibilité-rangement)) — pas seulement les
coffres vanilla.

## Crafts (grille d'établi)

```
Porte inactive     Clé              Pioche de l'Ender     Transmetteur
O O O              C                C C C                 I R I
O C N              P                . S .                 I E I
O O O              G                . S .                 I I I

Sac de l'Ender (sans forme) : Bundle + Coffre de l'Ender

O = Obsidienne pleureuse  C = Cristal de l'Ender   N = Nether Star
P = Perle d'Ender         G = Lingot d'or          S = Bâton
I = Bloc de fer           E = Coffre de l'Ender    R = Bloc de redstone
```

Le rituel d'éveil demande en plus une **Mace vanilla** (elle n'est pas
consommée, juste un peu usée à chaque éveil).

Bonus : 4 cristaux → 1 Bloc de l'Ender ; 4 Blocs de l'Ender → 4 Briques de l'Ender.

### La famille des briques

Les **Briques de l'Ender** sont **translucides au même titre que le Bloc de
l'Ender** — même opacité exactement. Un mur de briques laisse deviner ce qu'il
y a derrière, comme la masse dans laquelle on le bâtit. Elles déclinent
escaliers, dalles, muret et une variante **ciselée** gravée de l'œil des cadres
de portail. Tout se taille aussi au **tailleur de pierre**.

Les faces entre deux briques voisines ne sont pas dessinées : un mur épais ne
s'assombrit donc pas couche après couche, il reste une seule vitre.

**Le Guide de la Porte de l'Ender** : 8 cristaux autour d'un livre → un
livre écrit (10 pages) contenant le lore, tous les crafts et la
compatibilité rangement, traduit dans la langue du jeu (FR/EN).

## Les progrès

Un arbre complet, « Le paradis des cubes » : *Une pierre qui n'est pas d'ici* →
*Le seuil* → *Frapper le ciel* → *Le paradis des cubes*, puis quatre branches —
*Tailler sa galerie* (mille Blocs de l'Ender), *Le fond du monde*, *L'entrepôt*
→ *Tout ranger d'un geste*, et *Bâtir dans le translucide*.

## Compatibilité rangement

La compatibilité passe par la **capability `IItemHandler`** de NeoForge —
l'interface d'inventaire standard. Le Transmetteur/Sac fonctionne donc avec
**tout mod de rangement** qui l'expose, sans aucune dépendance de compilation :

* **Sophisticated Storage** — coffres, tonneaux, shulkers de tous tiers, et le
  Storage Controller (déposer dans le contrôleur répartit dans son réseau lié).
* **Sophisticated Backpacks** — sacs à dos posés au sol.
* **Tom's Simple Storage** — inventaires reliés à un Inventory Connector.
* Coffres/tonneaux vanilla, drawers, et la plupart des mods de rangement.

Comme l'insertion passe par `ItemHandlerHelper`, elle **respecte les filtres
et upgrades** de chaque rangement : un tonneau Sophisticated filtré fait le
**tri automatique** de ce que vous déversez.

### Pont de dépôt (Transmetteur + réseau)

Le Transmetteur ne remplace pas votre mod de rangement : il **s'y branche**.
Le Sac de l'Ender dépose dans le Transmetteur, qui **pousse les objets dans le
réseau accolé** :

* Collé à des **coffres/tonneaux** (vanilla, Sophisticated…) : le Sac les
  remplit directement.
* Collé à un **Connecteur d'inventaire (Tom's)** : le Sac déverse dans **tout
  le réseau Tom's**, et le Storage Terminal affiche vos objets — comptés une
  seule fois (le Transmetteur n'expose pas de vue agrégée, donc pas de double
  comptage).

L'insertion est conservatrice (aucune duplication) et respecte les filtres du
rangement cible.

## Immersive Portals (optionnel)

Le mod fonctionne seul (traversée par contact avec l'embrasure ouverte).
Si [Immersive Portals pour NeoForge](https://github.com/iPortalTeam/ImmersivePortalsModForNeo)
(mods `imm_ptl` / `immersive_portals_core`) est installé, l'ouverture de la
porte crée des portails « voir au travers » entre l'embrasure extérieure et
la salle intérieure — la continuité visuelle entre les deux dimensions,
comme un portail du Nether d'Immersive Portals. La paire est **bi-way** :
deux entités, une par embrasure, traversables dans les deux sens. Pas de
faces opposées — le fond opaque du caisson les cacherait de toute façon.

Pendant qu'Immersive Portals rend le monde d'en face, le caisson de la porte
opposée reste visible au travers du portail — c'est lui qui encadre la vue et
évite qu'il surgisse au franchissement. Seul son fond est omis : la caméra
virtuelle étant placée derrière cette porte, il boucherait toute la vue.

L'intégration passe par réflexion (aucune dépendance de compilation) : si
l'API d'Immersive Portals change, le mod bascule automatiquement sur sa
téléportation classique et l'indique dans les logs. Au démarrage, une ligne
`Immersive Portals détecté : true/false` confirme la détection.

## Shaders (Complementary Reimagined)

Le monde de l'Ender se déclare **comme le Nether** auprès des shaders, et
c'est ce qui fait tout : son `dimension_type` porte
`effects: minecraft:the_nether`. **Rien à configurer.**

Le détour mérite une explication, parce qu'il n'est pas évident. Iris choisit
le dossier de shaders (`world0` / `world-1` / `world1`) dans cet ordre
(`Iris.getCurrentDimension()`) :

1. une correspondance **exacte** de l'identifiant de dimension dans le
   `dimension.properties` du shaderpack ;
2. à défaut, le champ **`effects` du type de dimension** — `minecraft:the_end`
   donne `world1`, `minecraft:the_nether` donne `world-1` ;
3. à défaut, l'identifiant brut, que le `dimension.world0=*` de Complementary
   rattrape en Overworld.

Le mod ne peut pas écrire dans un shaderpack, mais il maîtrise son champ
`effects` : c'est la seule voie par laquelle il peut obtenir un rendu
souterrain sans rien demander au joueur. Sans ce champ, le `DoBorderFog` de
Complementary (`lib/atmospherics/fog/mainFog.glsl`) prenait sa branche
Overworld, avec trois conséquences visibles :

* **un horizon en plein sous-sol.** La couleur du fondu y est
  `GetSky(VdotU, …)` : le ciel échantillonné dans la direction du regard.
  Au-dessus de la ligne d'horizon on récoltait le gris du ciel, en dessous le
  noir du vide, avec une coupure nette à hauteur d'œil — là où `VdotU = 0`.
  La branche Nether emploie `netherColor`, une couleur unique sans terme
  directionnel : plus d'horizon, plus de ciel.
* **un bord cubique.** La distance de bordure est
  `max(length(playerPos.xz), abs(playerPos.y))`, une métrique de cube et non
  de sphère : on en voyait les arêtes.
* **une coupure brutale.** La courbe de l'Overworld est en
  `(distance / portée)^16`, plate sur presque toute la vue puis verticale au
  dernier moment. Celle du Nether est linéaire — un dégradé régulier.

En prime, la branche Nether apporte son brouillard atmosphérique et sa tempête
de cendres volumétrique, et respecte le réglage *Nether View Limit*.

Le prix à payer est mince et ne concerne que le **jeu sans shader** : la
dimension hérite alors des `DimensionSpecialEffects` du Nether, dont
`constantAmbientLight`. Les faces du haut et du bas des blocs sont éclairées à
0,9 au lieu de 1,0 et 0,5 ; les faces latérales (0,8 et 0,6) ne changent pas.
Sous shader, ce coût est nul : Complementary déclare `oldLighting = false`, et
Iris désactive alors complètement l'ombrage directionnel de vanilla
(`IrisRenderingPipeline.shouldDisableDirectionalShading()`).

Tout le reste des effets du monde reste piloté par le mod, par événements —
donc indépendamment du champ `effects` : lumière ambiante à **zéro** (loin
d'un filon, il fait réellement noir), pas de ciel, pas de nuages, brouillard
dense d'une teinte rendue **sans délavage** (c'est elle qui alimente
l'uniforme `fogColor` dont Complementary tire sa couleur de brume).

Le regard porte au travers de la masse translucide, donc jusqu'au bord de la
zone chargée. Un **fondu au noir** ferme la vue à 55 % de la distance de
rendu, et **jamais au-delà de 96 blocs** : la frontière des chunks n'est
jamais visible, quel que soit le réglage. De loin en loin, ce lointain
s'embrase une fraction de seconde — de **silencieuses lueurs d'orage**,
souvent redoublées.

Pour un **flou croissant avec la distance**, Complementary a ce qu'il faut
nativement, mais **désactivé par défaut** : *Camera Settings → World Blur →
World Blur → **Distance Blur***. C'est une option du shader, pas quelque chose
que le mod puisse fournir — sous Iris, les post-traitements de Minecraft sont
court-circuités.

L'intensité se règle ensuite sur le curseur du monde en cours : le monde de
l'Ender étant vu comme le Nether, c'est **« Dis. Blur — The Nether »**. Le
flou est proportionnel à cette valeur — `coc = clamp(distance × 0,001 ; 0 ;
0,1) × intensité × 0,03` — donc **plus la valeur est haute, plus c'est
flou**, et l'effet sature à 100 blocs. La valeur par défaut est 64.

### Forcer un autre rendu

L'étape 1 ci-dessus l'emporte sur le champ `effects` : pour reprendre la main,
ajoutez l'identifiant du monde à la ligne de votre choix dans
`shaderpacks/ComplementaryReimagined…/shaders/dimension.properties`. Par
exemple, pour le faire rendre comme l'End :

```
dimension.world1=minecraft:the_end minecraft:end enderportals:ender_world
```

## Compatibilité générale

Le mod est conçu pour cohabiter sereinement avec d'autres mods : aucun
mixin, tags vanilla additifs uniquement, écouteurs d'événements NeoForge
ciblés sur ses propres blocs, générateur de dimension auto-contenu et
thread-safe. La clé et le rituel respectent la spawn protection, le mode
aventure et les mods de protection de terrain (via `Level#mayInteract`).

## Compiler

Prérequis : **Java 21**.

```bash
./gradlew build
# → build/libs/enderportals-0.11.0.jar
```

Notes :

* Le jar se place dans `mods/` avec **NeoForge 21.1.x** pour Minecraft 1.21.1.
* Build géré par **ModDevGradle** (`net.neoforged.moddev`) ; NeoForge et les
  mappings officiels (Mojmap) sont téléchargés automatiquement.
* Versions épinglées dans `gradle.properties` (`minecraft_version=1.21.1`,
  `neo_version=21.1.93`) — vous pouvez les mettre à jour vers les derniers
  builds 1.21.1.
* La CI GitHub Actions (`.github/workflows/build.yml`) compile chaque push et
  publie le jar dans la pré-release `dev-latest`.

## Arborescence rapide

* `src/main/java/com/maxezify/enderportals/`
  * `EnderPortalsMod.java` — point d'entrée `@Mod`, enregistrements
    `DeferredRegister`, rituel de la Mace.
  * `tardis/` — activation, salle intérieure, matérialisation, traversées,
    état persistant des portes (`SavedData`), logique du Transmetteur/Sac
    (`CentralizerLogic` — nom de classe interne conservé, comme « Tardis* »,
    pour la compatibilité des sauvegardes).
  * `world/EnderWorldChunkGenerator.java` — le générateur du monde-caverne
    et de ses blocs-reliques.
  * `block/`, `item/` — porte inactive/active, bloc de l'Ender, clé, pioche,
    Transmetteur d'objet, Sac de l'Ender, livre-guide.
  * `client/TardisDoorRenderer.java` — le rendu de la porte avec fondu de
    matérialisation.
  * `compat/ImmPtlCompat.java` — l'intégration Immersive Portals par
    réflexion.
* `src/main/resources/data/enderportals/` — dimension, biome, minerai de
  l'End (biome modifier NeoForge), recettes, butins, tags.
* Les textures sont générées procéduralement (voir l'historique du dépôt) —
  remplacez-les librement par de vraies textures d'artiste.
