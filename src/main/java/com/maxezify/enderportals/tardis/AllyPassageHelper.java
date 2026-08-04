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

import java.util.List;
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
 * partir du lien porté par l'arche et d'une <b>échéance commune aux deux
 * arches</b> ({@link PassageData#openAt}), et elle s'écrit des <b>deux côtés à
 * la fois</b>. Deux propriétés en découlent, et ce sont elles qui comptent :</p>
 * <ul>
 *   <li>les deux arches ne peuvent pas diverger — quel que soit le côté qui
 *       calcule, il calcule la même chose et l'écrit aux deux ;</li>
 *   <li>aucun état n'est définitif — appelée une fois par seconde par chaque
 *       arche chargée, la réconciliation rattrape n'importe quel raté.</li>
 * </ul>
 *
 * <p>Depuis la 0.19.0 tout ceci se raisonne <b>par arche</b> et non par joueur :
 * un joueur en a autant qu'il a d'amis reliés, et deux de ses arches n'ont rien
 * à voir l'une avec l'autre.</p>
 */
public final class AllyPassageHelper {

    /** Durée de l'animation d'ouverture, en ticks. */
    public static final int OPENING_TICKS = 60;

    private static final int PORTAL_COOLDOWN_TICKS = 60;

    // ------------------------------------------------------------------
    // Entrées : le panneau, puis les arches elles-mêmes
    // ------------------------------------------------------------------

    /**
     * Le lien vient de s'ouvrir entre deux arches nommément désignées : les deux
     * s'engagent.
     *
     * <p>L'échéance est remise à zéro des deux côtés avant tout calcul, ce qui
     * garantit que l'animation d'ouverture est bien rejouée — y compris si les
     * deux arches portaient par hasard la même valeur d'une connexion
     * antérieure.</p>
     */
    public static void openBoth(MinecraftServer server, PassageData mine, PassageData theirs) {
        mine.openAt = 0L;
        theirs.openAt = 0L;
        TardisStateManager.get(server).setDirty();
        reconcile(server, mine);
        reconcile(server, theirs);
    }

    /** Une arche dont le lien vient d'être dénoué, et son ancien pair. */
    public static void closeBoth(MinecraftServer server, @Nullable PassageData mine,
                                 @Nullable PassageData theirs) {
        if (mine != null) {
            reconcile(server, mine);
        }
        if (theirs != null) {
            reconcile(server, theirs);
        }
    }

    /**
     * L'arche vient d'être cassée : elle quitte le registre, son lien se dénoue,
     * et celle d'en face se referme — sans quoi l'allié garderait une arche
     * ouverte sur une adresse vide.
     */
    public static void demolish(MinecraftServer server, BlockPos base) {
        TardisStateManager manager = TardisStateManager.get(server);
        TardisData owner = manager.findByPassage(base);
        if (owner == null) {
            return;
        }
        PassageData passage = TardisStateManager.passageAt(owner, base);
        if (passage != null) {
            dissolve(server, manager, owner, passage);
        }
    }

    /**
     * Retire une arche du registre et referme la paire.
     *
     * <p>Les portails sont retirés depuis <b>notre</b> arche avant qu'elle ne
     * disparaisse : ils y sont inscrits aussi, et une arche jetée avec ses
     * identifiants laisserait deux entités que plus personne ne saurait
     * retrouver. La réconciliation d'en face fait le reste.</p>
     */
    private static void dissolve(MinecraftServer server, TardisStateManager manager,
                                 TardisData owner, PassageData passage) {
        PassageData theirs = pairOf(manager, owner, passage);
        dropPortals(server, manager, passage);
        manager.unlink(owner, passage);
        manager.removePassage(owner, passage.pos);
        if (theirs != null) {
            theirs.openAt = 0L;
            reconcile(server, theirs);
        }
    }

    /**
     * Toutes les arches de ce joueur. À réserver aux changements qui ne
     * désignent aucune arche en particulier — un allié délogé, par exemple, dont
     * on ne sait pas par quel couloir il passait.
     */
    public static void reconcileAll(MinecraftServer server, UUID owner) {
        TardisData data = TardisStateManager.get(server).findByOwner(owner);
        if (data == null) {
            return;
        }
        // Copie : la réconciliation peut retirer une arche dont le bloc a disparu.
        for (PassageData passage : List.copyOf(data.passages)) {
            reconcile(server, passage);
        }
    }

    /** Réconciliation depuis l'arche elle-même, une fois par seconde. */
    public static void reconcileAt(ServerLevel level, BlockPos base) {
        MinecraftServer server = level.getServer();
        TardisStateManager manager = TardisStateManager.get(server);
        TardisData mine = manager.findByPassage(base);
        if (mine == null) {
            return;
        }
        PassageData passage = TardisStateManager.passageAt(mine, base);
        if (passage != null) {
            reconcile(server, passage);
        }
    }

    // ------------------------------------------------------------------
    // Le calcul
    // ------------------------------------------------------------------

    /**
     * Aligne cette arche — et celle de son allié — sur le lien qu'elle porte.
     *
     * <p>Idempotente : appelée en boucle sur un état déjà correct, elle ne
     * touche à rien. C'est ce qui permet de l'appeler à la fois sur décision du
     * panneau et à intervalle régulier.</p>
     */
    public static void reconcile(MinecraftServer server, PassageData mine) {
        TardisStateManager manager = TardisStateManager.get(server);
        ServerLevel level = server.getLevel(ModDimensions.ENDER_WORLD);
        if (level == null) {
            return;
        }
        TardisData owner = manager.findByPassage(mine.pos);
        PassageData theirs = pairOf(manager, owner, mine);

        if (theirs == null) {
            // Invariant : une arche liée a toujours son vis-à-vis, parce que
            // nouer et dénouer se font des deux côtés à la fois. Si le lien
            // pend malgré tout dans le vide, on le dénoue plutôt que de le
            // laisser survivre — une arche close est une arche libre, et c'est
            // ce qui permet de dire « close = personne » sans jamais mentir.
            manager.unlink(owner, mine);
            close(server, manager, level, mine, null);
            return;
        }

        long now = level.getGameTime();
        if (mine.openAt == 0L || theirs.openAt != mine.openAt) {
            // L'échéance est commune aux deux arches : c'est elle qui garantit
            // qu'elles perceront au même tick, y compris quand une seule des
            // deux parcelles tourne.
            long deadline = now + OPENING_TICKS;
            mine.openAt = deadline;
            theirs.openAt = deadline;
            manager.setDirty();
            // Invariant : une arche pleine ne porte jamais de portail. Pendant
            // les trois secondes d'animation elle l'est — un portail devant un
            // bloc plein, c'est une vue traversante qu'on ne peut pas franchir.
            dropPortals(server, manager, mine);
            dropPortals(server, manager, theirs);
            writePhase(level, manager, theirs, PassagePhase.OPENING);
            if (writePhase(level, manager, mine, PassagePhase.OPENING)) {
                level.playSound(null, mine.pos, SoundEvents.END_PORTAL_FRAME_FILL,
                        SoundSource.BLOCKS, 0.9f, 1.4f);
            }
            return;
        }
        if (now < mine.openAt) {
            writePhase(level, manager, mine, PassagePhase.OPENING);
            return;
        }

        // L'échéance est passée : la paire de portails doit exister. Elle est
        // recréée à la demande — si les entités ont disparu (rechargement du
        // monde, purge), tryCreatePassagePortals les refait.
        boolean wasActive = mine.portalsActive;
        int portalCount = mine.portalIds.size();
        boolean through = ImmPtlCompat.tryCreatePassagePortals(server, mine, theirs);
        if (wasActive != mine.portalsActive || portalCount != mine.portalIds.size()) {
            manager.setDirty();
        }
        PassagePhase target = through ? PassagePhase.THROUGH : PassagePhase.OPEN;
        if (writePhase(level, manager, mine, target)) {
            // On ne va toucher au chunk de l'allié que sur transition : le lire
            // à chaque seconde ferait charger sa parcelle pour rien.
            writePhase(level, manager, theirs, target);
            level.playSound(null, mine.pos, SoundEvents.END_PORTAL_SPAWN,
                    SoundSource.BLOCKS, 0.5f, 1.8f);
        }
    }

    /**
     * L'arche d'en face, si le lien est bien noué des deux côtés.
     *
     * <p>Un lien à sens unique ne compte pas : l'allié a pu casser son arche, et
     * l'on se retrouve alors avec une destination qui ne mène nulle part. Rendre
     * {@code null} referme la nôtre, ce qui est la seule chose honnête à
     * faire.</p>
     */
    @Nullable
    private static PassageData pairOf(TardisStateManager manager, @Nullable TardisData owner,
                                      PassageData mine) {
        if (owner == null || owner.ownerUuid == null || mine.ally == null) {
            return null;
        }
        return TardisStateManager.passageTo(manager.findByOwner(mine.ally), owner.ownerUuid);
    }

    private static void close(MinecraftServer server, TardisStateManager manager, ServerLevel level,
                              PassageData mine, @Nullable PassageData theirs) {
        dropPortals(server, manager, mine);
        if (theirs != null) {
            dropPortals(server, manager, theirs);
        }
        // L'échéance se remet à zéro des deux côtés : la prochaine ouverture
        // rejouera son animation au lieu de percer d'un coup sur une valeur
        // restée en arrière.
        if (mine.openAt != 0L || (theirs != null && theirs.openAt != 0L)) {
            mine.openAt = 0L;
            if (theirs != null) {
                theirs.openAt = 0L;
            }
            manager.setDirty();
        }
        if (writePhase(level, manager, mine, PassagePhase.CLOSED)) {
            level.playSound(null, mine.pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.7f, 1.6f);
        }
    }

    private static void dropPortals(MinecraftServer server, TardisStateManager manager,
                                    PassageData passage) {
        if (passage.portalsActive || !passage.portalIds.isEmpty()) {
            ImmPtlCompat.removePassagePortals(server, passage);
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
     * vide. Si le bloc a disparu entre-temps, l'arche est retirée du registre —
     * il ne doit pas garder une adresse qui ne mène nulle part.</p>
     */
    private static boolean writePhase(ServerLevel level, TardisStateManager manager,
                                      PassageData passage, PassagePhase phase) {
        BlockPos base = passage.pos;
        // getBlockState ne charge pas le chunk ; getChunk si.
        level.getChunk(base);
        BlockState lower = level.getBlockState(base);
        if (!AllyPassageBlock.isPassage(lower)) {
            forgetVanished(level.getServer(), manager, passage);
            return false;
        }
        boolean changed = writeOne(level, base, lower, phase);
        BlockState upper = level.getBlockState(base.above());
        if (AllyPassageBlock.isPassage(upper)) {
            changed |= writeOne(level, base.above(), upper, phase);
        }
        return changed;
    }

    /**
     * Une arche dont le bloc a disparu sans passer par {@code onRemove} — monde
     * édité hors du jeu, /setblock, explosion mal rattrapée. On la retire, et on
     * dénoue son lien : sans quoi l'allié garderait une arche ouverte sur une
     * adresse vide.
     */
    private static void forgetVanished(MinecraftServer server, TardisStateManager manager,
                                       PassageData passage) {
        TardisData owner = manager.findByPassage(passage.pos);
        if (owner != null) {
            dissolve(server, manager, owner, passage);
        }
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
     * Fait traverser un joueur vers l'arche d'en face — le repli quand Immersive
     * Portals est absent (phase {@link PassagePhase#OPEN}).
     *
     * <p>L'arrivée se fait devant l'arche de l'autre, pas dedans : y déposer le
     * joueur le ferait ressortir aussitôt par le même passage.</p>
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
        PassageData passage = TardisStateManager.passageAt(here, base);
        if (passage == null || passage.ally == null) {
            return;
        }
        PassageData arrival = TardisStateManager.passageTo(
                manager.findByOwner(passage.ally), here.ownerUuid);
        ServerLevel level = server.getLevel(ModDimensions.ENDER_WORLD);
        if (arrival == null || level == null) {
            return;
        }
        BlockPos landing = arrival.pos.relative(arrival.facing);
        player.setPortalCooldown(PORTAL_COOLDOWN_TICKS);
        player.changeDimension(new DimensionTransition(level, Vec3.atBottomCenterOf(landing),
                Vec3.ZERO, arrival.facing.toYRot(), 0.0f, DimensionTransition.DO_NOTHING));
        level.playSound(null, landing, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.2f);
        player.displayClientMessage(Component.translatable("enderportals.message.passage_crossed",
                manager.nameOf(passage.ally)), true);
    }

    private AllyPassageHelper() {
    }
}
