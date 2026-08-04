package com.maxezify.enderportals.network;

import com.maxezify.enderportals.EnderPortalsMod;
import com.maxezify.enderportals.tardis.ConsoleLog;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * L'état complet d'un Contrôle de l'amitié, envoyé du serveur au client.
 *
 * <p>Un état entier plutôt que des deltas : le panneau tient en une poignée de
 * lignes, et l'envoyer complet supprime toute possibilité de divergence entre
 * ce que le serveur sait et ce que le joueur voit. C'est aussi ce qui permet au
 * serveur de rafraîchir l'écran d'un joueur quand c'est l'action d'un
 * <i>autre</i> qui a changé son état.</p>
 *
 * <p>{@code passageAlly} nomme l'allié auquel mène l'arche de ce panneau — vide
 * si elle est libre. Avec plusieurs arches par joueur, le seul verdict
 * « ouvert » ne suffirait plus : il faut dire <b>avec qui</b>, sans quoi deux
 * panneaux voisins afficheraient la même phrase pour deux couloirs
 * différents.</p>
 *
 * <p>Le journal voyage avec le reste, sous forme de clés de traduction et
 * d'arguments : le terminal du panneau se remplit donc dans la langue de celui
 * qui le lit, et non dans celle de celui qui a déclenché le message.</p>
 */
public record ConsoleStatePayload(BlockPos console, int myCode, int passageState,
                                  String passageAlly, List<Ally> allies, List<ConsoleLog.Entry> log)
        implements CustomPacketPayload {

    /** Aucun passage à moi n'est accolé à ce panneau : il ne commande rien. */
    public static final int PASSAGE_NO_PANEL = 0;
    /**
     * Le panneau commande bien mon arche, mais elle n'est liée à personne.
     *
     * <p>Il n'existe pas de troisième cas. Une arche liée a toujours son
     * vis-à-vis — nouer et dénouer se font des deux côtés à la fois, et
     * {@code AllyPassageHelper.reconcile} dénoue tout lien qui pendrait dans le
     * vide. Close veut donc dire libre, et c'est ce qui permet à une arche close
     * de ne porter aucun nom sans jamais mentir.</p>
     */
    public static final int PASSAGE_CLOSED = 1;
    /** Ouvert des deux côtés. */
    public static final int PASSAGE_OPEN = 2;

    /** Longueur maximale d'un pseudo sur le fil. */
    public static final int NAME_LENGTH = 32;

    /** Déclaré de mon côté seulement : l'autre n'a pas encore tapé mon code. */
    public static final int PENDING = 0;
    /** Amitié scellée des deux côtés, passage fermé. */
    public static final int CONFIRMED = 1;
    /** Passage ouvert avec cet allié. */
    public static final int LINKED = 2;
    /** J'ai demandé la connexion, j'attends sa réponse. */
    public static final int AWAITING_THEM = 3;
    /** Il a demandé la connexion, c'est à moi de cliquer. */
    public static final int THEY_ASK = 4;

    /**
     * Une ligne de la liste. Le pseudo voyage avec l'entrée : le client ne peut
     * pas le retrouver seul pour un allié hors ligne.
     */
    public record Ally(UUID uuid, String name, int state) {
    }

    public static final CustomPacketPayload.Type<ConsoleStatePayload> TYPE =
            new CustomPacketPayload.Type<>(EnderPortalsMod.id("console_state"));

    public static final StreamCodec<FriendlyByteBuf, ConsoleStatePayload> STREAM_CODEC =
            StreamCodec.of(ConsoleStatePayload::write, ConsoleStatePayload::read);

    private static void write(FriendlyByteBuf buf, ConsoleStatePayload payload) {
        buf.writeBlockPos(payload.console());
        buf.writeVarInt(payload.myCode());
        buf.writeVarInt(payload.passageState());
        buf.writeUtf(payload.passageAlly(), NAME_LENGTH);
        buf.writeVarInt(payload.allies().size());
        for (Ally ally : payload.allies()) {
            buf.writeUUID(ally.uuid());
            buf.writeUtf(ally.name(), NAME_LENGTH);
            buf.writeVarInt(ally.state());
        }
        buf.writeVarInt(payload.log().size());
        for (ConsoleLog.Entry line : payload.log()) {
            buf.writeUtf(line.key(), ConsoleLog.MAX_KEY);
            buf.writeVarInt(line.tone());
            buf.writeVarInt(line.args().size());
            for (String arg : line.args()) {
                buf.writeUtf(arg, ConsoleLog.MAX_ARG);
            }
        }
    }

    private static ConsoleStatePayload read(FriendlyByteBuf buf) {
        BlockPos console = buf.readBlockPos();
        int code = buf.readVarInt();
        int passage = buf.readVarInt();
        String passageAlly = buf.readUtf(NAME_LENGTH);
        int count = buf.readVarInt();
        List<Ally> allies = new ArrayList<>(Math.min(count, 64));
        for (int i = 0; i < count; i++) {
            allies.add(new Ally(buf.readUUID(), buf.readUtf(NAME_LENGTH), buf.readVarInt()));
        }
        int lines = buf.readVarInt();
        List<ConsoleLog.Entry> log = new ArrayList<>(Math.min(lines, ConsoleLog.CAPACITY));
        for (int i = 0; i < lines; i++) {
            String key = buf.readUtf(ConsoleLog.MAX_KEY);
            int tone = buf.readVarInt();
            int argCount = buf.readVarInt();
            List<String> args = new ArrayList<>(Math.min(argCount, ConsoleLog.MAX_ARGS));
            for (int a = 0; a < argCount; a++) {
                args.add(buf.readUtf(ConsoleLog.MAX_ARG));
            }
            log.add(new ConsoleLog.Entry(key, args, tone));
        }
        return new ConsoleStatePayload(console, code, passage, passageAlly, allies, log);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
