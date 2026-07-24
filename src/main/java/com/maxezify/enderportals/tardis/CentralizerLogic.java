package com.maxezify.enderportals.tardis;

import com.maxezify.enderportals.ModBlocks;
import com.maxezify.enderportals.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * La mécanique du Sac de l'Ender : retrouver le Centraliseur du joueur dans le
 * monde de l'Ender, parcourir le réseau de coffres qui lui est accolé, et y
 * ranger la ligne du haut de l'inventaire contre un peu d'expérience.
 */
public final class CentralizerLogic {

    /** Coût en points d'expérience par case rangée. */
    private static final int XP_COST_PER_SLOT = 3;
    /** Garde-fou sur la taille du réseau de coffres exploré. */
    private static final int MAX_CHESTS = 256;
    /** Cases 9 à 17 : la rangée du haut du rangement principal. */
    private static final int ROW_START = 9;
    private static final int ROW_END = 17;

    public static void deposit(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        TardisStateManager manager = TardisStateManager.get(server);
        TardisData data = manager.findByOwner(player.getUUID());
        if (data == null || data.centralizerPos == null) {
            fail(player, "enderportals.message.no_centralizer");
            return;
        }
        ServerLevel enderWorld = server.getLevel(ModDimensions.ENDER_WORLD);
        BlockPos centralizer = data.centralizerPos;
        if (enderWorld == null || !enderWorld.getBlockState(centralizer).is(ModBlocks.CENTRALIZER.get())) {
            manager.clearCentralizer(centralizer);
            fail(player, "enderportals.message.no_centralizer");
            return;
        }

        List<Container> chests = collectChests(enderWorld, centralizer);
        if (chests.isEmpty()) {
            fail(player, "enderportals.message.no_chest");
            return;
        }

        Inventory inv = player.getInventory();
        int candidates = 0;
        for (int slot = ROW_START; slot <= ROW_END; slot++) {
            if (!inv.getItem(slot).isEmpty()) {
                candidates++;
            }
        }
        if (candidates == 0) {
            neutral(player);
            return;
        }
        // Coffres pleins (aucune place pour aucun objet candidat) : message
        // dédié, vérifié avant tout prélèvement d'XP.
        if (!hasAnyRoom(chests, inv)) {
            full(player);
            return;
        }
        if (getXpPoints(player) < XP_COST_PER_SLOT * candidates) {
            fail(player, "enderportals.message.not_enough_xp");
            return;
        }

        int moved = 0;
        for (int slot = ROW_START; slot <= ROW_END; slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int before = stack.getCount();
            insert(chests, stack);
            if (stack.getCount() != before) {
                moved++;
                if (stack.isEmpty()) {
                    inv.setItem(slot, ItemStack.EMPTY);
                }
            }
        }
        inv.setChanged();

        if (moved == 0) {
            // Filet de sécurité : hasAnyRoom garantit normalement moved >= 1.
            full(player);
            return;
        }
        // Force la synchronisation de l'inventaire modifié vers le client.
        player.containerMenu.broadcastChanges();
        player.giveExperiencePoints(-XP_COST_PER_SLOT * moved);
        success(player);
    }

