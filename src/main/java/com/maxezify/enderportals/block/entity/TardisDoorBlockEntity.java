package com.maxezify.enderportals.block.entity;

import com.maxezify.enderportals.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
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
        super(ModBlockEntities.TARDIS_DOOR, pos, state);
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
        return MathHelper.clamp(alpha, 0.0f, 1.0f);
    }

    public static void tick(World world, BlockPos pos, BlockState state, TardisDoorBlockEntity door) {
        door.age++;
        if (world.isClient) {
            boolean fading = door.dematerializing || door.age < FADE_IN_TICKS;
            if (fading && world.random.nextInt(2) == 0) {
                world.addParticle(ParticleTypes.REVERSE_PORTAL,
                        pos.getX() + world.random.nextDouble(),
                        pos.getY() + world.random.nextDouble() * 2.0,
                        pos.getZ() + world.random.nextDouble(),
                        0.0, 0.02, 0.0);
            }
            return;
        }
        if (door.dematerializing && door.age - door.dematStart >= FADE_OUT_TICKS) {
            // FORCE_STATE court-circuite les shape updates : sans lui, la moitié
            // orpheline serait retirée via Block#replace → breakBlock, qui joue
            // les particules et le son de casse du bloc.
            int flags = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;
            world.setBlockState(pos.up(), Blocks.AIR.getDefaultState(), flags);
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), flags);
        }
    }

    private void sync() {
        markDirty();
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.getChunkManager().markForUpdate(pos);
        }
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        tardisId = nbt.containsUuid("TardisId") ? nbt.getUuid("TardisId") : null;
        interior = nbt.getBoolean("Interior");
        age = nbt.getInt("Age");
        dematerializing = nbt.getBoolean("Dematerializing");
        dematStart = nbt.getInt("DematStart");
        portalActive = nbt.getBoolean("PortalActive");
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        if (tardisId != null) {
            nbt.putUuid("TardisId", tardisId);
        }
        nbt.putBoolean("Interior", interior);
        // Un rechargement de chunk ne doit pas rejouer le fondu d'apparition.
        nbt.putInt("Age", dematerializing ? age : Math.min(age, FADE_IN_TICKS));
        nbt.putBoolean("Dematerializing", dematerializing);
        nbt.putInt("DematStart", dematStart);
        nbt.putBoolean("PortalActive", portalActive);
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }
}
