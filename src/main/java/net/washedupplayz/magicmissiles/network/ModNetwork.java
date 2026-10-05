package net.washedupplayz.magicmissiles.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.washedupplayz.magicmissiles.client.ClientMissileManager;

public final class ModNetwork {
    // bump on incompatible wire changes
    private static final String PROTOCOL_VERSION = "1";

    private ModNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        // lambdas keep ClientMissileManager unloaded on a dedicated server
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(MissileSpawnPayload.TYPE, MissileSpawnPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientMissileManager.onSpawn(payload)));
        registrar.playToClient(MissileUpdatePayload.TYPE, MissileUpdatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientMissileManager.onUpdate(payload)));
        registrar.playToClient(MissileRemovePayload.TYPE, MissileRemovePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientMissileManager.onRemove(payload)));
    }

    public static void broadcast(ServerLevel level, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersInDimension(level, payload);
    }

    public static void sendTo(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
