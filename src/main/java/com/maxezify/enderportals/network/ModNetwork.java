package com.maxezify.enderportals.network;

import com.maxezify.enderportals.EnderPortalsMod;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Le seul canal réseau du mod : celui du Contrôle de l'amitié.
 *
 * <p>Tout le reste du mod se synchronise par les mécanismes ordinaires — états
 * de bloc et {@code getUpdateTag} des block entities. Le panneau, lui, affiche
 * des données qui ne vivent dans aucun bloc (le carnet d'amis est une donnée de
 * sauvegarde globale), d'où ce canal dédié.</p>
 *
 * <p>Les deux directions sont déclarées des deux côtés — le serveur a besoin de
 * connaître le type qu'il émet. Le gestionnaire client n'est atteint que sur un
 * client, et la classe qui touche à l'interface n'est donc jamais chargée sur un
 * serveur dédié ; le garde-fou {@link FMLEnvironment} le garantit
 * explicitement plutôt que de s'en remettre au chargement paresseux.</p>
 */
@EventBusSubscriber(modid = EnderPortalsMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModNetwork {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(ConsoleStatePayload.TYPE, ConsoleStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> receiveState(payload)));
        registrar.playToServer(ConsoleActionPayload.TYPE, ConsoleActionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> ConsoleServerLogic.handle(payload, context.player())));
    }

    private static void receiveState(ConsoleStatePayload payload) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.maxezify.enderportals.client.FriendshipConsoleScreen.show(payload);
        }
    }

    /** Envoie l'état du panneau à un joueur. */
    public static void toPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    private ModNetwork() {
    }
}
