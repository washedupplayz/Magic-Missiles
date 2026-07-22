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
