package net.washedupplayz.magicmissiles.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A client-side render "ghost" for a missile. It is a {@link GeoAnimatable} so the
 * existing GeckoLib model/animation can be drawn for it, but it is <em>not</em> an
 * entity — it carries only what the client needs to render and dead-reckon. Because
 * it is decoupled from the entity system, {@link GhostRenderer} can draw it at any
 * range the client can see (out to Distant Horizons / Voxy LOD distance), and it
 * never depends on chunk loading.
 *
 * <p>Between the server's periodic corrections the ghost extrapolates its own motion
 * (dead reckoning); {@link #applyServerState} snaps it back onto the authoritative
 * track. Rendering interpolates {@code prev → current} by the frame's partial tick.
 */
public class MissileGhost implements GeoAnimatable {
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.missile.fly");

    /** Number of past nozzle positions kept for the self-rendered smoke contrail. */
    private static final int TRAIL_MAX_POINTS = 50;

    /**
     * How far behind the tracked point the exhaust nozzle sits, in blocks. The tracked
     * point renders near the middle of the (long) missile model, so the contrail is
     * emitted from here to make it leave the tail rather than the belly. Tune to the
     * model's length if it looks off.
     */
    private static final double NOZZLE_OFFSET = 1.9;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    /** Recent nozzle positions, newest first, drawn as a ribbon by {@link GhostRenderer}. */
    private final Deque<Vec3> trail = new ArrayDeque<>();

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
    }

    /** Snap to an authoritative server correction. */
    public void applyServerState(double x, double y, double z, double vx, double vy, double vz) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        recomputeOrientation();
    }

    /** One client tick of dead reckoning. */
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
    }

    private void recordTrail() {
        this.trail.addFirst(nozzlePos());
        while (this.trail.size() > TRAIL_MAX_POINTS) {
            this.trail.removeLast();
        }
    }

    /** Current world position of the exhaust nozzle (tracked point offset back along the flight axis). */
    private Vec3 nozzlePos() {
        double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (speed < 1.0e-6) {
            return new Vec3(x, y, z);
        }
        double scale = NOZZLE_OFFSET / speed;
        return new Vec3(x - vx * scale, y - vy * scale, z - vz * scale);
    }

    /** Snapshot of the trail, newest first, for the renderer to build a ribbon from. */
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

    /** client ticks since the ghost appeared; drives the roll */
    public int age() {
        return this.age;
    }

    // --- GeoAnimatable ---

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "fly", state -> state.setAndContinue(FLY)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    @Override
    public double getTick(Object object) {
        return this.age;
    }
}
