# World of Ender — une base de poche derrière une porte d'obsidienne

*[English version](README.md)*

Mod **Minecraft 1.21.1 / NeoForge**. Vous forgez une porte, vous l'éveillez au
prix d'un rituel, et elle s'ouvre sur un monde qui n'existait pas : **l'Ender**,
une masse souterraine translucide où viennent se reposer les blocs détruits.
Derrière cette porte, une parcelle entière vous appartient — et vous l'emportez
partout : la porte se dématérialise dans votre poche et se repose où vous
voulez.

Version courante : **0.26.0**.

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

## Ce que le mod apporte

* **Une base transportable.** Une porte de deux blocs, posée d'un clic, qui
  contient une parcelle de 8192 blocs dans une dimension à part.
* **Un monde à creuser.** L'Ender est plein d'un bout à l'autre : pas une
  caverne, pas un tunnel. On s'y déplace en taillant sa galerie.
* **Un stockage à distance.** Où que vous soyez, videz votre inventaire dans les
  coffres de votre base d'un simple clic droit.
* **Un partage consenti.** Le Passage des Alliés relie deux bases — mais
  seulement si les deux joueurs le veulent, chacun de son côté.
* **Des matériaux.** Le Bloc de l'Ender et toute sa famille de briques :
  translucides, on bâtit des murs qu'on voit au travers.

---

## Démarrer

### 1. Trouver le minerai

Le **Minerai de l'Ender** se génère dans la pierre de l'End, entre **Y 10 et
Y 70**, en petits amas. Il n'apparaît nulle part ailleurs : le mod commence donc
par un voyage dans l'End.

Une **pioche en fer** suffit à le casser, et il rend des **Cristaux de
l'Ender**. Fortune augmente le rendement, Silk Touch ramasse le bloc de minerai
intact.

Le cristal est **violet**, taillé en gemme — la couleur de l'End, celle de la
porte et des machines. C'est à elle qu'on reconnaît, d'un coup d'œil dans un
coffre, ce qui appartient au mod.

Tout le reste du mod se construit à partir de ces cristaux.

### 2. Forger la porte

**7 obsidiennes pleureuses**, **1 Cristal de l'Ender** et **1 nether star**
donnent une **Porte de l'Ender inactive**. À ce stade elle ne fait rien : c'est
un caisson de deux blocs qui se pose et se récupère à la pioche. Elle dort.

### 3. Le rituel de la Mace

Pour l'éveiller, il faut la frapper avec assez de violence pour fêler l'espace.

Munissez-vous d'une **Mace** vanilla (heavy core + breeze rod). Posez la porte où
vous voulez, montez à **20 blocs au moins** au-dessus d'elle, sautez, et
**frappez-la en pleine chute** (clic gauche). C'est l'attaque écrasante de la
Mace, appliquée non à un monstre mais à une porte.

L'impact absorbe vos dégâts de chute, la foudre tombe, et la porte s'éveille :
sa salle intérieure vient d'être taillée dans l'Ender. La Mace n'est pas
consommée, seulement un peu usée.

> Une seule porte par joueur. Le rituel refuse d'en éveiller une seconde.

### 4. Forger la clé

**1 cristal + 1 perle d'Ender + 1 lingot d'or**, en colonne, donnent la **Clé de
l'Ender**. Faites un **clic droit dans le vide** pour la lier à votre porte :
elle n'obéira qu'à vous.

### 5. La pioche

**3 cristaux + 2 bâtons** donnent la **Pioche de l'Ender**. Elle est la seule à
récolter les Blocs de l'Ender, et elle les casse presque instantanément — c'est
avec elle qu'on se déplace dans la masse.

À défaut, l'enchantement **« Brisure d'Espace-Temps »** permet à n'importe quelle
pioche de les casser avec butin, à vitesse correcte. C'est un enchantement de
**trésor**, comme Raccommodage : on le trouve en butin ou en troc avec un
bibliothécaire, jamais à la table d'enchantement.

---

## La clé, au quotidien

