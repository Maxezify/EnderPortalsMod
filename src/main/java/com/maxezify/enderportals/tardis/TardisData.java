package com.maxezify.enderportals.tardis;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
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

    /** Porte intérieure (moitié basse), dans le monde de l'Ender. */
    public BlockPos interiorDoorPos;
    public Direction interiorFacing = Direction.SOUTH;

    /** Dernier emplacement de la porte extérieure. */
    public RegistryKey<World> exteriorWorld = World.OVERWORLD;
    public BlockPos exteriorPos = BlockPos.ORIGIN;
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

    public TardisData(UUID id, int plotIndex) {
        this.id = id;
        this.plotIndex = plotIndex;
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("Id", id);
        nbt.putInt("Plot", plotIndex);
        if (ownerUuid != null) {
            nbt.putUuid("Owner", ownerUuid);
        }
        putPos(nbt, "Interior", interiorDoorPos);
        nbt.putString("InteriorFacing", interiorFacing.getName());
        nbt.putString("ExteriorWorld", exteriorWorld.getValue().toString());
        putPos(nbt, "Exterior", exteriorPos);
        nbt.putString("ExteriorFacing", exteriorFacing.getName());
        nbt.putBoolean("Deployed", deployed);
        nbt.putBoolean("Open", open);
        NbtList portals = new NbtList();
        for (UUID portal : portalIds) {
            NbtCompound tag = new NbtCompound();
            tag.putUuid("Id", portal);
            portals.add(tag);
        }
        nbt.put("Portals", portals);
        nbt.putBoolean("ImmptlActive", immptlActive);
        if (centralizerPos != null) {
            putPos(nbt, "Centralizer", centralizerPos);
        }
        return nbt;
    }

    public static TardisData fromNbt(NbtCompound nbt) {
        TardisData data = new TardisData(nbt.getUuid("Id"), nbt.getInt("Plot"));
        data.ownerUuid = nbt.containsUuid("Owner") ? nbt.getUuid("Owner") : null;
        data.interiorDoorPos = getPos(nbt, "Interior");
        data.interiorFacing = directionOrDefault(nbt.getString("InteriorFacing"), Direction.SOUTH);
        data.exteriorWorld = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(nbt.getString("ExteriorWorld")));
        data.exteriorPos = getPos(nbt, "Exterior");
        data.exteriorFacing = directionOrDefault(nbt.getString("ExteriorFacing"), Direction.NORTH);
        data.deployed = nbt.getBoolean("Deployed");
        data.open = nbt.getBoolean("Open");
        for (NbtElement element : nbt.getList("Portals", NbtElement.COMPOUND_TYPE)) {
            data.portalIds.add(((NbtCompound) element).getUuid("Id"));
        }
        data.immptlActive = nbt.getBoolean("ImmptlActive");
        data.centralizerPos = nbt.contains("Centralizer", NbtElement.INT_ARRAY_TYPE)
                ? getPos(nbt, "Centralizer") : null;
        return data;
    }

    private static Direction directionOrDefault(String name, Direction fallback) {
        Direction direction = Direction.byName(name);
        return direction != null && direction.getAxis().isHorizontal() ? direction : fallback;
    }

    private static void putPos(NbtCompound nbt, String key, BlockPos pos) {
        nbt.putIntArray(key, new int[]{pos.getX(), pos.getY(), pos.getZ()});
    }

    private static BlockPos getPos(NbtCompound nbt, String key) {
        int[] xyz = nbt.getIntArray(key);
        return xyz.length == 3 ? new BlockPos(xyz[0], xyz[1], xyz[2]) : BlockPos.ORIGIN;
    }
}
