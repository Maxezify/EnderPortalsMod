package com.maxezify.enderportals.block.entity;

import com.maxezify.enderportals.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Block entity de la moitié basse de la porte du TARDIS. Porte l'identité du
 * TARDIS, le côté (extérieur/intérieur) et l'avancement du fondu de
 * matérialisation / dématérialisation.
 */
public class TardisDoorBlockEntity extends BlockEntity {

    public static final int FADE_IN_TICKS = 70;
    public static final int FADE_OUT_TICKS = 60;

    @Nullable
    private UUID tardisId;
    private boolean interior;
    private int age = FADE_IN_TICKS;
    private boolean dematerializing;
    private int dematStart;

    /** Un portail Immersive Portals couvre-t-il l'embrasure ? (synchronisé) */
    private boolean portalActive;

    public TardisDoorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TARDIS_DOOR.get(), pos, state);
    }

    public void initialize(UUID tardisId, boolean interior, boolean fadeIn) {
        this.tardisId = tardisId;
        this.interior = interior;
        this.age = fadeIn ? 0 : FADE_IN_TICKS;
        this.dematerializing = false;
        sync();
    }

    public void startDematerialize() {
        if (!dematerializing) {
            dematerializing = true;
            dematStart = age;
            sync();
        }
    }

    @Nullable
    public UUID getTardisId() {
        return tardisId;
    }

    public boolean isInterior() {
        return interior;
    }

    public boolean isDematerializing() {
        return dematerializing;
    }

    public boolean isPortalActive() {
        return portalActive;
    }

    /** Appelé côté serveur quand les portails Immersive Portals apparaissent/disparaissent. */
    public void setPortalActive(boolean portalActive) {
        if (this.portalActive != portalActive) {
            this.portalActive = portalActive;
            sync();
        }
    }

    /**
     * Opacité de la porte pour le rendu, avec une pulsation façon
     * matérialisation de TARDIS pendant les fondus.
     */
    public float getAlpha(float tickDelta) {
        float t = age + tickDelta;
        float alpha;
        if (dematerializing) {
            alpha = 1.0f - (t - dematStart) / FADE_OUT_TICKS;
        } else {
            alpha = Math.min(1.0f, t / FADE_IN_TICKS);
        }
        if (alpha < 1.0f) {
            alpha *= 0.7f + 0.3f * (float) Math.sin(t * 0.45f);
        }
        return Mth.clamp(alpha, 0.0f, 1.0f);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TardisDoorBlockEntity door) {
        door.age++;
        if (level.isClientSide) {
            boolean fading = door.dematerializing || door.age < FADE_IN_TICKS;
            if (fading && level.random.nextInt(2) == 0) {
                level.addParticle(ParticleTypes.REVERSE_PORTAL,
                        pos.getX() + level.random.nextDouble(),
                        pos.getY() + level.random.nextDouble() * 2.0,
                        pos.getZ() + level.random.nextDouble(),
                        0.0, 0.02, 0.0);
            }
            return;
        }
        if (door.dematerializing && door.age - door.dematStart >= FADE_OUT_TICKS) {
            // UPDATE_KNOWN_SHAPE court-circuite les shape updates : sans lui, la
            // moitié orpheline serait retirée via Block#updateOrDestroy →
            // destroyBlock, qui joue les particules et le son de casse du bloc.
            int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
            level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), flags);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), flags);
        }
    }

    private void sync() {
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.getChunkSource().blockChanged(worldPosition);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        tardisId = nbt.hasUUID("TardisId") ? nbt.getUUID("TardisId") : null;
        interior = nbt.getBoolean("Interior");
        age = nbt.getInt("Age");
        dematerializing = nbt.getBoolean("Dematerializing");
        dematStart = nbt.getInt("DematStart");
        portalActive = nbt.getBoolean("PortalActive");
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        if (tardisId != null) {
            nbt.putUUID("TardisId", tardisId);
        }
        nbt.putBoolean("Interior", interior);
        // Un rechargement de chunk ne doit pas rejouer le fondu d'apparition.
        nbt.putInt("Age", dematerializing ? age : Math.min(age, FADE_IN_TICKS));
        nbt.putBoolean("Dematerializing", dematerializing);
        nbt.putInt("DematStart", dematStart);
        nbt.putBoolean("PortalActive", portalActive);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
