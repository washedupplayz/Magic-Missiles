package net.washedupplayz.magicmissiles.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.washedupplayz.magicmissiles.MagicMissiles;

/**
 * Server → client announcement that a missile has launched. Carries the initial
 * position and velocity so the client can spawn a render "ghost" and dead-reckon
 * it between the (less frequent) {@link MissileUpdatePayload} corrections.
 *
 * <p>Broadcast to every player in the missile's dimension — missiles are rare,
 * high-value events, and clients must be able to see them far beyond entity
 * tracking range (out to Distant Horizons / Voxy LOD range).
 */
public record MissileSpawnPayload(long id, double x, double y, double z,
                                  double vx, double vy, double vz) implements CustomPacketPayload {
    public static final Type<MissileSpawnPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MagicMissiles.MOD_ID, "missile_spawn"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MissileSpawnPayload> STREAM_CODEC =
            StreamCodec.ofMember(MissileSpawnPayload::write, MissileSpawnPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeLong(id);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeDouble(vx);
        buf.writeDouble(vy);
        buf.writeDouble(vz);
    }

    private static MissileSpawnPayload read(RegistryFriendlyByteBuf buf) {
        return new MissileSpawnPayload(buf.readLong(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
