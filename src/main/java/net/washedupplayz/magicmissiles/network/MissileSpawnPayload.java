package net.washedupplayz.magicmissiles.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.washedupplayz.magicmissiles.MagicMissiles;

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
