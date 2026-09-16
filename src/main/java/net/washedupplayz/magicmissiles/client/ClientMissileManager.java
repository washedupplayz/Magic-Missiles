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

/**
 * Client-side registry of missile ghosts. Fed by the sync payloads (see
 * {@code ModNetwork}), advanced once per client tick (dead reckoning + exhaust
 * trail), and read each frame by {@link GhostRenderer}.
 *
 * <p>All access is on the client main thread (payload handlers run via
 * {@code enqueueWork}), so no synchronisation is needed.
 */
public final class ClientMissileManager {
    private static final Map<Long, MissileGhost> GHOSTS = new LinkedHashMap<>();

    private ClientMissileManager() {}

    public static void onSpawn(MissileSpawnPayload payload) {
        // A genuine launch: create the ghost, then let the ignition reach the player
        // in its own time. The motor comes up with the ghost inside adopt.
        adopt(payload.id(),
                payload.x(), payload.y(), payload.z(),
                payload.vx(), payload.vy(), payload.vz());
        AudioDirector.onLaunch(new Vec3(payload.x(), payload.y(), payload.z()));
    }

    public static void onUpdate(MissileUpdatePayload payload) {
        MissileGhost ghost = GHOSTS.get(payload.id());
        if (ghost == null) {
            // Joined mid-flight (or missed the spawn): adopt it silently — it is
            // already in the air, so there is no fresh launch to hear.
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
        AudioDirector.startMotor(ghost);
        return ghost;
    }

    public static void onRemove(MissileRemovePayload payload) {
        MissileGhost ghost = GHOSTS.remove(payload.id());
        if (ghost != null) {
            ghost.markRemoved(); // stops the motor loop
        }
        if (payload.detonated()) {
            // fired even with no ghost: the blast happened whether or not we tracked it
            AudioDirector.onImpact(new Vec3(payload.x(), payload.y(), payload.z()));
        }
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            GHOSTS.clear(); // left the world — drop stale ghosts
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
