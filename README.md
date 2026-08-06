# World of Ender — a pocket base behind an obsidian door

*[Version française](README.fr.md)*

A **Minecraft 1.21.1 / NeoForge** mod. You forge a door, you wake it at the cost
of a ritual, and it opens onto a world that was not there before: **the Ender**,
a translucent underground mass where destroyed blocks come to rest. Behind that
door, an entire plot of land is yours — and you carry it everywhere. The door
dematerialises into your pocket and sets itself down wherever you like.

Current version: **0.29.2**.

---

## The lore

The player has the Overworld, and beneath the Overworld, the Nether.
The Enderman has the End, and beneath the End, **the Ender**.

Endermen do not pick up blocks at random. Every block they lift is an offering:
matter taken out of the world, carried off to be given up — and what is given up
is sent to the Ender, where destroyed things come to rest. The paradise of
cubes.

Your door does not open onto a new world. It opens onto theirs.

---

## What the mod gives you

* **A base you can carry.** A two-block door, placed with a single click, that
  holds an 8192-block plot in a dimension of its own.
* **A world to dig.** The Ender is solid from end to end — not one cave, not one
  tunnel. You move through it by carving your own gallery.
* **Storage at a distance.** Wherever you are, empty your inventory into the
  chests back home with one right-click.
* **Sharing, by consent.** The Allies' Passage links two bases — but only if
  both players want it, each from their own side.
* **Materials.** The Ender Block and its whole brick family: translucent, so you
  build walls you can see through.

---

## Getting started

### 1. Find the ore

**Ender Ore** generates in End stone, between **Y 10 and Y 70**, in small
clusters. It grows nowhere else, so the mod opens with a trip to the End.

An **iron pickaxe** is enough to break it, and it yields **Ender Crystals**.
Fortune increases the yield; Silk Touch collects the ore block intact.

The crystal is **violet**, cut like a gem — the colour of the End, of the door
and of the machines. It is what tells you, at a glance in a chest, what belongs
to the mod.

Everything else in the mod is built from those crystals.

### 2. Forge the door

**7 crying obsidian**, **1 Ender Crystal** and **1 nether star** make an
**Inactive Ender Door**. At this stage it does nothing: it is a two-block casing
that you place and take back with a pickaxe. It is asleep.

### 3. The Mace ritual

To wake it, you have to strike it hard enough to crack space itself.

Take a vanilla **Mace** (heavy core + breeze rod). Place the door where you
want, climb **at least 20 blocks** above it, jump, and **hit it mid-fall**
(left-click). It is the Mace's smash attack, aimed not at a mob but at a door.

The impact absorbs your fall damage, lightning strikes, and the door wakes: its
inner room has just been carved out of the Ender. The Mace is not consumed, only
slightly worn.

> One door per player. The ritual refuses to wake a second one.

### 4. Forge the key

**1 crystal + 1 ender pearl + 1 gold ingot**, stacked in a column, make the
**Ender Key**. **Right-click in the air** to bind it to your door: it will obey
no one else.

### 5. The pickaxe

**3 crystals + 2 sticks** make the **Ender Pickaxe**. It is the only tool that
harvests Ender Blocks, and it breaks them almost instantly — it is how you move
through the mass.

Failing that, the **"Spacetime Breach"** enchantment lets any pickaxe break them
with drops, at a decent speed. It is a **treasure** enchantment, like Mending:
you find it in loot or trade for it with a librarian, never at an enchanting
table.

---

## The key, day to day

| You do… | What happens |
| --- | --- |
| Right-click the door, **bare-handed** | It opens, or closes. Like every door in the game. |
| Right-click the **inner door** while the door is stowed | It materialises outside, open: that is your way out. |
| **Key** + right-click **on the ground** | The door fades into place where you aimed, and vanishes from wherever it stood before. |
| **Key** + **sneak** + right-click the door | It dematerialises: your base goes back into your pocket. |
| **Key** + right-click **in the air** | Binds the key to your door. |

The key opens and closes too, if you happen to be holding it — same gesture, no
need to put it away. What only the key does is **make the door appear and
disappear**, and bind itself to it.

**One gesture at a time.** The door takes **1.75 s** to materialise, **1.5 s** to
fade away. While that fade lasts it turns the next order down and answers "The
door has not finished its passage." That covers everything — appearing,
disappearing, opening, closing, and setting it down with the key. The moment it
has finished reappearing, it obeys again.

### The passage

A fade is not an opacity going up. Here is what you actually see when the door
arrives:

