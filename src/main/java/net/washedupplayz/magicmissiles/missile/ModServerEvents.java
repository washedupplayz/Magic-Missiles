package net.washedupplayz.magicmissiles.missile;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Game-bus (per-tick) server hooks. Registered onto {@code NeoForge.EVENT_BUS}
 * from the mod constructor. Drives the per-level {@link MissileManager} each tick.
 */
public final class ModServerEvents {
    private ModServerEvents() {}

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            MissileManager.get(level).tick(level);
        }
    }
}
