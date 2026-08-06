package com.maxezify.enderportals.entity;

import com.maxezify.enderportals.ModBlocks;
import com.maxezify.enderportals.ModComponents;
import com.maxezify.enderportals.ModDimensions;
import com.maxezify.enderportals.ModItems;
import com.maxezify.enderportals.tardis.EnderChunks;
import com.maxezify.enderportals.tardis.EnderXp;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Ce que fait un clic droit sur un Téléporteur d'entité, et comment la coque
 * rejoint son Atterrisseur.
 *
 * <p>Le départ se joue en deux temps, et c'est délibéré. Au clic, on ne fait que
 * <b>demander les chunks d'arrivée</b> ; le voyage n'a lieu qu'une fois qu'ils
 * sont là. Déposer une créature dans un chunk non généré reviendrait à la
 * confier à un monde qui n'existe pas encore, et le seul moyen de forcer sa
 * génération sur-le-champ serait d'arrêter le fil du serveur — le gel que la
 * 0.17 a mis trois versions à supprimer. Voir {@link EnderChunks}.</p>
 *
 * <p>Une fois posée, la coque et ce qu'elle transporte ne font qu'un objet aux
 * yeux de la sauvegarde : les passagers sont écrits <b>dans</b> la fiche du
 * bateau. Le chunk peut donc se décharger derrière eux sans que personne ne se
 * perde — c'est exactement ce qui fait qu'un cochon en barque survit à
 * l'éloignement du joueur.</p>
 */
public final class EntityTeleporterLogic {

    /**
     * Prix d'un voyage, en points d'expérience, <b>par passager</b>.
     *
     * <p>Le Sac de l'Ender fait payer 3 points par case expédiée ; une créature
     * vivante n'est pas une case, et deux bêtes coûtent deux fois. Quarante
     * points pour une coque pleine, c'est à peu près ce que rapporte une soirée
     * de minage — assez pour que le voyage se décide, trop peu pour qu'il se
     * refuse.</p>
     *
     * <p>Le prélèvement n'a lieu qu'<b>après</b> l'arrivée : une machine qui
     * échoue ne coûte rien.</p>
     */
    public static final int XP_PER_PASSENGER = 20;

    /** Chunks mis en chantier autour de l'Atterrisseur. */
    private static final int WARM_RADIUS = 2;
    /**
     * Chunks dont on attend réellement la génération. Un carré de 3 sur 3 : la
     * coque fait 1,4 bloc de large et peut chevaucher une bordure.
     */
    private static final int WAIT_RADIUS = 1;

    /**
     * Un clic droit, et ce qu'il fait selon la posture.
     *
     * <p><b>Accroupi, on récupère</b> : la coque débarque ce qu'elle transporte
     * et retourne en main. C'est le geste qui manquait à la 0.20.0 — une machine
     * arrivée pleine ne pouvait plus rien faire, puisque son clic droit
     * relançait un départ. Il fallait la casser, donc perdre son lien, pour
     * libérer la créature et récupérer la machine.</p>
     *
     * <p>Debout, la machine part si elle transporte quelque chose, et retourne
     * en main si elle est vide.</p>
     */
    public static void click(ServerPlayer player, EntityTeleporterEntity machine) {
        if (player.isShiftKeyDown()) {
            // ejectPassengers laisse vanilla choisir où chacun se pose : c'est
            // lui qui sait éviter un mur ou un vide, pas nous.
            machine.ejectPassengers();
            pickUp(player, machine);
            return;
        }
        if (machine.getPassengers().isEmpty()) {
            pickUp(player, machine);
        } else {
            depart(player, machine);
        }
    }

