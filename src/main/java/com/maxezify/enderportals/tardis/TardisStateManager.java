package com.maxezify.enderportals.tardis;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
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

    /**
     * Longueur du code d'ami. Fixe, donc la frappe au pavé fait toujours
     * exactement huit touches et il n'y a aucune longueur variable à gérer.
     */
    public static final int CODE_DIGITS = 8;

    /**
     * Les codes ne sont tirés que dans <b>1 à 9</b> : aucun zéro, nulle part.
     *
     * <p>C'est une contrainte d'interface remontée dans les données. Le pavé du
     * Contrôle de l'amitié n'a ainsi que neuf touches, en trois rangées pleines,
     * sans dixième touche orpheline sous elles. Le prix est mince : 9⁸, soit
     * plus de quarante-trois millions de combinaisons, largement de quoi rendre
     * la découverte au hasard illusoire.</p>
     */
    private static final int CODE_ALPHABET = 9;

    /** Tirage des codes d'ami. Voir {@link #freshCode()} pour le choix de l'API. */
    private static final RandomSource RANDOM = RandomSource.create();

    private final Map<UUID, TardisData> tardises = new HashMap<>();
    private final AllyLinks allyLinks = new AllyLinks();
    private final ConsoleLog consoleLog = new ConsoleLog();
    private int nextPlot;

    public static TardisStateManager get(MinecraftServer server) {
        return server.getLevel(Level.OVERWORLD).getDataStorage()
                .computeIfAbsent(FACTORY, "enderportals_tardis");
    }

    public TardisData createTardis(UUID owner, String ownerName) {
        // Le code est tiré avant de consommer un numéro de parcelle : rien de ce
        // qui suit ne peut échouer, donc aucun index ne peut être perdu en route.
        int code = freshCode();
        int plot = nextPlot++;
        TardisData data = new TardisData(UUID.randomUUID(), plot);
        data.ownerUuid = owner;
        data.ownerName = ownerName;
        data.interiorDoorPos = plotOrigin(plot);
        data.friendCode = code;
        tardises.put(data.id, data);
        setDirty();
        return data;
    }

    public AllyLinks allies() {
        return allyLinks;
    }

    /** Le journal affiché sur le terminal du Contrôle de l'amitié. */
    public ConsoleLog log() {
        return consoleLog;
    }

    /**
     * Un code libre. Le tirage est repris tant qu'il collisionne : à huit
     * chiffres et pour un nombre de joueurs réaliste, la boucle ne tourne
     * quasiment jamais deux fois, mais un code en double casserait
     * l'identification.
     *
     * <p>Le tirage passe par {@link RandomSource} et non par
     * {@code RandomGenerator.getDefault()} : celui-ci résout son algorithme par
     * {@code ServiceLoader}, et le module {@code jdk.random} n'est pas exposé
     * dans la couche de modules de Minecraft. L'appel y lève
     * {@code IllegalArgumentException} — « No implementation of the random number
     * generator algorithm "L32X64MixRandom" is available » — au lieu de rendre un
     * générateur. Aucune API du JDK reposant sur la découverte de services n'est
     * utilisable ici.</p>
     */
    private int freshCode() {
        while (true) {
            int code = 0;
            for (int digit = 0; digit < CODE_DIGITS; digit++) {
                code = code * 10 + 1 + RANDOM.nextInt(CODE_ALPHABET);
            }
            if (findByCode(code) == null) {
                return code;
            }
        }
    }

    /**
     * Ce code a-t-il la forme attendue : huit chiffres, aucun zéro ?
     *
     * <p>Sert à repérer les codes tirés par une version antérieure, qui
     * pouvaient contenir des zéros. Un tel code serait aujourd'hui intapable —
     * le pavé n'a plus de touche zéro — donc il est retiré.</p>
     */
    private static boolean isWellFormed(int code) {
        if (code < 11_111_111 || code > 99_999_999) {
            return false;
        }
        for (int rest = code; rest > 0; rest /= 10) {
            if (rest % 10 == 0) {
                return false;
            }
        }
        return true;
    }

    /** La porte dont c'est le code d'ami, s'il en existe une. */
    @Nullable
    public TardisData findByCode(int code) {
        if (!isWellFormed(code)) {
            return null;
        }
        for (TardisData data : tardises.values()) {
            if (data.friendCode == code) {
                return data;
            }
        }
        return null;
    }

    /** Le code d'ami de ce joueur, ou 0 s'il n'a pas encore éveillé de porte. */
    public int codeOf(UUID owner) {
        TardisData data = findByOwner(owner);
        return data == null ? 0 : data.friendCode;
    }

    /** Le pseudo affiché pour ce joueur, tel que retenu à l'éveil de sa porte. */
    public String nameOf(UUID owner) {
        TardisData data = findByOwner(owner);
        return data == null ? "" : data.ownerName;
    }

    /** Lie le Passage des Alliés posé à la parcelle du joueur. */
    public void setPassage(UUID owner, BlockPos pos, Direction facing) {
        TardisData data = findByOwner(owner);
        if (data != null) {
            data.passagePos = pos;
            data.passageFacing = facing;
            setDirty();
        }
    }

    /** La porte à qui appartient le Passage posé à cette position. */
    @Nullable
    public TardisData findByPassage(BlockPos pos) {
        for (TardisData data : tardises.values()) {
            if (pos.equals(data.passagePos)) {
                return data;
            }
        }
        return null;
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
        manager.allyLinks.load(nbt.getCompound("Allies"));
        manager.consoleLog.load(nbt.getCompound("ConsoleLog"));
        // Deux cas à rattraper ici, une fois toutes les portes chargées pour que
        // freshCode() voie bien les codes déjà pris : celles éveillées avant
        // l'arrivée du Passage des Alliés n'ont pas de code du tout, et celles
        // d'avant l'abandon du zéro en portent un devenu intapable.
        for (TardisData data : manager.tardises.values()) {
            if (!isWellFormed(data.friendCode)) {
                data.friendCode = manager.freshCode();
                manager.setDirty();
            }
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
        nbt.put("Allies", allyLinks.toNbt());
        nbt.put("ConsoleLog", consoleLog.toNbt());
        return nbt;
    }
}
