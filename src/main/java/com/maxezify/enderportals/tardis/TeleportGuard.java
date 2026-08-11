package com.maxezify.enderportals.tardis;

import com.maxezify.enderportals.EnderPortalsMod;
import com.maxezify.enderportals.ModDimensions;
import com.maxezify.enderportals.ModSettings;
import com.maxezify.enderportals.world.EnderWorldChunkGenerator;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Le monde de l'Ender ne s'atteint que par une porte ou un passage.
 *
 * <p>Tout le mod repose sur deux gestes : <b>matérialiser sa porte</b> pour
 * rentrer chez soi, <b>ouvrir un Passage des Alliés</b> pour aller chez un
 * autre. Un mod de points de voyage — Waystones et ses semblables — les rend
 * tous les deux inutiles d'un seul coup : on se pose n'importe où, y compris
 * dans l'Ender, et plus rien n'a besoin d'être ouvert ni matérialisé. Ce n'est
 * pas un déséquilibre de dosage, c'est la disparition du sujet.</p>
 *
 * <h2>Deux couches, et pourquoi deux</h2>
 *
 * <p><b>La prévention.</b> {@link EntityTravelToDimensionEvent} est déclenché
 * par la toute première instruction de {@code Entity#changeDimension} : tout ce
 * qui change de monde par le chemin normal y passe, portails du Nether
 * compris. On y refuse, et rien ne se produit.</p>
 *
 * <p><b>Le filet.</b> La prévention ne voit que ce qui passe par
 * {@code changeDimension}, et cela ne couvre pas tout. Waystones appelle
 * {@code ServerPlayer#teleportTo(ServerLevel, …, Set, …)}, dont le désassemblage
 * des classes de 1.21.1 montre qu'elle se dédouble : vers un <b>autre monde</b>
 * elle retombe sur la surcharge qui appelle {@code changeDimension} — la
 * prévention agit, proprement — mais <b>dans le même monde</b> elle écrit
 * directement au client par {@code ServerGamePacketListenerImpl.teleport},
 * sans qu'aucun événement n'existe. Il n'y a alors rien à annuler.</p>
 *
 * <p>{@link #tick} relit donc la position de chaque joueur et la compare à
 * celle du tick précédent : peu importe par quel code il a bougé, franchir un
 * mur de parcelle le renvoie d'où il venait. Contrepartie assumée : un tick de
 * latence, et un mod qui a déjà encaissé son prix ne le rend pas.</p>
 *
 * <p>Les deux couches se partagent donc le travail proprement — la première
 * pour les changements de monde, la seconde pour les sauts entre parcelles —
 * et aucune des deux ne suffirait seule.</p>
 *
 * <p>Le filet ne surveille que ce qui est <b>impossible autrement</b> : entrer
 * dans l'Ender, en sortir, franchir un mur de parcelle. Un saut à l'intérieur
 * de sa propre parcelle n'est pas inquiété — perle de l'Ender, fruit chorus,
 * point de voyage posé chez soi : rien de tout cela ne dispense de matérialiser
 * sa porte ni d'ouvrir un passage, puisqu'il a fallu entrer d'abord. Une garde
 * qui interdit aussi ce qui ne casse rien finit par être désactivée en entier.</p>
 *
 * <h2>Le laissez-passer</h2>
 *
 * <p>Le mod se déplace lui-même, et doit donc s'autoriser. Plutôt qu'un drapeau
 * que chaque appelant penserait à poser, il n'y a qu'un seul chemin :
 * {@link #travel} est la seule façon dont ce mod change une entité de monde. Un
 * {@code changeDimension} appelé ailleurs serait refusé par sa propre garde —
 * l'oubli se voit tout de suite, au lieu d'ouvrir un trou silencieux.</p>
 *
 * <h2>Les opérateurs</h2>
 *
 * <p>Ils peuvent passer outre, mais {@link ModSettings#allowOperatorTeleports()} est
 * <b>faux par défaut</b>. La 0.31.0 le mettait à vrai : sur un serveur où celui
 * qui joue est aussi celui qui administre, cela n'a bloqué personne, et le
 * garde-fou a semblé cassé alors qu'il obéissait à la lettre. C'est la deuxième
 * fois qu'un contournement d'opérateur annule une garde de ce mod — la première
 * était écrit en dur dans la garde de parcelle. Une porte dérobée ouverte par
 * défaut n'est pas une porte dérobée, c'est l'entrée principale.</p>
 */
public final class TeleportGuard {

    /**
     * Entités dont le mod autorise le déplacement <b>en ce moment même</b>.
     *
     * <p>L'ensemble ne contient jamais quoi que ce soit entre deux appels : il
     * est rempli et vidé dans le même {@code try/finally}, sur le fil du
     * serveur. Ce n'est pas une liste de permissions, c'est le temps d'un
     * geste.</p>
     */
    private static final Set<UUID> ALLOWED = new HashSet<>();

    /** Dernière position tenue pour légitime, par joueur. */
    private static final Map<UUID, Anchor> LAST = new HashMap<>();

    private record Anchor(ResourceKey<Level> dimension, Vec3 pos, float yRot, float xRot) {
    }

    // ------------------------------------------------------------------
    // Le seul chemin par lequel le mod déplace une entité
    // ------------------------------------------------------------------

    /**
     * Change une entité de monde, avec l'accord de la garde.
     *
     * <p>Rend l'entité telle qu'elle existe à l'arrivée — la même pour un
     * joueur, une copie neuve pour toute autre — ou {@code null} si le
     * déplacement n'a pas eu lieu.</p>
     */
    @Nullable
    public static Entity travel(Entity entity, DimensionTransition transition) {
        UUID id = entity.getUUID();
        // add rend false si un appel englobant a déjà posé le laissez-passer :
        // c'est lui qui le retirera, pas nous.
        boolean mine = ALLOWED.add(id);
        Entity moved;
        try {
            moved = entity.changeDimension(transition);
        } finally {
            if (mine) {
                ALLOWED.remove(id);
            }
        }
        if (moved != null) {
            remember(moved);
        }
        return moved;
    }

    // ------------------------------------------------------------------
    // Couche 1 : la prévention
    // ------------------------------------------------------------------

    /** Refuse un changement de monde qui touche à l'Ender sans y avoir droit. */
    public static void onTravelToDimension(EntityTravelToDimensionEvent event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }
        if (!touchesEnderWorld(entity.level().dimension(), event.getDimension()) || permitted(entity)) {
            return;
        }
        event.setCanceled(true);
        refuse(entity, "changement de monde annulé");
    }

    /** L'un des deux bouts du trajet est-il le monde de l'Ender ? */
    private static boolean touchesEnderWorld(ResourceKey<Level> from, ResourceKey<Level> to) {
        return ModDimensions.ENDER_WORLD.equals(from) || ModDimensions.ENDER_WORLD.equals(to);
    }

    // ------------------------------------------------------------------
    // Couche 2 : le filet
    // ------------------------------------------------------------------

    /**
     * Relit la position de chaque joueur et défait ce qui n'aurait pas dû
     * arriver. Appelé une fois par tick de serveur.
     */
    public static void tick(MinecraftServer server) {
        // Une copie : renvoyer un joueur d'un monde à l'autre remue les listes
        // du serveur, et la liste parcourue ne doit pas être l'une d'elles.
        for (ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers())) {
            Anchor now = anchorOf(player);
            Anchor last = LAST.get(player.getUUID());
            if (last == null || permitted(player) || !suspicious(last, now)) {
                LAST.put(player.getUUID(), now);
                continue;
            }
            sendBack(player, last);
        }
    }

    /**
     * Ce déplacement était-il impossible autrement que par téléportation ?
     *
     * <p>Les trois cas retenus ne se franchissent pas à pied : le monde de
     * l'Ender n'a aucune sortie que ses portes, et les parcelles sont séparées
     * par un mur de bedrock qui monte d'une calotte à l'autre. Un joueur qui
     * change d'enclos entre deux ticks n'a pas marché.</p>
     */
    private static boolean suspicious(Anchor last, Anchor now) {
        boolean was = ModDimensions.ENDER_WORLD.equals(last.dimension());
        boolean is = ModDimensions.ENDER_WORLD.equals(now.dimension());
        if (!was && !is) {
            return false;
        }
        if (was != is) {
            return true;
        }
        return enclosure(last.pos()) != enclosure(now.pos());
    }

    /** Les deux rangs d'enclos d'une position, empaquetés pour comparaison. */
    private static long enclosure(Vec3 pos) {
        long x = EnderWorldChunkGenerator.enclosureIndex(Mth.floor(pos.x));
        long z = EnderWorldChunkGenerator.enclosureIndex(Mth.floor(pos.z));
        return (x << 32) ^ (z & 0xFFFFFFFFL);
    }

    /** Remet le joueur là où il était au tick précédent. */
    private static void sendBack(ServerPlayer player, Anchor anchor) {
        ServerLevel level = player.server.getLevel(anchor.dimension());
        if (level == null) {
            // Le monde d'où il venait n'existe plus : il n'y a rien à défaire,
            // et l'y renvoyer serait pire que le laisser où il est.
            LAST.put(player.getUUID(), anchorOf(player));
            return;
        }
        travel(player, new DimensionTransition(level, anchor.pos(), Vec3.ZERO,
                anchor.yRot(), anchor.xRot(), DimensionTransition.DO_NOTHING));
        refuse(player, "déplacement défait après coup");
    }

    // ------------------------------------------------------------------
    // Mémoire des positions
    // ------------------------------------------------------------------

    /**
     * Prend la position d'un joueur pour référence, sans rien vérifier.
     *
     * <p>À appeler quand il apparaît quelque part pour une raison qui n'est pas
     * un déplacement : connexion, renaissance. Sans cela, renaître dans son lit
     * posé au fond de sa parcelle passerait pour une intrusion, et le filet
     * renverrait le joueur à l'endroit où il vient de mourir.</p>
     */
    public static void reseed(Entity entity) {
        remember(entity);
    }

    private static void remember(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            LAST.put(player.getUUID(), anchorOf(player));
        }
    }

    /** Le joueur s'en va : sa dernière position ne sert plus à rien. */
    public static void forget(UUID player) {
        LAST.remove(player);
    }

    private static Anchor anchorOf(ServerPlayer player) {
        return new Anchor(player.level().dimension(), player.position(),
                player.getYRot(), player.getXRot());
    }

    // ------------------------------------------------------------------
    // Autorisations
    // ------------------------------------------------------------------

    private static boolean permitted(Entity entity) {
        return !ModSettings.blockTeleports()
                || ALLOWED.contains(entity.getUUID())
                || (ModSettings.allowOperatorTeleports() && entity instanceof ServerPlayer player
                        && player.hasPermissions(2));
    }

    /**
     * Refuse, et le dit — au joueur, et au journal du serveur.
     *
     * <p>La ligne de journal n'est pas du bavardage. Un refus est rare, et
     * quand la garde a l'air de ne rien faire, la seule question qui compte est
     * : n'a-t-elle rien vu, ou a-t-elle vu et laissé passer ? Le journal
     * tranche, et nomme laquelle des deux couches a agi.</p>
     */
    private static void refuse(Entity entity, String how) {
        if (entity instanceof ServerPlayer player) {
            player.displayClientMessage(
                    Component.translatable("enderportals.message.teleport_refused"), true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.CHAIN_HIT,
                    SoundSource.PLAYERS, 0.8f, 0.6f);
            EnderPortalsMod.LOGGER.info("Téléportation refusée à {} ({}) — {}",
                    player.getGameProfile().getName(), player.level().dimension().location(), how);
        }
    }

    private TeleportGuard() {
    }
}
