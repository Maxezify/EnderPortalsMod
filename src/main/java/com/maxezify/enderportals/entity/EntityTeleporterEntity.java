package com.maxezify.enderportals.entity;

import com.maxezify.enderportals.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Le Téléporteur d'entité : une coque qui se pose au sol comme un bateau, et
 * qui part rejoindre son Atterrisseur avec ce qu'elle transporte.
 *
 * <p><b>C'est un bateau, et c'est tout l'intérêt.</b> Le mod n'a pas à
 * réimplémenter l'embarquement des créatures, ni la limite de deux passagers,
 * ni le fait qu'un enderman assis ne peut plus se téléporter dehors : ces
 * règles vivent dans {@link Boat} et dans le code de vanilla qui teste
 * {@code instanceof Boat}. En hériter les obtient toutes, exactement telles que
 * le joueur les connaît. Les réécrire aurait été refaire — moins bien — une
 * mécanique que le jeu tient déjà.</p>
 *
 * <p>Ce que cette classe ajoute tient en trois choses : la destination, le
 * témoin de proue — vert si le départ est possible, rouge sinon — et le clic
 * droit qui remplace la monte par le départ. On ne s'assoit pas dans un
 * téléporteur.</p>
 */
public class EntityTeleporterEntity extends Boat {

    /**
     * Le départ est-il possible ? Synchronisé parce que c'est le rendu qui s'en
     * sert — le témoin de proue, vert ou rouge.
     *
     * <p>Deux conditions, et deux seulement : une destination et un passager.
     * On aurait pu y ajouter « l'Atterrisseur est-il toujours là ? », mais lire
     * un bloc de l'Ender depuis l'Overworld obligerait à charger son chunk à
     * chaque tick — exactement le gel que la 0.17 a mis trois versions à
     * supprimer. Le témoin dit donc l'état de la machine, pas celui du terrain,
     * et le terrain se vérifie au départ.</p>
     *
     * <p>Il ne dit rien non plus de l'expérience du joueur : le prix se paie à
     * l'usage, et la coque ne sait pas qui viendra la cliquer.</p>
     */
    private static final EntityDataAccessor<Boolean> READY =
            SynchedEntityData.defineId(EntityTeleporterEntity.class, EntityDataSerializers.BOOLEAN);

    /**
     * La coque est-elle en train de charger son départ ? Synchronisé, comme le
     * témoin, parce que c'est le client qui joue l'anneau et les grains.
     *
     * <p>Un départ n'est pas instantané : entre le clic et le voyage, les chunks
     * d'arrivée se font générer, ce qui prend de zéro à plusieurs secondes. Ce
     * temps mort était jusqu'ici muet et immobile — la machine disparaissait
     * sans prévenir. Il devient la charge.</p>
     */
    private static final EntityDataAccessor<Boolean> CHARGING =
            SynchedEntityData.defineId(EntityTeleporterEntity.class, EntityDataSerializers.BOOLEAN);

    /**
     * Durée au-delà de laquelle une charge est abandonnée, en ticks.
     *
     * <p>La charge s'arrête normalement au voyage ou au refus. Reste le cas où
     * l'attente des chunks ne rend jamais la main — un monde d'arrivée retiré,
     * une exception avalée : sans cette échéance, la coque bourdonnerait
     * indéfiniment. Quinze secondes laissent largement le temps à la génération
     * la plus lente.</p>
     */
    private static final int CHARGE_TIMEOUT = 300;

    /** Une note de charge tous les tant de ticks. */
    private static final int HUM_PERIOD = 5;

    /** Grains de portail aspirés par tick pendant la charge. */
    private static final int CHARGE_STREAM = 2;

    /** Rayon d'où viennent ces grains, en blocs. */
    private static final double CHARGE_RADIUS = 1.15;

    /** Points des deux couronnes de salve. */
    private static final int BURST_POINTS = 14;

    /**
     * L'Atterrisseur visé, dans le monde de l'Ender, ou {@code null} tant que
     * la machine n'a pas été liée.
     *
     * <p>La position suffit : le monde d'arrivée est toujours celui de l'Ender,
     * et un Atterrisseur qui aurait disparu depuis se vérifie à l'arrivée.</p>
     */
    @Nullable
    private BlockPos lander;

