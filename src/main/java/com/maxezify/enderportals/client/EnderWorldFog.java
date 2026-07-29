package com.maxezify.enderportals.client;

import com.maxezify.enderportals.EnderPortalsMod;
import com.maxezify.enderportals.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Le fondu au loin du monde de l'Ender : la masse s'éteint dans le noir bien
 * avant le dernier chunk chargé, pour qu'on n'en voie jamais la limite.
 *
 * <p>Le besoin est particulier à ce monde. Le Bloc de l'Ender est translucide :
 * le regard porte au travers de la matière pleine, donc jusqu'au bord de la
 * zone chargée, là où le vide se découpe net. Un monde ordinaire n'a pas ce
 * problème — sa pierre arrête l'œil au premier bloc.</p>
 *
 * <p>Le brouillard est asservi à la distance de rendu plutôt qu'à une distance
 * fixe : quel que soit le réglage du joueur, il atteint son opacité pleine à
 * {@value #FULL_FOG_AT} de cette distance, soit avec une marge confortable
 * avant la frontière des chunks.</p>
 *
 * <p>La couleur n'est pas un noir pur mais un violet très sombre. À l'œil c'est
 * du noir ; la nuance existe pour les shaders, qui ne lisent pas cette couleur
 * telle quelle. Complementary Reimagined en tire sa teinte de brume par
 * {@code fogColor * 0.6 + 0.2 * normalize(fogColor)} — ce terme normalisé
 * relève les couleurs sombres, et un noir pur y donnerait un gris neutre. En
 * gardant une trace de violet, la brume du monde reste violette.</p>
 */
@EventBusSubscriber(modid = EnderPortalsMod.MODID, value = Dist.CLIENT)
public final class EnderWorldFog {

    /** Fraction de la distance de rendu où le brouillard devient opaque. */
    private static final float FULL_FOG_AT = 0.55f;
    /** Fraction où il commence à monter — au-delà, la vue reste nette. */
    private static final float FOG_STARTS_AT = 0.12f;

    // Un violet à peine plus clair que le noir. Voir la note sur les shaders.
    private static final float FOG_RED = 0.012f;
    private static final float FOG_GREEN = 0.008f;
    private static final float FOG_BLUE = 0.020f;

    @SubscribeEvent
    public static void computeFogColor(ViewportEvent.ComputeFogColor event) {
        if (!appliesTo(event)) {
            return;
        }
        event.setRed(FOG_RED);
        event.setGreen(FOG_GREEN);
        event.setBlue(FOG_BLUE);
    }

    @SubscribeEvent
    public static void renderFog(ViewportEvent.RenderFog event) {
        if (!appliesTo(event)) {
            return;
        }
        float blocks = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16.0f;
        event.setNearPlaneDistance(blocks * FOG_STARTS_AT);
        event.setFarPlaneDistance(blocks * FULL_FOG_AT);
        // Sans annulation, NeoForge ignore les valeurs posées ici.
        event.setCanceled(true);
    }

    /**
     * Dans le monde de l'Ender, et tant que la caméra n'est pas immergée : le
     * brouillard de l'eau, de la lave ou de la neige poudreuse a ses propres
     * règles, qu'on n'a aucune raison de remplacer.
     */
    private static boolean appliesTo(ViewportEvent event) {
        if (event.getCamera().getFluidInCamera() != FogType.NONE) {
            return false;
        }
        ClientLevel level = Minecraft.getInstance().level;
        return level != null && level.dimension().equals(ModDimensions.ENDER_WORLD);
    }

    private EnderWorldFog() {
    }
}
