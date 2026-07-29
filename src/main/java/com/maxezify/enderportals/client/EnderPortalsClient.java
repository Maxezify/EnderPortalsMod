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
 *
 * <p>Le monde de l'Ender n'enregistre plus d'effets de dimension sur mesure :
 * son {@code dimension_type} déclare {@code effects: minecraft:the_nether}.
 * C'est ce champ qu'Iris consulte pour choisir le dossier de shaders à
 * appliquer, et lui seul permet d'obtenir le rendu souterrain sans demander au
 * joueur d'éditer son shaderpack. Voir {@code EnderWorldFog} pour ce que le mod
 * pilote encore par événements — brouillard et lueurs d'orage.</p>
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