    /** État du chargement au tick précédent, pour n'en sonner que les changements. */
    private boolean carried;
    /** Faux tant que le premier tick n'a pas eu lieu : au chargement, on ne sonne pas. */
    private boolean carriedKnown;

    /**
     * Ticks écoulés depuis le début de la charge, compté des deux côtés.
     *
     * <p>Le client le déduit du drapeau synchronisé plutôt que de le recevoir :
     * envoyer un compteur à chaque tick pour une animation coûterait un paquet
     * par tick et par machine, alors qu'un décalage d'un ou deux ticks ne se
     * voit pas.</p>
     */
    private int chargeAge;

    public EntityTeleporterEntity(EntityType<? extends EntityTeleporterEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(READY, false);
        builder.define(CHARGING, false);
    }

    /** Le témoin, lu par le rendu : vert si la coque peut partir, rouge sinon. */
    public boolean isReady() {
        return this.entityData.get(READY);
    }

    /** La coque charge-t-elle son départ ? */
    public boolean isCharging() {
        return this.entityData.get(CHARGING);
    }

    /** Ouvre ou ferme la charge. Sans effet si l'état ne change pas. */
    public void setCharging(boolean charging) {
        if (isCharging() != charging) {
            this.entityData.set(CHARGING, charging);
        }
    }

    /** Depuis combien de ticks la coque charge. Zéro si elle ne charge pas. */
    public int getChargeAge() {
        return chargeAge;
    }

    @Nullable
    public BlockPos getLander() {
        return lander;
    }

    public void setLander(@Nullable BlockPos lander) {
        this.lander = lander;
    }

    @Override
    public Item getDropItem() {
        return ModItems.ENTITY_TELEPORTER.get();
    }

    // ------------------------------------------------------------------
    // Cycle
    // ------------------------------------------------------------------

