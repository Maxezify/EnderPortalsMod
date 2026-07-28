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

    /**
     * Espacement entre deux parcelles intérieures, en blocs. Les parcelles
     * s'égrènent sur l'axe X. Le générateur du monde de l'Ender s'appuie sur
     * cette valeur pour placer ses murs de bedrock : les deux doivent rester
     * d'accord, d'où la constante partagée.
     */
    public static final int PLOT_SPACING = 8192;
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

    public TardisData createTardis(UUID owner, String ownerName) {
        int plot = nextPlot++;
        TardisData data = new TardisData(UUID.randomUUID(), plot);
        data.ownerUuid = owner;
        data.ownerName = ownerName;
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

    /**
     * Emplacement de la porte intérieure de la n-ième parcelle. Les parcelles
     * s'enroulent en spirale carrée autour de l'origine plutôt que de filer sur
     * le seul axe X : en ligne, la ~3660ᵉ porte serait sortie de la bordure du
     * monde (±29 999 984), alors que la spirale tient plus de 13 millions de
     * parcelles dans la même bordure.
     *
     * <p>Les parcelles 0 et 1 tombent aux mêmes coordonnées qu'avec l'ancienne
     * disposition en ligne, et les positions déjà attribuées sont de toute
     * façon persistées : aucune base existante ne bouge.</p>
     */
    private static BlockPos plotOrigin(int plot) {
        int[] cell = spiralCell(plot);
        return new BlockPos(cell[0] * PLOT_SPACING + 8, PLOT_Y, cell[1] * PLOT_SPACING + 8);
    }

    /**
     * Coordonnées de cellule {@code {x, z}} du n-ième point d'une spirale
     * carrée centrée sur l'origine : l'anneau {@code r} porte ses {@code 8r}
     * cellules, parcourues bord est, sud, ouest puis nord.
     */
    private static int[] spiralCell(int index) {
        if (index <= 0) {
            return new int[]{0, 0};
        }
        int ring = 0;
        while ((2 * ring + 1) * (2 * ring + 1) <= index) {
            ring++;
        }
        // Cellules des anneaux précédents, puis rang sur l'anneau courant.
        int offset = index - (2 * ring - 1) * (2 * ring - 1);
        int side = offset / (2 * ring);
        int step = offset % (2 * ring);
        return switch (side) {
            case 0 -> new int[]{ring, -ring + 1 + step};
            case 1 -> new int[]{ring - 1 - step, ring};
            case 2 -> new int[]{-ring, ring - 1 - step};
            default -> new int[]{-ring + 1 + step, -ring};
        };
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
