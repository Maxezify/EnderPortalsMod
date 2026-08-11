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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

    /** Longueur du code d'ami. Sa forme complète vit dans {@link FriendCode}. */
    public static final int CODE_DIGITS = FriendCode.DIGITS;

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

    /**
     * Inscrit un Passage des Alliés fraîchement posé au registre de son
     * propriétaire. Un joueur en pose autant qu'il veut : chacun s'ajoute à la
     * liste au lieu de remplacer le précédent.
     */
    public void addPassage(UUID owner, BlockPos pos, Direction facing) {
        TardisData data = findByOwner(owner);
        if (data == null) {
            return;
        }
        PassageData existing = passageAt(data, pos);
        if (existing != null) {
            existing.facing = facing;
        } else {
            data.passages.add(new PassageData(pos, facing));
        }
        setDirty();
    }

    /** Retire une arche du registre. */
    public void removePassage(TardisData data, BlockPos pos) {
        if (data.passages.remove(passageAt(data, pos))) {
            setDirty();
        }
    }

    /** La porte à qui appartient le Passage posé à cette position. */
    @Nullable
    public TardisData findByPassage(BlockPos pos) {
        for (TardisData data : tardises.values()) {
            if (passageAt(data, pos) != null) {
                return data;
            }
        }
        return null;
    }

    /** L'arche de ce joueur posée à cette position, ou {@code null}. */
    @Nullable
    public static PassageData passageAt(TardisData data, BlockPos pos) {
        for (PassageData passage : data.passages) {
            if (pos.equals(passage.pos)) {
                return passage;
            }
        }
        return null;
    }

    /** L'arche de ce joueur liée à cet allié, ou {@code null}. */
    @Nullable
    public static PassageData passageTo(@Nullable TardisData data, UUID ally) {
        if (data == null) {
            return null;
        }
        for (PassageData passage : data.passages) {
            if (passage.leadsTo(ally)) {
                return passage;
            }
        }
        return null;
    }

    /**
     * Le lien avec cet allié est-il noué des deux côtés ?
     *
     * <p>Comme l'amitié, il se lit au lieu de se stocker : deux arches liées
     * l'une vers l'autre. Aucun état ne peut donc prétendre qu'un seul des deux
     * est connecté — au pire une arche pointe dans le vide, et c'est ce que le
     * panneau appelle un lien à sens unique.</p>
     */
    public boolean isLinked(UUID a, UUID b) {
        return passageTo(findByOwner(a), b) != null && passageTo(findByOwner(b), a) != null;
    }

    /**
     * Dénoue le lien porté par cette arche, des deux côtés, et rend l'allié qui
     * vient d'être libéré. L'arche de l'allié est déliée elle aussi : un lien
     * est une paire, et n'en défaire qu'une moitié laisserait l'autre ouverte
     * sur rien.
     */
    @Nullable
    public UUID unlink(@Nullable TardisData owner, @Nullable PassageData passage) {
        if (owner == null || passage == null || passage.ally == null || owner.ownerUuid == null) {
            return null;
        }
        UUID ally = passage.ally;
        passage.ally = null;
        PassageData theirs = passageTo(findByOwner(ally), owner.ownerUuid);
        if (theirs != null) {
            theirs.ally = null;
        }
        setDirty();
        return ally;
    }

    /**
     * Noue le lien entre deux arches, en écartant d'abord ce qui s'y opposait.
     *
     * <p>Deux exclusions, et elles ne disent pas la même chose : une arche ne
     * porte qu'un lien, et un allié n'est joignable que par une arche. La
     * première déloge l'ancien occupant de l'arche ; la seconde évite qu'un même
     * ami se retrouve au bout de deux couloirs, ce que rien n'interdirait
     * autrement et que la traversée ne saurait pas départager.</p>
     *
     * <p>Les <b>quatre</b> dénouements précèdent les deux nouages, et l'ordre
     * n'est pas indifférent : dénouer une arche dénoue aussi celle d'en face, si
     * bien que lier les deux côtés l'un après l'autre défaisait le premier en
     * posant le second. Une paire se noue d'un seul geste.</p>
     *
     * <p>Les alliés ainsi libérés sont rendus à l'appelant, à qui il revient de
     * refermer leurs arches — sans quoi elles resteraient ouvertes sur rien.</p>
     */
    public List<UUID> bind(TardisData a, PassageData pa, TardisData b, PassageData pb) {
        List<UUID> displaced = new ArrayList<>(4);
        collectDisplaced(displaced, unlink(a, pa), b.ownerUuid);
        collectDisplaced(displaced, unlink(b, pb), a.ownerUuid);
        collectDisplaced(displaced, unlink(a, passageTo(a, b.ownerUuid)), b.ownerUuid);
        collectDisplaced(displaced, unlink(b, passageTo(b, a.ownerUuid)), a.ownerUuid);
        pa.ally = b.ownerUuid;
        pb.ally = a.ownerUuid;
        setDirty();
        return displaced;
    }

    /** Un allié n'est « délogé » que s'il n'est pas celui qu'on est en train de lier. */
    private static void collectDisplaced(List<UUID> into, @Nullable UUID freed, @Nullable UUID partner) {
        if (freed != null && !freed.equals(partner)) {
            into.add(freed);
        }
    }

    /**
     * Reporte sur les arches les liens d'une sauvegarde d'avant la 0.19.0.
     *
     * <p>Le lien y vivait dans le carnet, à raison d'un par joueur, et l'arche
     * y était unique : les deux se retrouvent donc sans ambiguïté. Passé ce
     * point les liens hérités sont oubliés, et la sauvegarde suivante n'en garde
     * aucune trace.</p>
     */
    private void adoptLegacyLinks() {
        for (TardisData data : tardises.values()) {
            if (data.ownerUuid == null || data.passages.size() != 1) {
                continue;
            }
            PassageData passage = data.passages.get(0);
            if (passage.ally == null) {
                passage.ally = allyLinks.legacyLinkOf(data.ownerUuid);
            }
        }
        allyLinks.forgetLegacyLinks();
    }

    @Nullable
    public TardisData getTardis(UUID id) {
        return tardises.get(id);
    }

    /**
     * Toutes les parcelles connues, en lecture seule.
     *
     * <p>Ouvert pour {@link PlotGuard}, qui doit retrouver la parcelle sous une
     * position. Il le fait en comparant des positions de porte plutôt qu'en
     * inversant la spirale : le rang d'une parcelle et l'endroit où sa porte a
     * réellement été posée se sont désaccordés le jour où l'espacement est passé
     * de 1024 à 8192 blocs, et les bases d'alors ont gardé leur position. Partir
     * de la position persistée n'a pas ce défaut — c'est la même fonction
     * d'enclos qui est appliquée aux deux bouts de la comparaison.</p>
     */
    public Collection<TardisData> all() {
        return Collections.unmodifiableCollection(tardises.values());
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
        manager.adoptLegacyLinks();
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