| Vous faites… | Il se passe… |
| --- | --- |
| Clic droit sur la porte, **à la main** | Elle s'ouvre, ou se referme. Comme toutes les portes du jeu. |
| Clic droit sur la **porte intérieure**, porte rangée | Elle se matérialise dehors, ouverte : c'est votre sortie. |
| **Clé** + clic droit **par terre** | La porte se matérialise en fondu à l'endroit visé, et disparaît de son ancien emplacement. |
| **Clé** + **accroupi** + clic droit sur la porte | Elle se dématérialise : votre base repart dans votre poche. |
| **Clé** + clic droit **dans le vide** | Lie la clé à votre porte. |

La clé ouvre et ferme aussi, si vous l'avez en main — c'est le même geste, elle
ne vous oblige pas à la ranger. Ce qu'elle seule fait, c'est **faire apparaître
et disparaître** la porte, et se lier à elle.

**Un geste à la fois.** La porte met **1,75 s** à se matérialiser, **1,5 s** à
s'effacer. Tant que le fondu dure, elle refuse l'ordre suivant et vous répond
« La porte n'a pas fini son passage. » Cela vaut pour tout — apparition,
disparition, ouverture, fermeture, et la pose au sol à la clé. Dès qu'elle a
fini de réapparaître, elle obéit de nouveau.

### Le passage

Un fondu, ce n'est pas une opacité qui monte. Voici ce que vous voyez quand la
porte arrive :

* **Une onde** de lumière blanche à bords violets remonte les quatre faces du
  caisson, du seuil au linteau. Elle s'allume en montant et s'éteint en sortant
  par le haut — à la disparition, elle redescend.
* **La matière converge.** Les grains de portail affluent depuis une couronne
  large de plus d'un bloc et se resserrent sur la porte à mesure qu'elle prend.
  Quand elle s'en va, tout repart vers l'extérieur et monte.
* **La porte tremble** de trois centimètres tant qu'elle n'est pas tout à fait
  là, et se stabilise en même temps qu'elle s'opacifie. Son battement d'opacité
  s'apaise de la même façon : elle ne claque pas à 100 %, elle se pose.
* **Elle atterrit** sur une couronne d'étincelles et deux notes — un choc grave
  et une résonance d'améthyste. En partant, elle claque sur un souffle aigu.

**Tout cela n'obéit qu'au propriétaire.** Un autre joueur qui clique votre porte,
à la main comme à la clé, lit « verrouillée ».

### La clé n'obéit qu'à vous

Perdue, jetée ou volée, elle est **inerte dans les mains d'un autre** : elle ne
matérialise rien, n'ouvre rien, et ne se relie pas à votre porte. Personne
n'entre chez vous en ramassant votre trousseau.

### Perdre sa clé ne coûte pas sa base

