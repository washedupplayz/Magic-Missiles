package net.washedupplayz.magicmissiles.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.washedupplayz.magicmissiles.client.audio.EmissionHistory;

public class MissileGhost {
    private static final int TRAIL_MAX_POINTS = 50;

    // tracked point is mid-body, the trail leaves from the tail
    private static final double NOZZLE_OFFSET = 1.6875;

    // newest first
    private final Deque<Vec3> trail = new ArrayDeque<>();
    // positions for the retarded sound source
    private final EmissionHistory emissions = new EmissionHistory();

    public final long id;
    public double x, y, z;
    public double prevX, prevY, prevZ;
    public double vx, vy, vz;
    public float yaw, pitch;
    public float prevYaw, prevPitch;

    private int age;
    private boolean removed;

    public MissileGhost(long id, double x, double y, double z, double vx, double vy, double vz) {
        this.id = id;
        this.x = this.prevX = x;
        this.y = this.prevY = y;
        this.z = this.prevZ = z;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        recomputeOrientation();
        this.prevYaw = this.yaw;
        this.prevPitch = this.pitch;
        this.trail.addFirst(nozzlePos());
        this.emissions.record(0L, pos());
    }

    public void applyServerState(double x, double y, double z, double vx, double vy, double vz) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        recomputeOrientation();
    }

    public void tickClient() {
        this.prevX = x;
        this.prevY = y;
        this.prevZ = z;
        this.prevYaw = yaw;
        this.prevPitch = pitch;
        this.x += vx;
        this.y += vy;
        this.z += vz;
        recomputeOrientation();
        recordTrail();
        this.age++;
        this.emissions.record(this.age, pos());
    }

    private void recordTrail() {
        this.trail.addFirst(nozzlePos());
        while (this.trail.size() > TRAIL_MAX_POINTS) {
            this.trail.removeLast();
        }
    }

    private Vec3 nozzlePos() {
        double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (speed < 1.0e-6) {
            return new Vec3(x, y, z);
        }
        double scale = NOZZLE_OFFSET / speed;
        return new Vec3(x - vx * scale, y - vy * scale, z - vz * scale);
    }

    public Vec3 pos() {
        return new Vec3(x, y, z);
    }

    public Vec3 vel() {
        return new Vec3(vx, vy, vz);
    }

    public EmissionHistory emissions() {
        return this.emissions;
    }

    public List<Vec3> trailSnapshot() {
        return new ArrayList<>(this.trail);
    }

    private void recomputeOrientation() {
        double horizontal = Math.sqrt(vx * vx + vz * vz);
        this.yaw = (float) (Mth.atan2(vx, vz) * (180.0 / Math.PI));
        this.pitch = (float) (Mth.atan2(vy, horizontal) * (180.0 / Math.PI));
    }

    public void markRemoved() {
        this.removed = true;
    }

    public boolean isRemoved() {
        return removed;
    }

    public int age() {
        return this.age;
    }
}
