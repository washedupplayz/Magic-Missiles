package net.washedupplayz.magicmissiles.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Vector helpers for missile/interceptor guidance: turn-rate–limited steering
 * built on spherical interpolation between unit direction vectors.
 */
public final class GuidanceMath {
    private static final double EPSILON = 1.0e-6;

    private GuidanceMath() {}

    /** Angle in radians between two vectors (0 if either is zero-length). */
    public static double angleBetween(Vec3 a, Vec3 b) {
        double la = a.length();
        double lb = b.length();
        if (la < EPSILON || lb < EPSILON) {
            return 0.0;
        }
        double cos = Mth.clamp(a.dot(b) / (la * lb), -1.0, 1.0);
        return Math.acos(cos);
    }

    /**
     * Turn a velocity toward {@code desiredDir}, rotating by at most
     * {@code maxTurnRad} this step, and return it rescaled to {@code speed}.
     */
    public static Vec3 steer(Vec3 velocity, Vec3 desiredDir, double maxTurnRad, double speed) {
        if (velocity.lengthSqr() < EPSILON || desiredDir.lengthSqr() < EPSILON) {
            return velocity;
        }
        Vec3 current = velocity.normalize();
        Vec3 desired = desiredDir.normalize();
        double angle = angleBetween(current, desired);

        Vec3 newDir;
        if (angle <= maxTurnRad || angle < EPSILON) {
            newDir = desired;
        } else {
            newDir = slerp(current, desired, maxTurnRad / angle);
        }
        return newDir.scale(speed);
    }

    /**
     * Desired flight direction for a lofted cruise-then-dive trajectory.
     *
     * <p>While the horizontal distance to {@code aim} exceeds {@code terminalRange},
     * the missile aims at a waypoint {@code lookahead} blocks ahead along the ground
     * track but at {@code ceiling} altitude — so it climbs to the ceiling, then flies
     * level once it is there. Within terminal range it aims straight at {@code aim}
     * (the dive). Keeping the cruise above the world build height is what makes free
     * flight over unloaded chunks safe: there is nothing up there to collide with.
     *
     * @return an un-normalised direction vector (feed to {@link #steer})
     */
    public static Vec3 loftedDirection(Vec3 pos, Vec3 aim, double ceiling,
                                       double terminalRange, double lookahead) {
        double dx = aim.x - pos.x;
        double dz = aim.z - pos.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal > terminalRange) {
            double inv = horizontal < EPSILON ? 0.0 : 1.0 / horizontal;
            double wx = pos.x + dx * inv * lookahead;
            double wz = pos.z + dz * inv * lookahead;
            return new Vec3(wx - pos.x, ceiling - pos.y, wz - pos.z);
        }
        return aim.subtract(pos);
    }

    /**
     * Spherical interpolation between two unit vectors by fraction {@code t}
     * (0 → a, 1 → b). Falls back gracefully when the vectors are (anti)parallel.
     */
    public static Vec3 slerp(Vec3 a, Vec3 b, double t) {
        double dot = Mth.clamp(a.dot(b), -1.0, 1.0);
        // Component of b orthogonal to a; undefined when (anti)parallel.
        Vec3 orthogonal = b.subtract(a.scale(dot));
        if (orthogonal.lengthSqr() < EPSILON) {
            return b;
        }
        orthogonal = orthogonal.normalize();
        double theta = Math.acos(dot) * t;
        return a.scale(Math.cos(theta)).add(orthogonal.scale(Math.sin(theta)));
    }
}
