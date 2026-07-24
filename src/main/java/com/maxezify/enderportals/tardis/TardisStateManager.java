package com.maxezify.enderportals.tardis;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Registre persistant (niveau sauvegarde) de tous les TARDIS. Attribue les
 * parcelles intérieures dans le monde de l'Ender.
 */
public class TardisStateManager extends SavedData {

    /** Espacement entre deux parcelles intérieures, en blocs. */
    private static final int PLOT_SPACING = 1024;
    /** Hauteur de la porte intérieure dans le monde de l'Ender. */
    private static final int PLOT_Y = 64;

    private static final SavedData.Factory<TardisStateManager> FACTORY =
            new SavedData.Factory<>(TardisStateManager::new, TardisStateManager::load, null);

    private final Map<UUID, TardisData> tardises = new HashMap<>();
    private int nextPlot;

    public static TardisStateManager get(MinecraftServer server) {
        return server.getLevel(Level.OVERWORLD).getDataStorage()
                .computeIfAbsent(FACTORY, "enderportals_tardis");
    }

    public TardisData createTardis(UUID owner) {
        int plot = nextPlot++;
        TardisData data = new TardisData(UUID.randomUUID(), plot);
        data.ownerUuid = owner;
        data.interiorDoorPos = plotOrigin(plot);
        tardises.put(data.id, data);
        setDirty();
        return data;
    }

    @Nullable
    public TardisData getTardis(UUID id) {
        return tardises.get(id);
    }

    /** La porte déjà éveillée par ce joueur, s'il en a une. */
    @Nullable
    public TardisData findByOwner(UUID owner) {
        for (TardisData data : tardises.values()) {
            if (owner.equals(data.ownerUuid)) {
                return data;
            }
        }
        return null;
    }

    /** Lie le centraliseur posé à la parcelle du joueur (le dernier posé gagne). */
    public void setCentralizer(UUID owner, BlockPos pos) {
        TardisData data = findByOwner(owner);
        if (data != null) {
            data.centralizerPos = pos;
            setDirty();
        }
    }

    /** Rompt le lien d'un centraliseur cassé à cette position, s'il existait. */
    public void clearCentralizer(BlockPos pos) {
        for (TardisData data : tardises.values()) {
            if (pos.equals(data.centralizerPos)) {
                data.centralizerPos = null;
                setDirty();
            }
        }
    }

    private static BlockPos plotOrigin(int plot) {
        return new BlockPos(plot * PLOT_SPACING + 8, PLOT_Y, 8);
    }

    public static TardisStateManager load(CompoundTag nbt, HolderLookup.Provider registries) {
        TardisStateManager manager = new TardisStateManager();
        manager.nextPlot = nbt.getInt("NextPlot");
        for (Tag element : nbt.getList("Tardises", Tag.TAG_COMPOUND)) {
            TardisData data = TardisData.fromNbt((CompoundTag) element);
            manager.tardises.put(data.id, data);
        }
        return manager;
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.putInt("NextPlot", nextPlot);
        ListTag list = new ListTag();
        for (TardisData data : tardises.values()) {
            list.add(data.toNbt());
        }
        nbt.put("Tardises", list);
        return nbt;
    }
}
