package com.maxezify.enderportals.network;

import com.maxezify.enderportals.ModAdvancements;
import com.maxezify.enderportals.ModBlocks;
import com.maxezify.enderportals.block.AllyPassageBlock;
import com.maxezify.enderportals.tardis.AllyLinks;
import com.maxezify.enderportals.tardis.AllyPassageHelper;
import com.maxezify.enderportals.tardis.ConsoleLog;
import com.maxezify.enderportals.tardis.PassageData;
import com.maxezify.enderportals.tardis.TardisData;
import com.maxezify.enderportals.tardis.TardisStateManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tout ce que le Contrôle de l'amitié décide, côté serveur.
 *
 * <p>Le client n'a aucune autorité : il envoie un code frappé ou un nom cliqué,
 * et cette classe revérifie chaque fois que le joueur est bien à portée du
 * panneau qu'il prétend manipuler, qu'il possède une porte, et que l'amitié est
 * réciproque. Après chaque action l'état complet repart vers l'écran — et aussi
 * vers celui du pair concerné, dont l'affichage vient de changer sans qu'il ait
 * touché à quoi que ce soit.</p>
 *
 * <p>Aucun message ne part plus directement dans le chat : tout passe par
 * {@link #say}, qui l'écrit au journal du destinataire — le terminal du panneau
 * l'affichera. Le chat n'est plus qu'un relais, et seulement pour un joueur qui
 * n'a pas ce panneau sous les yeux ; sans quoi il verrait la même phrase deux
 * fois.</p>
 */
public final class ConsoleServerLogic {

    /** Portée d'interaction admise, au carré. Huit blocs : large, mais borné. */
    private static final double REACH_SQR = 64.0;

    /** Période de relecture des panneaux ouverts, en ticks. Une seconde. */
    private static final int REFRESH_PERIOD = 20;

    /**
     * Dernier état envoyé à chaque écran ouvert.
     *
     * <p>Le panneau se relit chaque seconde parce que le témoin de passage
     * dépend de choses qu'aucun clic n'annonce — l'ouverture qui aboutit trois
     * secondes après la poignée de main, un allié qui casse son arche à l'autre
     * bout. Réémettre pour autant un état identique coûterait un paquet par
     * seconde et par écran, et surtout ferait retomber le terminal en bas du
     * journal à chaque envoi : impossible d'y remonter. On ne parle donc que
     * quand quelque chose a changé.</p>
     *
     * <p>L'entrée est retirée en même temps que le panneau se ferme, par
     * {@link #stopViewing} — sans quoi rouvrir un panneau inchangé n'enverrait
     * rien, et l'écran ne s'ouvrirait pas.</p>
     */
    private static final Map<UUID, ConsoleStatePayload> LAST_SENT = new HashMap<>();

    // ------------------------------------------------------------------
    // Ouverture et rafraîchissement
    // ------------------------------------------------------------------

    /**
     * Ouvre le panneau, si ce Contrôle est bien à ce joueur.
     *
     * <p>Un Contrôle appartient à qui appartient l'arche qu'il touche : c'est la
     * seule définition qui n'invente aucune donnée, et elle suffit. Chez un
     * allié, atteint par un passage ouvert, les Contrôles commandent ses arches
     * à lui — ils refusent donc de s'ouvrir, et le carnet reste celui de chacun,
     * sur ses propres panneaux.</p>
     *
     * <p>Un Contrôle qui ne touche aucune arche n'appartient à personne. Il ne
     * s'ouvre pas non plus : il ne commanderait rien, et le dire tout de suite
     * vaut mieux qu'un panneau qui s'ouvre pour annoncer son impuissance.</p>
     */
    public static void open(ServerPlayer player, BlockPos console) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        TardisStateManager manager = TardisStateManager.get(server);
        if (manager.findByOwner(player.getUUID()) == null) {
            // Ces messages restent hors du terminal : ils expliquent justement
            // pourquoi le panneau ne s'ouvre pas.
            player.displayClientMessage(
                    Component.translatable("enderportals.message.console_no_door"), true);
            return;
        }
        if (adjacentPassage(player, manager, console) == null) {
            player.displayClientMessage(Component.translatable(refusalFor(player, console)), true);
            return;
        }
        manager.allies().setViewing(player.getUUID(), console);
        // L'ouverture doit parler, même sur un état identique au dernier connu :
        // c'est ce paquet qui fait apparaître l'écran.
        LAST_SENT.remove(player.getUUID());
        send(player, manager, console);
    }

    /** Ce panneau n'est plus à l'écran de ce joueur. */
    public static void stopViewing(TardisStateManager manager, UUID player) {
        manager.allies().setViewing(player, null);
        LAST_SENT.remove(player);
    }

    /**
     * Relit les panneaux ouverts, une fois par seconde.
     *
     * <p>Le témoin de passage ne dépend pas que des clics : l'arche s'ouvre trois
     * secondes après la poignée de main, et un allié peut casser la sienne à
     * l'autre bout du monde. Sans cette relecture, le témoin garderait la
     * couleur qu'il avait au dernier geste — c'est-à-dire qu'il mentirait
     * précisément dans les cas où il sert.</p>
     */
    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % REFRESH_PERIOD != 0) {
            return;
        }
        TardisStateManager manager = TardisStateManager.get(server);
        for (UUID viewer : manager.allies().viewers()) {
            refresh(server, viewer);
        }
    }

    /** Renvoie l'état au joueur si son panneau est encore ouvert. */
    public static void refresh(MinecraftServer server, UUID owner) {
        ServerPlayer player = server.getPlayerList().getPlayer(owner);
        if (player == null) {
            return;
        }
        TardisStateManager manager = TardisStateManager.get(server);
        BlockPos console = manager.allies().viewedConsole(owner);
        if (console != null) {
            send(player, manager, console);
        }
    }

    private static void send(ServerPlayer player, TardisStateManager manager, BlockPos console) {
        ConsoleStatePayload state = buildState(player, manager, console);
        if (state.equals(LAST_SENT.get(player.getUUID()))) {
            return;
        }
        LAST_SENT.put(player.getUUID(), state);
        ModNetwork.toPlayer(player, state);
    }

    private static ConsoleStatePayload buildState(ServerPlayer player, TardisStateManager manager,
                                                  BlockPos console) {
        UUID me = player.getUUID();
        AllyLinks links = manager.allies();
        long gameTime = player.serverLevel().getGameTime();
        UUID myRequest = links.pendingRequestTarget(me, gameTime);

        List<ConsoleStatePayload.Ally> allies = new ArrayList<>();
        for (UUID other : links.declaredBy(me)) {
            int state;
            if (manager.isLinked(me, other)) {
                // Vert pour tout lien ouvert, par quelque arche que ce soit : le
                // carnet est celui du joueur, pas celui du panneau.
                state = ConsoleStatePayload.LINKED;
            } else if (!links.isConfirmed(me, other)) {
                state = ConsoleStatePayload.PENDING;
            } else if (other.equals(myRequest)) {
                state = ConsoleStatePayload.AWAITING_THEM;
            } else if (me.equals(links.pendingRequestTarget(other, gameTime))) {
                state = ConsoleStatePayload.THEY_ASK;
            } else {
                state = ConsoleStatePayload.CONFIRMED;
            }
            allies.add(new ConsoleStatePayload.Ally(other, displayName(manager, other), state));
        }
        // Ordre stable : les liens ouverts en tête, puis par pseudo. Sans cela
        // la liste danserait d'un rafraîchissement à l'autre (HashSet).
        allies.sort((a, b) -> a.state() == b.state()
                ? a.name().compareToIgnoreCase(b.name())
                : Integer.compare(rank(a.state()), rank(b.state())));

        PassageData adjacent = adjacentPassage(player, manager, console);
        return new ConsoleStatePayload(console, manager.codeOf(me),
                passageState(adjacent), boundAllyName(manager, adjacent),
                allies, manager.log().of(me));
    }

    /** Le pseudo de l'allié auquel l'arche de ce panneau mène, ou une chaîne vide. */
    private static String boundAllyName(TardisStateManager manager, @Nullable PassageData adjacent) {
        return adjacent == null || adjacent.ally == null ? "" : displayName(manager, adjacent.ally);
    }

    /**
     * Ce que le témoin du panneau doit montrer.
     *
     * <p>La condition d'ouverture est celle-là même que suit
     * {@code AllyPassageHelper.reconcile} pour percer les arches : un lien, et
     * un passage <b>de chaque côté</b>. La lire ici deux fois plutôt que de
     * regarder l'état des blocs évite de faire charger la parcelle de l'allié
     * chaque seconde, pour une réponse que le registre donne déjà.</p>
     *
     * <p>L'animation d'ouverture — trois secondes — compte comme ouverte : les
     * deux arches y sont engagées sur une échéance commune, et rien ne peut plus
     * l'interrompre. Clignoter en rouge à chaque ouverture réussie ferait
     * craindre un défaut là où tout se passe bien.</p>
     */
    private static int passageState(@Nullable PassageData adjacent) {
        if (adjacent == null) {
            return ConsoleStatePayload.PASSAGE_NO_PANEL;
        }
        return adjacent.ally == null
                ? ConsoleStatePayload.PASSAGE_CLOSED
                : ConsoleStatePayload.PASSAGE_OPEN;
    }

    /** Priorité d'affichage : ce qui demande une action du joueur remonte. */
    private static int rank(int state) {
        return switch (state) {
            case ConsoleStatePayload.LINKED -> 0;
            case ConsoleStatePayload.THEY_ASK -> 1;
            case ConsoleStatePayload.AWAITING_THEM -> 2;
            case ConsoleStatePayload.CONFIRMED -> 3;
            default -> 4;
        };
    }

    private static String displayName(TardisStateManager manager, UUID player) {
        String name = manager.nameOf(player);
        return name.isEmpty() ? player.toString().substring(0, 8) : name;
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    public static void handle(ConsoleActionPayload payload, @Nullable Player sender) {
        if (!(sender instanceof ServerPlayer player)) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        TardisStateManager manager = TardisStateManager.get(server);
        if (payload.action() == ConsoleActionPayload.CLOSE) {
            // Seulement si c'est bien le panneau noté comme ouvert : l'avis de
            // fermeture d'un écran remplacé par un autre arrive après l'avis
            // d'ouverture du nouveau, et effacerait celui-ci.
            BlockPos viewed = manager.allies().viewedConsole(player.getUUID());
            if (viewed != null && viewed.equals(payload.console())) {
                stopViewing(manager, player.getUUID());
            }
            return;
        }
        if (!canReach(player, payload.console())) {
            stopViewing(manager, player.getUUID());
            return;
        }
        if (manager.findByOwner(player.getUUID()) == null) {
            return;
        }
        switch (payload.action()) {
            case ConsoleActionPayload.SUBMIT_CODE -> submitCode(player, manager, payload);
            case ConsoleActionPayload.TOGGLE -> toggle(player, manager, payload);
            case ConsoleActionPayload.FORGET -> forget(player, manager, payload);
            default -> {
                return;
            }
        }
        // Le renvoi vers l'auteur du geste est fait ici, une fois pour toutes :
        // chaque action écrit au moins une ligne à son terminal, y compris les
        // refus, qui sortaient autrefois par des retours anticipés.
        refresh(server, player.getUUID());
    }

    /**
     * Le joueur est-il vraiment devant ce panneau ? La position vient du client,
     * donc rien ne garantit qu'elle soit ni proche ni même un panneau.
     */
    private static boolean canReach(ServerPlayer player, BlockPos console) {
        if (player.distanceToSqr(Vec3.atCenterOf(console)) > REACH_SQR) {
            return false;
        }
        return player.level().getBlockState(console).is(ModBlocks.FRIENDSHIP_CONSOLE.get());
    }

    private static void submitCode(ServerPlayer player, TardisStateManager manager,
                                   ConsoleActionPayload payload) {
        MinecraftServer server = player.server;
        UUID me = player.getUUID();
        TardisData target = manager.findByCode(payload.code());
        if (target == null || target.ownerUuid == null) {
            say(server, me, ConsoleLog.BAD, "enderportals.message.ally_unknown");
            return;
        }
        AllyLinks.DeclareResult result = manager.allies().declare(me, target.ownerUuid);
        manager.setDirty();
        switch (result) {
            case SELF -> say(server, me, ConsoleLog.WARN, "enderportals.message.ally_self");
            case ALREADY -> say(server, me, ConsoleLog.INFO,
                    "enderportals.message.ally_already", target.ownerName);
            case PENDING -> {
                say(server, me, ConsoleLog.INFO,
                        "enderportals.message.ally_pending", target.ownerName);
                say(server, target.ownerUuid, ConsoleLog.INFO, "enderportals.message.ally_wants_you",
                        player.getGameProfile().getName(), formatCode(manager.codeOf(me)));
            }
            case CONFIRMED -> {
                say(server, me, ConsoleLog.GOOD,
                        "enderportals.message.ally_confirmed", target.ownerName);
                say(server, target.ownerUuid, ConsoleLog.GOOD, "enderportals.message.ally_confirmed",
                        player.getGameProfile().getName());
            }
            default -> {
            }
        }
        refresh(server, target.ownerUuid);
    }

    /**
     * Un clic sur le nom d'un allié, devant un panneau donné.
     *
     * <p>Le panneau ne fait pas que valider : il <b>désigne l'arche</b>. Ouvrir
     * lie celle qu'il touche, et c'est ainsi qu'un joueur relie plusieurs amis
     * à la fois — un Contrôle par arche, chacun commandant la sienne.</p>
     *
     * <p>Fermer, en revanche, n'a pas besoin de panneau accolé : on doit
     * toujours pouvoir couper, y compris depuis un autre panneau et y compris si
     * l'arche a été démontée entre-temps.</p>
     */
    private static void toggle(ServerPlayer player, TardisStateManager manager,
                               ConsoleActionPayload payload) {
        UUID me = player.getUUID();
        UUID other = payload.target();
        MinecraftServer server = player.server;
        AllyLinks links = manager.allies();
        TardisData myDoor = manager.findByOwner(me);
        boolean wasLinked = manager.isLinked(me, other);
        PassageData adjacent = adjacentPassage(player, manager, payload.console());

        if (!wasLinked && adjacent == null) {
            say(server, me, ConsoleLog.BAD, "enderportals.message.passage_missing");
            return;
        }

        AllyLinks.Connect connect = links.toggleConnect(me, other,
                player.serverLevel().getGameTime(), wasLinked,
                adjacent == null ? BlockPos.ZERO : adjacent.pos);
        manager.setDirty();
        String otherName = displayName(manager, other);
        switch (connect.result()) {
            case NOT_FRIENDS -> say(server, me, ConsoleLog.WARN,
                    "enderportals.message.connect_not_friends", otherName);
            case REQUESTED -> {
                say(server, me, ConsoleLog.INFO,
                        "enderportals.message.connect_requested", otherName);
                say(server, other, ConsoleLog.WARN, "enderportals.message.connect_asked",
                        player.getGameProfile().getName());
            }
            case OPENED -> openLink(server, manager, player, myDoor, adjacent, other,
                    connect.theirPassage(), otherName);
            case CLOSED -> {
                closeLink(server, manager, myDoor, other);
                say(server, me, ConsoleLog.INFO, "enderportals.message.connect_closed", otherName);
                say(server, other, ConsoleLog.INFO, "enderportals.message.connect_closed",
                        player.getGameProfile().getName());
            }
            default -> {
            }
        }
        refresh(server, other);
    }

    /**
     * Noue le lien entre deux arches nommément désignées : la mienne, celle que
     * touche le panneau où je viens de cliquer ; la sienne, celle qu'il avait
     * sous les yeux au moment de sa demande.
     *
     * <p>Son arche peut avoir disparu dans l'intervalle — deux minutes suffisent
     * à casser un bloc. Le refus est alors explicite plutôt que d'ouvrir un lien
     * sur une adresse vide.</p>
     */
    private static void openLink(MinecraftServer server, TardisStateManager manager, ServerPlayer player,
                                 @Nullable TardisData myDoor, @Nullable PassageData mine, UUID other,
                                 @Nullable BlockPos theirPos, String otherName) {
        UUID me = player.getUUID();
        TardisData theirDoor = manager.findByOwner(other);
        PassageData theirs = theirDoor == null || theirPos == null
                ? null : TardisStateManager.passageAt(theirDoor, theirPos);
        if (myDoor == null || mine == null || theirDoor == null || theirs == null) {
            say(server, me, ConsoleLog.BAD, "enderportals.message.connect_vanished", otherName);
            return;
        }
        // Lier peut avoir délogé un lien antérieur, d'un côté ou de l'autre :
        // on referme les arches ainsi laissées sans pair.
        for (UUID freed : manager.bind(myDoor, mine, theirDoor, theirs)) {
            AllyPassageHelper.reconcileAll(server, freed);
            say(server, freed, ConsoleLog.WARN, "enderportals.message.connect_displaced");
            refresh(server, freed);
        }
        AllyPassageHelper.openBoth(server, mine, theirs);
        ModAdvancements.award(player, ModAdvancements.ALLIES);
        ServerPlayer ally = server.getPlayerList().getPlayer(other);
        if (ally != null) {
            ModAdvancements.award(ally, ModAdvancements.ALLIES);
        }
        say(server, me, ConsoleLog.GOOD, "enderportals.message.connect_opened", otherName);
        say(server, other, ConsoleLog.GOOD, "enderportals.message.connect_opened",
                player.getGameProfile().getName());
    }

    /** Dénoue le lien avec cet allié, par quelque arche qu'il passe. */
    private static void closeLink(MinecraftServer server, TardisStateManager manager,
                                  @Nullable TardisData myDoor, UUID other) {
        PassageData mine = TardisStateManager.passageTo(myDoor, other);
        PassageData theirs = myDoor == null || myDoor.ownerUuid == null
                ? null : TardisStateManager.passageTo(manager.findByOwner(other), myDoor.ownerUuid);
        manager.unlink(myDoor, mine);
        AllyPassageHelper.closeBoth(server, mine, theirs);
    }

    private static void forget(ServerPlayer player, TardisStateManager manager,
                               ConsoleActionPayload payload) {
        UUID me = player.getUUID();
        UUID other = payload.target();
        MinecraftServer server = player.server;
        TardisData myDoor = manager.findByOwner(me);
        // Ne refermer que si un lien portait bien sur l'allié oublié.
        boolean wasLinked = manager.isLinked(me, other);
        String otherName = displayName(manager, other);
        manager.allies().forget(me, other);
        manager.setDirty();
        if (wasLinked) {
            closeLink(server, manager, myDoor, other);
            say(server, other, ConsoleLog.INFO, "enderportals.message.connect_closed",
                    player.getGameProfile().getName());
        }
        say(server, me, ConsoleLog.WARN, "enderportals.message.ally_forgotten", otherName);
        refresh(server, other);
    }

    // ------------------------------------------------------------------
    // Outils
    // ------------------------------------------------------------------

    /**
     * L'arche que ce panneau commande : celle de ses quatre voisines qui
     * appartient au joueur.
     *
     * <p>C'est la pièce maîtresse depuis que l'on peut en poser plusieurs. Un
     * panneau ne commande pas « le passage du joueur » — il n'y en a plus un —
     * mais celui qu'il touche, et c'est donc l'emplacement du panneau qui décide
     * quelle arche un clic va lier. Poser un Contrôle contre chaque arche est ce
     * qui rend plusieurs amis simultanés utilisables.</p>
     */
    /**
     * Pourquoi ce Contrôle ne s'ouvre pas. Trois refus, trois gestes différents :
     * l'écarter d'une arche, l'accoler à l'une des siennes, ou comprendre qu'il
     * est à quelqu'un d'autre. N'est appelé que lorsqu'il n'en commande pas
     * exactement une, ce qui rend les trois cas exhaustifs.
     */
    private static String refusalFor(ServerPlayer player, BlockPos console) {
        int touching = AllyPassageBlock.passagesTouching(player.level(), console, null).size();
        if (touching > 1) {
            return "enderportals.message.console_ambiguous";
        }
        return touching == 0
                ? "enderportals.message.console_unattached"
                : "enderportals.message.console_not_yours";
    }

    /**
     * L'arche que ce Contrôle commande, ou {@code null} s'il n'en commande pas
     * exactement une.
     *
     * <p>Zéro, et il n'est pas à ce joueur. Deux, et il faudrait en choisir une :
     * la pose l'interdit depuis la 0.19.2, mais une base bâtie avant elle peut
     * encore présenter le cas — on refuse alors plutôt que de trancher au
     * hasard, ce qui aurait rendu une arche sourde à son propre panneau.</p>
     */
    @Nullable
    private static PassageData adjacentPassage(ServerPlayer player, TardisStateManager manager,
                                               BlockPos console) {
        TardisData mine = manager.findByOwner(player.getUUID());
        if (mine == null) {
            return null;
        }
        Set<BlockPos> touching = AllyPassageBlock.passagesTouching(player.level(), console, null);
        if (touching.size() != 1) {
            return null;
        }
        return TardisStateManager.passageAt(mine, touching.iterator().next());
    }

    /**
     * Dit quelque chose à un joueur.
     *
     * <p>La ligne est d'abord écrite à son journal — c'est le terminal du
     * panneau qui la porte, et elle l'y attendra même s'il est hors ligne. Le
     * chat ne prend le relais que dans le seul cas où le terminal ne peut rien :
     * un joueur connecté qui n'a pas de panneau ouvert. Une demande de connexion
     * n'a que deux minutes de validité ; la laisser dormir dans un écran fermé
     * serait la perdre.</p>
     */
    private static void say(MinecraftServer server, UUID who, int tone, String key, Object... args) {
        TardisStateManager manager = TardisStateManager.get(server);
        manager.log().add(who, tone, key, args);
        manager.setDirty();
        ServerPlayer target = server.getPlayerList().getPlayer(who);
        if (target != null && manager.allies().viewedConsole(who) == null) {
            target.sendSystemMessage(Component.translatable(key, args).withStyle(chatColor(tone)));
        }
    }

    private static ChatFormatting chatColor(int tone) {
        return switch (tone) {
            case ConsoleLog.GOOD -> ChatFormatting.GREEN;
            case ConsoleLog.WARN -> ChatFormatting.GOLD;
            case ConsoleLog.BAD -> ChatFormatting.RED;
            default -> ChatFormatting.AQUA;
        };
    }

    /** Un code d'ami se dicte par groupes de quatre, comme il s'affiche. */
    private static String formatCode(int code) {
        String digits = Integer.toString(code);
        return digits.length() == 8 ? digits.substring(0, 4) + " " + digits.substring(4) : digits;
    }

    private ConsoleServerLogic() {
    }
}
