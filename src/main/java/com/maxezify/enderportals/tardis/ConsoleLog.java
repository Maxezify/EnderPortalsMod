package com.maxezify.enderportals.tardis;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Le journal du Contrôle de l'amitié : ce que le panneau a à dire à chaque
 * joueur, gardé côté serveur et affiché sur le terminal du panneau.
 *
 * <p>Ces messages partaient jusqu'ici dans le chat, en bleu. Le chat est le
 * mauvais endroit pour eux : ils arrivent pendant qu'on manipule le panneau,
 * c'est-à-dire au moment précis où l'on regarde ailleurs, et ils se noient dans
 * le reste du bavardage. Ils sont donc conservés ici et affichés là où on les
 * attend — sur l'appareil qui les produit.</p>
 *
 * <p>Le journal est <b>persisté</b>, et c'est ce qui le rend meilleur que le
 * chat plutôt qu'équivalent : un message destiné à un joueur hors ligne était
 * simplement perdu ({@code getPlayerList().getPlayer(…)} rend {@code null}), là
 * où il l'attend désormais à sa prochaine ouverture du panneau. « X est votre
 * ami de passage » se lit enfin même quand X a tapé le code pendant la nuit.</p>
 *
 * <p>La taille est bornée à {@value #CAPACITY} lignes par joueur : un journal
 * qui grandit sans fin finirait dans le fichier de sauvegarde du monde.</p>
 */
public final class ConsoleLog {

    /** Ton d'une ligne — ce qui décide de sa couleur au terminal. */
    public static final int INFO = 0;
    /** Une bonne nouvelle : l'amitié scellée, le passage ouvert. */
    public static final int GOOD = 1;
    /** Ce qui appelle un geste, ou une opération refusée sans gravité. */
    public static final int WARN = 2;
    /** Un refus net : code inconnu, passage absent. */
    public static final int BAD = 3;

    /** Lignes gardées par joueur. Au-delà, la plus ancienne tombe. */
    public static final int CAPACITY = 24;

    /** Longueurs admises sur le fil. Le lecteur du paquet s'y accorde. */
    public static final int MAX_KEY = 96;
    public static final int MAX_ARG = 48;
    public static final int MAX_ARGS = 4;

    /**
     * Une ligne du journal : la clé de traduction et ses arguments, jamais le
     * texte résolu. Chacun lit ainsi son terminal dans sa propre langue, et le
     * message d'un joueur anglais reste français chez son allié français.
     */
    public record Entry(String key, List<String> args, int tone) {

        public Entry {
            args = List.copyOf(args);
        }
    }

    private final Map<UUID, Deque<Entry>> lines = new HashMap<>();

    /**
     * Ajoute une ligne au journal de ce joueur, qu'il soit en ligne ou non.
     *
     * <p>Les arguments sont convertis en texte à l'écriture : ce sont des
     * pseudos et des codes, et les figer ici évite d'avoir à les retrouver plus
     * tard — le joueur cité peut très bien avoir été oublié du carnet entre
     * temps.</p>
     */
    public void add(UUID player, int tone, String key, Object... args) {
        List<String> text = new ArrayList<>(Math.min(args.length, MAX_ARGS));
        for (int i = 0; i < args.length && i < MAX_ARGS; i++) {
            String value = String.valueOf(args[i]);
            text.add(value.length() <= MAX_ARG ? value : value.substring(0, MAX_ARG));
        }
        Deque<Entry> mine = lines.computeIfAbsent(player, ignored -> new ArrayDeque<>(CAPACITY));
        mine.addLast(new Entry(key, text, tone));
        while (mine.size() > CAPACITY) {
            mine.removeFirst();
        }
    }

    /** Les lignes de ce joueur, de la plus ancienne à la plus récente. */
    public List<Entry> of(UUID player) {
        Deque<Entry> mine = lines.get(player);
        return mine == null ? List.of() : List.copyOf(mine);
    }

    // ------------------------------------------------------------------
    // Persistance
    // ------------------------------------------------------------------

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        ListTag players = new ListTag();
        for (Map.Entry<UUID, Deque<Entry>> entry : lines.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Player", entry.getKey());
            ListTag written = new ListTag();
            for (Entry line : entry.getValue()) {
                CompoundTag one = new CompoundTag();
                one.putString("Key", line.key());
                one.putByte("Tone", (byte) line.tone());
                ListTag args = new ListTag();
                for (String arg : line.args()) {
                    args.add(StringTag.valueOf(arg));
                }
                one.put("Args", args);
                written.add(one);
            }
            tag.put("Lines", written);
            players.add(tag);
        }
        nbt.put("Players", players);
        return nbt;
    }

    public void load(CompoundTag nbt) {
        lines.clear();
        for (Tag element : nbt.getList("Players", Tag.TAG_COMPOUND)) {
            CompoundTag tag = (CompoundTag) element;
            Deque<Entry> mine = new ArrayDeque<>(CAPACITY);
            for (Tag one : tag.getList("Lines", Tag.TAG_COMPOUND)) {
                CompoundTag line = (CompoundTag) one;
                List<String> args = new ArrayList<>(MAX_ARGS);
                for (Tag arg : line.getList("Args", Tag.TAG_STRING)) {
                    args.add(arg.getAsString());
                }
                mine.addLast(new Entry(line.getString("Key"), args, line.getByte("Tone")));
            }
            // Une sauvegarde écrite par une version aux bornes plus larges ne
            // doit pas faire déborder le paquet d'état : on tronque à l'entrée.
            while (mine.size() > CAPACITY) {
                mine.removeFirst();
            }
            if (!mine.isEmpty()) {
                lines.put(tag.getUUID("Player"), mine);
            }
        }
    }
}