* **A wave** of white light with violet edges climbs all four faces of the
  casing, from threshold to lintel. It lights up as it rises and goes out as it
  leaves through the top — on the way out, it travels back down.
* **Matter converges.** Portal grains stream in from a ring over a block wide
  and tighten onto the door as it takes hold. When it leaves, everything flies
  back outwards and rises.
* **The door shivers** by three centimetres for as long as it is not quite
  there, and steadies as it turns solid. Its opacity throb calms the same way:
  it does not snap to 100 %, it settles.
* **It lands** on a ring of sparks and two notes — a deep impact and an amethyst
  resonance. On the way out, it snaps shut on a high whisper.

**All of this obeys the owner alone.** Anyone else clicking your door, with or
without a key, reads "locked".

### The key obeys only you

Dropped, lost or stolen, it is **inert in anyone else's hands**: it materialises
nothing, opens nothing, and will not bind itself to your door. Nobody walks into
your base by picking up your keyring.

### Losing your key does not cost you your base

Forge another one (crystal, pearl, gold ingot) and right-click in the air: it
binds to your door, wherever that door happens to be. This matters, because a
dematerialised door leaves nothing to click on and the ritual refuses to wake a
second one — without that re-bind, the base would be gone for good.

### If someone builds where your door was

Picture this. You go home, you close the door behind you, and it disappears from
the outside world. While you are inside, someone builds on the exact spot where
it stood — or a tree grows there.

Now you want out. You click the inner door to recall it outside… and there is no
room. Without a safeguard you would be locked in: your plot is walled in
bedrock, and that door is the only way out.

So **the recall cannot fail**. It tries, in order:

1. the exact spot where you left it;
2. failing that, a free space nearby — up to 8 blocks around and 4 above or
   below, on solid ground;
3. failing that, your respawn point.

If it had to move the door, it tells you the new coordinates.

**And without the key?** The door opens by hand, so nothing can shut you in. That
is the reason for the split: dying outside leaves the key on the ground with
everything else, and if your bed is in the Ender you respawn inside without it.
Forging another key needs an ender pearl — and no creature spawns in the Ender. A
door that only answered to its key therefore condemned the base and everything
in it.

**Placing** the door from outside, on the other hand, is allowed to refuse. There
you chose the spot yourself, "not enough room" is an honest answer, and you are
not locked anywhere — just aim somewhere else.

---

## The Ender

A wholly underground world, carved out of a mass of **Ender Blocks**: grey,
translucent like a honey block, faintly luminous.

Through that matter you **catch sight of the relic blocks** that came here to
die — stone, logs, ores up to diamond, bookshelves, sponges. They are not
scattered one by one but **gathered in clouds** of a single material, so diffuse
that no two blocks touch: the cloud frays into the mass instead of stopping
dead.

Rare **veins of pearlescent froglight** run through it in long trails of violet
light. They are the only landmarks in a world that looks the same everywhere —
and the only light: everywhere else, the darkness is total. A pale ash falls
without end.

**The mass is solid.** No cave, no tunnel, no empty space. You only move through
it with the Ender Pickaxe, digging your own gallery.

### Your plot

Every awakened door receives its own **8192-block plot**. Build whatever you
like there: base, farms, storage. **Beds work** — you can sleep and set your
respawn point. Respawn anchors do not; they explode here just as they do in the
Overworld. Nether portals will not light.

The world runs from **-64 to 320**, like the Overworld, and is sealed top and
bottom by two layers of bedrock. Your arrival room sits at **Y 64**.

The owner's name is displayed on a small plate on the front of the door.

### Everyone in their own place

Plots are separated by a **grid of bedrock walls** running from the floor of the
world to its ceiling. You can dig in every direction without ever stumbling into
someone else's base, and without anyone stumbling into yours.

The **Allies' Passage** is the door you deliberately open in that wall.

---

## The Allies' Passage

Two pocket bases, one threshold.

The **Allies' Passage** is a pale casing — quartz and gold veining, the opposite
of the Ender Door's obsidian. It can only be placed **inside the Ender**. On its
own it stays sealed: you have to set a **Friendship Control** against it, the
keypad that drives everything.

* **Allies' Passage**: 4 quartz blocks, 4 crystals, 1 ender chest.
* **Friendship Control**: 6 quartz, 2 redstone, 1 crystal.

The panel has to touch the passage — flush against it, on any of its four sides.

