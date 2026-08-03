package com.maxezify.enderportals.tardis;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * L'état d'un TARDIS : sa parcelle intérieure dans le monde de l'Ender et la
 * position de sa porte extérieure dans le monde "réel".
 */
public class TardisData {

    public final UUID id;
    public final int plotIndex;

    /** Le joueur qui a éveillé cette porte — une seule porte par personne. */
    @Nullable
    public UUID ownerUuid;

    /** Pseudo du propriétaire, affiché sur le panneau de la porte. */
    public String ownerName = "";

    /** Porte intérieure (moitié basse), dans le monde de l'Ender. */
    public BlockPos interiorDoorPos;
    public Direction interiorFacing = Direction.SOUTH;

    /** Dernier emplacement de la porte extérieure. */
    public ResourceKey<Level> exteriorWorld = Level.OVERWORLD;
    public BlockPos exteriorPos = BlockPos.ZERO;
    public Direction exteriorFacing = Direction.NORTH;

    /** La porte extérieure est-elle actuellement matérialisée ? */
    public boolean deployed;
    /** Les portes sont-elles ouvertes ? */
    public boolean open;

    /** Portails Immersive Portals actifs (si le mod est présent). */
    public final List<UUID> portalIds = new ArrayList<>();
    public boolean immptlActive;

    /** Centraliseur d'objet du joueur, dans le monde de l'Ender (ou null). */
    @Nullable
    public BlockPos centralizerPos;

    /** Passage des Alliés du joueur, dans le monde de l'Ender (ou null). */
    @Nullable
    public BlockPos passagePos;
    public Direction passageFacing = Direction.NORTH;

    /**
     * Portails Immersive Portals du Passage des Alliés. Les deux mêmes
     * identifiants sont inscrits chez les deux alliés : le lien est symétrique,
     * et chacun doit pouvoir nettoyer la paire depuis son côté.
     */
    public final List<UUID> passagePortalIds = new ArrayList<>();
    public boolean passagePortalsActive;

    /**
     * Tick de jeu où l'arche finit de s'ouvrir ; zéro tant qu'elle est close.
     *
     * <p>La même valeur est écrite dans les <b>deux</b> fiches d'une paire. Une
     * échéance partagée plutôt qu'un compte à rebours par arche : les deux
     * percent alors au même tick, y compris quand une seule des deux parcelles
     * tourne — et c'est très exactement ce qui manquait à la 0.15.0, où chaque
     * arche attendait son propre tick programmé et où celle du chunk endormi
     * n'en recevait jamais.</p>
     */
    public long passageOpenAt;

    /**
     * Code d'ami : huit chiffres, tapés au pavé numérique du Contrôle de
     * l'amitié. Zéro tant qu'il n'a pas été attribué — les portes créées avant
     * l'arrivée du Passage des Alliés en reçoivent un au chargement.
     */
    public int friendCode;

    public TardisData(UUID id, int plotIndex) {
        this.id = id;
        this.plotIndex = plotIndex;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID("Id", id);
        nbt.putInt("Plot", plotIndex);
        if (ownerUuid != null) {
            nbt.putUUID("Owner", ownerUuid);
        }
        nbt.putString("OwnerName", ownerName);
        putPos(nbt, "Interior", interiorDoorPos);
        nbt.putString("InteriorFacing", interiorFacing.getName());
        nbt.putString("ExteriorWorld", exteriorWorld.location().toString());
        putPos(nbt, "Exterior", exteriorPos);
        nbt.putString("ExteriorFacing", exteriorFacing.getName());
        nbt.putBoolean("Deployed", deployed);
        nbt.putBoolean("Open", open);
        ListTag portals = new ListTag();
        for (UUID portal : portalIds) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Id", portal);
            portals.add(tag);
        }
        nbt.put("Portals", portals);
        nbt.putBoolean("ImmptlActive", immptlActive);
        if (centralizerPos != null) {
            putPos(nbt, "Centralizer", centralizerPos);
        }
        if (passagePos != null) {
            putPos(nbt, "Passage", passagePos);
            nbt.putString("PassageFacing", passageFacing.getName());
        }
        nbt.putInt("FriendCode", friendCode);
        ListTag passagePortals = new ListTag();
        for (UUID portal : passagePortalIds) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Id", portal);
            passagePortals.add(tag);
        }
        nbt.put("PassagePortals", passagePortals);
        nbt.putBoolean("PassagePortalsActive", passagePortalsActive);
        nbt.putLong("PassageOpenAt", passageOpenAt);
        return nbt;
    }

    public static TardisData fromNbt(CompoundTag nbt) {
        TardisData data = new TardisData(nbt.getUUID("Id"), nbt.getInt("Plot"));
        data.ownerUuid = nbt.hasUUID("Owner") ? nbt.getUUID("Owner") : null;
        data.ownerName = nbt.getString("OwnerName");
        data.interiorDoorPos = getPos(nbt, "Interior");
        data.interiorFacing = directionOrDefault(nbt.getString("InteriorFacing"), Direction.SOUTH);
        // tryParse plutôt que parse : une sauvegarde abîmée ferait échouer le
        // chargement du monde entier plutôt que de dégrader une seule porte.
        ResourceLocation exteriorWorld = ResourceLocation.tryParse(nbt.getString("ExteriorWorld"));
        data.exteriorWorld = exteriorWorld != null
                ? ResourceKey.create(Registries.DIMENSION, exteriorWorld)
                : Level.OVERWORLD;
        data.exteriorPos = getPos(nbt, "Exterior");
        data.exteriorFacing = directionOrDefault(nbt.getString("ExteriorFacing"), Direction.NORTH);
        data.deployed = nbt.getBoolean("Deployed");
        data.open = nbt.getBoolean("Open");
        for (Tag element : nbt.getList("Portals", Tag.TAG_COMPOUND)) {
            data.portalIds.add(((CompoundTag) element).getUUID("Id"));
        }
        data.immptlActive = nbt.getBoolean("ImmptlActive");
        data.centralizerPos = nbt.contains("Centralizer", Tag.TAG_INT_ARRAY)
                ? getPos(nbt, "Centralizer") : null;
        data.passagePos = nbt.contains("Passage", Tag.TAG_INT_ARRAY)
                ? getPos(nbt, "Passage") : null;
        data.passageFacing = directionOrDefault(nbt.getString("PassageFacing"), Direction.NORTH);
        data.friendCode = nbt.getInt("FriendCode");
        for (Tag element : nbt.getList("PassagePortals", Tag.TAG_COMPOUND)) {
            data.passagePortalIds.add(((CompoundTag) element).getUUID("Id"));
        }
        data.passagePortalsActive = nbt.getBoolean("PassagePortalsActive");
        data.passageOpenAt = nbt.getLong("PassageOpenAt");
        return data;
    }

    private static Direction directionOrDefault(String name, Direction fallback) {
        Direction direction = Direction.byName(name);
        return direction != null && direction.getAxis().isHorizontal() ? direction : fallback;
    }

    private static void putPos(CompoundTag nbt, String key, BlockPos pos) {
        nbt.putIntArray(key, new int[]{pos.getX(), pos.getY(), pos.getZ()});
    }

    private static BlockPos getPos(CompoundTag nbt, String key) {
        int[] xyz = nbt.getIntArray(key);
        return xyz.length == 3 ? new BlockPos(xyz[0], xyz[1], xyz[2]) : BlockPos.ZERO;
    }
}
