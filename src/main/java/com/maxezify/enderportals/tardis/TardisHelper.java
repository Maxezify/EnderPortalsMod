package com.maxezify.enderportals.tardis;

import com.maxezify.enderportals.EnderPortalsMod;
import com.maxezify.enderportals.ModBlocks;
import com.maxezify.enderportals.ModDimensions;
import com.maxezify.enderportals.block.TardisDoorBlock;
import com.maxezify.enderportals.block.entity.TardisDoorBlockEntity;
import com.maxezify.enderportals.compat.ImmPtlCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Toute la mécanique TARDIS : activation, salle intérieure, matérialisation,
 * dématérialisation, ouverture et traversées.
 */
public final class TardisHelper {

    private static final int PORTAL_COOLDOWN_TICKS = 60;

    // ------------------------------------------------------------------
    // Activation (coup de masse)
    // ------------------------------------------------------------------

    public static void activate(ServerLevel level, BlockPos base, Direction facing, ServerPlayer player) {
        MinecraftServer server = level.getServer();
        ServerLevel enderWorld = server.getLevel(ModDimensions.ENDER_WORLD);
        if (enderWorld == null) {
            EnderPortalsMod.LOGGER.error("Dimension enderportals:ender_world introuvable !");
            return;
        }

        TardisStateManager manager = TardisStateManager.get(server);
        TardisData data = manager.createTardis(player.getUUID(), player.getGameProfile().getName());
        buildInteriorRoom(enderWorld, data);
        // Le voisinage se taille en fond, pendant que le joueur se relève de sa
        // chute : la première ouverture n'aura plus rien à attendre.
        warmInterior(enderWorld, data.interiorDoorPos);

        // Remplace la porte inactive par la porte active, sans réactions de voisins.
        int swapFlags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        level.setBlock(base, Blocks.AIR.defaultBlockState(), swapFlags);
        level.setBlock(base.above(), Blocks.AIR.defaultBlockState(), swapFlags);
        placeDoor(level, base, facing, false, data, false, false);

        data.deployed = true;
        data.open = false;
        data.exteriorWorld = level.dimension();
        data.exteriorPos = base;
        data.exteriorFacing = facing;
        manager.setDirty();

        LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
        bolt.moveTo(Vec3.atBottomCenterOf(base));
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);
        level.playSound(null, base, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.5f, 0.6f);
        player.displayClientMessage(Component.translatable("enderportals.message.activated"), false);
    }

    // ------------------------------------------------------------------
    // Préchauffage de la parcelle
    // ------------------------------------------------------------------

    /**
     * Ticket temporaire posé autour de la salle intérieure, le temps que ses
     * chunks se génèrent.
     *
     * <p>Sans lui, la parcelle ne se génère qu'au moment où quelqu'un la
     * réclame — et le premier à la réclamer est le fil du serveur lui-même, qui
     * s'arrête alors jusqu'à ce que la génération finisse. C'est ce gel de
     * quelques secondes que l'on voyait à la première ouverture, et c'est aussi
     * ce qui distingue notre porte d'un portail du Nether : là-bas le monde
     * d'arrivée se taille sur les fils de génération pendant que le jeu
     * continue de tourner.</p>
     *
     * <p>Un ticket remet ce travail à sa place : à l'avance, en fond, pendant
     * que le joueur est encore dehors. Il expire de lui-même au bout de
     * {@value #WARMUP_TICKS} ticks — une parcelle que personne n'occupe n'a
     * aucune raison de rester chargée.</p>
     */
    private static final int WARMUP_TICKS = 600;

    private static final TicketType<ChunkPos> WARMUP =
            TicketType.create("enderportals_warmup", Comparator.comparingLong(ChunkPos::toLong), WARMUP_TICKS);

    /**
     * Rayon préchauffé, en chunks. Neuf chunks de côté : de quoi couvrir la
     * salle et ses abords immédiats sans mettre en chantier un carré de
     * plusieurs centaines de chunks dont on ne verra rien.
     */
    private static final int WARMUP_RADIUS = 4;

    /**
     * Rayon, en chunks, dont on attend la génération avant de toucher à la
     * salle. Un carré de 3 sur 3 : la salle fait 13 blocs sur 13 et peut donc
     * chevaucher quatre chunks, et {@link #buildInteriorRoom} y écrit
     * directement — un seul chunk garanti ne suffirait pas.
     */
    private static final int INTERIOR_RADIUS = 1;

    /** Pose le ticket de préchauffage et rend le chunk visé. */
    private static ChunkPos warmInterior(ServerLevel enderWorld, BlockPos interior) {
        ChunkPos center = new ChunkPos(interior);
        enderWorld.getChunkSource().addRegionTicket(WARMUP, center, WARMUP_RADIUS, center);
        return center;
    }

    /**
     * Exécute une tâche dès que les chunks de la salle sont prêts, <b>sans
     * arrêter le fil du serveur</b>.
     *
     * <p>C'est ici que se jouait le gel. Poser un ticket ne suffit pas : la
     * génération qu'il déclenche est asynchrone, et la moindre lecture qui
     * suit — un simple {@code getBlockState} sur le monde de l'Ender — repasse
     * par {@code getChunk(…, FULL, true)}, qui arrête le fil du serveur jusqu'à
     * ce que le chunk soit prêt. Le ticket de la 0.17.1 ne servait donc à rien :
     * la ligne suivante bloquait sur le chunk qu'il venait de demander.</p>
     *
     * <p>Pire, un seul chunk réclamé au statut FULL en entraîne une vingtaine :
     * il lui faut ses voisins pour ses structures et sa lumière. Le fil du
     * serveur attendait donc la génération de tout un voisinage.</p>
     *
     * <p>D'où cette continuation. On demande les chunks, on rend la main, et le
     * travail reprend sur le fil du serveur quand ils arrivent — exactement ce
     * que fait un portail du Nether, dont le monde d'arrivée se taille pendant
     * que le jeu continue de tourner.</p>
     */
    private static void whenInteriorReady(MinecraftServer server, @Nullable BlockPos interior,
                                          Consumer<ServerLevel> task) {
        ServerLevel enderWorld = server.getLevel(ModDimensions.ENDER_WORLD);
        if (enderWorld == null || interior == null) {
            return;
        }
        ChunkPos center = warmInterior(enderWorld, interior);
        ServerChunkCache chunks = enderWorld.getChunkSource();

        List<CompletableFuture<?>> pending = new ArrayList<>();
        for (int dx = -INTERIOR_RADIUS; dx <= INTERIOR_RADIUS; dx++) {
            for (int dz = -INTERIOR_RADIUS; dz <= INTERIOR_RADIUS; dz++) {
                pending.add(chunks.getChunkFuture(center.x + dx, center.z + dz, ChunkStatus.FULL, true));
            }
        }
        // thenRunAsync(…, server) : la suite repart sur le fil du serveur, seul
        // endroit d'où l'on ait le droit de toucher au monde.
        CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new))
                .thenRunAsync(() -> task.accept(enderWorld), server);
    }

    // ------------------------------------------------------------------
    // Salle intérieure
    // ------------------------------------------------------------------

    /**
     * Creuse la salle de départ dans le monde de l'Ender et y pose la porte
     * intérieure. La porte est dans le mur nord et regarde vers la salle (sud).
     */
    public static void buildInteriorRoom(ServerLevel enderWorld, TardisData data) {
        BlockPos door = data.interiorDoorPos;
        int x0 = door.getX() - 6, x1 = door.getX() + 6;
        int y0 = door.getY() - 1, y1 = door.getY() + 5;
        int z0 = door.getZ(), z1 = door.getZ() + 12;

        BlockState bricks = ModBlocks.ENDER_BRICKS.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    boolean shell = x == x0 || x == x1 || y == y0 || y == y1 || z == z0 || z == z1;
                    enderWorld.setBlock(cursor.set(x, y, z), shell ? bricks : air, Block.UPDATE_CLIENTS);
                }
            }
        }
        // Un peu de lumière aux quatre coins du sol — du froglight perlescent,
        // la même matière que les filons du monde, pour que la salle appartienne
        // au même univers lumineux.
        BlockState lantern = Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState();
        enderWorld.setBlock(cursor.set(x0 + 1, y0, z0 + 2), lantern, Block.UPDATE_CLIENTS);
        enderWorld.setBlock(cursor.set(x1 - 1, y0, z0 + 2), lantern, Block.UPDATE_CLIENTS);
        enderWorld.setBlock(cursor.set(x0 + 1, y0, z1 - 2), lantern, Block.UPDATE_CLIENTS);
        enderWorld.setBlock(cursor.set(x1 - 1, y0, z1 - 2), lantern, Block.UPDATE_CLIENTS);

        // La porte intérieure, encastrée dans le mur nord (z0), face au sud.
        placeDoor(enderWorld, door, data.interiorFacing, false, data, true, false);
    }

    // ------------------------------------------------------------------
    // Matérialisation / dématérialisation de la porte extérieure
    // ------------------------------------------------------------------

    /**
     * Matérialise la porte extérieure à l'endroit donné (avec fondu). Si elle
     * était déployée ailleurs, elle s'y dématérialise d'abord.
     */
    public static boolean deployExterior(MinecraftServer server, TardisData data, ServerLevel level,
                                         BlockPos base, Direction facing, boolean open,
                                         @Nullable Player feedback) {
        if (!level.getBlockState(base).canBeReplaced() || !level.getBlockState(base.above()).canBeReplaced()) {
            if (feedback != null) {
                feedback.displayClientMessage(Component.translatable("enderportals.message.no_space"), true);
            }
            return false;
        }
        if (data.deployed) {
            dismissExterior(server, data);
        }

        placeDoor(level, base, facing, open, data, false, true);
        data.deployed = true;
        data.open = open;
        data.exteriorWorld = level.dimension();
        data.exteriorPos = base;
        data.exteriorFacing = facing;
        TardisStateManager.get(server).setDirty();

        level.playSound(null, base, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.2f, 0.5f);
        level.playSound(null, base, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0f, 0.6f);

        whenInteriorReady(server, data.interiorDoorPos, enderWorld -> {
            setOpen(enderWorld, data.interiorDoorPos, open);
            if (open) {
                ImmPtlCompat.tryCreatePortals(server, data);
            }
            updatePortalFlags(server, data);
        });
        return true;
    }

    // ------------------------------------------------------------------
    // Rappel depuis l'intérieur
    // ------------------------------------------------------------------

    /** Rayon de recherche autour de l'emplacement mémorisé, en blocs. */
    private static final int RECALL_RADIUS = 8;
    /** Débattement vertical de cette recherche. */
    private static final int RECALL_HEIGHT = 4;

    /**
     * Rappelle la porte extérieure depuis l'intérieur, sans jamais laisser le
     * joueur enfermé.
     *
     * <p>C'est la distinction qui gouverne cette méthode : un placement
     * <b>choisi</b> — clic droit au sol — peut échouer et le dire, puisque le
     * joueur a désigné l'endroit. Un <b>rappel</b>, lui, est la seule issue d'une
     * parcelle cloisonnée de bedrock sur 8192 blocs : le refuser enferme. Il
     * suffisait qu'un joueur bâtisse sur les deux blocs mémorisés — ou qu'un
     * arbre y pousse — pour que la base devienne une prison dont on ne sortait
     * qu'en mourant, et même pas si l'on avait un lit à l'intérieur.</p>
     *
     * <p>Trois tentatives, de la plus fidèle à la plus sûre : l'emplacement exact,
     * puis un logement libre au voisinage, puis le point de réapparition du
     * joueur. La dernière ne peut échouer que si ce point est lui aussi muré sur
     * huit blocs, et le joueur en est alors averti.</p>
     */
    public static void recallExterior(MinecraftServer server, TardisData data, Player player) {
        ServerLevel level = server.getLevel(data.exteriorWorld);
        if (level != null) {
            // L'emplacement mémorisé n'est testé que sur l'encombrement, sans
            // exiger de sol : la porte y était, elle y retourne à l'identique.
            if (isClear(level, data.exteriorPos)
                    && deploySilently(server, data, level, data.exteriorPos)) {
                // Rappel à l'identique : le message n'a pas de coordonnées à
                // donner, le joueur sait où il avait laissé sa porte.
                player.displayClientMessage(
                        Component.translatable("enderportals.message.tardis_recalled"), true);
                return;
            }
            BlockPos nearby = findFreeSpot(level, data.exteriorPos);
            if (nearby != null && deploySilently(server, data, level, nearby)) {
                announce(player, "enderportals.message.tardis_recalled_nearby", nearby);
                return;
            }
        }
        if (recallToRespawn(server, data, player)) {
            return;
        }
        player.displayClientMessage(
                Component.translatable("enderportals.message.tardis_recall_failed"), true);
    }

    /**
     * Dernier recours : la porte réapparaît au point de réapparition du joueur.
     *
     * <p>Un lit posé <i>dans</i> le monde de l'Ender est écarté — y renvoyer la
     * porte ne sortirait personne. On retombe alors sur le spawn du monde.</p>
     */
    private static boolean recallToRespawn(MinecraftServer server, TardisData data, Player player) {
        ServerLevel level = server.overworld();
        BlockPos origin = level.getSharedSpawnPos();
        if (player instanceof ServerPlayer serverPlayer) {
            BlockPos bed = serverPlayer.getRespawnPosition();
            ServerLevel bedLevel = server.getLevel(serverPlayer.getRespawnDimension());
            if (bed != null && bedLevel != null
                    && !bedLevel.dimension().equals(ModDimensions.ENDER_WORLD)) {
                level = bedLevel;
                origin = bed;
            }
        }
        BlockPos spot = isClear(level, origin) ? origin : findFreeSpot(level, origin);
        if (spot == null || !deploySilently(server, data, level, spot)) {
            return false;
        }
        announce(player, "enderportals.message.tardis_recalled_spawn", spot);
        return true;
    }

    /**
     * Le premier logement libre autour de ce point, par anneaux croissants pour
     * que la porte se pose au plus près de là où on l'avait laissée. Contrairement
     * à l'emplacement mémorisé, un logement de remplacement doit reposer sur du
     * solide : la porte ne doit pas se retrouver suspendue en l'air.
     */
    @Nullable
    private static BlockPos findFreeSpot(ServerLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int ring = 1; ring <= RECALL_RADIUS; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    // Seul le bord de l'anneau : l'intérieur a déjà été vu.
                    if (Math.abs(dx) != ring && Math.abs(dz) != ring) {
                        continue;
                    }
                    for (int dy = 0; dy <= RECALL_HEIGHT; dy++) {
                        for (int sign = dy == 0 ? 0 : -1; sign <= 1; sign += 2) {
                            cursor.set(origin.getX() + dx, origin.getY() + dy * sign,
                                    origin.getZ() + dz);
                            if (isClear(level, cursor) && isGrounded(level, cursor)) {
                                return cursor.immutable();
                            }
                            if (dy == 0) {
                                break;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /** Les deux blocs de la porte sont-ils libres, et dans le monde ? */
    private static boolean isClear(ServerLevel level, BlockPos base) {
        if (base.getY() < level.getMinBuildHeight()
                || base.getY() + 1 >= level.getMaxBuildHeight()) {
            return false;
        }
        return level.getBlockState(base).canBeReplaced()
                && level.getBlockState(base.above()).canBeReplaced();
    }

    private static boolean isGrounded(ServerLevel level, BlockPos base) {
        BlockPos below = base.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    /**
     * Pose la porte sans parler : c'est {@link #recallExterior} qui annonce, avec
     * le message qui convient à la tentative retenue.
     *
     * <p>Le booléen est rendu plutôt qu'ignoré. {@code isClear} vérifie déjà tout
     * ce que {@code deployExterior} vérifie, et un peu plus, donc l'échec est
     * aujourd'hui impossible — mais s'en remettre à cet invariant ferait annoncer
     * une réussite qui n'a pas eu lieu le jour où l'une des deux méthodes
     * changerait.</p>
     */
    private static boolean deploySilently(MinecraftServer server, TardisData data, ServerLevel level,
                                          BlockPos base) {
        return deployExterior(server, data, level, base, data.exteriorFacing, true, null);
    }

    private static void announce(Player player, String key, BlockPos where) {
        player.displayClientMessage(Component.translatable(key,
                where.getX(), where.getY(), where.getZ()), true);
    }

    /**
     * Referme les portes et dématérialise la porte extérieure (fondu de
     * disparition, puis les blocs s'effacent).
     */
    public static void dismissExterior(MinecraftServer server, TardisData data) {
        data.open = false;
        whenInteriorReady(server, data.interiorDoorPos, enderWorld -> {
            ImmPtlCompat.removePortals(server, data);
            setOpen(enderWorld, data.interiorDoorPos, false);
            updatePortalFlags(server, data);
        });

        ServerLevel level = server.getLevel(data.exteriorWorld);
        if (level != null && data.deployed) {
            BlockPos base = data.exteriorPos;
            BlockState state = level.getBlockState(base);
            if (state.is(ModBlocks.TARDIS_DOOR.get())) {
                setOpen(level, base, false);
                if (level.getBlockEntity(base) instanceof TardisDoorBlockEntity door) {
                    door.startDematerialize();
                }
                level.playSound(null, base, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.2f, 0.5f);
                level.playSound(null, base, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0f, 0.5f);
            }
        }
        data.deployed = false;
        TardisStateManager.get(server).setDirty();
    }

    /**
     * Ouvre ou ferme les deux portes (extérieure et intérieure) d'un coup.
     */
    public static void setDoorsOpen(MinecraftServer server, TardisData data, boolean open) {
        data.open = open;
        if (data.deployed) {
            ServerLevel level = server.getLevel(data.exteriorWorld);
            if (level != null) {
                setOpen(level, data.exteriorPos, open);
                level.playSound(null, data.exteriorPos,
                        open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE,
                        SoundSource.BLOCKS, 1.0f, 1.0f);
            }
        }
        TardisStateManager.get(server).setDirty();

        // La porte extérieure a déjà répondu au clic — son bruit, son battant.
        // Tout ce qui touche au monde de l'Ender attend que ses chunks se
        // présentent : c'est ce qui gelait le jeu.
        whenInteriorReady(server, data.interiorDoorPos, enderWorld -> {
            if (data.open != open) {
                // Le joueur a rouvert ou refermé entre-temps : l'ordre en cours
                // est le seul qui vaille, celui-ci est périmé.
                return;
            }
            setOpen(enderWorld, data.interiorDoorPos, open);
            if (open && data.deployed) {
                ImmPtlCompat.tryCreatePortals(server, data);
            } else {
                ImmPtlCompat.removePortals(server, data);
            }
            updatePortalFlags(server, data);
            TardisStateManager.get(server).setDirty();
        });
    }

    /**
     * Reporte {@code data.immptlActive} sur les block entities des deux
     * portes : le client affiche le voile de vide dans l'embrasure ouverte
     * uniquement quand aucun portail Immersive Portals ne la couvre.
     */
    private static void updatePortalFlags(MinecraftServer server, TardisData data) {
        boolean active = data.immptlActive;
        if (data.deployed) {
            ServerLevel level = server.getLevel(data.exteriorWorld);
            if (level != null && level.getBlockEntity(data.exteriorPos) instanceof TardisDoorBlockEntity door) {
                door.setPortalActive(active);
            }
        }
        ServerLevel enderWorld = server.getLevel(ModDimensions.ENDER_WORLD);
        if (enderWorld != null && data.interiorDoorPos != null
                && enderWorld.getBlockEntity(data.interiorDoorPos) instanceof TardisDoorBlockEntity door) {
            door.setPortalActive(active);
        }
    }

    private static void setOpen(ServerLevel level, BlockPos base, boolean open) {
        BlockState lower = level.getBlockState(base);
        if (lower.is(ModBlocks.TARDIS_DOOR.get())) {
            level.setBlock(base, lower.setValue(TardisDoorBlock.OPEN, open), Block.UPDATE_ALL);
        }
        BlockState upper = level.getBlockState(base.above());
        if (upper.is(ModBlocks.TARDIS_DOOR.get())) {
            level.setBlock(base.above(), upper.setValue(TardisDoorBlock.OPEN, open), Block.UPDATE_ALL);
        }
    }

    /**
     * Pose les deux moitiés d'une porte de TARDIS et initialise son block
     * entity.
     */
    private static void placeDoor(ServerLevel level, BlockPos base, Direction facing, boolean open,
                                  TardisData data, boolean interior, boolean fadeIn) {
        BlockState lower = ModBlocks.TARDIS_DOOR.get().defaultBlockState()
                .setValue(TardisDoorBlock.FACING, facing)
                .setValue(TardisDoorBlock.HALF, DoubleBlockHalf.LOWER)
                .setValue(TardisDoorBlock.OPEN, open);
        level.setBlock(base, lower, Block.UPDATE_ALL);
        level.setBlock(base.above(), lower.setValue(TardisDoorBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        if (level.getBlockEntity(base) instanceof TardisDoorBlockEntity door) {
            door.initialize(data.id, interior, fadeIn, data.ownerName);
        }
    }

    // ------------------------------------------------------------------
    // Traversées
    // ------------------------------------------------------------------

    public static void enterTardis(ServerPlayer player, TardisData data) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ServerLevel enderWorld = server.getLevel(ModDimensions.ENDER_WORLD);
        if (enderWorld == null || data.interiorDoorPos == null) {
            return;
        }
        BlockPos front = data.interiorDoorPos.relative(data.interiorFacing);
        player.setPortalCooldown(PORTAL_COOLDOWN_TICKS);
        player.changeDimension(new DimensionTransition(enderWorld, Vec3.atBottomCenterOf(front),
                Vec3.ZERO, data.interiorFacing.toYRot(), 0.0f, DimensionTransition.DO_NOTHING));
        enderWorld.playSound(null, front, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 0.9f);
    }

    public static void exitTardis(ServerPlayer player, TardisData data) {
        MinecraftServer server = player.getServer();
        if (server == null || !data.deployed) {
            return;
        }
        ServerLevel level = server.getLevel(data.exteriorWorld);
        if (level == null) {
            return;
        }
        BlockPos front = data.exteriorPos.relative(data.exteriorFacing);
        player.setPortalCooldown(PORTAL_COOLDOWN_TICKS);
        player.changeDimension(new DimensionTransition(level, Vec3.atBottomCenterOf(front),
                Vec3.ZERO, data.exteriorFacing.toYRot(), 0.0f, DimensionTransition.DO_NOTHING));
        level.playSound(null, front, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 0.9f);
    }

    private TardisHelper() {
    }
}