Reforgez-en une (cristal, perle, lingot d'or) et faites un clic droit dans le
vide : elle se relie à votre porte, où que celle-ci se trouve. C'est important,
parce qu'une porte dématérialisée n'offre plus rien à cliquer et que le rituel
refuse d'en éveiller une seconde — sans cette relance, la base serait perdue
pour de bon.

### Si l'on bâtit à la place de votre porte

Imaginez la scène. Vous rentrez chez vous, vous refermez la porte derrière vous,
elle disparaît du monde extérieur. Pendant que vous êtes à l'intérieur,
quelqu'un construit à l'endroit exact où elle se tenait — ou un arbre y pousse.

Vous voulez ressortir. Vous cliquez sur la porte intérieure pour la rappeler
dehors… et il n'y a plus de place. Sans précaution, vous seriez enfermé : votre
parcelle est entourée de bedrock, et cette porte est la seule sortie.

**Le rappel ne peut donc pas échouer.** Il cherche, dans l'ordre :

1. l'endroit exact où vous l'aviez laissée ;
2. sinon, un espace libre à proximité — jusqu'à 8 blocs autour et 4 au-dessus ou
   en dessous, sur un sol solide ;
3. sinon, votre point de réapparition.

S'il a dû déplacer la porte, il vous en donne les coordonnées.

**Et sans la clé ?** La porte s'ouvre à la main, donc rien ne peut vous enfermer.
C'était la raison de ce partage des rôles : mourir dehors laisse la clé au sol
avec le reste, et si votre lit est dans l'Ender vous réapparaissez à l'intérieur,
sans elle. Reforger une clé demande une perle de l'Ender — or aucune créature
n'apparaît dans l'Ender. Une porte qui n'ouvrait qu'à la clé condamnait donc la
base et tout ce qu'elle contenait.

**Poser** la porte au sol depuis l'extérieur, en revanche, peut être refusé :
là, c'est vous qui avez désigné l'endroit, « pas assez de place » est une
réponse honnête, et vous n'êtes enfermé nulle part — il suffit de viser
ailleurs.

---

## L'Ender

Un monde entièrement souterrain, taillé dans une masse de **Blocs de l'Ender** :
gris, translucides comme le bloc de miel, doucement luminescents.

Au travers de cette matière, on **entrevoit les blocs-reliques** venus mourir
ici — pierre, troncs, minerais jusqu'au diamant, bibliothèques, éponges. Ils ne
sont pas éparpillés un par un mais **groupés en nuées** d'une même matière, si
diffuses qu'aucun bloc n'en touche un autre : la nuée s'effiloche dans la masse
au lieu de s'arrêter net.

De rares **filons de froglight perlescent** la traversent en longs semis de
lueurs violettes. Ce sont les seuls repères d'un monde qui se ressemble partout
— et la seule lumière : partout ailleurs, l'obscurité est totale. Une cendre
claire tombe en continu.

**La masse est pleine.** Pas une caverne, pas un tunnel, pas un vide. On ne s'y
déplace qu'à la Pioche de l'Ender, en creusant sa propre galerie.

### Votre parcelle

Chaque porte éveillée reçoit sa **parcelle de 8192 blocs**. Vous y bâtissez ce
que vous voulez : base, fermes, stockage. **Les lits fonctionnent** : on y dort
et on y fixe son point de réapparition. Les ancres de réapparition, non — elles
explosent ici comme dans l'Overworld. Les portails du Nether ne s'y allument
pas.

Le monde va de **-64 à 320**, comme l'Overworld, et il est fermé en haut et en
bas par deux couches de bedrock. Votre salle d'arrivée est à **Y 64**.

Le pseudo du propriétaire s'affiche sur un petit panneau à l'avant de la porte.

### Chacun chez soi

Les parcelles sont séparées par un **quadrillage de murs de bedrock**, montant
du plancher au plafond du monde. Vous pouvez creuser dans toutes les directions
sans jamais tomber sur la base de quelqu'un d'autre, ni qu'on tombe sur la
vôtre.

Le **Passage des Alliés** est la porte que l'on ouvre volontairement dans ce
mur.

---

## Le Passage des Alliés

Deux bases de poche, un seul seuil.

Le **Passage des Alliés** est un caisson clair — quartz et veines dorées, tout
l'opposé de l'obsidienne de la Porte de l'Ender. Il se pose **dans l'Ender
uniquement**. Seul, il reste scellé : il faut lui **accoler un Contrôle de
l'amitié**, le pavé numérique qui commande tout.

* **Passage des Alliés** : 4 blocs de quartz, 4 cristaux, 1 coffre de l'Ender.
* **Contrôle de l'amitié** : 6 quartz, 2 redstone, 1 cristal.

Le panneau doit toucher le passage — collé à lui, à l'un des quatre côtés.

**Posez autant de passages que vous avez d'amis à relier.** Chaque arche porte
un allié, et c'est le panneau accolé qui la commande : cliquer un nom sur un
panneau lie *l'*arche qu'il touche. Une base reliée à trois amis a donc trois
arches, chacune avec son Contrôle.

Un Contrôle appartient à qui appartient l'arche qu'il touche. Chez un allié, les
siens refusent de s'ouvrir : votre carnet vit sur vos propres panneaux. Un
Contrôle qui ne touche aucune arche n'est à personne et ne s'ouvre pour
personne — il ne commanderait rien.

**Un Contrôle ne commande qu'une arche, et une seule.** Le poser là où il en
toucherait deux est refusé, et poser une arche qui mettrait un Contrôle
existant à cheval l'est aussi. Laisser un bloc vide entre chaque groupe
Contrôle-et-arche est la façon simple de toujours y satisfaire. Deux arches
côte à côte ne gênent pas en elles-mêmes — ce qui ne doit jamais arriver, c'est
un Contrôle qui pourrait désigner l'une ou l'autre, car rien à l'écran ne dirait
laquelle il a choisie.

Une arche close est une arche liée à personne, et elle ne porte aucun nom. Il
n'y a pas d'entre-deux : nouer et dénouer se font toujours des deux côtés à la
fois, donc une arche est soit libre et close, soit liée et ouverte.

Rien n'oblige à en avoir plusieurs : une arche, un Contrôle, un ami à la fois
fonctionne exactement comme avant.

### Le code d'ami

Chaque porte éveillée reçoit un **code de huit chiffres, tous compris entre 1 et
9**. Vous le lisez sur l'infobulle de votre clé, et il est rappelé sous les
boutons du panneau. C'est ce code que l'on se dicte.

### Le terminal

Le bas du panneau est un terminal, et tout ce que le Contrôle a à dire s'y
affiche : un code refusé, une amitié scellée, un allié qui demande à se
connecter, un passage ouvert ou refermé. Ces phrases partaient auparavant dans
le chat, où elles arrivaient pendant qu'on était occupé au pavé, noyées dans le
reste.

Le journal est tenu par le serveur, un par joueur : un message reçu pendant que
vous étiez ailleurs vous attend à la prochaine ouverture du panneau — y compris
un message reçu hors ligne, que le chat perdait purement et simplement. La
molette le fait défiler.

Le chat ne prend le relais que si vous n'avez aucun panneau ouvert. Une demande
de connexion ne vaut que deux minutes : elle ne servirait à personne à dormir
dans un écran fermé.

À côté du titre du terminal, un témoin dit la seule chose qui compte : **le
passage est-il ouvert des deux côtés ?** **Fonctionne** en vert quand il l'est,
**Ne fonctionne pas** en rouge sinon.

Le verdict seul ne dirait pas quoi y faire : la ligne du bas du terminal en
nomme la cause, et elle y reste tant qu'elle dure — aucun passage à vous contre
ce panneau, aucun lien ouvert, ou un allié qui a cassé sa propre arche. Elle est
hors du journal, sous un filet : le journal raconte ce qui est arrivé, cette
ligne dit où l'on en est.

### Devenir amis

Tapez le code de l'autre sur le pavé, chiffre par chiffre, puis **VALIDER**
(`EFFACER` remet à zéro). Au clavier, **Retour arrière** corrige et **Entrée**
valide.

Son nom apparaît dans le carnet, à gauche, accompagné d'un **sablier** : un seul
des deux codes a été tapé. L'autre le lit sur son terminal et doit taper le vôtre
de son côté. Quand c'est fait, le sablier laisse place à sa **tête de joueur** :
vous êtes amis.

L'amitié est symétrique par construction — elle n'est que la rencontre de deux
déclarations. Il n'existe aucun état où l'un se croirait ami et l'autre non.

**Maj + clic** sur un nom le retire de votre carnet.

### Ouvrir le passage

Cliquez le nom de votre allié : sa bordure passe au **jaune**, vous attendez. Il le
lit sur son terminal et dispose de **deux minutes** pour cliquer le vôtre — chez
lui, votre nom clignote en **orange**, le seul état qui réclame un geste. Passé
ce délai, la demande est abandonnée.

Dès que les deux ont cliqué, **les deux caissons s'animent trois secondes** puis
s'ouvrent, bordure **verte** de chaque côté. Le lien reste ouvert aussi
longtemps que vous le voulez, et **on le franchit dans les deux sens** : chacun
va librement chez l'autre.

Pour refermer, cliquez le nom entouré de vert — depuis n'importe lequel de vos
panneaux. Pour rouvrir, recommencez l'opération : chacun reclique le nom de
l'autre.

Deux de vos arches ne se gênent jamais : en ouvrir une laisse les autres
tranquilles. Le seul délogement qui subsiste est interne à une arche — la lier à
un nouvel ami libère celui qu'elle portait, et cet ami en est averti.

Une fois le lien établi, les deux bases restent reliées **en permanence**, y
compris quand l'un des deux joueurs est déconnecté : son passage reste ouvert de
votre côté et vous pouvez toujours aller chez lui.

Le passage se referme aussi tout seul si l'un des deux casse son caisson, ou si
l'un de vous ouvre un lien avec quelqu'un d'autre — on n'a qu'un passage à la
fois.

### Avec Immersive Portals

Si le mod est installé, le passage ouvert n'est plus un seuil : **on voit la
base de l'allié par l'embrasure**, et on y entre à pied, sans écran de
chargement. Sans Immersive Portals, tout fonctionne pareil, simplement le
franchissement se fait au contact.

---

## Le stockage à distance

Deux objets transforment votre base de poche en entrepôt joignable de n'importe
où :

* **Le Transmetteur d'objet** (7 blocs de fer, 1 bloc de redstone, 1 coffre de
  l'Ender) se pose **dans l'Ender**, contre vos rangements. Il fédère tout le
  réseau accolé : de bloc en bloc, le réseau grandit.
* **Le Sac de l'Ender** (un bundle + un coffre de l'Ender) se range dans
  l'inventaire. **Prenez une pile au curseur** — clic gauche dessus — puis
  **clic droit sur le sac** : elle part dans ce réseau, où que vous soyez dans
  le monde.

