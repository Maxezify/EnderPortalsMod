package com.maxezify.enderportals.compat;

import com.maxezify.enderportals.EnderPortalsMod;
import com.maxezify.enderportals.ModDimensions;
import com.maxezify.enderportals.tardis.TardisData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Intégration optionnelle et EXPÉRIMENTALE avec Immersive Portals
 * (mod id {@code imm_ptl_core}). Quand le mod est présent, l'ouverture de la
 * porte crée une paire de portails "voir au travers" entre l'embrasure
 * extérieure et l'embrasure intérieure — la continuité visuelle entre les
 * deux dimensions.
 *
 * <p>Tout passe par la réflexion : aucune dépendance de compilation. Si l'API
 * d'Immersive Portals change, on se rabat proprement sur la téléportation
 * classique du mod (contact avec la porte ouverte).</p>
 */
public final class ImmPtlCompat {

    private static final boolean LOADED = ModList.get().isLoaded("imm_ptl_core")
            || ModList.get().isLoaded("immersive_portals");

    /** Passe à true au premier échec de réflexion : on n'insiste pas. */
    private static boolean broken;

    public static boolean isLoaded() {
        return LOADED && !broken;
    }

    /**
     * Tente de créer la paire de portails de l'embrasure. Positionne
     * {@code data.immptlActive} en conséquence.
     */
    public static void tryCreatePortals(MinecraftServer server, TardisData data) {
        if (!isLoaded() || data.immptlActive || !data.deployed || !data.open) {
            return;
        }
        try {
            ServerLevel exteriorWorld = server.getLevel(data.exteriorWorld);
            ServerLevel enderWorld = server.getLevel(ModDimensions.ENDER_WORLD);
            if (exteriorWorld == null || enderWorld == null) {
                return;
            }
            Vec3 exteriorCenter = doorwayCenter(data.exteriorPos, data.exteriorFacing);
            Vec3 interiorCenter = doorwayCenter(data.interiorDoorPos, data.interiorFacing);
            // La traversée mappe la direction d'entrée (−facing extérieur) sur la
            // direction de sortie (+facing intérieur), d'où le +180°. Le signe est
            // inversé car le yaw Minecraft est horaire (vu de dessus) alors que la
            // rotation autour de l'axe +Y est anti-horaire — validé en jeu : sans
            // cette inversion, les portes face est/ouest montraient la salle à
            // l'envers (nord/sud étant insensibles au signe).
            double rotation = Mth.wrapDegrees(
                    data.exteriorFacing.toYRot() - data.interiorFacing.toYRot() + 180.0);

            UUID outer = spawnPortal(exteriorWorld, exteriorCenter, data.exteriorFacing,
                    ModDimensions.ENDER_WORLD, interiorCenter, rotation);
            UUID inner = spawnPortal(enderWorld, interiorCenter, data.interiorFacing,
                    data.exteriorWorld, exteriorCenter, Mth.wrapDegrees(-rotation));

            data.portalIds.add(outer);
            data.portalIds.add(inner);
            data.immptlActive = true;
            EnderPortalsMod.LOGGER.info("Portails Immersive Portals créés pour le TARDIS {}", data.id);
        } catch (Throwable t) {
            broken = true;
            data.immptlActive = false;
            removePortals(server, data);
            EnderPortalsMod.LOGGER.warn(
                    "Intégration Immersive Portals indisponible (API changée ?) — retour à la téléportation classique.", t);
        }
    }

    /** Supprime les portails de l'embrasure s'ils existent. */
    public static void removePortals(MinecraftServer server, TardisData data) {
        if (data.portalIds.isEmpty()) {
            data.immptlActive = false;
            return;
        }
        ServerLevel exteriorWorld = server.getLevel(data.exteriorWorld);
        ServerLevel enderWorld = server.getLevel(ModDimensions.ENDER_WORLD);
        for (UUID id : data.portalIds) {
            discardEntity(exteriorWorld, id);
            discardEntity(enderWorld, id);
        }
        data.portalIds.clear();
        data.immptlActive = false;
    }