    /**
     * Le réseau a-t-il de la place pour au moins un objet de la ligne du haut ?
     * Lecture seule, court-circuit dès la première place trouvée.
     */
    private static boolean hasAnyRoom(List<Container> chests, Inventory inv) {
        for (int slot = ROW_START; slot <= ROW_END; slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            for (Container chest : chests) {
                int size = chest.getContainerSize();
                for (int i = 0; i < size; i++) {
                    if (!chest.canPlaceItem(i, stack)) {
                        continue;
                    }
                    ItemStack slotStack = chest.getItem(i);
                    if (slotStack.isEmpty()) {
                        return true;
                    }
                    if (ItemStack.isSameItemSameComponents(slotStack, stack)
                            && slotStack.getCount() < Math.min(chest.getMaxStackSize(), slotStack.getMaxStackSize())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Réseau de coffres
    // ------------------------------------------------------------------

    private static List<Container> collectChests(ServerLevel level, BlockPos centralizer) {
        List<Container> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();

        // Amorce : les coffres directement collés à une face du centraliseur.
        for (Direction dir : Direction.values()) {
            BlockPos p = centralizer.relative(dir);
            if (level.getBlockEntity(p) instanceof ChestBlockEntity) {
                queue.add(p);
            }
        }
        while (!queue.isEmpty() && result.size() < MAX_CHESTS) {
            BlockPos p = queue.poll();
            if (!visited.add(p)) {
                continue;
            }
            if (level.getBlockEntity(p) instanceof ChestBlockEntity chest) {
                result.add(chest);
                for (Direction dir : Direction.values()) {
                    BlockPos n = p.relative(dir);
                    if (!visited.contains(n) && level.getBlockEntity(n) instanceof ChestBlockEntity) {
                        queue.add(n);
                    }
                }
            }
        }
        return result;
    }

    // ------------------------------------------------------------------
    // Insertion (fusion sur piles existantes, puis cases vides)
    // ------------------------------------------------------------------

    private static void insert(List<Container> targets, ItemStack stack) {
        for (Container inv : targets) {
            if (stack.isEmpty()) {
                return;
            }
            mergeInto(inv, stack);
        }
    }

    private static void mergeInto(Container inv, ItemStack stack) {
        int size = inv.getContainerSize();
        boolean changed = false;

        for (int i = 0; i < size && !stack.isEmpty(); i++) {
            if (!inv.canPlaceItem(i, stack)) {
                continue;
            }
            ItemStack slotStack = inv.getItem(i);
            if (slotStack.isEmpty() || !ItemStack.isSameItemSameComponents(slotStack, stack)) {
                continue;
            }
            int max = Math.min(inv.getMaxStackSize(), slotStack.getMaxStackSize());
            int space = max - slotStack.getCount();
            if (space > 0) {
                int add = Math.min(space, stack.getCount());
                slotStack.grow(add);
                stack.shrink(add);
                changed = true;
            }
        }
        for (int i = 0; i < size && !stack.isEmpty(); i++) {
            if (!inv.canPlaceItem(i, stack) || !inv.getItem(i).isEmpty()) {
                continue;
            }
            int max = Math.min(inv.getMaxStackSize(), stack.getMaxStackSize());
            int add = Math.min(max, stack.getCount());
            ItemStack placed = stack.copy();
            placed.setCount(add);
            inv.setItem(i, placed);
            stack.shrink(add);
            changed = true;
        }
        if (changed) {
            inv.setChanged();
        }
    }

    // ------------------------------------------------------------------
    // Expérience
    // ------------------------------------------------------------------

    /** Total des points d'expérience actuellement détenus par le joueur. */
    private static int getXpPoints(Player player) {
        return xpForLevel(player.experienceLevel)
                + Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
    }

    /** Points cumulés nécessaires pour atteindre un niveau (formule vanilla). */
    private static int xpForLevel(int level) {
        if (level <= 0) {
            return 0;
        }
        if (level <= 16) {
            return level * level + 6 * level;
        }
        if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360.0);
        }
        return (int) (4.5 * level * level - 162.5 * level + 2220.0);
    }

    // ------------------------------------------------------------------
    // Retours sonores
    // ------------------------------------------------------------------

    private static void success(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        // Mystique (chime d'améthyste + rangement du coffre de l'Ender).
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_CHEST_CLOSE, SoundSource.PLAYERS, 0.6f, 1.1f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5f, 1.4f);
    }

    private static void fail(ServerPlayer player, String messageKey) {
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5f, 0.5f);
        player.displayClientMessage(Component.translatable(messageKey), true);
    }

    /** Coffres pleins : son grave dédié + message. */
    private static void full(ServerPlayer player) {
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6f, 0.6f);
        player.displayClientMessage(Component.translatable("enderportals.message.chests_full"), true);
    }

    private static void neutral(ServerPlayer player) {
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 1.0f);
    }

    private CentralizerLogic() {
    }
}
