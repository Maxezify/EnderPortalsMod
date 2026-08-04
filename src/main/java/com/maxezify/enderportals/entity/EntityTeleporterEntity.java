package com.maxezify.enderportals.entity;

import com.maxezify.enderportals.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
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
 * témoin vert quand une créature est à bord, et le clic droit qui remplace la
 * monte par le départ. On ne s'assoit pas dans un téléporteur.</p>
 */
public class EntityTeleporterEntity extends Boat {

    /**
     * Une créature est-elle à bord ? Synchronisé parce que c'est le rendu qui
     * s'en sert — le témoin vert de la coque.
     */
    private static final EntityDataAccessor<Boolean> LOADED =
            SynchedEntityData.defineId(EntityTeleporterEntity.class, EntityDataSerializers.BOOLEAN);

    /**
     * L'Atterrisseur visé, dans le monde de l'Ender, ou {@code null} tant que
     * la machine n'a pas été liée.
     *
     * <p>La position suffit : le monde d'arrivée est toujours celui de l'Ender,
     * et un Atterrisseur qui aurait disparu depuis se vérifie à l'arrivée.</p>
     */
    @Nullable
    private BlockPos lander;

    public EntityTeleporterEntity(EntityType<? extends EntityTeleporterEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LOADED, false);
    }

    /** Le témoin vert, lu par le rendu. */
    public boolean isLoaded() {
        return this.entityData.get(LOADED);
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
     * Le passage de « vide » à « occupé » sonne et allume le témoin.
     *
     * <p>C'est bien une <b>transition</b> qui est guettée, pas un état : une
     * créature à bord le reste, et rejouer le son à chaque tick serait
     * insupportable.</p>
     */
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        boolean carrying = !this.getPassengers().isEmpty();
        if (carrying != this.isLoaded()) {
            this.entityData.set(LOADED, carrying);
            if (carrying) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.7f, 1.9f);
            }
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
