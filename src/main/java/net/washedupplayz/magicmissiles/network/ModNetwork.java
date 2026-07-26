package net.washedupplayz.magicmissiles.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.washedupplayz.magicmissiles.client.ClientMissileManager;

/**
 * Registers the missile sync payloads and provides server-side broadcast helpers.
 *
 * <p>All three payloads are {@code playToClient}. The client handlers dispatch into
 * {@link ClientMissileManager}; they are wrapped in lambdas so the client class is
 * only loaded when a handler actually runs (i.e. never on a dedicated server).
 */
public final class ModNetwork {
    /** Bump when the payload wire format changes incompatibly. */
    private static final String PROTOCOL_VERSION = "1";

    private ModNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(MissileSpawnPayload.TYPE, MissileSpawnPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientMissileManager.onSpawn(payload)));
        registrar.playToClient(MissileUpdatePayload.TYPE, MissileUpdatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientMissileManager.onUpdate(payload)));
        registrar.playToClient(MissileRemovePayload.TYPE, MissileRemovePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientMissileManager.onRemove(payload)));
    }

    /** Send to every player in the missile's dimension (far observers included). */
    public static void broadcast(ServerLevel level, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersInDimension(level, payload);
    }

    /** Send to a single player (used to resync missiles to someone who just joined). */
    public static void sendTo(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
