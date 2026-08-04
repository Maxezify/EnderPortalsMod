package com.maxezify.enderportals.tardis;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Une arche de Passage des Alliés, et l'allié à qui elle mène.
 *
 * <p>Un joueur en pose autant qu'il a d'amis à relier : c'est l'arche, et non le
 * joueur, qui porte le lien. Ce déplacement est tout le sujet de la 0.19.0.
 * Auparavant le lien vivait dans un {@code Map<UUID, UUID>} du carnet — une
 * seule entrée par joueur, donc un seul allié à la fois, et ouvrir avec l'un
 * fermait mécaniquement avec l'autre.</p>
 *
 * <p>Ce que le lien gagne à vivre ici : il ne peut plus désigner une arche
 * inexistante, puisqu'il <b>est</b> l'arche ; il se persiste et se supprime avec
 * elle ; et deux arches d'un même joueur n'ont plus rien à se disputer.</p>
 *
 * <p>Une paire d'arches liées partage son échéance d'ouverture
 * ({@link #openAt}) et ses identifiants de portails : la même valeur est écrite
 * des deux côtés, pour que les deux percent au même tick même si une seule des
 * deux parcelles tourne. Voir {@link AllyPassageHelper}.</p>
 */
public class PassageData {

    /** Bloc bas de l'arche, dans le monde de l'Ender. */
    public BlockPos pos;
    public Direction facing;

    /**
     * L'allié auquel cette arche est liée, ou {@code null} si elle est libre.
     *
     * <p>Le lien n'est ouvert que si l'allié tient de son côté une arche liée en
     * retour. Un lien à sens unique — l'autre a cassé la sienne — laisse ce
     * champ renseigné et l'arche close ; c'est un état que le panneau nomme,
     * plutôt qu'un état impossible qu'il faudrait empêcher.</p>
     */
    @Nullable
    public UUID ally;

    /**
     * Tick de jeu où l'arche finit de s'ouvrir ; zéro tant qu'elle est close.
     * Commun aux deux arches d'une paire.
     */
    public long openAt;

    /** Portails Immersive Portals de la paire, inscrits des deux côtés. */
    public final List<UUID> portalIds = new ArrayList<>();
    public boolean portalsActive;

    public PassageData(BlockPos pos, Direction facing) {
        this.pos = pos;
        this.facing = facing;
    }

    /** Cette arche mène-t-elle à ce joueur ? */
    public boolean leadsTo(UUID player) {
        return player.equals(ally);
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        TardisData.putPos(nbt, "Pos", pos);
        nbt.putString("Facing", facing.getName());
        if (ally != null) {
            nbt.putUUID("Ally", ally);
        }
        nbt.putLong("OpenAt", openAt);
        ListTag portals = new ListTag();
        for (UUID portal : portalIds) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Id", portal);
            portals.add(tag);
        }
        nbt.put("Portals", portals);
        nbt.putBoolean("PortalsActive", portalsActive);
        return nbt;
    }

    public static PassageData fromNbt(CompoundTag nbt) {
        PassageData passage = new PassageData(TardisData.getPos(nbt, "Pos"),
                TardisData.directionOrDefault(nbt.getString("Facing"), Direction.NORTH));
        passage.ally = nbt.hasUUID("Ally") ? nbt.getUUID("Ally") : null;
        passage.openAt = nbt.getLong("OpenAt");
        for (Tag element : nbt.getList("Portals", Tag.TAG_COMPOUND)) {
            passage.portalIds.add(((CompoundTag) element).getUUID("Id"));
        }
        passage.portalsActive = nbt.getBoolean("PortalsActive");
        return passage;
    }

    /**
     * L'arche unique des sauvegardes d'avant la 0.19.0, relue depuis les champs
     * que la fiche du TARDIS portait elle-même.
     *
     * <p>L'échéance et les portails sont repris : sans eux, un monde rechargé au
     * milieu d'un lien ouvert aurait laissé derrière lui deux entités de portail
     * que plus personne n'aurait su retrouver. L'allié, lui, ne se trouve pas ici
     * — il est dans le carnet, et {@link TardisStateManager#load} l'y reprend.</p>
     */
    static PassageData fromLegacyNbt(CompoundTag nbt) {
        PassageData passage = new PassageData(TardisData.getPos(nbt, "Passage"),
                TardisData.directionOrDefault(nbt.getString("PassageFacing"), Direction.NORTH));
        passage.openAt = nbt.getLong("PassageOpenAt");
        for (Tag element : nbt.getList("PassagePortals", Tag.TAG_COMPOUND)) {
            passage.portalIds.add(((CompoundTag) element).getUUID("Id"));
        }
        passage.portalsActive = nbt.getBoolean("PassagePortalsActive");
        return passage;
    }
}
