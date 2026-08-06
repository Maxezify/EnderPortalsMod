package com.maxezify.enderportals.block.entity;

import com.maxezify.enderportals.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
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

    /**
     * Durée des fondus, en ticks : 1,75 s pour apparaître, 1,5 s pour partir.
     *
     * <p>C'était le double avant la 0.26.0, et c'était trop long — on
     * attendait devant sa porte. Le passage se joue maintenant en deux fois
     * moins de temps, à charge pour l'onde, les grains et les deux notes de
     * ponctuation d'occuper l'œil : un fondu court et dense se regarde, un fondu
     * long et nu se subit.</p>
     */
    public static final int FADE_IN_TICKS = 35;
    public static final int FADE_OUT_TICKS = 30;

    /**
     * Vitesse angulaire de la pulsation d'opacité. Doublée en même temps que les
     * durées étaient divisées : la porte bat le même nombre de fois, deux fois
     * plus vite — c'est ce battement qui fait la matérialisation, pas sa durée.
     */
    private static final float PULSE = 0.9f;

    /** Creux maximal du battement d'opacité, atteint au tout début du fondu. */
    private static final float PULSE_DEPTH = 0.55f;

    /** Grains d'énergie émis par tick pendant un fondu. */
    private static final int STREAM_PER_TICK = 4;
    /** Points de la couronne d'étincelles des deux salves. */
    private static final int RING_POINTS = 16;

    @Nullable
    private UUID tardisId;
    private boolean interior;
    private int age = FADE_IN_TICKS;
    private boolean dematerializing;
    private int dematStart;

    /** Un portail Immersive Portals couvre-t-il l'embrasure ? (synchronisé) */
    private boolean portalActive;

    /** Pseudo du propriétaire, affiché sur le panneau de la porte (synchronisé). */
    private String ownerName = "";

    public TardisDoorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TARDIS_DOOR.get(), pos, state);
    }

    public void initialize(UUID tardisId, boolean interior, boolean fadeIn, String ownerName) {
        this.tardisId = tardisId;
        this.interior = interior;
        this.age = fadeIn ? 0 : FADE_IN_TICKS;
        this.dematerializing = false;
        this.ownerName = ownerName == null ? "" : ownerName;
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

    public String getOwnerName() {
        return ownerName;
    }

    /** Appelé côté serveur quand les portails Immersive Portals apparaissent/disparaissent. */
    public void setPortalActive(boolean portalActive) {
        if (this.portalActive != portalActive) {
            this.portalActive = portalActive;
            sync();
        }
    }

    /**
     * Avancement du fondu en cours, de 0 (il commence) à 1 (il est fini). Vaut
     * 1 hors fondu. Sert au rendu de l'onde comme à l'émission des grains.
     */
    public float getFadeProgress(float tickDelta) {
        float t = age + tickDelta;
        return Mth.clamp(dematerializing
                ? (t - dematStart) / FADE_OUT_TICKS
                : t / FADE_IN_TICKS, 0.0f, 1.0f);
    }

    /** Temps écoulé depuis la pose, en ticks fractionnaires : la phase des oscillations. */
    public float getFadeTime(float tickDelta) {
        return age + tickDelta;
    }

    /**
     * Opacité de la porte pour le rendu, avec une pulsation façon
     * matérialisation de TARDIS pendant les fondus.
     *
     * <p>L'amplitude du battement suit la distance qui reste à parcourir : forte
     * quand la porte n'est qu'une silhouette, nulle une fois qu'elle a pris.
     * Une amplitude constante — ce qu'on avait avant la 0.26.0 — laissait la
     * porte à 40 % d'opacité à l'avant-dernière image, puis à 100 % à la
     * suivante : le fondu ne se terminait pas, il claquait. Le battement
     * s'inverse à la dématérialisation, où l'instabilité gagne à mesure que la
     * porte se défait.</p>
     */
    public float getAlpha(float tickDelta) {
        float progress = getFadeProgress(tickDelta);
        float base = dematerializing ? 1.0f - progress : progress;
        float depth = PULSE_DEPTH * (1.0f - base);
        // Dans [1 - depth, 1] : le battement creuse l'opacité, il ne la dépasse
        // jamais, si bien que la porte ne peut pas « rebondir » au-delà.
        float shimmer = 1.0f - depth * (0.5f - 0.5f * Mth.sin((age + tickDelta) * PULSE));
        return Mth.clamp(base * shimmer, 0.0f, 1.0f);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TardisDoorBlockEntity door) {
        boolean fading = door.dematerializing || door.age < FADE_IN_TICKS;
        // L'âge se fige une fois la matérialisation terminée : il ne sert plus
        // à rien passé ce point, et il grimpait sans fin (dérive du compteur,
        // perte de précision de age + tickDelta en float à la longue).
        if (fading) {
            door.age++;
        }
        if (level.isClientSide) {
            if (fading) {
                streamParticles(level, pos, door);
            }
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        // L'atterrissage : la porte vient de finir sa prise. Les deux salves
        // ponctuent le fondu à ses instants forts — sans elles, un fondu deux
        // fois plus court se termine sans qu'on l'ait vu se terminer.
        if (fading && !door.dematerializing && door.age >= FADE_IN_TICKS) {
            // Onde de choc : les étincelles partent de la coque vers l'extérieur.
            burst(serverLevel, pos, ParticleTypes.END_ROD, 0.55, 0.12);
            level.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0f, 0.6f);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.7f, 0.7f);
        }
        if (door.dematerializing && door.age - door.dematStart >= FADE_OUT_TICKS) {
            // La salve du départ part d'ici, et non du tick client : les blocs
            // disparaissent dans la foulée, il n'y aura plus de block entity
            // pour l'émettre. Rayon nul, grande vitesse : les grains de portail
            // convergent, ils s'engouffrent donc au point que la porte vient de
            // quitter au lieu d'en jaillir.
            burst(serverLevel, pos, ParticleTypes.PORTAL, 0.0, 1.1);
            level.playSound(null, pos, SoundEvents.ENDER_EYE_DEATH, SoundSource.BLOCKS, 0.9f, 1.3f);
            // UPDATE_KNOWN_SHAPE court-circuite les shape updates : sans lui, la
            // moitié orpheline serait retirée via Block#updateOrDestroy →
            // destroyBlock, qui joue les particules et le son de casse du bloc.
            int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
            level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), flags);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), flags);
        }
    }

    /**
     * Les grains d'énergie du fondu, côté client — la porte ne se contente pas
     * de s'estomper, elle se rassemble ou se disperse.
     *
     * <p>À la matérialisation, la matière converge : les particules de portail
     * reçoivent la porte pour <b>destination</b> et l'écart pour vitesse (c'est
     * la convention de {@code PortalParticle}, celle de la téléportation d'un
     * Enderman), et la couronne se resserre à mesure que la porte prend. À la
     * dématérialisation, tout repart vers l'extérieur et monte.</p>
     */
    private static void streamParticles(Level level, BlockPos pos, TardisDoorBlockEntity door) {
        RandomSource random = level.random;
        boolean leaving = door.dematerializing;
        float progress = door.getFadeProgress(0.0f);
        // Large au début d'une apparition, resserré à la fin ; l'inverse en
        // partant.
        double radius = 0.35 + 1.15 * (leaving ? progress : 1.0f - progress);
        double cx = pos.getX() + 0.5;
        double cz = pos.getZ() + 0.5;
        for (int i = 0; i < STREAM_PER_TICK; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double y = pos.getY() + random.nextDouble() * 2.0;
            double ox = Math.cos(angle) * radius;
            double oz = Math.sin(angle) * radius;
            if (leaving) {
                level.addParticle(ParticleTypes.REVERSE_PORTAL, cx + ox * 0.35, y, cz + oz * 0.35,
                        ox * 0.05, 0.06, oz * 0.05);
            } else {
                level.addParticle(ParticleTypes.PORTAL, cx, y, cz, ox, 0.0, oz);
            }
        }
        // Quelques étincelles franches par-dessus le voile violet : elles
        // donnent au fondu son grain net, que le portail seul n'a pas.
        if (random.nextInt(3) == 0) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double ox = Math.cos(angle);
            double oz = Math.sin(angle);
            double speed = leaving ? 0.12 : -0.12;
            level.addParticle(ParticleTypes.END_ROD, cx + ox * 0.6,
                    pos.getY() + random.nextDouble() * 2.0, cz + oz * 0.6,
                    ox * speed, 0.02, oz * speed);
        }
    }

    /**
     * Couronne de grains émise depuis le serveur, à hauteur d'homme.
     *
     * <p>{@code radius} place les points de la couronne, {@code speed} règle le
     * vecteur passé à chaque grain. Ce que ce vecteur signifie dépend du type :
     * une vraie vitesse pour une étincelle, l'écart au point d'arrivée pour un
     * grain de portail — d'où deux réglages très différents selon la salve.</p>
     */
    private static void burst(ServerLevel level, BlockPos pos, ParticleOptions type,
                              double radius, double speed) {
        for (int i = 0; i < RING_POINTS; i++) {
            double angle = i * (Math.PI * 2.0 / RING_POINTS);
            double ox = Math.cos(angle);
            double oz = Math.sin(angle);
            // count = 0 : les trois « distances » sont alors le vecteur du grain,
            // ce qui est le seul moyen d'orienter chaque point de la couronne.
            level.sendParticles(type, pos.getX() + 0.5 + ox * radius, pos.getY() + 1.0,
                    pos.getZ() + 0.5 + oz * radius, 0, ox, 0.3, oz, speed);
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
        ownerName = nbt.getString("OwnerName");
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
        nbt.putString("OwnerName", ownerName);
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
