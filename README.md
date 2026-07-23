# Ender Portals — la Porte de l'Ender, une base de poche

Mod **Minecraft 1.21.1 / Fabric** : une porte d'obsidienne plus grande à
l'intérieur qu'à l'extérieur, qui s'ouvre sur **le monde de l'Ender** — le
paradis des cubes, un monde-caverne où viennent se reposer les blocs
détruits.

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
5. **La Pioche de l'Ender** (3 cristaux, 2 bâtons) est la seule capable de
   récolter les Blocs de l'Ender — et elle les casse presque instantanément.

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
sa parcelle (espacées de 1024 blocs) : construisez-y base, fermes et
stockage — les lits et ancres de réapparition y fonctionnent.

## Crafts (grille d'établi)

```
Porte inactive        Clé                  Pioche de l'Ender
C C                   C                    C C C
C N                   P                    . S .
C C                   G                    . S .

C = Cristal de l'Ender   N = Nether Star   S = Bâton
P = Perle d'Ender        G = Lingot d'or
```

Le rituel d'éveil demande en plus une **Mace vanilla** (elle n'est pas
consommée, juste un peu usée à chaque éveil).

Bonus : 4 cristaux → 1 Bloc de l'Ender ; 4 Blocs de l'Ender → 4 Briques de l'Ender.

**Le Guide de la Porte de l'Ender** : 8 cristaux autour d'un livre → un
livre écrit contenant le lore et tous les crafts, traduit dans la langue du
jeu (FR/EN).

## Compatibilité

Le mod est conçu pour cohabiter sereinement avec d'autres mods : aucun
mixin, tags vanilla additifs uniquement, événements Fabric standards ciblés
sur ses propres blocs, générateur de dimension auto-contenu et thread-safe,
intégration Immersive Portals par réflexion avec repli automatique. La clé
et le rituel respectent la spawn protection, le mode aventure et les mods
de protection de terrain branchés sur `canPlayerModifyAt`.

## Immersive Portals (optionnel, expérimental)

Le mod fonctionne seul (traversée par contact avec l'embrasure ouverte).
Si [Immersive Portals](https://github.com/iPortalTeam/ImmersivePortalsMod)
(`imm_ptl_core`) est installé, l'ouverture de la porte tente de créer une
paire de portails « voir au travers » entre l'embrasure extérieure et la
salle intérieure — la continuité visuelle entre les deux dimensions.
L'intégration passe par réflexion (aucune dépendance de compilation) : si
l'API d'Immersive Portals change, le mod bascule automatiquement sur sa
téléportation classique et l'indique dans les logs.

## Compiler

Prérequis : **Java 21**.

```bash
./gradlew build
# → build/libs/enderportals-0.1.0.jar
```

Notes :

* Le jar se place dans `mods/` avec **Fabric Loader ≥ 0.16** et
  **Fabric API** pour 1.21.1.
* Ce dépôt a été écrit dans un environnement sans accès aux dépôts Maven de
  Mojang/FabricMC : le code n'a **pas encore été compilé**. Si `gradle`
  signale un écart mineur d'API (les mappings Yarn évoluent entre builds),
  la correction devrait être locale et évidente — n'hésite pas à me
  redonner l'erreur.
* Versions épinglées dans `gradle.properties` (Yarn `1.21.1+build.3`,
  Loader `0.16.9`, Fabric API `0.102.1+1.21.1`, Loom `1.9.2`) — vous pouvez
  les mettre à jour vers les derniers builds 1.21.1.

## Arborescence rapide

* `src/main/java/com/maxezify/enderportals/`
  * `tardis/` — activation, salle intérieure, matérialisation, traversées,
    état persistant des portes (nommage interne historique « Tardis* »,
    conservé pour la compatibilité des sauvegardes).
  * `world/EnderWorldChunkGenerator.java` — le générateur du monde-caverne
    et de ses blocs-reliques.
  * `block/`, `item/` — porte inactive/active, bloc de l'Ender, masse, clé,
    pioche.
  * `client/TardisDoorRenderer.java` — le rendu de la porte avec fondu de
    matérialisation.
  * `compat/ImmPtlCompat.java` — l'intégration Immersive Portals par
    réflexion.
* `src/main/resources/data/enderportals/` — dimension, biome, minerai de
  l'End, recettes, butins.
* Les textures sont générées procéduralement (voir l'historique du dépôt) —
  remplacez-les librement par de vraies textures d'artiste.
