package com.maxezify.enderportals.tardis;

import com.maxezify.enderportals.ModBlocks;
import net.minecraft.ChatFormatting;
import com.maxezify.enderportals.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.inventory.Slot;
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
import java.util.function.Consumer;

/**
 * La mécanique du Sac de l'Ender : retrouver le Transmetteur du joueur dans le
 * monde de l'Ender, parcourir le réseau de rangements qui lui est accolé, et y
 * ranger la pile prise au curseur contre un peu d'expérience.
 *
 * <p>Le réseau est collecté via la capability {@link IItemHandler} de NeoForge
 * (et non plus le seul coffre vanilla) : tout rangement qui l'expose est
 * reconnu — coffres/tonneaux vanilla, Sophisticated Storage/Backpacks, Tom's
 * Storage, drawers, etc. L'insertion passe par {@link ItemHandlerHelper}, donc
 * elle respecte les filtres et upgrades propres à chaque rangement.</p>
 */
public final class CentralizerLogic {

    /**
     * Coût en points d'expérience d'une pile expédiée.
     *
     * <p>Le prix est le même pour une pile de soixante-quatre blocs que pour un
     * objet seul : ce qu'on paie, c'est le voyage, pas le poids. C'était déjà le
     * cas quand le sac vidait une rangée entière — trois points par case, quel
     * que soit son contenu.</p>
     */
    public static final int XP_COST_PER_STACK = 3;
    /** Garde-fou sur la taille du réseau de rangements exploré. */
    private static final int MAX_STORAGES = 256;

    /**
     * Expédie la pile portée au curseur ; le reliquat y retourne.
     *
     * <p>Le geste : une pile prise au curseur, un clic droit sur le sac posé
     * dans une case.</p>
     */
    public static void sendCarried(ServerPlayer player, ItemStack carried, SlotAccess carriedAccess) {
        send(player, carried, carriedAccess::set);
    }

    /**
     * Expédie la pile qui dort dans une case ; le reliquat y reste.
     *
     * <p>Le geste inverse : le sac au curseur, un clic droit sur la pile. Les
     * deux sens font le même travail — d'où {@link #send}, qui les porte tous
     * les deux et ne diffère que par l'endroit où revient le reliquat.</p>
     */
    public static void sendSlot(ServerPlayer player, Slot slot) {
        send(player, slot.getItem(), slot::set);
    }

    /**
     * Le voyage, quel que soit le geste qui l'a demandé.
     *
     * <p>L'ordre des vérifications n'est pas indifférent : le réseau d'abord,
     * l'expérience ensuite, l'insertion en dernier. On ne prélève donc jamais
     * pour un voyage qui n'a pas eu lieu, et une pile qui ne rentre nulle part
     * revient intacte, sans avoir rien coûté.</p>
     *
     * <p>Rien ici n'appelle {@code broadcastChanges} : ce serait sans effet. Le
     * serveur suspend les mises à jour du menu pendant qu'il rejoue le clic, et
     * les reprend juste après pour envoyer d'un coup ce qui a changé — dont
     * cette case, ou ce curseur, que le client croyait encore pleins.</p>
     *
     * @param writeBack où déposer ce qui n'est pas parti : le curseur pour un
     *                  geste, la case cliquée pour l'autre
     */
    private static void send(ServerPlayer player, ItemStack stack, Consumer<ItemStack> writeBack) {
        if (stack.isEmpty()) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        TardisStateManager manager = TardisStateManager.get(server);
        TardisData data = manager.findByOwner(player.getUUID());
        if (data == null || data.centralizerPos == null) {
            refuse(player, "enderportals.message.no_centralizer");
            return;
        }
        ServerLevel enderWorld = server.getLevel(ModDimensions.ENDER_WORLD);
        BlockPos centralizer = data.centralizerPos;
        if (enderWorld == null || !enderWorld.getBlockState(centralizer).is(ModBlocks.CENTRALIZER.get())) {
            manager.clearCentralizer(centralizer);
            refuse(player, "enderportals.message.no_centralizer");
            return;
        }

        List<IItemHandler> storages = collectHandlers(enderWorld, centralizer);
        if (storages.isEmpty()) {
            refuse(player, "enderportals.message.no_chest");
            return;
        }
        if (!EnderXp.has(player, XP_COST_PER_STACK)) {
            refuse(player, "enderportals.message.not_enough_xp", XP_COST_PER_STACK);
            return;
        }

        ItemStack remainder = insert(storages, stack, false);
        if (remainder.getCount() == stack.getCount()) {
            // Pas une seule unité n'est passée : le réseau est plein pour cet
            // objet-là. Rien n'est prélevé.
            refuse(player, "enderportals.message.chests_full");
            return;
        }
        writeBack.accept(remainder);
        EnderXp.charge(player, XP_COST_PER_STACK);
        success(player);
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
        // Mystique, et pour ce joueur seulement : le geste se fait dans un
        // inventaire, pas dans le monde. playNotifySound n'envoie le son qu'à
        // lui, là où playSound l'aurait fait entendre à tout le voisinage —
        // insupportable quand on range pile après pile.
        player.playNotifySound(SoundEvents.ENDER_CHEST_CLOSE, SoundSource.PLAYERS, 0.6f, 1.1f);
        player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5f, 1.4f);
    }

    /**
     * Le refus : un son sec, et la raison au-dessus de la barre d'action.
     *
     * <p>Un seul son pour tous les refus. Trois timbres pour trois causes
     * obligeraient le joueur à les apprendre, alors que la phrase les nomme
     * déjà.</p>
     */
    private static void refuse(ServerPlayer player, String messageKey, Object... args) {
        player.playNotifySound(SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8f, 0.8f);
        player.displayClientMessage(
                Component.translatable(messageKey, args).withStyle(ChatFormatting.RED), true);
    }

    private CentralizerLogic() {
    }
}
