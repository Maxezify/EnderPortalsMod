package com.maxezify.enderportals.network;

import com.maxezify.enderportals.ModAdvancements;
import com.maxezify.enderportals.ModBlocks;
import com.maxezify.enderportals.block.AllyPassageBlock;
import com.maxezify.enderportals.tardis.AllyLinks;
import com.maxezify.enderportals.tardis.AllyPassageHelper;
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
import java.util.List;
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
 */
public final class ConsoleServerLogic {

    /** Portée d'interaction admise, au carré. Huit blocs : large, mais borné. */
    private static final double REACH_SQR = 64.0;

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
            player.displayClientMessage(
                    Component.translatable("enderportals.message.console_no_door"), true);
            return;
        }
        manager.allies().setViewing(player.getUUID(), console);
        send(player, manager, console);
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
        ModNetwork.toPlayer(player, buildState(player, manager, console));
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
                hasOwnPassageNextTo(player, manager, console), allies);
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
            manager.allies().setViewing(player.getUUID(), null);
            return;
        }
        if (!canReach(player, payload.console())) {
            manager.allies().setViewing(player.getUUID(), null);
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
            }
        }
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
        TardisData target = manager.findByCode(payload.code());
        if (target == null || target.ownerUuid == null) {
            player.displayClientMessage(
                    Component.translatable("enderportals.message.ally_unknown").withStyle(ChatFormatting.RED),
                    true);
            return;
        }
        UUID me = player.getUUID();
        AllyLinks.DeclareResult result = manager.allies().declare(me, target.ownerUuid);
        manager.setDirty();
        MinecraftServer server = player.server;
        switch (result) {
            case SELF -> player.displayClientMessage(
                    Component.translatable("enderportals.message.ally_self"), true);
            case ALREADY -> player.displayClientMessage(
                    Component.translatable("enderportals.message.ally_already", target.ownerName), true);
            case PENDING -> {
                player.displayClientMessage(Component.translatable(
                        "enderportals.message.ally_pending", target.ownerName), true);
                notify(server, target.ownerUuid, "enderportals.message.ally_wants_you",
                        player.getGameProfile().getName(), manager.codeOf(me));
            }
            case CONFIRMED -> {
                player.displayClientMessage(Component.translatable(
                        "enderportals.message.ally_confirmed", target.ownerName)
                        .withStyle(ChatFormatting.GREEN), false);
                notify(server, target.ownerUuid, "enderportals.message.ally_confirmed",
                        player.getGameProfile().getName());
            }
            default -> {
            }
        }
        refresh(server, me);
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
            player.displayClientMessage(
                    Component.translatable("enderportals.message.passage_missing")
                            .withStyle(ChatFormatting.RED), true);
            return;
        }

        AllyLinks.ConnectResult result = links.toggleConnect(me, other, player.serverLevel().getGameTime());
        manager.setDirty();
        String otherName = displayName(manager, other);
        switch (result) {
            case NOT_FRIENDS -> player.displayClientMessage(
                    Component.translatable("enderportals.message.connect_not_friends", otherName), true);
            case REQUESTED -> {
                player.displayClientMessage(Component.translatable(
                        "enderportals.message.connect_requested", otherName), true);
                notify(server, other, "enderportals.message.connect_asked",
                        player.getGameProfile().getName());
            }
            case OPENED -> {
                // Ouvrir peut avoir délogé un lien antérieur, d'un côté ou de
                // l'autre : on referme les arches ainsi laissées sans pair.
                for (UUID displaced : links.takeDisplaced()) {
                    AllyPassageHelper.closeOne(server, displaced);
                    refresh(server, displaced);
                }
                AllyPassageHelper.openBoth(server, me, other);
                ModAdvancements.award(player, ModAdvancements.ALLIES);
                ServerPlayer ally = server.getPlayerList().getPlayer(other);
                if (ally != null) {
                    ModAdvancements.award(ally, ModAdvancements.ALLIES);
                }
                player.displayClientMessage(Component.translatable(
                        "enderportals.message.connect_opened", otherName)
                        .withStyle(ChatFormatting.GREEN), false);
                notify(server, other, "enderportals.message.connect_opened",
                        player.getGameProfile().getName());
            }
            case CLOSED -> {
                AllyPassageHelper.closeBoth(server, me, other);
                player.displayClientMessage(Component.translatable(
                        "enderportals.message.connect_closed", otherName), false);
                notify(server, other, "enderportals.message.connect_closed",
                        player.getGameProfile().getName());
            }
            default -> {
            }
        }
        refresh(server, me);
        refresh(server, other);
    }

    private static void forget(ServerPlayer player, TardisStateManager manager,
                               ConsoleActionPayload payload) {
        UUID me = player.getUUID();
        UUID other = payload.target();
        MinecraftServer server = player.server;
        // Ne refermer que si le lien portait bien sur l'allié oublié.
        boolean wasLinked = other.equals(manager.allies().linkOf(me));
        manager.allies().forget(me, other);
        manager.setDirty();
        if (wasLinked) {
            AllyPassageHelper.closeBoth(server, me, other);
        }
        player.displayClientMessage(Component.translatable(
                "enderportals.message.ally_forgotten", displayName(manager, other)), true);
        refresh(server, me);
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

    private static void notify(MinecraftServer server, UUID who, String key, Object... args) {
        ServerPlayer target = server.getPlayerList().getPlayer(who);
        if (target != null) {
            target.sendSystemMessage(Component.translatable(key, args).withStyle(ChatFormatting.AQUA));
        }
    }

    private ConsoleServerLogic() {
    }
}
