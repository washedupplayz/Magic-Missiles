package net.washedupplayz.magicmissiles.client;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.washedupplayz.magicmissiles.client.audio.AudioDirector;
import net.washedupplayz.magicmissiles.network.MissileRemovePayload;
import net.washedupplayz.magicmissiles.network.MissileSpawnPayload;
import net.washedupplayz.magicmissiles.network.MissileUpdatePayload;

// main thread only, payload handlers run via enqueueWork
public final class ClientMissileManager {
    private static final Map<Long, MissileGhost> GHOSTS = new LinkedHashMap<>();

    private ClientMissileManager() {}

    public static void onSpawn(MissileSpawnPayload payload) {
        // only a genuine launch is heard, mid-flight adoptions stay silent
        MissileGhost ghost = adopt(payload.id(),
                payload.x(), payload.y(), payload.z(),
                payload.vx(), payload.vy(), payload.vz());
        AudioDirector.onLaunch(ghost);
    }

    public static void onUpdate(MissileUpdatePayload payload) {
        MissileGhost ghost = GHOSTS.get(payload.id());
        if (ghost == null) {
            // joined mid-flight or missed the spawn
            adopt(payload.id(),
                    payload.x(), payload.y(), payload.z(),
                    payload.vx(), payload.vy(), payload.vz());
            return;
        }
        ghost.applyServerState(payload.x(), payload.y(), payload.z(),
                payload.vx(), payload.vy(), payload.vz());
    }

    private static MissileGhost adopt(long id, double x, double y, double z,
                                      double vx, double vy, double vz) {
        MissileGhost ghost = new MissileGhost(id, x, y, z, vx, vy, vz);
        GHOSTS.put(id, ghost);
        return ghost;
    }

    public static void onRemove(MissileRemovePayload payload) {
        MissileGhost ghost = GHOSTS.remove(payload.id());
        if (ghost != null) {
            ghost.markRemoved(); // flight sound stops once its last emission is heard
        }
        if (payload.detonated()) {
            // also without a ghost
            AudioDirector.onImpact(new Vec3(payload.x(), payload.y(), payload.z()));
        }
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            GHOSTS.clear(); // left the world
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        for (MissileGhost ghost : GHOSTS.values()) {
            ghost.tickClient();
        }
    }

    public static Collection<MissileGhost> ghosts() {
        return GHOSTS.values();
    }
}