Le geste marche **dans les deux sens** : le sac dans une case et la pile au
curseur, ou le sac au curseur et la pile dans la case. C'est celui des bourses
de vanilla, et il n'occupe plus votre seconde main. Attention au second sens :
on déplace un sac dans son inventaire plus souvent qu'on ne range, et pendant ce
déplacement un clic droit sur une pile l'expédie.
Chaque pile expédiée coûte **3 points d'expérience** — le prix du voyage, pas du
poids : une pile de soixante-quatre blocs coûte autant qu'un objet seul. Réseau
plein ou XP insuffisante : rien ne part, la pile reste au curseur, un son sec le
dit et le message donne la raison.

### Il se branche sur votre mod de rangement

Le Transmetteur ne remplace pas votre système de stockage, il **s'y raccorde**.
Il reconnaît tout rangement qui expose un inventaire au standard NeoForge, donc
en pratique la quasi-totalité des mods :

* **Sophisticated Storage** — coffres, tonneaux, shulkers de tous tiers, et le
  Storage Controller.
* **Sophisticated Backpacks** — sacs à dos posés au sol.
* **Tom's Simple Storage** — collez le Transmetteur à un Inventory Connector et
  le Sac déverse dans tout le réseau ; le Storage Terminal affiche vos objets,
  comptés une seule fois.
