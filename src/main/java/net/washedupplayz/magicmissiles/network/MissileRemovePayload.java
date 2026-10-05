package net.washedupplayz.magicmissiles.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.washedupplayz.magicmissiles.MagicMissiles;

// detonated false is a silent fizzle
public record MissileRemovePayload(long id, double x, double y, double z,
                                   boolean detonated) implements CustomPacketPayload {
    public static final Type<MissileRemovePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MagicMissiles.MOD_ID, "missile_remove"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MissileRemovePayload> STREAM_CODEC =
            StreamCodec.ofMember(MissileRemovePayload::write, MissileRemovePayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeLong(id);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeBoolean(detonated);
    }

    private static MissileRemovePayload read(RegistryFriendlyByteBuf buf) {
        return new MissileRemovePayload(buf.readLong(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
