package com.maxezify.enderportals.client;

import com.maxezify.enderportals.EnderPortalsMod;
import com.maxezify.enderportals.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Le lointain du monde de l'Ender : son fondu au noir, et les lueurs d'orage
 * qui le traversent.
 *
 * <p>Le besoin de fondu est particulier à ce monde. Le Bloc de l'Ender est
 * translucide : le regard porte au travers de la matière pleine, donc jusqu'au
 * bord de la zone chargée, là où le vide se découpe net. Un monde ordinaire
 * n'a pas ce problème — sa pierre arrête l'œil au premier bloc.</p>
 *
 * <p>Le brouillard est asservi à la distance de rendu — il atteint son opacité
 * pleine à {@value #FULL_FOG_AT} de cette distance, avec une marge confortable
 * avant la frontière des chunks — mais sans jamais dépasser
 * {@value #FULL_FOG_MAX_BLOCKS} blocs. Ce plafond fixe la portée de vue du
 * monde : c'est lui qui garantit que les calottes de bedrock, à 127 blocs sous
 * la base et 254 au-dessus, restent hors de vue quel que soit le réglage du
 * joueur.</p>
 *
 * <p>Sa couleur n'est pas un noir pur mais un violet très sombre. À l'œil c'est
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
    /** Fraction où il commence à monter — en deçà, la vue reste nette. */
    private static final float FOG_STARTS_AT = 0.12f;

    /**
     * Plafonds absolus, en blocs, appliqués par-dessus les fractions ci-dessus.
     *
     * <p>Sans eux, la portée de vue du monde de l'Ender suivait indéfiniment le
     * réglage du joueur : à 32 chunks, le brouillard ne saturait plus qu'à 281
     * blocs, assez loin pour laisser apparaître les calottes de bedrock. Les
     * fractions restent utiles en dessous — à faible distance de rendu, ce sont
     * elles qui gardent le brouillard à l'intérieur des chunks chargés — mais
     * au-delà de 8 chunks c'est ce plafond qui décide, et l'aspect du monde
     * devient le même pour tout le monde.</p>
     */
    private static final float FULL_FOG_MAX_BLOCKS = 96.0f;
    private static final float FOG_STARTS_MAX_BLOCKS = 20.0f;

    // Un violet à peine plus clair que le noir. Voir la note sur les shaders.
    private static final float FOG_RED = 0.012f;
    private static final float FOG_GREEN = 0.008f;
    private static final float FOG_BLUE = 0.020f;

    // ------------------------------------------------------------------
    // Lueurs d'orage
    // ------------------------------------------------------------------

    /**
     * Les éclairs du monde de l'Ender ne sont pas des entités : c'est le
     * brouillard lui-même qui s'illumine brièvement.
     *
     * <p>Ce détour n'est pas une commodité, c'est la seule voie praticable.
     * Le clignotement céleste de vanilla ({@code setSkyFlashTime}) module la
     * composante de lumière du ciel dans la lightmap — or cette dimension n'en
     * a aucune ({@code has_skylight: false}), et l'effet y serait rigoureusement
     * nul. Une vraie entité d'éclair, elle, joue son tonnerre quoi qu'on fasse
     * de {@code setVisualOnly}, et dessinerait une colonne verticale au milieu
     * d'une masse pleine.</p>
     *
     * <p>Éclairer le brouillard donne exactement ce qu'on cherche : une lueur
     * lointaine et silencieuse, puisque le brouillard ne commence qu'à
     * {@value #FOG_STARTS_AT} de la distance de rendu. Le proche ne bouge pas,
     * seul l'horizon s'embrase.</p>
     */
    private static final int FLASH_TICKS = 7;
    /** Intervalle entre deux orages, en ticks : de 6 à 18 secondes. */
    private static final int FLASH_MIN_INTERVAL = 120;
    private static final int FLASH_MAX_INTERVAL = 360;
    /** Part de la teinte d'éclair atteinte au sommet du flash. */
    private static final float FLASH_PEAK = 0.55f;
    private static final float FLASH_RED = 0.34f;
    private static final float FLASH_GREEN = 0.27f;
    private static final float FLASH_BLUE = 0.52f;

    private static final RandomSource RANDOM = RandomSource.create();

    private static int ticksToFlash = FLASH_MIN_INTERVAL;
    private static int flashTicks;
    private static boolean restrikePending;

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().equals(ModDimensions.ENDER_WORLD)) {
            flashTicks = 0;
            restrikePending = false;
            return;
        }
        if (flashTicks > 0) {
            flashTicks--;
            return;
        }
        if (--ticksToFlash > 0) {
            return;
        }
        flashTicks = FLASH_TICKS;
        if (restrikePending) {
            restrikePending = false;
            ticksToFlash = nextInterval();
        } else {
            // Un éclair sur deux frappe une seconde fois, quelques instants
            // après : c'est ce redoublement qui fait lire la lueur comme un
            // éclair plutôt que comme une variation de lumière.
            restrikePending = RANDOM.nextBoolean();
            ticksToFlash = restrikePending ? 3 + RANDOM.nextInt(4) : nextInterval();
        }
    }

    private static int nextInterval() {
        return FLASH_MIN_INTERVAL + RANDOM.nextInt(FLASH_MAX_INTERVAL - FLASH_MIN_INTERVAL);
    }

    /** Intensité de la lueur, décroissance rapide façon éclair. */
    private static float flashIntensity(float partialTick) {
        if (flashTicks <= 0) {
            return 0.0f;
        }
        float remaining = Mth.clamp((flashTicks - partialTick) / FLASH_TICKS, 0.0f, 1.0f);
        return remaining * remaining * FLASH_PEAK;
    }

    // ------------------------------------------------------------------
    // Brouillard
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void computeFogColor(ViewportEvent.ComputeFogColor event) {
        if (!appliesTo(event)) {
            return;
        }
        float flash = flashIntensity((float) event.getPartialTick());
        event.setRed(Mth.lerp(flash, FOG_RED, FLASH_RED));
        event.setGreen(Mth.lerp(flash, FOG_GREEN, FLASH_GREEN));
        event.setBlue(Mth.lerp(flash, FOG_BLUE, FLASH_BLUE));
    }

    @SubscribeEvent
    public static void renderFog(ViewportEvent.RenderFog event) {
        if (!appliesTo(event)) {
            return;
        }
        float blocks = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16.0f;
        event.setNearPlaneDistance(Math.min(blocks * FOG_STARTS_AT, FOG_STARTS_MAX_BLOCKS));
        event.setFarPlaneDistance(Math.min(blocks * FULL_FOG_AT, FULL_FOG_MAX_BLOCKS));
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
