package com.maxezify.enderportals.network;

import com.maxezify.enderportals.ModAdvancements;
import com.maxezify.enderportals.ModBlocks;
import com.maxezify.enderportals.block.AllyPassageBlock;
import com.maxezify.enderportals.tardis.AllyLinks;
import com.maxezify.enderportals.tardis.AllyPassageHelper;
import com.maxezify.enderportals.tardis.ConsoleLog;
import com.maxezify.enderportals.tardis.TardisData;
import com.maxezify.enderportals.tardis.TardisStateManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /** Ouvre le panneau chez ce joueur, s'il a bien une porte éveillée. */
    public static void open(ServerPlayer player, BlockPos console) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        TardisStateManager manager = TardisStateManager.get(server);
        if (manager.findByOwner(player.getUUID()) == null) {
            // Le seul message qui reste hors du terminal : il explique
            // justement pourquoi le panneau ne s'ouvre pas.
            player.displayClientMessage(
                    Component.translatable("enderportals.message.console_no_door"), true);
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
        UUID linked = links.linkOf(me);
        UUID myRequest = links.pendingRequestTarget(me, gameTime);

        List<ConsoleStatePayload.Ally> allies = new ArrayList<>();
        for (UUID other : links.declaredBy(me)) {
            int state;
            if (other.equals(linked)) {
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

        return new ConsoleStatePayload(console, manager.codeOf(me),
                passageState(player, manager, console), allies, manager.log().of(me));
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
    private static int passageState(ServerPlayer player, TardisStateManager manager,
                                    BlockPos console) {
        if (!hasOwnPassageNextTo(player, manager, console)) {
            return ConsoleStatePayload.PASSAGE_NO_PANEL;
        }
        UUID allyId = manager.allies().linkOf(player.getUUID());
        if (allyId == null) {
            return ConsoleStatePayload.PASSAGE_CLOSED;
        }
        TardisData ally = manager.findByOwner(allyId);
        return ally != null && ally.passagePos != null
                ? ConsoleStatePayload.PASSAGE_OPEN
                : ConsoleStatePayload.PASSAGE_ONE_SIDED;
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

    private static void toggle(ServerPlayer player, TardisStateManager manager,
                               ConsoleActionPayload payload) {
        UUID me = player.getUUID();
        UUID other = payload.target();
        MinecraftServer server = player.server;
        AllyLinks links = manager.allies();
        boolean wasLinked = other.equals(links.linkOf(me));

        // Ouvrir exige le panneau accolé au passage ; refermer, non — on doit
        // toujours pouvoir couper, même si le passage a été démonté depuis.
        if (!wasLinked && !hasOwnPassageNextTo(player, manager, payload.console())) {
            say(server, me, ConsoleLog.BAD, "enderportals.message.passage_missing");
            return;
        }

        AllyLinks.ConnectResult result = links.toggleConnect(me, other, player.serverLevel().getGameTime());
        manager.setDirty();
        String otherName = displayName(manager, other);
        switch (result) {
            case NOT_FRIENDS -> say(server, me, ConsoleLog.WARN,
                    "enderportals.message.connect_not_friends", otherName);
            case REQUESTED -> {
                say(server, me, ConsoleLog.INFO,
                        "enderportals.message.connect_requested", otherName);
                say(server, other, ConsoleLog.WARN, "enderportals.message.connect_asked",
                        player.getGameProfile().getName());
            }
            case OPENED -> {
                // Ouvrir peut avoir délogé un lien antérieur, d'un côté ou de
                // l'autre : on referme les arches ainsi laissées sans pair.
                for (UUID displaced : links.takeDisplaced()) {
                    AllyPassageHelper.closeOne(server, displaced);
                    say(server, displaced, ConsoleLog.WARN, "enderportals.message.connect_displaced");
                    refresh(server, displaced);
                }
                AllyPassageHelper.openBoth(server, me, other);
                ModAdvancements.award(player, ModAdvancements.ALLIES);
                ServerPlayer ally = server.getPlayerList().getPlayer(other);
                if (ally != null) {
                    ModAdvancements.award(ally, ModAdvancements.ALLIES);
                }
                say(server, me, ConsoleLog.GOOD, "enderportals.message.connect_opened", otherName);
                say(server, other, ConsoleLog.GOOD, "enderportals.message.connect_opened",
                        player.getGameProfile().getName());
            }
            case CLOSED -> {
                AllyPassageHelper.closeBoth(server, me, other);
                say(server, me, ConsoleLog.INFO, "enderportals.message.connect_closed", otherName);
                say(server, other, ConsoleLog.INFO, "enderportals.message.connect_closed",
                        player.getGameProfile().getName());
            }
            default -> {
            }
        }
        refresh(server, other);
    }

    private static void forget(ServerPlayer player, TardisStateManager manager,
                               ConsoleActionPayload payload) {
        UUID me = player.getUUID();
        UUID other = payload.target();
        MinecraftServer server = player.server;
        // Ne refermer que si le lien portait bien sur l'allié oublié.
        boolean wasLinked = other.equals(manager.allies().linkOf(me));
        String otherName = displayName(manager, other);
        manager.allies().forget(me, other);
        manager.setDirty();
        if (wasLinked) {
            AllyPassageHelper.closeBoth(server, me, other);
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
     * Un Passage des Alliés appartenant à ce joueur est-il accolé au panneau ?
     * On regarde les quatre côtés, à la hauteur du panneau : « collé à elle »
     * au sens propre.
     */
    private static boolean hasOwnPassageNextTo(ServerPlayer player, TardisStateManager manager,
                                               BlockPos console) {
        TardisData mine = manager.findByOwner(player.getUUID());
        if (mine == null || mine.passagePos == null) {
            return false;
        }
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos neighbour = console.relative(side);
            BlockState state = player.level().getBlockState(neighbour);
            if (AllyPassageBlock.isPassage(state)
                    && AllyPassageBlock.baseOf(state, neighbour).equals(mine.passagePos)) {
                return true;
            }
        }
        return false;
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