**Place as many passages as you have friends to link.** Each arch carries one
ally, and the panel next to it is what commands that arch: clicking a name at a
panel binds *the* passage it touches. So a base with three friends has three
arches, each with its own Control.

A Control belongs to whoever owns the arch it touches. Standing in an ally's
base, theirs will not open for you — your book lives on your own panels. A
Control touching no arch belongs to nobody and opens for no one; it would
command nothing.

**A Control commands one arch and one only.** Placing it where it would touch
two is refused, and so is placing an arch that would put an existing Control
astride two. Leaving one empty block between each Control-and-arch group is the
simple way to always satisfy that. Two arches side by side are fine in
themselves — what must never happen is a Control that could mean either one,
because nothing on screen would say which it chose.

A closed arch is an arch bound to nobody, and it carries no name. There is no
in-between state: binding and unbinding always happen on both sides at once, so
an arch is either free and closed, or bound and open.

Nothing forces you to have several. One arch, one Control, one friend at a time
works exactly as before.

### The three degrees of trust

Opening a passage is no longer giving everything away. To the right of each name,
as soon as the link is **green**, a badge says what that ally may do **at your
place**. Click it: it steps to the next degree, and wraps back to the first.

| Degree | Walk in | Use your blocks | Break and build |
| --- | :---: | :---: | :---: |
| **Visitor** | ✔ | | |
| **Guest** | ✔ | ✔ | |
| **Partner** | ✔ | ✔ | ✔ |

"Use" covers **anything you can act on**: chests, furnaces, crafting tables,
levers. A visitor does not open those — but they do **get around**: doors,
trapdoors, fence gates, buttons and pressure plates stay available. Walking
through someone's place is not helping yourself to it.

That split is an **allowlist**, and that is what makes it safe. The mod does not
try to recognise storage in order to refuse it — it did for a while, and
Sophisticated Storage chests slipped through, because they open their screen by
their own route. Instead it enumerates what is **permitted** and refuses
everything else: the next mod's storage is covered in advance, with nobody having
to foresee it. The list lives in the `enderportals:visitor_usable` tag, built from
vanilla tags — modded doors that join `#minecraft:doors` come along for free — and
a pack that wants levers in there has one line to write.

Three things to know:

* **The degree runs one way.** Opening my chests to you does not oblige me to
  open yours. Each of you sets their own plot, on their own panel.
* **It survives the passage closing.** Asking for it again on every reopening
  would have turned a setting into a chore — and pushed everyone to leave partner
  as the default, which is exactly what this is meant to avoid.
* **Everyone starts as a visitor**, including alliances sealed before this
  version. Nobody wakes up with rights they were never given.
* **No exceptions, not even for operators.** An admin still has creative mode and
  commands, which do not go through this; but on a server where everyone is an
  operator — a test server, typically — a guard that steps aside for them never
  fires at all, and simply looks broken.

The ally is told on their terminal on every change: otherwise they would discover
their degree by being refused a chest, with no idea anything had moved. Forgetting
an ally (**Shift + click** on the name) resets the degree both ways.

> **What this does not cover.** The three direct routes are held: breaking,
> placing, opening storage. A tamed animal killed, an item frame emptied, TNT lit
> from outside your plot are all still possible. That is a deliberate scope
> rather than a watertightness claimed too cheaply.
>
> **And if you close the passage while a friend is inside?** They leave through
> your inner door if it is open. Closed, they cannot open it — it obeys you alone
> — and your plot is walled in bedrock. So close up once they are home. That point
> predates the trust degrees and is unchanged by them.

### The friend code

Every awakened door receives an **eight-digit code, every digit between 1 and
9**. You read it on your key's tooltip, and it is repeated under the panel's
buttons. That is the code you tell each other.

### The terminal

The bottom of the panel is a terminal, and everything the Control has to say
lands there: a code refused, a friendship sealed, an ally asking to connect, a
passage opened or closed. Those lines used to go to the chat, where they arrived
while you were busy at the keypad and got buried under everything else.

The log is kept by the server, one per player, so a message sent while you were
elsewhere is waiting the next time you open the panel — including one sent while
you were offline, which the chat simply dropped. The wheel scrolls it.

The chat only takes over when you have no panel open. A connection request is
valid for two minutes; it would be no use to anyone sleeping in a closed screen.

Next to the terminal's title, a lamp reports the one thing that matters:
**is the passage open on both sides?** **Working** in green when it is,
**Not working** in red otherwise.