    private static void discardEntity(ServerLevel level, UUID id) {
        if (level != null) {
            Entity entity = level.getEntity(id);
            if (entity != null) {
                entity.discard();
            }
        }
    }

    /** Centre de l'embrasure (1 × 2 blocs) d'une porte. */
    private static Vec3 doorwayCenter(BlockPos base, Direction facing) {
        return Vec3.atCenterOf(base).add(0.0, 0.5, 0.0)
                .add(vector(facing).scale(0.06));
    }

    private static Vec3 vector(Direction direction) {
        return new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }

    /**
     * Crée un portail Immersive Portals par réflexion.
     * Plan 0.9 × 2.0, normale = {@code facing}.
     */
    private static UUID spawnPortal(ServerLevel level, Vec3 origin, Direction facing,
                                    ResourceKey<Level> destinationWorld, Vec3 destination,
                                    double rotationDegrees) throws Exception {
        Class<?> portalClass = Class.forName("qouteall.imm_ptl.core.portal.Portal");
        EntityType<?> type = (EntityType<?>) staticFieldValue(portalClass, "entityType", "ENTITY_TYPE");
        Entity portal = type.create(level);
        if (portal == null) {
            throw new IllegalStateException("EntityType du portail introuvable");
        }

        invoke(portalClass, portal, "setOriginPos", new Class<?>[]{Vec3.class}, origin);
        invoke(portalClass, portal, "setDestinationDimension", new Class<?>[]{ResourceKey.class}, destinationWorld);
        invoke(portalClass, portal, "setDestination", new Class<?>[]{Vec3.class}, destination);

        // axisW × axisH doit pointer vers l'extérieur de la porte (= facing).
        Vec3 axisW = vector(facing.getCounterClockWise());
        Vec3 axisH = new Vec3(0.0, 1.0, 0.0);
        // 0,8 × 1,9 : le plan du portail doit tenir dans l'embrasure du caisson
        // (parois à ±0,44, plancher/plafond à 0,03/1,97) sans les traverser.
        invoke(portalClass, portal, "setOrientationAndSize",
                new Class<?>[]{Vec3.class, Vec3.class, double.class, double.class},
                axisW, axisH, 0.8, 1.9);

        applyRotation(portalClass, portal, rotationDegrees);

        if (!level.addFreshEntity(portal)) {
            throw new IllegalStateException("Le monde a refusé le portail");
        }
        return portal.getUUID();
    }

    /**
     * Applique la rotation du portail. Sans elle la vue serait inversée :
     * en cas d'échec on préfère lever l'exception et laisser
     * {@link #tryCreatePortals} retomber sur la téléportation classique.
     */
    private static void applyRotation(Class<?> portalClass, Entity portal, double degrees) throws Exception {
        if (Math.abs(Mth.wrapDegrees(degrees)) < 1.0) {
            return;
        }
        Class<?> quaternionClass = Class.forName("qouteall.q_misc_util.my_util.DQuaternion");
        Object rotation = quaternionClass
                .getMethod("rotationByDegrees", Vec3.class, double.class)
                .invoke(null, new Vec3(0.0, 1.0, 0.0), degrees);
        for (String methodName : new String[]{"setRotationTransformation", "setRotation", "setRotationTransformationD"}) {
            try {
                portalClass.getMethod(methodName, quaternionClass).invoke(portal, rotation);
                return;
            } catch (NoSuchMethodException ignored) {
                // On tente le nom suivant.
            }
        }
        Field field = portalClass.getField("rotation");
        field.set(portal, rotation);
    }

    private static Object staticFieldValue(Class<?> owner, String... names) throws Exception {
        for (String name : names) {
            try {
                Field field = owner.getField(name);
                return field.get(null);
            } catch (NoSuchFieldException ignored) {
                // On tente le nom suivant.
            }
        }
        throw new NoSuchFieldException(String.join("/", names));
    }

    private static void invoke(Class<?> owner, Object target, String name, Class<?>[] parameterTypes, Object... args)
            throws Exception {
        Method method = owner.getMethod(name, parameterTypes);
        method.invoke(target, args);
    }

    private ImmPtlCompat() {
    }
}
