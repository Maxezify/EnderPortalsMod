package com.maxezify.enderportals.tardis;

import com.maxezify.enderportals.ModDimensions;
import com.maxezify.enderportals.block.AllyPassageBlock;
import com.maxezify.enderportals.block.PassagePhase;
import com.maxezify.enderportals.compat.ImmPtlCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * L'unique endroit qui décide de l'état d'un Passage des Alliés.
 *
 * <p>Une paire d'arches n'a qu'un état, pas deux. C'est l'enseignement du test à
 * deux joueurs de la 0.15.0 : la phase y était posée par une transition unique,
 * armée au moment de la connexion, et chaque arche vivait ensuite sa vie. Il
 * suffisait qu'une des deux rate sa transition — un tick programmé ne part pas
 * dans un chunk qui ne tourne pas — pour qu'elle reste pleine sous un portail
 * parfaitement fonctionnel. On voyait la base d'en face sans pouvoir y aller.</p>
 *
 * <p>D'où la règle que tient {@link #reconcile} : la phase se <b>calcule</b>, à
 * partir du lien et d'une <b>échéance commune aux deux arches</b>
 * ({@link TardisData#passageOpenAt}), et elle s'écrit des <b>deux côtés à la
 * fois</b>. Deux propriétés en découlent, et ce sont elles qui comptent :</p>
 * <ul>
 *   <li>les deux arches ne peuvent pas diverger — quel que soit le côté qui
 *       calcule, il calcule la même chose et l'écrit aux deux ;</li>
 *   <li>aucun état n'est définitif — appelée une fois par seconde par chaque
 *       arche chargée, la réconciliation rattrape n'importe quel raté.</li>
 * </ul>
 */
public final class AllyPassageHelper {

    /** Durée de l'animation d'ouverture, en ticks. */
    public static final int OPENING_TICKS = 60;

    private static final int PORTAL_COOLDOWN_TICKS = 60;

    // ------------------------------------------------------------------
    // Entrées : le panneau, puis les arches elles-mêmes
    // ------------------------------------------------------------------

    /** Le lien vient de s'ouvrir : les deux arches s'engagent. */
    public static void openBoth(MinecraftServer server, UUID a, UUID b) {
        // L'échéance est remise à zéro des deux côtés avant tout calcul : c'est
        // ce qui garantit que l'animation d'ouverture est bien rejouée, y
        // compris si les deux fiches portaient par hasard la même valeur d'une
        // connexion antérieure.
        TardisStateManager manager = TardisStateManager.get(server);
        forget(manager.findByOwner(a));
        forget(manager.findByOwner(b));
        manager.setDirty();
        reconcile(server, a);
        reconcile(server, b);
    }

    private static void forget(@Nullable TardisData data) {
        if (data != null) {
            data.passageOpenAt = 0L;
        }
    }

    /** Le lien vient d'être coupé : les deux arches se referment. */
    public static void closeBoth(MinecraftServer server, UUID a, @Nullable UUID b) {
        reconcile(server, a);
        if (b != null) {
            reconcile(server, b);
        }
    }

    /**
     * Un seul joueur : le pair délogé par une nouvelle connexion. Son lien vient
     * d'être retiré, donc la réconciliation le referme — portails compris, sans
     * quoi il resterait une vue, et un chemin, vers une base dont le lien vient
     * d'être coupé.
     */
    public static void closeOne(MinecraftServer server, UUID player) {
        reconcile(server, player);
    }

    /**
     * Réconciliation depuis l'arche elle-même, une fois par seconde. Rend le
     * pseudo à afficher sur la façade close, ou une chaîne vide si l'arche
     * n'appartient à personne.
     */
    public static String reconcileAt(ServerLevel level, BlockPos base) {
        MinecraftServer server = level.getServer();
        TardisData mine = TardisStateManager.get(server).findByPassage(base);
        if (mine == null || mine.ownerUuid == null) {
            return "";
        }
        reconcile(server, mine.ownerUuid);
        return mine.ownerName;
    }

    // ------------------------------------------------------------------
    // Le calcul
    // ------------------------------------------------------------------

    /**
     * Aligne l'arche de ce joueur — et celle de son allié — sur le lien noté
     * côté serveur.
     *
     * <p>Idempotente : appelée en boucle sur un état déjà correct, elle ne
     * touche à rien. C'est ce qui permet de l'appeler à la fois sur décision du
     * panneau et à intervalle régulier.</p>
     */
    public static void reconcile(MinecraftServer server, UUID owner) {
        TardisStateManager manager = TardisStateManager.get(server);
        TardisData mine = manager.findByOwner(owner);
        ServerLevel level = server.getLevel(ModDimensions.ENDER_WORLD);
        if (mine == null || level == null) {
            return;
        }
        UUID allyId = manager.allies().linkOf(owner);
        TardisData ally = allyId == null ? null : manager.findByOwner(allyId);
        boolean linked = mine.passagePos != null && ally != null && ally.passagePos != null;

        if (!linked) {
            close(server, manager, level, mine, ally);
            return;
        }

        long now = level.getGameTime();
        if (mine.passageOpenAt == 0L || ally.passageOpenAt != mine.passageOpenAt) {
            // L'échéance est commune aux deux fiches : c'est elle qui garantit
            // que les deux arches perceront au même tick, y compris quand une
            // seule des deux parcelles tourne.
            long deadline = now + OPENING_TICKS;
            mine.passageOpenAt = deadline;
            ally.passageOpenAt = deadline;
            manager.setDirty();
            // Invariant : une arche pleine ne porte jamais de portail. Pendant
            // les trois secondes d'animation elle l'est — un portail devant un
            // bloc plein, c'est une vue traversante qu'on ne peut pas franchir.
            dropPortals(server, manager, mine);
            dropPortals(server, manager, ally);
            writePhase(level, manager, ally, PassagePhase.OPENING);
            if (writePhase(level, manager, mine, PassagePhase.OPENING)) {
                level.playSound(null, mine.passagePos, SoundEvents.END_PORTAL_FRAME_FILL,
                        SoundSource.BLOCKS, 0.9f, 1.4f);
            }
            return;
        }
        if (now < mine.passageOpenAt) {
            writePhase(level, manager, mine, PassagePhase.OPENING);
            return;
        }

        // L'échéance est passée : la paire de portails doit exister. Elle est
        // recréée à la demande — si les entités ont disparu (rechargement du
        // monde, purge), tryCreatePassagePortals les refait.
        boolean wasActive = mine.passagePortalsActive;
        int portalCount = mine.passagePortalIds.size();
        boolean through = ImmPtlCompat.tryCreatePassagePortals(server, mine, ally);
        if (wasActive != mine.passagePortalsActive || portalCount != mine.passagePortalIds.size()) {
            manager.setDirty();
        }
        PassagePhase target = through ? PassagePhase.THROUGH : PassagePhase.OPEN;
        if (writePhase(level, manager, mine, target)) {
            // On ne va toucher au chunk de l'allié que sur transition : le lire
            // à chaque seconde ferait charger sa parcelle pour rien.
            writePhase(level, manager, ally, target);
            level.playSound(null, mine.passagePos, SoundEvents.END_PORTAL_SPAWN,
                    SoundSource.BLOCKS, 0.5f, 1.8f);
        }
    }

    private static void close(MinecraftServer server, TardisStateManager manager, ServerLevel level,
                              TardisData mine, @Nullable TardisData ally) {
        dropPortals(server, manager, mine);
        if (ally != null) {
            dropPortals(server, manager, ally);
        }
        // L'échéance se remet à zéro des deux côtés : la prochaine ouverture
        // rejouera son animation au lieu de percer d'un coup sur une valeur
        // restée en arrière.
        if (mine.passageOpenAt != 0L || (ally != null && ally.passageOpenAt != 0L)) {
            mine.passageOpenAt = 0L;
            if (ally != null) {
                ally.passageOpenAt = 0L;
            }
            manager.setDirty();
        }
        BlockPos base = mine.passagePos;
        if (base != null && writePhase(level, manager, mine, PassagePhase.CLOSED)) {
            level.playSound(null, base, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.7f, 1.6f);
        }
    }

    private static void dropPortals(MinecraftServer server, TardisStateManager manager, TardisData data) {
        if (data.passagePortalsActive || !data.passagePortalIds.isEmpty()) {
            ImmPtlCompat.removePassagePortals(server, data);
            manager.setDirty();
        }
    }

    /**
     * Impose une phase aux deux moitiés d'une arche, et rend {@code true} si
     * quelque chose a changé — c'est ce qui distingue une transition d'une
     * confirmation, et évite de rejouer un son à chaque seconde.
     *
     * <p>Le chunk est chargé au passage : l'arche d'un allié hors ligne doit
     * pouvoir s'ouvrir et se refermer, et sans cela l'écriture partirait dans le
     * vide. Si le bloc a disparu entre-temps, la position mémorisée est
     * nettoyée — le registre ne doit pas garder une adresse qui ne mène nulle
     * part.</p>
     */
    private static boolean writePhase(ServerLevel level, TardisStateManager manager,
                                      TardisData data, PassagePhase phase) {
        BlockPos base = data.passagePos;
        if (base == null) {
            return false;
        }
        // getBlockState ne charge pas le chunk ; getChunk si.
        level.getChunk(base);
        BlockState lower = level.getBlockState(base);
        if (!AllyPassageBlock.isPassage(lower)) {
            data.passagePos = null;
            manager.setDirty();
            return false;
        }
        boolean changed = writeOne(level, base, lower, phase);
        BlockState upper = level.getBlockState(base.above());
        if (AllyPassageBlock.isPassage(upper)) {
            changed |= writeOne(level, base.above(), upper, phase);
        }
        return changed;
    }

    private static boolean writeOne(ServerLevel level, BlockPos pos, BlockState state, PassagePhase phase) {
        if (state.getValue(AllyPassageBlock.PHASE) == phase) {
            return false;
        }
        // UPDATE_KNOWN_SHAPE court-circuite les shape updates : les deux moitiés
        // se surveillent l'une l'autre, et une mise à jour de forme au milieu de
        // l'écriture les ferait se détruire mutuellement.
        level.setBlock(pos, state.setValue(AllyPassageBlock.PHASE, phase),
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        return true;
    }

    // ------------------------------------------------------------------
    // Traversée de repli
    // ------------------------------------------------------------------

    /**
     * Fait traverser un joueur vers le passage de son allié — le repli quand
     * Immersive Portals est absent (phase {@link PassagePhase#OPEN}).
     *
     * <p>L'arrivée se fait devant le passage de l'autre, pas dedans : y déposer
     * le joueur le ferait ressortir aussitôt par le même passage.</p>
     */
    public static void cross(ServerPlayer player, BlockPos passageBase, DoubleBlockHalf half) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        BlockPos base = half == DoubleBlockHalf.UPPER ? passageBase.below() : passageBase;
        TardisStateManager manager = TardisStateManager.get(server);
        TardisData here = manager.findByPassage(base);
        if (here == null || here.ownerUuid == null) {
            return;
        }
        UUID ally = manager.allies().linkOf(here.ownerUuid);
        if (ally == null) {
            return;
        }
        TardisData there = manager.findByOwner(ally);
        if (there == null || there.passagePos == null) {
            return;
        }
        ServerLevel level = server.getLevel(ModDimensions.ENDER_WORLD);
        if (level == null) {
            return;
        }
        BlockPos arrival = there.passagePos.relative(there.passageFacing);
        player.setPortalCooldown(PORTAL_COOLDOWN_TICKS);
        player.changeDimension(new DimensionTransition(level, Vec3.atBottomCenterOf(arrival),
                Vec3.ZERO, there.passageFacing.toYRot(), 0.0f, DimensionTransition.DO_NOTHING));
        level.playSound(null, arrival, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.2f);
        player.displayClientMessage(
                Component.translatable("enderportals.message.passage_crossed", there.ownerName), true);
    }

    private AllyPassageHelper() {
    }
}
