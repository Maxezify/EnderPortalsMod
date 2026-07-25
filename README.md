# Ender Portals — la Porte de l'Ender, une base de poche

Mod **Minecraft 1.21.1 / NeoForge** : une porte d'obsidienne plus grande à
l'intérieur qu'à l'extérieur, qui s'ouvre sur **le monde de l'Ender** — le
paradis des cubes, un monde-caverne où viennent se reposer les blocs
détruits. Version courante : **0.3.0**.

## La progression

1. **Le Minerai de l'Ender** se génère dans la pierre de l'End (Y 10–70).
   Minez-le (pioche en fer minimum) pour obtenir des **Cristaux de l'Ender**
   (Fortune fonctionne, Silk Touch ramasse le minerai).
2. **La Porte inactive** se crafte avec 5 cristaux et une **nether star**.
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
ici : pierre, troncs, minerais (jusqu'au diamant), glowstone, éponges…
Cavernes et tunnels serpentent dans la masse. Chaque porte éveillée reçoit
sa parcelle (espacées de 8192 blocs) : construisez-y base, fermes et
stockage — les lits et ancres de réapparition y fonctionnent. Le pseudo du
propriétaire s'affiche sur un petit panneau à l'avant de la porte, et tout
fonctionne en multijoueur (logique côté serveur, données synchronisées).

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

**Le Guide de la Porte de l'Ender** : 8 cristaux autour d'un livre → un
livre écrit (10 pages) contenant le lore, tous les crafts et la
compatibilité rangement, traduit dans la langue du jeu (FR/EN).

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
porte crée une paire de portails « voir au travers » entre l'embrasure
extérieure et la salle intérieure — la continuité visuelle entre les deux
dimensions, comme un portail du Nether d'Immersive Portals.

L'intégration passe par réflexion (aucune dépendance de compilation) : si
l'API d'Immersive Portals change, le mod bascule automatiquement sur sa
téléportation classique et l'indique dans les logs. Au démarrage, une ligne
`Immersive Portals détecté : true/false` confirme la détection.

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
# → build/libs/enderportals-0.3.0.jar
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
