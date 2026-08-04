package com.maxezify.enderportals.tardis;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Le carnet d'adresses du Passage des Alliés : qui a déclaré qui, qui attend
 * une réponse, et quels passages sont ouverts.
 *
 * <p>Trois notions distinctes, qu'il vaut mieux ne pas confond:</p>
 * <ol>
 *   <li><b>La déclaration</b> est à sens unique — j'ai tapé le code de
 *       quelqu'un sur mon panneau. Elle est <b>durable</b>.</li>
 *   <li><b>L'amitié</b> est la conjonction de deux déclarations opposées. Elle
 *       n'est donc pas stockée : elle se lit. C'est ce qui garantit qu'elle ne
 *       peut pas se désynchroniser — il n'existe aucun état où l'un se croirait
 *       ami et l'autre non.</li>
 *   <li><b>La connexion</b> est éphémère : une poignée de main à deux mains,
 *       valable {@value #REQUEST_TIMEOUT_TICKS} ticks, qui aboutit à un
 *       <b>lien</b> ouvert jusqu'à ce que l'un des deux le ferme.</li>
 * </ol>
 *
 * <p>Les liens sont stockés dans les deux sens. C'est une redondance assumée :
 * elle rend la lecture immédiate depuis n'importe quel bout, et
 * {@link #openLink} / {@link #closeLink} sont les deux seuls endroits qui
 * écrivent, donc les deux seuls à pouvoir la rompre.</p>
 */
public class AllyLinks {

    /** Deux minutes pour que l'autre réponde à une demande de connexion. */
    public static final long REQUEST_TIMEOUT_TICKS = 2400L;

    /** Déclarations, à sens unique : déclarant → déclarés. */
    private final Map<UUID, Set<UUID>> declared = new HashMap<>();
    /** Demandes de connexion en cours — au plus une par joueur. */
    private final Map<UUID, Request> requests = new HashMap<>();
    /** Liens ouverts, stockés dans les deux sens. */
    private final Map<UUID, UUID> links = new HashMap<>();

    /**
     * Panneaux ouverts à l'écran, pour pouvoir rafraîchir l'affichage d'un
     * joueur quand c'est l'action d'un <i>autre</i> qui change son état. Non
     * persisté : un écran ne survit pas à une déconnexion.
     */
    private final Map<UUID, BlockPos> viewers = new HashMap<>();

    /** Une demande de connexion : vers qui, et jusqu'à quand. */
    private record Request(UUID target, long expiresAt) {
    }

    /** Ce que devient une déclaration de code. */
    public enum DeclareResult {
        /** Enregistrée, en attente que l'autre fasse de même. */
        PENDING,
        /** L'autre avait déjà déclaré : l'amitié est scellée. */
        CONFIRMED,
        /** Déjà déclaré auparavant. */
        ALREADY,
        /** Son propre code. */
        SELF,
        /** Aucun joueur ne porte ce code. */
        UNKNOWN,
    }

    /** Ce que devient un clic sur le nom d'un ami. */
    public enum ConnectResult {
        /** Demande envoyée, l'autre a deux minutes pour répondre. */
        REQUESTED,
        /** L'autre attendait : le passage s'ouvre. */
        OPENED,
        /** Le lien existait : il se ferme. */
        CLOSED,
        /** Pas (encore) amis des deux côtés. */
        NOT_FRIENDS,
    }

    // ------------------------------------------------------------------
    // Déclarations
    // ------------------------------------------------------------------

    public DeclareResult declare(UUID from, UUID to) {
        if (from.equals(to)) {
            return DeclareResult.SELF;
        }
        Set<UUID> mine = declared.computeIfAbsent(from, key -> new HashSet<>());
        if (!mine.add(to)) {
            return DeclareResult.ALREADY;
        }
        return isConfirmed(from, to) ? DeclareResult.CONFIRMED : DeclareResult.PENDING;
    }

    /** Les joueurs que celui-ci a déclarés, dans l'ordre de son carnet. */
    public List<UUID> declaredBy(UUID player) {
        Set<UUID> mine = declared.get(player);
        return mine == null ? List.of() : new ArrayList<>(mine);
    }

    public boolean hasDeclared(UUID from, UUID to) {
        Set<UUID> mine = declared.get(from);
        return mine != null && mine.contains(to);
    }

    /** L'amitié se lit, elle ne se stocke pas : les deux sens doivent exister. */
    public boolean isConfirmed(UUID a, UUID b) {
        return hasDeclared(a, b) && hasDeclared(b, a);
    }

    /**
     * Retire un joueur du carnet d'un autre.
     *
     * <p>Tout ce qui est touché ici est <b>circonscrit à cette paire</b>.
     * Oublier un allié ne doit annuler aucune demande ni fermer aucun passage
     * concernant un tiers — c'est pourquoi chaque suppression est conditionnée à
     * ce qu'elle porte bien sur l'autre bout de la paire.</p>
     */
    public void forget(UUID from, UUID to) {
        Set<UUID> mine = declared.get(from);
        if (mine != null) {
            mine.remove(to);
            if (mine.isEmpty()) {
                declared.remove(from);
            }
        }
        if (targetOf(from) != null && to.equals(targetOf(from))) {
            requests.remove(from);
        }
        if (targetOf(to) != null && from.equals(targetOf(to))) {
            requests.remove(to);
        }
        if (to.equals(links.get(from))) {
            closeLink(from);
        }
    }

    /** Cible brute d'une demande, sans regarder sa péremption. */
    @Nullable
    private UUID targetOf(UUID player) {
        Request request = requests.get(player);
        return request == null ? null : request.target();
    }

    // ------------------------------------------------------------------
    // Connexions
    // ------------------------------------------------------------------

    /**
     * Un joueur clique sur le nom d'un ami. Selon l'état : ferme le lien
     * ouvert, scelle la connexion si l'autre attendait, ou pose une demande.
     */
    public ConnectResult toggleConnect(UUID from, UUID to, long gameTime) {
        if (to.equals(linkOf(from))) {
            closeLink(from);
            return ConnectResult.CLOSED;
        }
        if (!isConfirmed(from, to)) {
            return ConnectResult.NOT_FRIENDS;
        }
        // L'autre a-t-il une demande vivante tournée vers moi ?
        Request theirs = liveRequest(to, gameTime);
        if (theirs != null && theirs.target().equals(from)) {
            requests.remove(to);
            requests.remove(from);
            lastDisplaced.clear();
            lastDisplaced.addAll(openLink(from, to));
            return ConnectResult.OPENED;
        }
        requests.put(from, new Request(to, gameTime + REQUEST_TIMEOUT_TICKS));
        return ConnectResult.REQUESTED;
    }

    /**
     * La demande de ce joueur si elle est encore valable. Les demandes périmées
     * sont retirées à la lecture : elles sont au plus une par joueur, une purge
     * périodique serait du travail pour rien.
     */
    @Nullable
    private Request liveRequest(UUID player, long gameTime) {
        Request request = requests.get(player);
        if (request == null) {
            return null;
        }
        if (gameTime >= request.expiresAt()) {
            requests.remove(player);
            return null;
        }
        return request;
    }

    /** Vers qui ce joueur a une demande en cours, ou {@code null}. */
    @Nullable
    public UUID pendingRequestTarget(UUID player, long gameTime) {
        Request request = liveRequest(player, gameTime);
        return request == null ? null : request.target();
    }

    /** Le joueur avec qui ce passage est ouvert, ou {@code null}. */
    @Nullable
    public UUID linkOf(UUID player) {
        return links.get(player);
    }

    /**
     * Un joueur n'a qu'un passage : ouvrir un lien ferme donc le précédent, des
     * deux côtés. Les pairs ainsi délogés sont rendus à l'appelant, à qui il
     * revient de refermer leurs arches — sans quoi elles resteraient ouvertes à
     * l'écran sans mener nulle part.
     */
    private List<UUID> openLink(UUID a, UUID b) {
        List<UUID> displaced = new ArrayList<>(2);
        UUID freedByA = closeLink(a);
        UUID freedByB = closeLink(b);
        if (freedByA != null && !freedByA.equals(b)) {
            displaced.add(freedByA);
        }
        if (freedByB != null && !freedByB.equals(a)) {
            displaced.add(freedByB);
        }
        links.put(a, b);
        links.put(b, a);
        return displaced;
    }

    /** Pairs délogés par la dernière ouverture — vidé à chaque appel. */
    private final List<UUID> lastDisplaced = new ArrayList<>();

    /**
     * Les pairs que la dernière connexion ouverte a délogés.
     *
     * <p>À n'appeler qu'immédiatement après un {@link ConnectResult#OPENED},
     * dans le même tick : la liste n'est renseignée que sur ce chemin, et rien
     * ne la vide ailleurs. Appelée dans un autre contexte, elle rendrait le
     * résultat d'une ouverture antérieure.</p>
     */
    public List<UUID> takeDisplaced() {
        List<UUID> copy = List.copyOf(lastDisplaced);
        lastDisplaced.clear();
        return copy;
    }

    /** Ferme le lien de ce joueur et rend le pair qui vient d'être libéré. */
    @Nullable
    public UUID closeLink(UUID player) {
        UUID other = links.remove(player);
        if (other != null) {
            links.remove(other);
        }
        return other;
    }

    // ------------------------------------------------------------------
    // Panneaux ouverts
    // ------------------------------------------------------------------

    public void setViewing(UUID player, @Nullable BlockPos console) {
        if (console == null) {
            viewers.remove(player);
        } else {
            viewers.put(player, console);
        }
    }

    @Nullable
    public BlockPos viewedConsole(UUID player) {
        return viewers.get(player);
    }

    /**
     * Les joueurs qui ont un panneau à l'écran. La copie est délibérée :
     * l'appelant leur renvoie leur état, et ce parcours retire au passage les
     * demandes de connexion périmées — donc écrit dans les cartes voisines.
     */
    public List<UUID> viewers() {
        return List.copyOf(viewers.keySet());
    }

    // ------------------------------------------------------------------
    // Persistance
    // ------------------------------------------------------------------

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        ListTag declarations = new ListTag();
        for (Map.Entry<UUID, Set<UUID>> entry : declared.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("From", entry.getKey());
            ListTag targets = new ListTag();
            for (UUID target : entry.getValue()) {
                CompoundTag one = new CompoundTag();
                one.putUUID("To", target);
                targets.add(one);
            }
            tag.put("To", targets);
            declarations.add(tag);
        }
        nbt.put("Declared", declarations);

        ListTag openLinks = new ListTag();
        Set<UUID> written = new HashSet<>();
        for (Map.Entry<UUID, UUID> entry : links.entrySet()) {
            // Un lien par paire : on n'écrit que le premier bout rencontré.
            if (written.contains(entry.getKey()) || written.contains(entry.getValue())) {
                continue;
            }
            written.add(entry.getKey());
            written.add(entry.getValue());
            CompoundTag tag = new CompoundTag();
            tag.putUUID("A", entry.getKey());
            tag.putUUID("B", entry.getValue());
            openLinks.add(tag);
        }
        nbt.put("Links", openLinks);
        // Les demandes en cours ne sont pas persistées : deux minutes ne
        // survivent pas à un redémarrage de serveur, et les faire survivre
        // ouvrirait un passage que plus personne n'attend.
        return nbt;
    }

    public void load(CompoundTag nbt) {
        declared.clear();
        links.clear();
        requests.clear();
        for (Tag element : nbt.getList("Declared", Tag.TAG_COMPOUND)) {
            CompoundTag tag = (CompoundTag) element;
            Set<UUID> targets = new HashSet<>();
            for (Tag one : tag.getList("To", Tag.TAG_COMPOUND)) {
                targets.add(((CompoundTag) one).getUUID("To"));
            }
            if (!targets.isEmpty()) {
                declared.put(tag.getUUID("From"), targets);
            }
        }
        for (Tag element : nbt.getList("Links", Tag.TAG_COMPOUND)) {
            CompoundTag tag = (CompoundTag) element;
            UUID a = tag.getUUID("A");
            UUID b = tag.getUUID("B");
            links.put(a, b);
            links.put(b, a);
        }
    }
}
