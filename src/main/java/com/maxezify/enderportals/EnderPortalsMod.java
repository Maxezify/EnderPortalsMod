package com.maxezify.enderportals;

import com.maxezify.enderportals.block.InactiveTardisDoorBlock;
import com.maxezify.enderportals.compat.ImmPtlCompat;
import com.maxezify.enderportals.network.ConsoleServerLogic;
import com.maxezify.enderportals.tardis.PlotGuard;
import com.maxezify.enderportals.tardis.TardisStateManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.minecraft.stats.Stats;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Point d'entrée NeoForge du mod World of Ender.
 */
@Mod(EnderPortalsMod.MODID)
public class EnderPortalsMod {

    public static final String MODID = "enderportals";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    /**
     * Distance de chute (en blocs) requise avant de frapper la porte inactive
     * à la Mace pour l'éveiller — la mécanique de l'attaque écrasante.
     */
    public static final float ACTIVATION_FALL_DISTANCE = 20.0f;

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public EnderPortalsMod(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModComponents.COMPONENTS.register(modBus);
        ModRegistries.CHUNK_GENERATORS.register(modBus);
        ModRegistries.CREATIVE_TABS.register(modBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modBus);

        NeoForge.EVENT_BUS.addListener(this::onLeftClickBlock);
        NeoForge.EVENT_BUS.addListener(this::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(this::onBlockBroken);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        NeoForge.EVENT_BUS.addListener(this::onPlotBreak);
        NeoForge.EVENT_BUS.addListener(this::onPlotPlace);
        NeoForge.EVENT_BUS.addListener(this::onPlotRightClick);

        LOGGER.info("World of Ender (NeoForge) initialisé — le vortex vous attend.");
        LOGGER.info("Immersive Portals détecté : {}", ImmPtlCompat.isLoaded());
    }

    /**
     * « Brisure d'Espace-Temps » : une pioche enchantée casse le Bloc de
     * l'Ender à une vitesse correcte (la récolte est autorisée par
     * {@link com.maxezify.enderportals.block.EnderBlock#canHarvestBlock}). La
     * Pioche de l'Ender, elle, reste bien plus rapide.
     */
    private void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (event.getState().is(ModBlocks.ENDER_BLOCK.get())
                && ModEnchantments.allowsEnderBlock(event.getEntity().level(), event.getEntity().getMainHandItem())) {
            event.setNewSpeed(Math.max(event.getNewSpeed(), 9.0f));
        }
    }

    /**
     * Un joueur déconnecté n'a plus de Contrôle de l'amitié à l'écran.
     *
     * <p>Le serveur retient quel panneau chaque joueur regarde, pour savoir où
     * porter ses messages : sur le terminal du panneau, ou dans le chat quand il
     * n'en a aucun d'ouvert. Cette note ne survit pas à une déconnexion, sans
     * quoi le joueur reviendrait en jeu réputé devant un panneau fermé — et ses
     * messages iraient dormir dans un terminal qu'il ne regarde pas.</p>
     */
    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ConsoleServerLogic.stopViewing(TardisStateManager.get(player.server), player.getUUID());
        }
    }

    /**
     * Tient à jour les Contrôles de l'amitié ouverts à l'écran.
     *
     * <p>Leur témoin de passage dépend de choses qu'aucun clic n'annonce :
     * l'arche s'ouvre trois secondes après la poignée de main, et un allié peut
     * casser la sienne à l'autre bout du monde. La cadence et l'économie de
     * paquets sont réglées dans {@link ConsoleServerLogic#tick} ; il n'y a ici
     * que le branchement.</p>
     */
    private void onServerTick(ServerTickEvent.Post event) {
        ConsoleServerLogic.tick(event.getServer());
    }

    /**
     * L'attaque écrasante de la Mace appliquée à la porte inactive : frappée
     * (clic gauche) en pleine chute, elle s'éveille.
     */
    private void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getLevel().getBlockState(event.getPos()).is(ModBlocks.INACTIVE_TARDIS_DOOR.get())
                && player.getMainHandItem().is(net.minecraft.world.item.Items.MACE)
                && InactiveTardisDoorBlock.tryActivate((ServerLevel) event.getLevel(), event.getPos(),
                        player, player.getMainHandItem())) {
            ModAdvancements.award(player, ModAdvancements.RITUAL);
        }
    }

    /**
     * Casser chez un autre : réservé à l'associé.
     *
     * <p>Cet écouteur-ci est distinct de {@link #onBlockBroken} bien qu'ils
     * guettent le même événement. Celui-là compte pour un progrès et ne refuse
     * jamais rien ; celui-ci décide. Les mêler aurait fait dépendre le progrès
     * d'un ordre d'exécution que rien ne garantit.</p>
     */
    private void onPlotBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player
                && !PlotGuard.mayBuild(player, event.getPos())) {
            event.setCanceled(true);
            refuse(player, "enderportals.message.plot_no_build");
        }
    }

    /** Poser chez un autre : réservé à l'associé, par le même chemin. */
    private void onPlotPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && !PlotGuard.mayBuild(player, event.getPos())) {
            event.setCanceled(true);
            refuse(player, "enderportals.message.plot_no_build");
        }
    }

    /**
     * Le clic droit chez un autre, découpé en deux : ce que l'objet en main
     * ferait, et ce que le bloc fait de lui-même.
     *
     * <p>C'est cette distinction qui rend le tri exact sans énumérer les objets.
     * Refuser l'<b>objet</b> ferme d'un coup la pose de blocs, le seau de lave,
     * le briquet et la houe, quels qu'ils soient et sans liste à tenir à jour.
     * Refuser en plus le <b>bloc</b>, pour un visiteur, ferme tout ce qui
     * s'actionne — coffres de mods compris, qu'aucun test de contenu n'aurait
     * su reconnaître à coup sûr.</p>
     *
     * <p>Annuler l'événement entier aurait fait les deux à la fois, et surtout
     * ouvert une faille chez l'invité : accroupi, vanilla saute l'usage du bloc
     * et passe la main à l'objet, si bien qu'un invité accroupi devant un coffre
     * aurait posé son bloc.</p>
     */
    private void onPlotRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        BlockPos pos = event.getPos();
        if (PlotGuard.mayBuild(player, pos)) {
            return;
        }
        event.setUseItem(TriState.FALSE);
        if (!PlotGuard.mayUse(player, pos)) {
            event.setUseBlock(TriState.FALSE);
            refuse(player, "enderportals.message.plot_no_use");
        }
    }

    /** Un refus se dit sur la barre d'action, sans encombrer le chat. */
    private static void refuse(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    /**
     * « Tailler sa galerie » : mille Blocs de l'Ender cassés. Le compte est
     * celui de la statistique vanilla, pas un compteur maison — il survit donc
     * aux redémarrages et reste juste si le progrès arrive après coup.
     *
     * <p>La statistique n'est incrémentée qu'après l'événement, d'où le
     * {@code + 1} : sans lui, le progrès tomberait au 1001ᵉ bloc.</p>
     */
    private void onBlockBroken(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)
                || !event.getState().is(ModBlocks.ENDER_BLOCK.get())) {
            return;
        }
        int mined = player.getStats().getValue(Stats.BLOCK_MINED.get(ModBlocks.ENDER_BLOCK.get()));
        if (mined + 1 >= ModAdvancements.GALLERY_TARGET) {
            ModAdvancements.award(player, ModAdvancements.GALLERY);
        }
    }
}
