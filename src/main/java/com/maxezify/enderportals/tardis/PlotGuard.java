package com.maxezify.enderportals.tardis;

import com.maxezify.enderportals.ModDimensions;
import com.maxezify.enderportals.world.EnderWorldChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Qui a le droit de faire quoi dans la parcelle d'un autre.
 *
 * <p>Jusqu'à la 0.29.0, le Passage des Alliés était consenti mais le
 * consentement était total : celui qu'on laissait entrer pouvait casser les
 * murs et vider les coffres, et rien dans le mod ne s'y opposait — la parcelle
 * n'avait aucune notion de permission. Pour une mécanique dont tout le propos
 * est le partage choisi, c'était le trou.</p>
 *
 * <p>Trois degrés, accordés un par un depuis le Contrôle de l'amitié :
 * <b>visiteur</b> entre et regarde, <b>invité</b> ouvre en plus les rangements,
 * <b>associé</b> casse et pose comme chez lui. Le degré est à sens unique — vous
 * ouvrir mes coffres ne vous oblige pas à m'ouvrir les vôtres — et le défaut est
 * le plus fermé, y compris pour les alliances nouées avant cette version.</p>
 *
 * <p><b>Ce que cette classe ne garde pas.</b> Elle tient les trois vecteurs
 * directs : casser, poser, ouvrir un rangement. Une créature apprivoisée tuée,
 * un cadre d'objet vidé, un TNT allumé depuis l'extérieur de la parcelle ou un
 * entonnoir posé dessous par un associé restent possibles. C'est un tri
 * délibéré : ces voies demandent chacune leur propre écouteur, et prétendre à
 * l'étanchéité sans les traiter serait pire que d'annoncer la portée réelle.</p>
 */
public final class PlotGuard {

    private PlotGuard() {
    }

    /**
     * La parcelle qui contient cette position, ou {@code null} — hors de
     * l'Ender, ou sur une cellule que personne n'a reçue.
     */
    @Nullable
    public static TardisData plotAt(MinecraftServer server, Level level, BlockPos pos) {
        if (!level.dimension().equals(ModDimensions.ENDER_WORLD)) {
            return null;
        }
        int index = TardisStateManager.plotIndexOfCell(
                EnderWorldChunkGenerator.enclosureIndex(pos.getX()),
                EnderWorldChunkGenerator.enclosureIndex(pos.getZ()));
        return TardisStateManager.get(server).findByPlot(index);
    }

    /** Ce que l'hôte de cette parcelle accorde à ce joueur. */
    public static int trustAt(MinecraftServer server, Level level, BlockPos pos, Player player) {
        TardisData plot = plotAt(server, level, pos);
        if (plot == null || plot.ownerUuid == null || player.getUUID().equals(plot.ownerUuid)) {
            return AllyLinks.PARTNER;
        }
        return TardisStateManager.get(server).allies().trustOf(plot.ownerUuid, player.getUUID());
    }

    /**
     * Ce joueur peut-il agir ici au degré demandé ?
     *
     * <p>Les opérateurs passent outre. C'est le choix habituel, et le seul qui
     * laisse réparer une parcelle dont le propriétaire ne joue plus ; ils ont de
     * toute façon la commande qui déplace n'importe quel bloc.</p>
     */
    public static boolean allows(ServerPlayer player, BlockPos pos, int needed) {
        MinecraftServer server = player.getServer();
        if (server == null || player.hasPermissions(2)) {
            return true;
        }
        return trustAt(server, player.level(), pos, player) >= needed;
    }

    /** Casser ou poser : réservé à l'associé. */
    public static boolean mayBuild(ServerPlayer player, BlockPos pos) {
        return allows(player, pos, AllyLinks.PARTNER);
    }

    /** Ouvrir un rangement : à partir de l'invité. */
    public static boolean mayOpen(ServerPlayer player, BlockPos pos) {
        return allows(player, pos, AllyLinks.GUEST);
    }

    /**
     * Ce bloc garde-t-il quelque chose ?
     *
     * <p>Les deux tests ne font pas double emploi : {@link Container} attrape les
     * coffres, tonneaux, fours et entonnoirs de vanilla, {@link MenuProvider} les
     * rangements des mods qui n'exposent leur contenu que par un écran. Un bloc
     * qui n'est ni l'un ni l'autre — une porte, un levier — n'a rien à protéger,
     * et un visiteur peut s'en servir.</p>
     */
    public static boolean isStorage(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof Container || be instanceof MenuProvider;
    }
}
