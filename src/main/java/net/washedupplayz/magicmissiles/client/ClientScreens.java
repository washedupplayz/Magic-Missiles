package net.washedupplayz.magicmissiles.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

// only called from client-side branches, keeps screen classes off the server
public final class ClientScreens {
    private ClientScreens() {}

    public static void openTerminal(BlockPos pos) {
        Minecraft.getInstance().setScreen(new ControlTerminalScreen(pos));
    }
}
