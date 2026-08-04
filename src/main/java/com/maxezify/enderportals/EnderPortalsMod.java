package com.maxezify.enderportals;

import com.maxezify.enderportals.block.InactiveTardisDoorBlock;
import com.maxezify.enderportals.compat.ImmPtlCompat;
import com.maxezify.enderportals.tardis.TardisStateManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.minecraft.stats.Stats;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
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
        ModComponents.COMPONENTS.register(modBus);
        ModRegistries.CHUNK_GENERATORS.register(modBus);
        ModRegistries.CREATIVE_TABS.register(modBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modBus);

        NeoForge.EVENT_BUS.addListener(this::onLeftClickBlock);
        NeoForge.EVENT_BUS.addListener(this::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(this::onBlockBroken);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);

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
            TardisStateManager.get(player.server).allies().setViewing(player.getUUID(), null);
        }
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
