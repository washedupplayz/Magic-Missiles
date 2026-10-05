package net.washedupplayz.magicmissiles.missile;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public final class ModServerEvents {
    private ModServerEvents() {}

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            MissileManager.get(level).tick(level);
        }
    }
}
