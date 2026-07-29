package com.maxezify.enderportals.tardis;

import com.maxezify.enderportals.ModBlocks;
import com.maxezify.enderportals.ModDimensions;
import com.maxezify.enderportals.block.AllyPassageBlock;
import com.maxezify.enderportals.block.PassagePhase;
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
 * L'ouverture, la fermeture et la traversée du Passage des Alliés.
 *
 * <p>Le passage d'un joueur est une donnée du serveur ({@link TardisData}), pas
 * du bloc : c'est ce qui permet d'ouvrir le passage d'un allié <b>hors ligne</b>
 * ou dont la parcelle n'est pas chargée. Les chunks concernés sont donc chargés
 * à la demande au moment d'écrire l'état des blocs — sans cela, l'écriture
 * partirait dans le vide et les deux passages se désaccorderaient.</p>
 */
public final class AllyPassageHelper {

    /** Durée de l'animation d'ouverture, en ticks. */
    public static final int OPENING_TICKS = 60;

    private static final int PORTAL_COOLDOWN_TICKS = 60;

    /** Ouvre les deux passages d'une paire fraîchement connectée. */
    public static void openBoth(MinecraftServer server, UUID a, UUID b) {
        setPhase(server, a, PassagePhase.OPENING);
        setPhase(server, b, PassagePhase.OPENING);
    }

    /** Referme les deux passages d'une paire. */
    public static void closeBoth(MinecraftServer server, UUID a, @Nullable UUID b) {
        setPhase(server, a, PassagePhase.CLOSED);
        if (b != null) {
            setPhase(server, b, PassagePhase.CLOSED);
        }
    }

    /**
     * Impose une phase aux deux moitiés du passage de ce joueur, s'il en a posé
     * un. Sans effet — et sans dommage — si le bloc a été cassé entre-temps :
     * la position mémorisée est alors nettoyée.
     */
    public static void setPhase(MinecraftServer server, UUID owner, PassagePhase phase) {
        TardisStateManager manager = TardisStateManager.get(server);
        TardisData data = manager.findByOwner(owner);
        if (data == null || data.passagePos == null) {
            return;
        }
        ServerLevel level = server.getLevel(ModDimensions.ENDER_WORLD);
        if (level == null) {
            return;
        }
        BlockPos base = data.passagePos;
        // getBlockState ne charge pas le chunk ; getChunk si, et c'est
        // indispensable pour une parcelle que personne n'occupe.
        level.getChunk(base);
        BlockState lower = level.getBlockState(base);
        if (!lower.is(ModBlocks.ALLY_PASSAGE.get())) {
            data.passagePos = null;
            manager.setDirty();
            return;
        }
        writePhase(level, base, lower, phase);
        BlockState upper = level.getBlockState(base.above());
        if (upper.is(ModBlocks.ALLY_PASSAGE.get())) {
            writePhase(level, base.above(), upper, phase);
        }
        if (phase == PassagePhase.OPENING) {
            level.scheduleTick(base, ModBlocks.ALLY_PASSAGE.get(), OPENING_TICKS);
            level.playSound(null, base, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 0.9f, 1.4f);
        } else if (phase == PassagePhase.CLOSED) {
            level.playSound(null, base, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.7f, 1.6f);
        }
    }

    private static void writePhase(ServerLevel level, BlockPos pos, BlockState state, PassagePhase phase) {
        if (state.getValue(AllyPassageBlock.PHASE) != phase) {
            level.setBlock(pos, state.setValue(AllyPassageBlock.PHASE, phase),
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    /**
     * Fait traverser un joueur vers le passage de son allié.
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
