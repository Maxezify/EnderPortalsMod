package com.maxezify.enderportals.tardis;

import com.maxezify.enderportals.EnderPortalsTiming;
import com.maxezify.enderportals.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Attendre qu'un coin du monde de l'Ender soit prêt, sans arrêter le serveur.
 *
 * <p>C'est ici que se jouait le gel de la 0.17. Poser un ticket de chunk ne
 * suffit pas : la génération qu'il déclenche est asynchrone, et la moindre
 * lecture qui suit — un simple {@code getBlockState} — repasse par
 * {@code getChunk(…, FULL, true)}, qui <b>arrête le fil du serveur</b> jusqu'à
 * ce que le chunk soit prêt. Le ticket de la 0.17.1 ne servait donc à rien : la
 * ligne suivante bloquait sur le chunk qu'il venait de demander.</p>
 *
 * <p>Pire, un seul chunk réclamé au statut FULL en entraîne une vingtaine : il
 * lui faut ses voisins pour ses structures et sa lumière. Le fil du serveur
 * attendait donc la génération de tout un voisinage.</p>
 *
 * <p>D'où {@link #whenReady} : on demande les chunks, on rend la main, et le
 * travail reprend sur le fil du serveur quand ils arrivent — exactement ce que
 * fait un portail du Nether, dont le monde d'arrivée se taille pendant que le
 * jeu continue de tourner.</p>
 *
 * <p>Ce raisonnement a coûté trois versions à établir. Il n'existe qu'ici,
 * précisément pour qu'une correction future n'ait qu'un seul endroit où
 * s'appliquer : la salle d'une porte qui s'éveille et l'atterrissage d'un
 * Téléporteur d'entité posent le même problème.</p>
 */
public final class EnderChunks {

    /**
     * Durée de vie d'un ticket, en ticks. Trente secondes : de quoi couvrir la
     * génération la plus lente, après quoi une parcelle que personne n'occupe
     * n'a aucune raison de rester chargée.
     */
    private static final int WARMUP_TICKS = 600;

    private static final TicketType<ChunkPos> WARMUP =
            TicketType.create("enderportals_warmup", Comparator.comparingLong(ChunkPos::toLong), WARMUP_TICKS);

    /** Pose le ticket de préchauffage autour de ce point, et rend le chunk visé. */
    public static ChunkPos warm(ServerLevel enderWorld, BlockPos around, int radius) {
        ChunkPos center = new ChunkPos(around);
        enderWorld.getChunkSource().addRegionTicket(WARMUP, center, radius, center);
        return center;
    }

    /**
     * Exécute une tâche dès que les chunks autour de ce point sont prêts.
     *
     * @param warmRadius rayon du ticket, en chunks — ce qu'on met en chantier
     * @param waitRadius rayon dont on attend réellement la génération. Il peut
     *                   être plus petit : on ne dépend que de ce que l'on va
     *                   toucher, et attendre davantage retarderait pour rien
     * @param label      ce qui apparaît au journal si l'attente dépasse le seuil
     */
    public static void whenReady(MinecraftServer server, @Nullable BlockPos target, int warmRadius,
                                 int waitRadius, String label, Consumer<ServerLevel> task) {
        ServerLevel enderWorld = server.getLevel(ModDimensions.ENDER_WORLD);
        if (enderWorld == null || target == null) {
            return;
        }
        ChunkPos center = warm(enderWorld, target, warmRadius);
        ServerChunkCache chunks = enderWorld.getChunkSource();

        List<CompletableFuture<?>> pending = new ArrayList<>();
        for (int dx = -waitRadius; dx <= waitRadius; dx++) {
            for (int dz = -waitRadius; dz <= waitRadius; dz++) {
                pending.add(chunks.getChunkFuture(center.x + dx, center.z + dz, ChunkStatus.FULL, true));
            }
        }
        // thenRunAsync(…, server) : la suite repart sur le fil du serveur, seul
        // endroit d'où l'on ait le droit de toucher au monde.
        long chrono = EnderPortalsTiming.start();
        CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new))
                .thenRunAsync(() -> {
                    EnderPortalsTiming.since("attente des chunks — " + label, chrono);
                    EnderPortalsTiming.measure("travaux sur le fil du serveur — " + label,
                            () -> task.accept(enderWorld));
                }, server);
    }

    private EnderChunks() {
    }
}
