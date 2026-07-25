package net.washedupplayz.magicmissiles.client;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.washedupplayz.magicmissiles.entity.MissileEntity;

/**
 * Client-only, game-bus listeners. Registered from {@link ModClientEvents} during
 * client setup. Starts a {@link MissileLaunchSound} for each missile as it appears,
 * so the launch sound is bound to that missile and stops when it detonates.
 */
public final class ModClientGameEvents {
    private ModClientGameEvents() {}

    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof MissileEntity missile) {
            Minecraft.getInstance().getSoundManager().play(new MissileLaunchSound(missile));
        }
    }
}
