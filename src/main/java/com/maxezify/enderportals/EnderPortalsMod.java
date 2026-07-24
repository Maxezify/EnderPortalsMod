package com.maxezify.enderportals;

import com.maxezify.enderportals.block.InactiveTardisDoorBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Point d'entrée NeoForge du mod Ender Portals.
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
        ModRecipes.RECIPE_SERIALIZERS.register(modBus);
        ModRegistries.CHUNK_GENERATORS.register(modBus);
        ModRegistries.CREATIVE_TABS.register(modBus);

        NeoForge.EVENT_BUS.addListener(this::onLeftClickBlock);

        LOGGER.info("Ender Portals (NeoForge) initialisé — le vortex vous attend.");
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
                && player.getMainHandItem().is(net.minecraft.world.item.Items.MACE)) {
            InactiveTardisDoorBlock.tryActivate((ServerLevel) event.getLevel(), event.getPos(),
                    player, player.getMainHandItem());
        }
    }
}
