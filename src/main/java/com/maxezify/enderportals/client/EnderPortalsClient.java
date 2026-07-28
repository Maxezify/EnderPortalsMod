package com.maxezify.enderportals.client;

import com.maxezify.enderportals.EnderPortalsMod;
import com.maxezify.enderportals.ModBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;

/**
 * Enregistrements côté client. Les couches de rendu (translucide / cutout) des
 * blocs sont déclarées via {@code "render_type"} dans leurs modèles JSON ; il
 * reste ici le renderer du block entity de la porte et les effets visuels du
 * monde de l'Ender.
 */
@EventBusSubscriber(modid = EnderPortalsMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class EnderPortalsClient {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.TARDIS_DOOR.get(), TardisDoorRenderer::new);
    }

    /**
     * L'identifiant doit correspondre au champ {@code effects} du
     * {@code dimension_type} : c'est par lui que le client relie la dimension
     * à ses effets visuels.
     */
    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(EnderPortalsMod.id("ender_world"), new EnderWorldEffects());
    }

    private EnderPortalsClient() {
    }
}