The verdict alone would not tell you what to do about it, so the bottom line of
the terminal names the cause and stays there as long as it lasts: no passage of
yours against this panel, no link open, or an ally who has broken their own
arch. It sits outside the log, below a rule — the log tells you what happened,
that line tells you where you stand.

### Becoming friends

Type the other player's code on the keypad, digit by digit, then **CONFIRM**
(`CLEAR` resets it). On the keyboard, **Backspace** corrects and **Enter**
confirms.

Their name appears in the address book on the left, with an **hourglass**: only
one of the two codes has been entered. The other player reads it on their own terminal
and has to type yours from their side. Once they have, the hourglass gives way to their
**player head**: you are friends.

The friendship is symmetric by construction — it is nothing but the meeting of
two declarations. There is no state in which one of you thinks you are friends
and the other does not.

**Shift + click** a name to remove it from your book.

### Opening the passage

Click your ally's name: its border turns **yellow** — you are waiting. They read it on their
terminal and have **two minutes** to click yours; on their screen, your name
blinks **orange**, the one state that calls for an action. After that, the
request is dropped.

As soon as both of you have clicked, **both casings light up for three seconds**
and then open, with a **green** border on either side. The link stays open as
long as you want it, and **you cross it both ways**: each of you walks freely
into the other's base.

To close it, click the name ringed in green — from any of your panels. To
reopen it, start over: both players click each other's name again.

Two arches of yours never interfere: opening one leaves the others alone. The
only displacement left is within a single arch — binding it to a new friend
releases the one it held, and that friend is told.

Once the link is established, the two bases stay connected **permanently**,
including while one of the two players is offline: their passage remains open on
your side and you can still walk over to their base.

The passage also closes on its own if either player breaks their casing, or if
one of you opens a link with somebody else — you only get one passage at a time.

### With Immersive Portals

If the mod is installed, an open passage is no longer a threshold: **you see
your ally's base through the doorway** and walk into it, with no loading screen.
Without Immersive Portals everything works the same, you simply cross on
contact.

---

## Storage at a distance

Two items turn your pocket base into a warehouse you can reach from anywhere:

* **The Item Transmitter** (7 iron blocks, 1 redstone block, 1 ender chest) is
  placed **inside the Ender**, against your storage. It federates the whole
  network next to it: block by block, the network grows.
* **The Ender Bag** (a bundle + an ender chest) sits in your inventory. **Pick a
  stack onto the cursor** — left-click it — then **right-click the bag**: it goes
  into that network, wherever you are in the world.

The gesture works **both ways**: the bag in a slot and the stack on the cursor,
or the bag on the cursor and the stack in the slot. It is the vanilla bundle
gesture, and it no longer takes up your off hand. Mind the second direction: you
move a bag around your inventory more often than you store things, and during
that move a right-click on a stack sends it away. Every
stack sent costs **3 experience points** — the price of the trip, not of the
weight: a stack of sixty-four blocks costs the same as a single item. If the
network is full or you are short on XP, nothing leaves, the stack stays on your
cursor, a dry sound says so and the message gives the reason.

### It plugs into your storage mod

The Transmitter does not replace your storage system, it **connects to it**. It
recognises any storage that exposes an inventory through the NeoForge standard,
which in practice means nearly every mod:

* **Sophisticated Storage** — chests, barrels and shulkers of every tier, and the
  Storage Controller.
* **Sophisticated Backpacks** — backpacks placed on the ground.
* **Tom's Simple Storage** — set the Transmitter against an Inventory Connector
  and the Bag pours into the whole network; the Storage Terminal shows your
  items, counted exactly once.
* Vanilla chests and barrels, drawers, and the rest.

Because insertion goes through the standard path, it **respects each storage's
filters and upgrades**: a filtered Sophisticated barrel will sort automatically
whatever you pour in.

---

## The Entity Teleporter

The key only takes you. To bring an animal into the Ender you need two pieces:

* **The Entity Lander** (6 crying obsidian, 2 crystals, 1 slime block — you get
  two) is placed **inside the Ender**, wherever you want the animal to arrive.
* **The Entity Teleporter** (5 crying obsidian, 1 ender pearl) is a hull of steel
  and black obsidian, placed and boarded **like a boat**.

Three steps. **Holding the teleporter, right-click a Lander**: the hull remembers
that spot, and its tooltip now shows the coordinates in green. **Set it down**
where you like, then **lure the animal aboard** as you would into a boat — a
confirmation sound plays and the prow lamp turns green. **Right-click the hull**:
it leaves with whatever it carries.

