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

    /** Classe pivot d'Immersive Portals, commune au mod officiel et à ses forks. */
    private static final String PORTAL_CLASS = "qouteall.imm_ptl.core.portal.Portal";

    private static final boolean LOADED = detect();

    /**
     * Détection d'Immersive Portals. On teste les modIds connus, puis — et
     * surtout — la simple présence de la classe {@code Portal} : les forks
     * (compatibilité Sodium/Iris/Distant Horizons, etc.) changent souvent de
     * modId tout en conservant le paquetage d'origine, et c'est bien la classe
     * qui compte pour la réflexion qui suit.
     */
    private static boolean detect() {
        for (String modId : new String[]{"immersive_portals_core", "imm_ptl", "imm_ptl_core", "immersive_portals"}) {
            if (ModList.get().isLoaded(modId)) {
                EnderPortalsMod.LOGGER.info("Immersive Portals détecté (modId « {} »).", modId);
                return true;
            }
        }
        try {
            Class.forName(PORTAL_CLASS, false, ImmPtlCompat.class.getClassLoader());
            EnderPortalsMod.LOGGER.info("Immersive Portals détecté via la classe {} (fork).", PORTAL_CLASS);
            return true;
        } catch (ClassNotFoundException e) {
            EnderPortalsMod.LOGGER.info(
                    "Immersive Portals absent : traversée classique de la porte (contact avec l'embrasure).");
            return false;
        }
    }

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
        if (!isLoaded()) {
            EnderPortalsMod.LOGGER.debug(
                    "Portails Immersive Portals non créés : mod absent ou intégration désactivée (broken={}).", broken);
            return;
        }
        if (data.immptlActive) {
            if (portalsAlive(server, data)) {
                return;
            }
            // Les entités de portail ont disparu (rechargement du monde, purge
            // d'entités…) : on repart d'un état propre plutôt que de croire à
            // des portails qui n'existent plus.
            EnderPortalsMod.LOGGER.info("Portails Immersive Portals introuvables pour le TARDIS {} — recréation.",
                    data.id);
            data.portalIds.clear();
            data.immptlActive = false;
        }
        if (!data.deployed || !data.open) {
            EnderPortalsMod.LOGGER.info(
                    "Portails Immersive Portals non créés pour le TARDIS {} : deployed={}, open={}.",
                    data.id, data.deployed, data.open);
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

            Entity outer = spawnPortal(exteriorWorld, exteriorCenter, data.exteriorFacing,
                    ModDimensions.ENDER_WORLD, interiorCenter, rotation);
            Entity inner = spawnPortal(enderWorld, interiorCenter, data.interiorFacing,
                    data.exteriorWorld, exteriorCenter, Mth.wrapDegrees(-rotation));

            data.portalIds.add(outer.getUUID());
            data.portalIds.add(inner.getUUID());

            // Chaque portail est complété par sa face opposée : l'ensemble
            // devient bi-way ET bi-faced (4 entités), comme un portail du
            // Nether. La porte ouverte montre alors la destination des DEUX
            // côtés — l'arrière du caisson n'apparaît plus comme un
            // encadrement vide, faute de portail de ce côté.
            addFlipped(data, outer);
            addFlipped(data, inner);
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

    /** Les entités de portail enregistrées existent-elles toujours ? */
    private static boolean portalsAlive(MinecraftServer server, TardisData data) {
        if (data.portalIds.isEmpty()) {
            return false;
        }
        ServerLevel exteriorWorld = server.getLevel(data.exteriorWorld);
        ServerLevel enderWorld = server.getLevel(ModDimensions.ENDER_WORLD);
        for (UUID id : data.portalIds) {
            boolean found = (exteriorWorld != null && exteriorWorld.getEntity(id) != null)
                    || (enderWorld != null && enderWorld.getEntity(id) != null);
            if (!found) {
                return false;
            }
        }
        return true;
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

    /**
     * Décalage du plan du portail par rapport au centre du bloc, le long de la
     * façade — le portail est un plan sans épaisseur (0,8 × 1,9).
     *
     * <p>Le placer au fond du caisson (−0,36) a été essayé pour que la paroi
     * arrière tombe derrière lui : la vue traversante disparaissait et la
     * porte n'était plus franchissable. On le garde donc à l'avant.</p>
     */
    private static final double PORTAL_DEPTH_OFFSET = 0.06;

    /** Centre de l'embrasure (1 × 2 blocs) d'une porte. */
    private static Vec3 doorwayCenter(BlockPos base, Direction facing) {
        return Vec3.atCenterOf(base).add(0.0, 0.5, 0.0)
                .add(vector(facing).scale(PORTAL_DEPTH_OFFSET));
    }

    private static Vec3 vector(Direction direction) {
        return new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }

    /**
     * Crée un portail Immersive Portals par réflexion.
     * Plan 0.9 × 2.0, normale = {@code facing}.
     */
    private static Entity spawnPortal(ServerLevel level, Vec3 origin, Direction facing,
                                    ResourceKey<Level> destinationWorld, Vec3 destination,
                                    double rotationDegrees) throws Exception {
        Class<?> portalClass = Class.forName(PORTAL_CLASS);
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
        return portal;
    }

    /**
     * Complète un portail par sa face opposée (bi-faced) via
     * {@code PortalManipulation.completeBiFacedPortal}, qui crée ET fait
     * apparaître le portail retourné. Son identifiant rejoint la liste pour que
     * la fermeture de la porte le supprime aussi.
     *
     * <p>Un échec ici n'est pas fatal : la paire principale reste fonctionnelle,
     * seule la visibilité par l'arrière est perdue.</p>
     */
    private static void addFlipped(TardisData data, Entity portal) {
        try {
            Class<?> portalClass = Class.forName(PORTAL_CLASS);
            Class<?> manipulation = Class.forName("qouteall.imm_ptl.core.portal.PortalManipulation");
            Object flipped = manipulation
                    .getMethod("completeBiFacedPortal", portalClass, EntityType.class)
                    .invoke(null, portal, portal.getType());
            if (flipped instanceof Entity entity) {
                data.portalIds.add(entity.getUUID());
            }
        } catch (Throwable t) {
            EnderPortalsMod.LOGGER.warn(
                    "Face opposée du portail indisponible — la porte ne sera traversable que par l'avant.", t);
        }
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
