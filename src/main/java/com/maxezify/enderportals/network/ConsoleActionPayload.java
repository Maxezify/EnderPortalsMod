package com.maxezify.enderportals.network;

import com.maxezify.enderportals.EnderPortalsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/**
 * Une action du joueur sur son Contrôle de l'amitié, du client vers le serveur.
 *
 * <p>Le client n'envoie que des intentions — un code frappé, un nom cliqué. Rien
 * n'est décidé de son côté : c'est le serveur qui vérifie que le joueur est bien
 * à portée du panneau désigné, que le code existe, que l'amitié est réciproque.
 * Un client modifié ne peut donc rien obtenir d'autre que ce qu'un joueur
 * ordinaire obtiendrait en cliquant.</p>
 */
public record ConsoleActionPayload(BlockPos console, int action, int code, UUID target)
        implements CustomPacketPayload {

    /** Valider un code frappé au pavé. */
    public static final int SUBMIT_CODE = 0;
    /** Cliquer un nom : demander, accepter ou refermer selon l'état. */
    public static final int TOGGLE = 1;
    /** L'écran se referme — le serveur n'a plus à le rafraîchir. */
    public static final int CLOSE = 2;
    /** Retirer un allié du carnet (maj + clic). */
    public static final int FORGET = 3;

    /** Cible absente : les actions sans destinataire portent cet UUID. */
    public static final UUID NO_TARGET = new UUID(0L, 0L);

    public static final CustomPacketPayload.Type<ConsoleActionPayload> TYPE =
            new CustomPacketPayload.Type<>(EnderPortalsMod.id("console_action"));

    public static final StreamCodec<FriendlyByteBuf, ConsoleActionPayload> STREAM_CODEC =
            StreamCodec.of(ConsoleActionPayload::write, ConsoleActionPayload::read);

    public static ConsoleActionPayload code(BlockPos console, int code) {
        return new ConsoleActionPayload(console, SUBMIT_CODE, code, NO_TARGET);
    }

    public static ConsoleActionPayload toggle(BlockPos console, UUID target) {
        return new ConsoleActionPayload(console, TOGGLE, 0, target);
    }

    public static ConsoleActionPayload forget(BlockPos console, UUID target) {
        return new ConsoleActionPayload(console, FORGET, 0, target);
    }

    public static ConsoleActionPayload close(BlockPos console) {
        return new ConsoleActionPayload(console, CLOSE, 0, NO_TARGET);
    }

    private static void write(FriendlyByteBuf buf, ConsoleActionPayload payload) {
        buf.writeBlockPos(payload.console());
        buf.writeVarInt(payload.action());
        buf.writeVarInt(payload.code());
        buf.writeUUID(payload.target());
    }

    private static ConsoleActionPayload read(FriendlyByteBuf buf) {
        return new ConsoleActionPayload(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(), buf.readUUID());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