    /**
     * Reprend la machine vide, son lien avec elle.
     *
     * <p>Sans ce geste il faudrait la casser pour la déplacer, et donc la relier
     * à chaque voyage — alors que faire l'aller-retour est précisément son
     * usage.</p>
     */
    private static void pickUp(ServerPlayer player, EntityTeleporterEntity machine) {
        ItemStack stack = new ItemStack(ModItems.ENTITY_TELEPORTER.get());
        BlockPos lander = machine.getLander();
        if (lander != null) {
            stack.set(ModComponents.LANDER_POS.get(), lander);
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        machine.level().playSound(null, machine.getX(), machine.getY(), machine.getZ(),
                SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.7f, 1.4f);
        machine.discard();
    }

    /** Le prix du voyage que cette coque s'apprête à faire. */
    private static int price(EntityTeleporterEntity machine) {
        return XP_PER_PASSENGER * machine.getPassengers().size();
    }

    /** Le départ : vérifications, puis attente des chunks d'arrivée. */
    private static void depart(ServerPlayer player, EntityTeleporterEntity machine) {
        // Un départ déjà lancé ne se relance pas. Sans ce garde-fou, recliquer
        // pendant la génération des chunks mettait une seconde attente en file :
        // sans conséquence d'un monde à l'autre, où la coque est retirée et la
        // seconde arrivée ne trouve plus rien, mais pas dans l'Ender vers un
        // autre Atterrisseur — là, la coque survit au voyage, repart une
        // seconde fois et le joueur paie deux fois.
        if (machine.isCharging()) {
            say(player, "enderportals.message.teleporter_departing", ChatFormatting.AQUA);
            return;
        }
        BlockPos lander = machine.getLander();
        if (lander == null) {
            say(player, "enderportals.message.teleporter_unlinked", ChatFormatting.RED);
            return;
        }
        int cost = price(machine);
        if (!EnderXp.has(player, cost)) {
            // Refus immédiat : inutile de faire générer des chunks pour un
            // voyage qu'on ne pourra pas payer.
            say(player, ChatFormatting.RED, "enderportals.message.teleporter_no_xp", cost);
            return;
        }
        MinecraftServer server = player.server;
        if (server.getLevel(ModDimensions.ENDER_WORLD) == null) {
            return;
        }
        say(player, "enderportals.message.teleporter_departing", ChatFormatting.AQUA);
        machine.level().playSound(null, machine.getX(), machine.getY(), machine.getZ(),
                SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.8f, 1.5f);
        // La charge commence ici et tient tout le temps de la génération des
        // chunks d'arrivée : c'est elle qui occupe cette attente sans durée
        // prévisible, pendant laquelle la machine ne montrait rien.
        machine.setCharging(true);

        EnderChunks.whenReady(server, lander, WARM_RADIUS, WAIT_RADIUS, "atterrisseur",
                enderWorld -> land(player, machine, lander, enderWorld));
    }

    /**
     * Coupe la charge et signale l'abandon.
     *
     * <p>Un refus qui survient <b>après</b> le début de la charge doit se voir :
     * le joueur regarde une machine qui bourdonne et s'illumine, un message seul
     * se perdrait dans le spectacle.</p>
     */
    private static void abort(ServerPlayer player, EntityTeleporterEntity machine, String key) {
        machine.setCharging(false);
        machine.level().playSound(null, machine.getX(), machine.getY(), machine.getZ(),
                SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 0.8f, 0.7f);
        say(player, key, ChatFormatting.RED);
    }

    /**
     * L'arrivée, une fois les chunks prêts.
     *
     * <p>Tout est revérifié : entre le clic et ce point, il a pu s'écouler
     * plusieurs secondes de génération, pendant lesquelles la machine a pu être
     * cassée, vidée, ou l'Atterrisseur démonté.</p>
     */
    private static void land(ServerPlayer player, EntityTeleporterEntity machine, BlockPos lander,
                             ServerLevel enderWorld) {
        if (machine.isRemoved() || machine.getPassengers().isEmpty()) {
            machine.setCharging(false);
            return;
        }
        if (!enderWorld.getBlockState(lander).is(ModBlocks.ENTITY_LANDER.get())) {
            abort(player, machine, "enderportals.message.teleporter_no_lander");
            machine.setLander(null);
            return;
        }
        // Le prix est revérifié ici : le joueur a pu dépenser son expérience
        // pendant que les chunks se généraient. Un joueur déconnecté entre-temps
        // ne peut plus rien payer ni rien apprendre — sa bête part quand même,
        // parce qu'elle est déjà embarquée et qu'un demi-voyage la perdrait.
        boolean payer = !player.isRemoved();
        int cost = price(machine);
        if (payer && !EnderXp.has(player, cost)) {
            machine.setCharging(false);
            machine.level().playSound(null, machine.getX(), machine.getY(), machine.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 0.8f, 0.7f);
            say(player, ChatFormatting.RED, "enderportals.message.teleporter_no_xp", cost);
            return;
        }
        // Le lieu du départ, relevé avant le voyage : après, la machine est
        // ailleurs — et dans le cas de deux mondes, ce n'est même plus la même
        // entité. Or c'est là que se tient le joueur, et donc là que la scène
        // doit se jouer.
        Vec3 origin = machine.position();
        ServerLevel departure = machine.level() instanceof ServerLevel from ? from : null;

        Vec3 arrival = Vec3.atBottomCenterOf(lander.above());
        EntityTeleporterEntity arrived = move(machine, enderWorld, arrival);
        if (arrived == null) {
            abort(player, machine, "enderportals.message.teleporter_failed");
            return;
        }
        // La coque survit au voyage quand il se fait dans le même monde : c'est
        // sur elle qu'il faut couper la charge, la neuve d'un changement de
        // dimension naissant déjà éteinte.
        arrived.setCharging(false);
        if (payer) {
            EnderXp.charge(player, cost);
        }
        if (departure != null) {
            EntityTeleporterEntity.departureBurst(departure, origin);
        }
        EntityTeleporterEntity.arrivalBurst(enderWorld, arrival);
        enderWorld.playSound(null, arrival.x, arrival.y, arrival.z,
                SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.6f, 1.7f);
        say(player, "enderportals.message.teleporter_arrived", ChatFormatting.GREEN);
    }

    /**
     * Déplace la coque et ses passagers, et rend la coque telle qu'elle existe
     * à l'arrivée.
     *
     * <p>Deux mondes, deux méthodes. Dans le même monde, {@code teleportTo}
     * suffit : il repositionne les passagers avec le véhicule. D'un monde à
     * l'autre, chaque entité est <b>recréée</b> de l'autre côté — l'ancienne est
     * retirée et une copie apparaît — et c'est là qu'il faut être explicite.</p>
     *
     * <p>Les passagers sont <b>débarqués avant</b> le changement de dimension,
     * puis remontés à l'arrivée sur la coque neuve. Laisser vanilla s'en charger
     * aurait marché ou non selon la façon dont il traite un véhicule chargé, et
     * une créature laissée derrière est une perte que le joueur ne peut pas
     * réparer. Débarquer d'abord retire la question.</p>
     */
    @Nullable
    private static EntityTeleporterEntity move(EntityTeleporterEntity machine, ServerLevel enderWorld,
                                               Vec3 arrival) {
        if (machine.level() == enderWorld) {
            machine.teleportTo(arrival.x, arrival.y, arrival.z);
            return machine;
        }
        List<Entity> riders = List.copyOf(machine.getPassengers());
        for (Entity rider : riders) {
            rider.stopRiding();
        }
        Entity moved = machine.changeDimension(transition(enderWorld, arrival, machine.getYRot()));
        if (!(moved instanceof EntityTeleporterEntity arrived)) {
            return null;
        }
        for (Entity rider : riders) {
            Entity landed = rider.changeDimension(transition(enderWorld, arrival, rider.getYRot()));
            if (landed != null) {
                landed.startRiding(arrived, true);
            }
        }
        return arrived;
    }

    private static DimensionTransition transition(ServerLevel enderWorld, Vec3 arrival, float yRot) {
        return new DimensionTransition(enderWorld, arrival, Vec3.ZERO, yRot, 0.0f,
                DimensionTransition.DO_NOTHING);
    }

    private static void say(ServerPlayer player, String key, ChatFormatting color) {
        say(player, color, key);
    }

    private static void say(ServerPlayer player, ChatFormatting color, String key, Object... args) {
        player.displayClientMessage(Component.translatable(key, args).withStyle(color), true);
    }

    private EntityTeleporterLogic() {
    }
}
