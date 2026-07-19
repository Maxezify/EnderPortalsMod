package com.maxezify.enderportals.tardis;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Registre persistant (niveau sauvegarde) de tous les TARDIS. Attribue les
 * parcelles intérieures dans le monde de l'Ender.
 */
public class TardisStateManager extends PersistentState {

    /** Espacement entre deux parcelles intérieures, en blocs. */
    private static final int PLOT_SPACING = 1024;
    /** Hauteur de la porte intérieure dans le monde de l'Ender. */
    private static final int PLOT_Y = 64;

    private static final PersistentState.Type<TardisStateManager> TYPE =
            new PersistentState.Type<>(TardisStateManager::new, TardisStateManager::fromNbt, null);

    private final Map<UUID, TardisData> tardises = new HashMap<>();
    private int nextPlot;

    public static TardisStateManager get(MinecraftServer server) {
        return server.getWorld(World.OVERWORLD).getPersistentStateManager()
                .getOrCreate(TYPE, "enderportals_tardis");
    }

    public TardisData createTardis() {
        int plot = nextPlot++;
        TardisData data = new TardisData(UUID.randomUUID(), plot);
        data.interiorDoorPos = plotOrigin(plot);
        tardises.put(data.id, data);
        markDirty();
        return data;
    }

    @Nullable
    public TardisData getTardis(UUID id) {
        return tardises.get(id);
    }

    private static BlockPos plotOrigin(int plot) {
        return new BlockPos(plot * PLOT_SPACING + 8, PLOT_Y, 8);
    }

    public static TardisStateManager fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        TardisStateManager manager = new TardisStateManager();
        manager.nextPlot = nbt.getInt("NextPlot");
        for (NbtElement element : nbt.getList("Tardises", NbtElement.COMPOUND_TYPE)) {
            TardisData data = TardisData.fromNbt((NbtCompound) element);
            manager.tardises.put(data.id, data);
        }
        return manager;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        nbt.putInt("NextPlot", nextPlot);
        NbtList list = new NbtList();
        for (TardisData data : tardises.values()) {
            list.add(data.toNbt());
        }
        nbt.put("Tardises", list);
        return nbt;
    }
}
