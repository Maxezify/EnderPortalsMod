package com.maxezify.enderportals.client;

import com.maxezify.enderportals.EnderPortalsMod;
import com.maxezify.enderportals.ModBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Enregistrements côté client. Les couches de rendu (translucide / cutout) des
 * blocs sont déclarées via {@code "render_type"} dans leurs modèles JSON ; il
 * ne reste ici que le renderer du block entity de la porte.
 */
@EventBusSubscriber(modid = EnderPortalsMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class EnderPortalsClient {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.TARDIS_DOOR.get(), TardisDoorRenderer::new);
    }

    private EnderPortalsClient() {
    }
}