    /**
     * Le passage de « vide » à « occupé » sonne, et le témoin suit l'état.
     *
     * <p>C'est bien une <b>transition</b> qui est guettée, pas un état : une
     * créature à bord le reste, et rejouer le son à chaque tick serait
     * insupportable. {@link #carriedKnown} évite le cas tordu du chargement de
     * chunk, où une coque déjà pleine « embarquerait » à nouveau son passager
     * aux oreilles du joueur qui passe par là.</p>
     */
    @Override
    public void tick() {
        super.tick();
        boolean charging = isCharging();
        this.chargeAge = charging ? this.chargeAge + 1 : 0;
        if (this.level().isClientSide) {
            if (charging) {
                chargeParticles();
            }
            return;
        }
        if (charging) {
            if (this.chargeAge % HUM_PERIOD == 0) {
                // La note monte tant que la charge dure : c'est elle qui dit au
                // joueur que quelque chose se prépare, alors même que l'attente
                // des chunks n'a pas de durée prévisible.
                float pitch = 0.7f + Math.min(1.3f, this.chargeAge * 0.02f);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.55f, pitch);
            }
            if (this.chargeAge > CHARGE_TIMEOUT) {
                setCharging(false);
            }
        }
        boolean carrying = !this.getPassengers().isEmpty();
        if (carrying != this.carried) {
            if (carrying && this.carriedKnown) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.7f, 1.9f);
            }
            this.carried = carrying;
        }
        this.carriedKnown = true;

        boolean ready = carrying && this.lander != null;
        if (ready != this.isReady()) {
            this.entityData.set(READY, ready);
        }
    }

    /**
     * Les grains de la charge, côté client : la machine aspire de quoi partir.
     *
     * <p>Les particules de portail reçoivent la coque pour <b>destination</b> et
     * l'écart pour vitesse — la convention de {@code PortalParticle}, celle de
     * la téléportation d'un Enderman — si bien qu'elles convergent depuis une
     * couronne d'un bon mètre au lieu de retomber au hasard. Les étincelles, à
     * l'inverse, jaillissent des quatre cristaux : ce sont eux les émetteurs, et
     * la charge doit se lire sur eux.</p>
     */
    private void chargeParticles() {
        RandomSource random = this.level().random;
        double cx = this.getX();
        double cy = this.getY();
        double cz = this.getZ();
        for (int i = 0; i < CHARGE_STREAM; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            this.level().addParticle(ParticleTypes.PORTAL, cx, cy + 0.35, cz,
                    Math.cos(angle) * CHARGE_RADIUS,
                    random.nextDouble() * 1.2 - 0.1,
                    Math.sin(angle) * CHARGE_RADIUS);
        }
        if (this.chargeAge % 2 == 0) {
            // Un cristal sur quatre, à tour de rôle : l'étincelle tourne autour
            // de la coque au lieu de sortir toujours du même coin.
            int corner = (this.chargeAge / 2) % 4;
            double px = (corner < 2 ? -0.54 : 0.54);
            double pz = (corner % 2 == 0 ? -0.68 : 0.68);
            float yaw = this.getYRot() * ((float) Math.PI / 180.0f);
            double sin = Math.sin(-yaw);
            double cos = Math.cos(-yaw);
            this.level().addParticle(ParticleTypes.END_ROD,
                    cx + px * cos - pz * sin, cy + 0.6, cz + px * sin + pz * cos,
                    0.0, 0.06 + random.nextDouble() * 0.04, 0.0);
        }
    }

    /**
     * La salve du départ, sur le point que la coque vient de quitter.
     *
     * <p>Appelée depuis le serveur, et depuis lui seul : le voyage retire la
     * machine du monde de départ, il n'y aura plus personne sur place pour
     * l'émettre. Les grains de portail s'engouffrent — position au centre, écart
     * pour vitesse — pendant que les étincelles montent en colonne.</p>
     */
    public static void departureBurst(ServerLevel level, Vec3 at) {
        for (int i = 0; i < BURST_POINTS; i++) {
            double angle = i * (Math.PI * 2.0 / BURST_POINTS);
            double ox = Math.cos(angle);
            double oz = Math.sin(angle);
            level.sendParticles(ParticleTypes.PORTAL, at.x, at.y + 0.4, at.z, 0,
                    ox, 0.4, oz, 1.3);
            level.sendParticles(ParticleTypes.END_ROD, at.x + ox * 0.25, at.y + 0.2, at.z + oz * 0.25,
                    0, ox * 0.15, 1.0, oz * 0.15, 0.35);
        }
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.BLOCKS, 1.0f, 0.7f);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.END_PORTAL_FRAME_FILL,
                SoundSource.BLOCKS, 0.9f, 1.4f);
    }

    /**
     * La salve de l'arrivée, au-dessus de l'Atterrisseur : une couronne
     * d'étincelles qui s'écarte, et une colonne violette qui s'élève.
     */
    public static void arrivalBurst(ServerLevel level, Vec3 at) {
        for (int i = 0; i < BURST_POINTS; i++) {
            double angle = i * (Math.PI * 2.0 / BURST_POINTS);
            double ox = Math.cos(angle);
            double oz = Math.sin(angle);
            level.sendParticles(ParticleTypes.END_ROD, at.x + ox * 0.6, at.y + 0.1, at.z + oz * 0.6,
                    0, ox, 0.5, oz, 0.14);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x + ox * 0.3, at.y + 0.1, at.z + oz * 0.3,
                    0, ox * 0.2, 1.0, oz * 0.2, 0.09);
        }
    }

    /**
     * Le clic droit fait partir la machine — il ne fait jamais monter le joueur.
     *
     * <p>{@link Boat} ferait asseoir celui qui clique ; ce serait ici le geste
     * le plus courant transformé en son contraire, puisqu'on clique justement
     * pour envoyer la coque. Le joueur n'a d'ailleurs rien à faire à bord : ce
     * n'est pas un véhicule, c'est une machine.</p>
     *
     * <p>Machine vide, on la reprend en main — avec son lien. Sans quoi il
     * faudrait la casser pour la déplacer, et la relier à chaque voyage.</p>
     */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer server) {
            EntityTeleporterLogic.click(server, this);
        }
        return InteractionResult.CONSUME;
    }

    // ------------------------------------------------------------------
    // Persistance
    // ------------------------------------------------------------------

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        if (lander != null) {
            nbt.putIntArray("Lander", new int[]{lander.getX(), lander.getY(), lander.getZ()});
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        lander = null;
        if (nbt.contains("Lander", Tag.TAG_INT_ARRAY)) {
            int[] xyz = nbt.getIntArray("Lander");
            if (xyz.length == 3) {
                lander = new BlockPos(xyz[0], xyz[1], xyz[2]);
            }
        }
    }
}
