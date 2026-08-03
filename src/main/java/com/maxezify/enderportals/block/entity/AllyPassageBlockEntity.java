package com.maxezify.enderportals.block.entity;

import com.maxezify.enderportals.ModBlockEntities;
import com.maxezify.enderportals.block.AllyPassageBlock;
import com.maxezify.enderportals.block.PassagePhase;
import com.maxezify.enderportals.tardis.AllyPassageHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Block entity de la moitié basse du Passage des Alliés.
 *
 * <p>Il ne décide de rien, et c'est délibéré : tout l'état de la paire d'arches
 * se calcule dans {@link AllyPassageHelper#reconcile}, à partir du lien noté
 * côté serveur. Ce block entity n'apporte que trois choses, et rien d'autre —
 * un battement de cœur qui appelle la réconciliation une fois par seconde, le
 * pseudo à afficher sur la façade close, et l'ancrage du
 * {@code AllyPassageRenderer} qui dessine le caisson creux.</p>
 *
 * <p>Le battement est ce qui rend le passage increvable. La 0.15.0 pilotait
 * l'arche par une transition unique — un tick programmé — et une transition
 * unique n'a pas de seconde chance : ratée, l'arche restait figée, pleine sous
 * son portail. Ici, n'importe quel raté se corrige à la seconde suivante.</p>
 */
public class AllyPassageBlockEntity extends BlockEntity {

    /** Période de réconciliation, en ticks. Une seconde suffit largement. */
    private static final int RECONCILE_PERIOD = 20;

    /** Ticks avant la prochaine réconciliation (serveur). */
    private int untilReconcile = 1;

    /** Pseudo du propriétaire, affiché sur la façade close (synchronisé). */
    private String ownerName = "";

    /** Âge, pour l'ondulation du voile côté client. */
    private int age;

    public AllyPassageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALLY_PASSAGE.get(), pos, state);
    }

    public String getOwnerName() {
        return ownerName;
    }

    /** Âge continu pour l'animation du voile, interpolé sur la sous-frame. */
    public float getAge(float tickDelta) {
        return age + tickDelta;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AllyPassageBlockEntity passage) {
        if (level.isClientSide) {
            // L'âge ne sert qu'au voile ; il se fige quand il n'y en a pas, pour
            // ne pas dériver sans fin (précision de age + tickDelta en float).
            PassagePhase phase = state.getValue(AllyPassageBlock.PHASE);
            if (phase == PassagePhase.OPENING || phase == PassagePhase.OPEN) {
                passage.age++;
            }
            return;
        }
        if (!(level instanceof ServerLevel serverLevel) || --passage.untilReconcile > 0) {
            return;
        }
        passage.untilReconcile = RECONCILE_PERIOD;
        passage.setOwnerName(AllyPassageHelper.reconcileAt(serverLevel, pos));
    }

    private void setOwnerName(String name) {
        String value = name == null ? "" : name;
        if (!ownerName.equals(value)) {
            ownerName = value;
            setChanged();
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.getChunkSource().blockChanged(worldPosition);
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        ownerName = nbt.getString("OwnerName");
        // Le compte à rebours n'est pas persisté : au chargement, la première
        // réconciliation doit venir vite — c'est là que l'arche rattrape ce qui
        // a pu changer pendant qu'elle dormait.
        untilReconcile = 1;
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        nbt.putString("OwnerName", ownerName);
    }

    @Override
    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
