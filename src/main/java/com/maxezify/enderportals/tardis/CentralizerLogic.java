package com.maxezify.enderportals.tardis;

import com.maxezify.enderportals.ModBlocks;
import com.maxezify.enderportals.ModDimensions;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

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

    public static void deposit(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        TardisStateManager manager = TardisStateManager.get(server);
        TardisData data = manager.findByOwner(player.getUuid());
        if (data == null || data.centralizerPos == null) {
            fail(player, "enderportals.message.no_centralizer");
            return;
        }
        ServerWorld enderWorld = server.getWorld(ModDimensions.ENDER_WORLD);
        BlockPos centralizer = data.centralizerPos;
        if (enderWorld == null || !enderWorld.getBlockState(centralizer).isOf(ModBlocks.CENTRALIZER)) {
            manager.clearCentralizer(centralizer);
            fail(player, "enderportals.message.no_centralizer");
            return;
        }

        List<Inventory> chests = collectChests(enderWorld, centralizer);
        if (chests.isEmpty()) {
            fail(player, "enderportals.message.no_chest");
            return;
        }

        PlayerInventory inv = player.getInventory();
        int candidates = 0;
        for (int slot = ROW_START; slot <= ROW_END; slot++) {
            if (!inv.getStack(slot).isEmpty()) {
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
            ItemStack stack = inv.getStack(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int before = stack.getCount();
            insert(chests, stack);
            if (stack.getCount() != before) {
                moved++;
                if (stack.isEmpty()) {
                    inv.setStack(slot, ItemStack.EMPTY);
                }
            }
        }
        inv.markDirty();

        if (moved == 0) {
            // Filet de sécurité : hasAnyRoom garantit normalement moved >= 1.
            full(player);
            return;
        }
        // Force la synchronisation de l'inventaire modifié vers le client.
        player.currentScreenHandler.sendContentUpdates();
        player.addExperience(-XP_COST_PER_SLOT * moved);
        success(player);
    }

    /**
     * Le réseau a-t-il de la place pour au moins un objet de la ligne du haut ?
     * Lecture seule, court-circuit dès la première place trouvée.
     */
    private static boolean hasAnyRoom(List<Inventory> chests, PlayerInventory inv) {
        for (int slot = ROW_START; slot <= ROW_END; slot++) {
            ItemStack stack = inv.getStack(slot);
            if (stack.isEmpty()) {
                continue;
            }
            for (Inventory chest : chests) {
                int size = chest.size();
                for (int i = 0; i < size; i++) {
                    if (!chest.isValid(i, stack)) {
                        continue;
                    }
                    ItemStack slotStack = chest.getStack(i);
                    if (slotStack.isEmpty()) {
                        return true;
                    }
                    if (ItemStack.areItemsAndComponentsEqual(slotStack, stack)
                            && slotStack.getCount() < Math.min(chest.getMaxCountPerStack(), slotStack.getMaxCount())) {
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

    private static List<Inventory> collectChests(ServerWorld world, BlockPos centralizer) {
        List<Inventory> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();

        // Amorce : les coffres directement collés à une face du centraliseur.
        for (Direction dir : Direction.values()) {
            BlockPos p = centralizer.offset(dir);
            if (world.getBlockEntity(p) instanceof ChestBlockEntity) {
                queue.add(p);
            }
        }
        while (!queue.isEmpty() && result.size() < MAX_CHESTS) {
            BlockPos p = queue.poll();
            if (!visited.add(p)) {
                continue;
            }
            if (world.getBlockEntity(p) instanceof ChestBlockEntity chest) {
                result.add(chest);
                for (Direction dir : Direction.values()) {
                    BlockPos n = p.offset(dir);
                    if (!visited.contains(n) && world.getBlockEntity(n) instanceof ChestBlockEntity) {
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

    private static void insert(List<Inventory> targets, ItemStack stack) {
        for (Inventory inv : targets) {
            if (stack.isEmpty()) {
                return;
            }
            mergeInto(inv, stack);
        }
    }

    private static void mergeInto(Inventory inv, ItemStack stack) {
        int size = inv.size();
        boolean changed = false;

        for (int i = 0; i < size && !stack.isEmpty(); i++) {
            if (!inv.isValid(i, stack)) {
                continue;
            }
            ItemStack slotStack = inv.getStack(i);
            if (slotStack.isEmpty() || !ItemStack.areItemsAndComponentsEqual(slotStack, stack)) {
                continue;
            }
            int max = Math.min(inv.getMaxCountPerStack(), slotStack.getMaxCount());
            int space = max - slotStack.getCount();
            if (space > 0) {
                int add = Math.min(space, stack.getCount());
                slotStack.increment(add);
                stack.decrement(add);
                changed = true;
            }
        }
        for (int i = 0; i < size && !stack.isEmpty(); i++) {
            if (!inv.isValid(i, stack) || !inv.getStack(i).isEmpty()) {
                continue;
            }
            int max = Math.min(inv.getMaxCountPerStack(), stack.getMaxCount());
            int add = Math.min(max, stack.getCount());
            ItemStack placed = stack.copy();
            placed.setCount(add);
            inv.setStack(i, placed);
            stack.decrement(add);
            changed = true;
        }
        if (changed) {
            inv.markDirty();
        }
    }

    // ------------------------------------------------------------------
    // Expérience
    // ------------------------------------------------------------------

    /** Total des points d'expérience actuellement détenus par le joueur. */
    private static int getXpPoints(PlayerEntity player) {
        return xpForLevel(player.experienceLevel)
                + Math.round(player.experienceProgress * player.getNextLevelExperience());
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

    private static void success(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        // Mystique (chime d'améthyste + rangement du coffre de l'Ender).
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_ENDER_CHEST_CLOSE, SoundCategory.PLAYERS, 0.6f, 1.1f);
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.5f, 1.4f);
    }

    private static void fail(ServerPlayerEntity player, String messageKey) {
        player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.5f, 0.5f);
        player.sendMessage(Text.translatable(messageKey), true);
    }

    /** Coffres pleins : son grave dédié + message. */
    private static void full(ServerPlayerEntity player) {
        player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.6f, 0.6f);
        player.sendMessage(Text.translatable("enderportals.message.chests_full"), true);
    }

    private static void neutral(ServerPlayerEntity player) {
        player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.PLAYERS, 0.6f, 1.0f);
    }

    private CentralizerLogic() {
    }
}
