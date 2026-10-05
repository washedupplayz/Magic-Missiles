package net.washedupplayz.magicmissiles.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.washedupplayz.magicmissiles.MagicMissiles;

public record MissileUpdatePayload(long id, double x, double y, double z,
                                   double vx, double vy, double vz) implements CustomPacketPayload {
    public static final Type<MissileUpdatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MagicMissiles.MOD_ID, "missile_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MissileUpdatePayload> STREAM_CODEC =
            StreamCodec.ofMember(MissileUpdatePayload::write, MissileUpdatePayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeLong(id);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeDouble(vx);
        buf.writeDouble(vy);
        buf.writeDouble(vz);
    }

    private static MissileUpdatePayload read(RegistryFriendlyByteBuf buf) {
        return new MissileUpdatePayload(buf.readLong(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
