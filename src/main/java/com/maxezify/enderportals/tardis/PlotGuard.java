package com.maxezify.enderportals.tardis;

import com.maxezify.enderportals.ModDimensions;
import com.maxezify.enderportals.ModTags;
import com.maxezify.enderportals.world.EnderWorldChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Qui a le droit de faire quoi dans la parcelle d'un autre.
 *
 * <p>Jusqu'à la 0.29.0, le Passage des Alliés était consenti mais le
 * consentement était total : celui qu'on laissait entrer pouvait casser les
 * murs et vider les coffres, et rien dans le mod ne s'y opposait. Trois degrés
 * y répondent : <b>visiteur</b> entre, circule et n'ouvre rien, <b>invité</b> se
 * sert en plus des blocs — coffres compris —, <b>associé</b> casse et pose comme
 * chez lui.
 * Le degré est à sens unique, et le défaut est le plus fermé.</p>
 *
 * <p><b>Aucune exception, pas même pour les opérateurs.</b> La 0.29.0 les
 * laissait passer, au motif habituel qu'un administrateur doit pouvoir réparer.
 * C'était une mauvaise idée : sur un serveur de test, où tout le monde est
 * opérateur, la garde ne se déclenchait jamais et le système paraissait
 * simplement ne pas fonctionner. Un administrateur a de toute façon le mode
 * créatif et les commandes, qui ne passent ni par l'un ni par l'autre de ces
 * événements.</p>
 *
 * <p><b>Ce que cette classe ne garde pas.</b> Elle tient les trois vecteurs
 * directs : casser, poser, se servir d'un bloc. Une créature apprivoisée tuée,
 * un cadre d'objet vidé, un TNT allumé depuis l'extérieur de la parcelle
 * restent possibles. C'est un tri délibéré : ces voies demandent chacune leur
 * propre écouteur, et prétendre à l'étanchéité sans les traiter serait pire que
 * d'annoncer la portée réelle.</p>
 */
public final class PlotGuard {

    private PlotGuard() {
    }

    /**
     * La parcelle qui contient cette position, ou {@code null} — hors de
     * l'Ender, ou sur un enclos que personne n'occupe.
     *
     * <p>La parcelle est retrouvée en comparant des <b>positions de porte</b>,
     * et non en inversant la spirale qui attribue les rangs. L'inversion était
     * exacte, mais elle supposait que le rang d'une parcelle et l'endroit où sa
     * porte a réellement été posée s'accordent : deux choses qui se sont
     * séparées le jour où l'espacement est passé de 1024 à 8192 blocs, les bases
     * d'alors ayant gardé leur position d'origine. Là, la même fonction d'enclos
     * est appliquée aux deux bouts de la comparaison : elles ne peuvent pas
     * diverger.</p>
     */
    @Nullable
    public static TardisData plotAt(MinecraftServer server, Level level, BlockPos pos) {
        if (!level.dimension().equals(ModDimensions.ENDER_WORLD)) {
            return null;
        }
        int cellX = EnderWorldChunkGenerator.enclosureIndex(pos.getX());
        int cellZ = EnderWorldChunkGenerator.enclosureIndex(pos.getZ());
        for (TardisData data : TardisStateManager.get(server).all()) {
            BlockPos door = data.interiorDoorPos;
            if (door != null
                    && EnderWorldChunkGenerator.enclosureIndex(door.getX()) == cellX
                    && EnderWorldChunkGenerator.enclosureIndex(door.getZ()) == cellZ) {
                return data;
            }
        }
        return null;
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
     * <p>Le monde est celui du joueur, et non celui que porte l'événement : un
     * joueur casse, pose et clique toujours dans le monde où il se tient, et
     * l'événement expose un {@code LevelAccessor} qu'il aurait fallu convertir —
     * donc une branche capable de sauter la vérification sans rien dire.</p>
     */
    public static boolean allows(ServerPlayer player, BlockPos pos, int needed) {
        MinecraftServer server = player.getServer();
        return server == null || trustAt(server, player.level(), pos, player) >= needed;
    }

    /** Casser ou poser : réservé à l'associé. */
    public static boolean mayBuild(ServerPlayer player, BlockPos pos) {
        return allows(player, pos, AllyLinks.PARTNER);
    }

    /**
     * Se servir d'un bloc — coffre, four, établi : à partir de l'invité, sauf
     * pour la courte liste de ce qu'un visiteur peut actionner malgré tout.
     *
     * <p>Le tri ne cherche pas à reconnaître les rangements pour les refuser :
     * c'est ce que faisait la 0.29.0, en testant {@code Container} et
     * {@code MenuProvider}, et les coffres de Sophisticated Storage passaient au
     * travers faute d'exposer l'un ou l'autre au bloc. Le sens est inversé.
     * {@link ModTags#VISITOR_USABLE} énumère ce qui est <b>permis</b> — portes,
     * trappes, portillons, boutons — et tout le reste est refusé, y compris ce
     * que le mod ne connaît pas encore.</p>
     *
     * <p>Les plaques de pression figurent dans ce tag pour la forme : on les
     * déclenche en marchant dessus, pas d'un clic droit, si bien qu'elles n'ont
     * jamais été empêchées. Ce qui vaut de la même façon pour les fils de
     * détente et les capteurs — la garde ne tient que le clic droit, le coup de
     * pioche et la pose.</p>
     */
    public static boolean mayUse(ServerPlayer player, BlockPos pos, BlockState state) {
        return allows(player, pos, AllyLinks.GUEST) || state.is(ModTags.VISITOR_USABLE);
    }
}
