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
 * Le carnet d'adresses du Passage des Alliés : qui a déclaré qui, et qui attend
 * une réponse.
 *
 * <p>Trois notions distinctes, qu'il vaut mieux ne pas confondre :</p>
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
 * <p>Le lien lui-même n'est plus ici depuis la 0.19.0 : il vit sur l'arche, dans
 * {@link PassageData#ally}. Un joueur pose autant de Passages qu'il a d'amis à
 * relier, et une carte {@code joueur → allié} ne pouvait en tenir qu'un. La
 * conséquence pour cette classe est que la demande de connexion doit retenir
 * <b>de quelle arche</b> elle part : c'est le panneau utilisé qui désigne
 * l'arche à lier, et la réponse arrive une minute plus tard, devant une
 * autre.</p>
 */
public class AllyLinks {

    /** Deux minutes pour que l'autre réponde à une demande de connexion. */
    public static final long REQUEST_TIMEOUT_TICKS = 2400L;

    /** Déclarations, à sens unique : déclarant → déclarés. */
    private final Map<UUID, Set<UUID>> declared = new HashMap<>();
    /** Demandes de connexion en cours — au plus une par joueur. */
    private final Map<UUID, Request> requests = new HashMap<>();

    /**
     * Liens lus dans une sauvegarde d'avant la 0.19.0, le temps d'un chargement.
     *
     * <p>{@link TardisStateManager#load} les reporte sur l'arche unique de
     * chaque joueur puis vide cette carte, qui n'est jamais réécrite : la
     * migration ne se joue qu'une fois, et la sauvegarde suivante n'en garde
     * aucune trace.</p>
     */
    private final Map<UUID, UUID> legacyLinks = new HashMap<>();

    /**
     * Panneaux ouverts à l'écran, pour pouvoir rafraîchir l'affichage d'un
     * joueur quand c'est l'action d'un <i>autre</i> qui change son état. Non
     * persisté : un écran ne survit pas à une déconnexion.
     */
    private final Map<UUID, BlockPos> viewers = new HashMap<>();

    /**
     * Une demande de connexion : vers qui, depuis quelle arche, et jusqu'à
     * quand. L'arche est celle que commandait le panneau utilisé — c'est elle
     * qui se liera si l'autre accepte, et non « l'arche du joueur », qui n'a
     * plus de sens depuis qu'il peut en avoir plusieurs.
     */
    private record Request(UUID target, BlockPos passage, long expiresAt) {
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
        // Le lien lui-même vit sur l'arche : c'est l'appelant qui le dénoue,
        // parce que lui seul voit les deux fiches de TARDIS.
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
     * Ce qu'un clic sur le nom d'un ami produit, et l'arche à lier quand il
     * aboutit.
     *
     * <p>{@code theirPassage} n'est renseigné que sur {@link ConnectResult#OPENED} :
     * c'est l'arche que l'autre avait sous les yeux au moment de sa demande.
     * Sans elle, le serveur saurait qu'il faut ouvrir mais pas <b>laquelle</b>
     * de ses arches lier — une question qui ne se posait pas tant qu'il n'en
     * avait qu'une.</p>
     */
    public record Connect(ConnectResult result, @Nullable BlockPos theirPassage) {
    }

    /**
     * Un joueur clique sur le nom d'un ami, devant le panneau qui commande
     * l'arche {@code myPassage}. Selon l'état : scelle la connexion si l'autre
     * attendait, ferme le lien ouvert, ou pose une demande.
     *
     * <p>{@code alreadyLinked} vient de l'appelant : le lien est sur les arches,
     * et cette classe ne les voit pas.</p>
     */
    public Connect toggleConnect(UUID from, UUID to, long gameTime, boolean alreadyLinked,
                                 BlockPos myPassage) {
        if (alreadyLinked) {
            requests.remove(from);
            return new Connect(ConnectResult.CLOSED, null);
        }
        if (!isConfirmed(from, to)) {
            return new Connect(ConnectResult.NOT_FRIENDS, null);
        }
        // L'autre a-t-il une demande vivante tournée vers moi ?
        Request theirs = liveRequest(to, gameTime);
        if (theirs != null && theirs.target().equals(from)) {
            requests.remove(to);
            requests.remove(from);
            return new Connect(ConnectResult.OPENED, theirs.passage());
        }
        requests.put(from, new Request(to, myPassage, gameTime + REQUEST_TIMEOUT_TICKS));
        return new Connect(ConnectResult.REQUESTED, null);
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

        // Ni les liens ni les demandes ne s'écrivent ici. Les liens sont sur les
        // arches depuis la 0.19.0 ; les demandes ne survivent pas à un
        // redémarrage, deux minutes n'ayant aucun sens en travers d'un arrêt —
        // et les faire survivre ouvrirait un passage que plus personne n'attend.
        return nbt;
    }

    public void load(CompoundTag nbt) {
        declared.clear();
        requests.clear();
        legacyLinks.clear();
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
        // « Links » n'existe que dans les sauvegardes d'avant la 0.19.0.
        for (Tag element : nbt.getList("Links", Tag.TAG_COMPOUND)) {
            CompoundTag tag = (CompoundTag) element;
            UUID a = tag.getUUID("A");
            UUID b = tag.getUUID("B");
            legacyLinks.put(a, b);
            legacyLinks.put(b, a);
        }
    }

    /**
     * L'allié auquel ce joueur était lié dans une sauvegarde d'avant la 0.19.0.
     * Vidé par {@link #forgetLegacyLinks()} sitôt reporté sur les arches.
     */
    @Nullable
    UUID legacyLinkOf(UUID player) {
        return legacyLinks.get(player);
    }

    void forgetLegacyLinks() {
        legacyLinks.clear();
    }
}