Place as many of either as you want; each hull keeps its own link.

### The prow lamp

It never goes out; it tells you whether the trip is possible.

* **Green** — the hull has a destination *and* a passenger: it can leave.
* **Red** — one of the two is missing.

It speaks of the machine, not of the ground: knowing whether the Lander is still
there would mean keeping its chunk loaded at all times, which is exactly what we
avoid. That check happens at departure, and you are told if the Lander is gone.

### The lamp, the crystals and the pad

The machine carries four **crystal-topped pylons**, a **gold rail** along its
top edge, a **prow console** holding the lamp, and above all a **violet
departure pad** set into its floor — that is where the animal stands, and what
you see from above. Crystals and pad glow on their own: the machine is easy to
find at night.

### What a trip costs

Like the Ender Bag, the machine is paid for in **experience**: **20 points per
creature carried**. It is only taken **on arrival** — a failed trip costs nothing
— and departure is refused up front if you cannot pay for it.

### Getting the hull back

* **Empty, a right-click** puts it back in your hand — with its link, so you do
  not have to re-bind it for every trip.
* **Sneak + right-click** also picks it up and **lets its passenger out**: that is
  the arrival gesture, once the animal is where you wanted it.

Breaking the hull works too, but loses its Lander.

### The arrival chunk

The Ender does not keep corners loaded where nobody is standing. So the trip does
not start on the click: the chunks around the Lander are **requested, then waited
for**, and the hull only moves once they really exist. Once it has landed, hull
and passenger are **a single record** in the save — the chunk can unload behind
them without anything being lost, exactly like a pig in a boat on the far side of
the world.

### Charging, and leaving

That wait lasts anywhere from nothing to a few seconds, and there is no telling
in advance. The machine makes a show of it:

* **A ring of light** travels up the hull on a loop, from under the keel to above
  the crystals, for as long as the charge lasts. It is the same light as the
  door's wave — the two machines are the same technology.
* **The hull draws in.** Portal grains converge from a ring over a metre wide,
  while the four crystals spit a spark upward, each in turn.
* **A note climbs**, higher and higher, until the arrival ground is ready. As
  long as it climbs, work is happening.
* **The departure.** The hull leaves on a column of sparks and a violet burst
  collapsing into the spot it just left — which is what you see, since you stay
  on this side. At the other end, the ring spreads out above the Lander.
* **If the trip is refused** after charging has begun — the Lander is gone, the
  experience was spent in the meantime — the ring goes out on a dry click. A
  silent refusal in the middle of all that would go unnoticed.

Clicking again while it charges restarts nothing: the hull answers that it is
already on its way.

---

## Building: the brick family

**4 crystals** make an **Ender Block**, and **4 Ender Blocks** make **4 Ender
Bricks**.

The bricks are **translucent exactly like the block they come from**: a brick
wall lets you make out what is behind it, just like the mass you build it in.
And a thick wall does not darken layer by layer — it stays a single pane.

In your hand and in the inventory, though, blocks and bricks are **solid**: a
thing you carry is not a window, and seeing distant terrain through the cube you
are holding looked like nothing at all.

The family comes with **stairs, slabs, a wall** and a **chiselled** variant
engraved with the eye of the End portal frames. Everything can also be cut on
the **stonecutter**.

---

## Recipe summary

```
Inactive door        Key            Ender Pickaxe        Item Transmitter
O O O                C              C C C                I R I
O C N                P              . S .                I E I
O O O                G              . S .                I I I

Allies' Passage      Friendship Control     World of Ender Guide
Q C Q                q q q                  C C C
C E C                r C r                  C L C
Q C Q                q q q                  C C C

Entity Teleporter      Entity Lander (×2)
.                      O S O
O p O                  O C O
O O O                  O C O

Ender Bag (shapeless): Bundle + Ender Chest
Ender Block: 4 crystals     Bricks: 4 Ender Blocks → 4 bricks

O = Crying obsidian     C = Ender Crystal      N = Nether Star
P = Ender Pearl         G = Gold ingot         S = Stick
I = Iron block          E = Ender Chest        R = Redstone block
Q = Quartz block        q = Quartz             r = Redstone dust
L = Book                S = Slime block        p = Ender pearl
```

The waking ritual also needs a vanilla **Mace**, which is not consumed.

**The World of Ender Guide** (8 crystals around a book) is a thirty-two page
written book: the lore, every machine and every recipe — the door, the pickaxe,
the bricks, the Transmitter, the Bag, the Allies' Passage and the Entity
Teleporter — in the game's language (English or French).