* Coffres et tonneaux vanilla, drawers, et le reste.

Comme le dépôt passe par l'insertion standard, il **respecte les filtres et les
upgrades** de chaque rangement : un tonneau Sophisticated filtré fait le tri
automatique de ce que vous déversez.

---

## Le Téléporteur d'entité

La clé ne prend que vous. Pour emmener une bête dans l'Ender, il faut deux
pièces :

* **L'Atterrisseur d'entité** (6 obsidiennes pleureuses, 2 cristaux, 1 bloc de
  slime — il en sort deux) se pose **dans l'Ender**, à l'endroit où vous voulez
  que la bête arrive.
* **Le Téléporteur d'entité** (5 obsidiennes pleureuses, 1 perle de l'Ender) est
  une coque d'acier et d'obsidienne noire, qui se pose et se charge **comme une
  barque**.

Le geste tient en trois temps. **Le téléporteur en main, clic droit sur un
Atterrisseur** : la coque retient ce point, et son infobulle affiche désormais les
coordonnées en vert. **Posez-la** où vous voulez, puis **attirez la bête dessus**
comme dans une barque — un son de validation, et le témoin de proue passe au
vert. **Clic droit sur la coque** : elle part avec ce qu'elle transporte.

Posez-en autant que vous voulez, des uns comme des autres ; chaque coque garde
son propre lien.

### Le témoin de proue

Il ne s'éteint jamais : il dit si le départ est possible.

* **Vert** — la coque a une destination *et* un passager : elle peut partir.
* **Rouge** — il lui manque l'un ou l'autre.

Il parle de la machine, pas du terrain : savoir si l'Atterrisseur est toujours en
place demanderait de charger son chunk à chaque instant, et c'est justement ce
qu'on évite. Cette vérification-là se fait au départ, et vous êtes prévenu si
l'Atterrisseur a disparu.

### Le témoin, les cristaux et la plaque

La machine porte quatre **pylônes coiffés de cristal**, un **rail d'or** sur
l'arête, un **tableau de proue** où loge le témoin, et surtout une **plaque de
départ violette** incrustée dans le plancher — c'est sur elle que la bête se
tient, et c'est elle qu'on voit d'en haut. Cristaux et plaque brillent de leur
propre lumière : la machine se repère de nuit.

### Le prix du voyage

