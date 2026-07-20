package com.maxezify.enderportals.compat;

import com.maxezify.enderportals.EnderPortalsMod;
import com.maxezify.enderportals.ModDimensions;
import com.maxezify.enderportals.tardis.TardisData;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

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

    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded("imm_ptl_core")
            || FabricLoader.getInstance().isModLoaded("immersive_portals");

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
            ServerWorld exteriorWorld = server.getWorld(data.exteriorWorld);
            ServerWorld enderWorld = server.getWorld(ModDimensions.ENDER_WORLD);
            if (exteriorWorld == null || enderWorld == null) {
                return;
            }
            Vec3d exteriorCenter = doorwayCenter(data.exteriorPos, data.exteriorFacing);
            Vec3d interiorCenter = doorwayCenter(data.interiorDoorPos, data.interiorFacing);
            // La traversée mappe la direction d'entrée (−facing extérieur) sur la
            // direction de sortie (+facing intérieur), d'où le +180°.
            double rotation = MathHelper.wrapDegrees(
                    data.interiorFacing.asRotation() - data.exteriorFacing.asRotation() + 180.0);

            UUID outer = spawnPortal(exteriorWorld, exteriorCenter, data.exteriorFacing,
                    ModDimensions.ENDER_WORLD, interiorCenter, rotation);
            UUID inner = spawnPortal(enderWorld, interiorCenter, data.interiorFacing,
                    data.exteriorWorld, exteriorCenter, MathHelper.wrapDegrees(-rotation));

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
        ServerWorld exteriorWorld = server.getWorld(data.exteriorWorld);
        ServerWorld enderWorld = server.getWorld(ModDimensions.ENDER_WORLD);
        for (UUID id : data.portalIds) {
            discardEntity(exteriorWorld, id);
            discardEntity(enderWorld, id);
        }
        data.portalIds.clear();
        data.immptlActive = false;
    }

    private static void discardEntity(ServerWorld world, UUID id) {
        if (world != null) {
            Entity entity = world.getEntity(id);
            if (entity != null) {
                entity.discard();
            }
        }
    }

    /** Centre de l'embrasure (1 × 2 blocs) d'une porte. */
    private static Vec3d doorwayCenter(net.minecraft.util.math.BlockPos base, Direction facing) {
        return Vec3d.ofCenter(base).add(0.0, 0.5, 0.0)
                .add(Vec3d.of(facing.getVector()).multiply(0.06));
    }

    /**
     * Crée un portail Immersive Portals par réflexion.
     * Plan 0.9 × 2.0, normale = {@code facing}.
     */
    private static UUID spawnPortal(ServerWorld world, Vec3d origin, Direction facing,
                                    RegistryKey<World> destinationWorld, Vec3d destination,
                                    double rotationDegrees) throws Exception {
        Class<?> portalClass = Class.forName("qouteall.imm_ptl.core.portal.Portal");
        EntityType<?> type = (EntityType<?>) staticFieldValue(portalClass, "entityType", "ENTITY_TYPE");
        Entity portal = type.create(world);
        if (portal == null) {
            throw new IllegalStateException("EntityType du portail introuvable");
        }

        invoke(portalClass, portal, "setOriginPos", new Class<?>[]{Vec3d.class}, origin);
        invoke(portalClass, portal, "setDestinationDimension", new Class<?>[]{RegistryKey.class}, destinationWorld);
        invoke(portalClass, portal, "setDestination", new Class<?>[]{Vec3d.class}, destination);

        // axisW × axisH doit pointer vers l'extérieur de la porte (= facing).
        Vec3d axisW = Vec3d.of(facing.rotateYCounterclockwise().getVector());
        Vec3d axisH = new Vec3d(0.0, 1.0, 0.0);
        invoke(portalClass, portal, "setOrientationAndSize",
                new Class<?>[]{Vec3d.class, Vec3d.class, double.class, double.class},
                axisW, axisH, 0.9, 2.0);

        applyRotation(portalClass, portal, rotationDegrees);

        if (!world.spawnEntity(portal)) {
            throw new IllegalStateException("Le monde a refusé le portail");
        }
        return portal.getUuid();
    }

    /**
     * Applique la rotation du portail. Sans elle la vue serait inversée :
     * en cas d'échec on préfère lever l'exception et laisser
     * {@link #tryCreatePortals} retomber sur la téléportation classique.
     */
    private static void applyRotation(Class<?> portalClass, Entity portal, double degrees) throws Exception {
        if (Math.abs(MathHelper.wrapDegrees(degrees)) < 0.01) {
            return;
        }
        Class<?> quaternionClass = Class.forName("qouteall.q_misc_util.my_util.DQuaternion");
        Object rotation = quaternionClass
                .getMethod("rotationByDegrees", Vec3d.class, double.class)
                .invoke(null, new Vec3d(0.0, 1.0, 0.0), degrees);
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