---

## Tooltips

Every machine here has enough to fill five or six lines. Shown all at once, they
covered the screen the moment you hovered over a chest — so the instructions only
unfold while you **hold SHIFT**, the way storage mods do it. The key name is
written in yellow against the grey of the line, so you spot it without reading
it.

What stays visible without pressing anything is **the state of that particular
item**: the door this key is bound to and its friend code, the Lander this
teleporter is aiming at. That is what tells two items apart in a chest, and what
you came to read. The rest explains how they work, and you do not read that
thirty times.

---

## Advancements

A full tree, **"The paradise of cubes"**:

*A stone not from here* → *The threshold* → *Striking the sky* → *The paradise of
cubes*, then five branches — *Carving your gallery* (a thousand Ender Blocks),
*The bottom of the world*, *The warehouse* → *Everything away in one gesture*,
*Building in the translucent*, and *Passing friend*.

---

## Shaders

The Ender declares itself to shaders **as the Nether**, and that is what gives
it its underground look: no horizon, no sky, with fog and an ash storm. **There
is nothing to configure.** Tested with Complementary Reimagined.

Your gaze carries through the translucent mass, so it carries very far. A fade
to black closes the view before the chunk limit — you will never see the edge of
the loaded world, whatever your render distance. Now and then that far distance
flares for a fraction of a second: silent storm light.

The mod targets no shaderpack in particular: it declares its dimension type, and
Iris derives the shader folder from that. **Photon** therefore renders the Ender
with its Nether pass just as Complementary does, with nothing to configure — it
ships no `dimension.properties` that could say otherwise, and its `world-1`
folder is a complete program set.

One Photon setting does decide the fog's **colour**, and it is worth knowing:
*Light Sources → **NETHER_USE_BIOME_COLOR***. Enabled — its default — Photon
takes whatever colour the world hands it, so ours. Disabled, it falls back to
its own Nether tint, a vivid orange: the Ender would turn to fire. Leave it on.

### Forcing a different pass

Nothing obliges you to keep the Nether one. A single line in the shaderpack's
`dimension.properties` (inside its `shaders/` folder, create it if absent)
redirects the world wherever you like — Photon, for one, has a volumetric fog
written for the End:

```
dimension.world1=minecraft:the_end enderportals:ender_world
dimension.world0=*
```

An explicit mapping wins over everything else. It is the only place this can be
changed: a mod cannot write inside a shaderpack.

### A pause on the first crossing, with shaders

The very first time you open the door in a play session, the screen freezes for
two to four seconds. That is **Iris compiling its render pipeline** for a
dimension that had never appeared before — work done on the render thread, over
which no mod has any control.

Immersive Portals is what makes the pause visible: to show you the Ender through
the doorway, it creates the destination world **while you are still outside**.
The same compilation happens for a Nether portal, but there it hides behind the
travel loading screen.

You pay it **once per session**, and not at all without shaders.

**Want depth blur?** Complementary provides it, but off by default: *Camera
Settings → World Blur → World Blur → **Distance Blur***. Then set the strength on
the **"Dis. Blur — The Nether"** slider (default 64): the higher the value, the
blurrier.

---

## Multiplayer and compatibility

The mod is **server-authoritative**: all the logic — ritual, key, passages,
storage — runs on the server, and the client only draws. It works as-is on a
dedicated server.

It is also built to coexist: **no mixins**, purely additive vanilla tags, event
listeners restricted to its own blocks, and a self-contained dimension
generator. The key and the ritual respect **spawn protection**, **adventure
mode** and **land protection mods**.

**Immersive Portals** is optional. If it is present, both the door and the
passage become see-through portals; if it is missing, or if its API changes, the
mod falls back to its own teleport on its own and says so in the logs.

---

## Installing

Drop the jar in `mods/`, with **NeoForge 21.1.x** for **Minecraft 1.21.1**.

Every build is published automatically to the
[`dev-latest`](../../releases/tag/dev-latest)
pre-release.

## Building

Requires **Java 21**.

```bash
./gradlew build
# → build/libs/enderportals-0.29.2.jar
```

The build is handled by **ModDevGradle**; NeoForge and the official mappings are
downloaded automatically. Versions are pinned in `gradle.properties`.

> The mod is named *World of Ender*, but its internal id stays `enderportals` —
> it is written into every block, item, recipe and existing save. Changing it
> would break every world already created.
