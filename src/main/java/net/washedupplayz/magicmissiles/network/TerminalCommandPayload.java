package net.washedupplayz.magicmissiles.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.washedupplayz.magicmissiles.MagicMissiles;

public record TerminalCommandPayload(BlockPos pos, String line) implements CustomPacketPayload {
    public static final int MAX_LENGTH = 256;

    public static final Type<TerminalCommandPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MagicMissiles.MOD_ID, "terminal_command"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalCommandPayload> STREAM_CODEC =
            StreamCodec.ofMember(TerminalCommandPayload::write, TerminalCommandPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(line, MAX_LENGTH);
    }

    private static TerminalCommandPayload read(RegistryFriendlyByteBuf buf) {
        return new TerminalCommandPayload(buf.readBlockPos(), buf.readUtf(MAX_LENGTH));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