Comme le Sac de l'Ender, la machine se paie en **expérience** : **20 points par
créature transportée**. Le prélèvement n'a lieu qu'**à l'arrivée** — un voyage
qui échoue ne coûte rien — et le départ est refusé d'emblée si vous n'avez pas de
quoi le payer.

### Reprendre la coque

* **À vide, un clic droit** la reprend en main — avec son lien, pour ne pas avoir
  à la relier à chaque voyage.
* **Accroupi, le clic droit** la reprend aussi et **libère ce qu'elle
  transporte** : c'est le geste de l'arrivée, quand la bête est à destination.

Casser la coque marche également, mais lui fait perdre son Atterrisseur.

### Le chunk d'arrivée

L'Ender ne charge pas les recoins où personne ne se trouve. Au clic, le voyage ne
part donc pas tout de suite : les chunks autour de l'Atterrisseur sont
**demandés, puis attendus**, et la coque ne bouge qu'une fois qu'ils existent
vraiment. Une fois posée, la coque et sa passagère ne font qu'**une seule fiche**
dans la sauvegarde — le chunk peut se décharger derrière elles sans que rien ne
se perde, exactement comme un cochon en barque à l'autre bout du monde.

---

## Bâtir : la famille des briques

**4 cristaux** donnent un **Bloc de l'Ender**, et **4 Blocs de l'Ender** donnent
**4 Briques de l'Ender**.

Les briques sont **translucides exactement comme le bloc dont elles sortent** :
un mur de briques laisse deviner ce qu'il y a derrière, comme la masse dans
laquelle vous le bâtissez. Et un mur épais ne s'assombrit pas couche après
couche — il reste une seule vitre.

En main et dans l'inventaire, en revanche, briques et blocs sont **pleins** :
un objet qu'on transporte n'est pas une vitre, et voir le paysage lointain au
travers du cube qu'on tient ne ressemblait à rien.

La famille se décline en **escaliers, dalles, muret** et une variante
**ciselée**, gravée de l'œil des cadres de portail de l'End. Tout se taille
aussi au **tailleur de pierre**.

---

## Récapitulatif des crafts

```
Porte inactive       Clé            Pioche de l'Ender    Transmetteur d'objet
O O O                C              C C C                I R I
O C N                P              . S .                I E I
O O O                G              . S .                I I I

Passage des Alliés   Contrôle de l'amitié   Guide de World of Ender
Q C Q                q q q                  C C C
C E C                r C r                  C L C
Q C Q                q q q                  C C C

Téléporteur d'entité   Atterrisseur d'entité (×2)
.                      O S O
O p O                  O C O
O O O                  O C O

Sac de l'Ender (sans forme) : Bundle + Coffre de l'Ender
Bloc de l'Ender : 4 cristaux    Briques : 4 Blocs de l'Ender → 4 briques

O = Obsidienne pleureuse   C = Cristal de l'Ender    N = Nether Star
P = Perle d'Ender          G = Lingot d'or           S = Bâton
I = Bloc de fer            E = Coffre de l'Ender     R = Bloc de redstone
Q = Bloc de quartz         q = Quartz                r = Redstone
L = Livre                  S = Bloc de slime         p = Perle de l'Ender
```

Le rituel d'éveil demande en plus une **Mace** vanilla, qui n'est pas consommée.

