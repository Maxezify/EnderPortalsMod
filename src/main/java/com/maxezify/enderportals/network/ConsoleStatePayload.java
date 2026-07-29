package com.maxezify.enderportals.network;

import com.maxezify.enderportals.EnderPortalsMod;
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
 */
public record ConsoleStatePayload(BlockPos console, int myCode, boolean passageReady, List<Ally> allies)
        implements CustomPacketPayload {

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
        buf.writeBoolean(payload.passageReady());
        buf.writeVarInt(payload.allies().size());
        for (Ally ally : payload.allies()) {
            buf.writeUUID(ally.uuid());
            buf.writeUtf(ally.name(), 32);
            buf.writeVarInt(ally.state());
        }
    }

    private static ConsoleStatePayload read(FriendlyByteBuf buf) {
        BlockPos console = buf.readBlockPos();
        int code = buf.readVarInt();
        boolean ready = buf.readBoolean();
        int count = buf.readVarInt();
        List<Ally> allies = new ArrayList<>(Math.min(count, 64));
        for (int i = 0; i < count; i++) {
            allies.add(new Ally(buf.readUUID(), buf.readUtf(32), buf.readVarInt()));
        }
        return new ConsoleStatePayload(console, code, ready, allies);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
