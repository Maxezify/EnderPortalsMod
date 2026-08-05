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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * La mécanique du Sac de l'Ender : retrouver le Centraliseur du joueur dans le
 * monde de l'Ender, parcourir le réseau de rangements qui lui est accolé, et y
 * ranger la ligne du haut de l'inventaire contre un peu d'expérience.
 *
 * <p>Le réseau est collecté via la capability {@link IItemHandler} de NeoForge
 * (et non plus le seul coffre vanilla) : tout rangement qui l'expose est
 * reconnu — coffres/tonneaux vanilla, Sophisticated Storage/Backpacks, Tom's
 * Storage, drawers, etc. L'insertion passe par {@link ItemHandlerHelper}, donc
 * elle respecte les filtres et upgrades propres à chaque rangement.</p>
 */
public final class CentralizerLogic {

    /** Coût en points d'expérience par case rangée. */
    private static final int XP_COST_PER_SLOT = 3;
    /** Garde-fou sur la taille du réseau de rangements exploré. */
    private static final int MAX_STORAGES = 256;
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

        List<IItemHandler> storages = collectHandlers(enderWorld, centralizer);
        if (storages.isEmpty()) {
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
        // Rangements pleins (aucune place pour aucun objet candidat) : message
        // dédié, vérifié avant tout prélèvement d'XP.
        if (!hasAnyRoom(storages, inv)) {
            full(player);
            return;
        }
        if (!EnderXp.has(player, XP_COST_PER_SLOT * candidates)) {
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
            ItemStack remainder = insert(storages, stack, false);
            if (remainder.getCount() != before) {
                moved++;
                inv.setItem(slot, remainder);
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
        EnderXp.charge(player, XP_COST_PER_SLOT * moved);
        success(player);
    }

    /**
     * Le réseau a-t-il de la place pour au moins un objet de la ligne du haut ?
     * Insertion simulée, court-circuit dès la première place trouvée.
     */
    private static boolean hasAnyRoom(List<IItemHandler> storages, Inventory inv) {
        for (int slot = ROW_START; slot <= ROW_END; slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (insert(storages, stack, true).getCount() < stack.getCount()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Insère {@code stack} à travers les rangements du réseau et retourne le
     * reliquat. {@code stack} n'est pas modifié ({@link ItemHandlerHelper}
     * renvoie une copie). En mode réel ({@code simulate=false}), l'appelant
     * remplace la pile d'origine par le reliquat.
     */
    private static ItemStack insert(List<IItemHandler> storages, ItemStack stack, boolean simulate) {
        ItemStack remaining = stack;
        for (IItemHandler handler : storages) {
            if (remaining.isEmpty()) {
                break;
            }
            remaining = ItemHandlerHelper.insertItem(handler, remaining, simulate);
        }
        return remaining;
    }

    // ------------------------------------------------------------------
    // Réseau de rangements (capability IItemHandler)
    // ------------------------------------------------------------------

    /**
     * Parcourt le réseau de rangements accolé au Transmetteur : BFS sur les
     * blocs voisins, en récoltant tout {@link IItemHandler} exposé (côté
     * {@code null}) — y compris le handler d'un Connecteur d'inventaire (Tom's)
     * ou d'un contrôleur (Sophisticated), dans lequel le dépôt est alors
     * relayé. Le flood ne se propage qu'à travers les blocs qui exposent un
     * handler ; un autre Transmetteur n'est jamais traversé.
     */
    private static List<IItemHandler> collectHandlers(Level level, BlockPos centralizer) {
        List<IItemHandler> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();

        // Le Transmetteur ne se collecte jamais lui-même.
        visited.add(centralizer);
        for (Direction dir : Direction.values()) {
            queue.add(centralizer.relative(dir));
        }
        while (!queue.isEmpty() && result.size() < MAX_STORAGES) {
            BlockPos p = queue.poll();
            if (!visited.add(p)) {
                continue;
            }
            if (level.getBlockState(p).is(ModBlocks.CENTRALIZER.get())) {
                continue;
            }
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, p, null);
            if (handler == null) {
                continue;
            }
            // Déduplication par référence : un double coffre expose le même
            // handler des deux moitiés — on ne le compte qu'une fois.
            if (!containsSame(result, handler)) {
                result.add(handler);
            }
            for (Direction dir : Direction.values()) {
                BlockPos n = p.relative(dir);
                if (!visited.contains(n)) {
                    queue.add(n);
                }
            }
        }
        return result;
    }

    private static boolean containsSame(List<IItemHandler> handlers, IItemHandler handler) {
        for (IItemHandler h : handlers) {
            if (h == handler) {
                return true;
            }
        }
        return false;
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

    /** Rangements pleins : son grave dédié + message. */
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