**Le Guide de World of Ender** (8 cristaux autour d'un livre) est un livre écrit
de trente-deux pages : le lore, chaque machine et chaque craft — la porte, la
pioche, les briques, le Transmetteur, le Sac, le Passage des Alliés et le
Téléporteur d'entité — dans la langue du jeu (français ou anglais).

---

## Les progrès

Un arbre complet, **« Le paradis des cubes »** :

*Une pierre qui n'est pas d'ici* → *Le seuil* → *Frapper le ciel* → *Le paradis
des cubes*, puis cinq branches — *Tailler sa galerie* (mille Blocs de l'Ender),
*Le fond du monde*, *L'entrepôt* → *Tout ranger d'un geste*, *Bâtir dans le
translucide*, et *Ami de passage*.

---

## Shaders

L'Ender se déclare auprès des shaders **comme le Nether** : c'est ce qui lui
donne son rendu souterrain, sans horizon ni ciel, avec son brouillard et sa
tempête de cendres. **Il n'y a rien à configurer.** Testé avec Complementary
Reimagined.

Le regard porte au travers de la masse translucide, donc très loin. Un fondu au
noir ferme la vue avant la limite des chunks — vous ne verrez jamais le bord du
monde chargé, quel que soit votre réglage de distance de rendu. De loin en loin,
ce lointain s'embrase une fraction de seconde : de silencieuses lueurs d'orage.

Le mod ne cible aucun shaderpack en particulier : il déclare son type de
dimension, et c'est Iris qui en déduit le dossier de shaders à appliquer.
**Photon** rend donc l'Ender avec sa passe du Nether, comme Complementary et
sans le moindre réglage — il n'embarque pas de `dimension.properties` qui
pourrait dire le contraire, et son dossier `world-1` est un jeu de programmes
complet.

Un réglage de Photon décide cependant de la **couleur** du brouillard, et il
vaut d'être connu : *Light Sources → **NETHER_USE_BIOME_COLOR***. Activé — c'est
son défaut — Photon prend la couleur que le monde lui donne, donc la nôtre.
Désactivé, il retombe sur sa teinte du Nether, un orange vif : l'Ender virerait
au feu. Laissez-le tel quel.

### Forcer une autre passe de rendu

Rien ne vous oblige à garder celle du Nether. Une ligne dans le fichier
`dimension.properties` du shaderpack (dans son dossier `shaders/`, à créer s'il
n'existe pas) redirige le monde où vous voulez — Photon a par exemple un
brouillard volumétrique dédié à l'End :

```
dimension.world1=minecraft:the_end enderportals:ender_world
dimension.world0=*
```

Cette correspondance explicite l'emporte sur tout le reste. C'est le seul
endroit d'où l'on puisse changer d'avis : un mod ne peut pas écrire dans un
shaderpack.

### Une pause au premier passage, avec shaders

À la toute première ouverture de la porte dans une session de jeu, l'écran se
fige deux à quatre secondes. C'est **Iris qui compile sa passe de rendu** pour
une dimension qui n'était encore jamais apparue — un travail qui se fait sur le
fil d'affichage, et sur lequel aucun mod n'a la main.

Immersive Portals rend la pause visible : pour vous montrer l'Ender à travers
l'embrasure, il crée le monde d'arrivée **pendant que vous êtes encore dehors**.
La même compilation existe pour un portail du Nether, mais elle s'y cache
derrière l'écran de chargement du voyage.

Elle ne se paie **qu'une fois par session**, et pas du tout sans shaders.

**Envie d'un flou de profondeur ?** Complementary le fournit, mais désactivé par
défaut : *Camera Settings → World Blur → World Blur → **Distance Blur***. Réglez
ensuite l'intensité sur le curseur **« Dis. Blur — The Nether »** (défaut 64) :
plus la valeur est haute, plus c'est flou.

---

## Multijoueur et compatibilité

Le mod est **serveur-autoritaire** : toute la logique — rituel, clé, passages,
stockage — s'exécute côté serveur, le client ne fait qu'afficher. Il fonctionne
tel quel en serveur dédié.

Il est aussi conçu pour cohabiter : **aucun mixin**, des tags vanilla purement
additifs, des écouteurs d'événements limités à ses propres blocs, et un
générateur de dimension autonome. La clé et le rituel respectent la **spawn
protection**, le **mode aventure** et les **mods de protection de terrain**.

**Immersive Portals** est optionnel. S'il est présent, la porte comme le passage
deviennent des portails « voir au travers » ; s'il est absent ou si son API
change, le mod bascule seul sur sa téléportation classique et le signale dans
les logs.

---

## Installer

Placez le jar dans `mods/`, avec **NeoForge 21.1.x** pour **Minecraft 1.21.1**.

Chaque build est publié automatiquement dans la pré-release
[`dev-latest`](../../releases/tag/dev-latest).

## Compiler

Prérequis : **Java 21**.

```bash
./gradlew build
# → build/libs/enderportals-0.26.0.jar
```

Le build est géré par **ModDevGradle** ; NeoForge et les mappings officiels sont
téléchargés automatiquement. Les versions sont épinglées dans
`gradle.properties`.

> Le mod s'appelle *World of Ender*, mais son identifiant interne reste
> `enderportals` — il est inscrit dans chaque bloc, objet, recette et
> sauvegarde existante. En changer casserait tous les mondes déjà créés.
