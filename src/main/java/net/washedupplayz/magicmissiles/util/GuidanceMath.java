package net.washedupplayz.magicmissiles.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class GuidanceMath {
    private static final double EPSILON = 1.0e-6;

    private GuidanceMath() {}

    public static double angleBetween(Vec3 a, Vec3 b) {
        double la = a.length();
        double lb = b.length();
        if (la < EPSILON || lb < EPSILON) {
            return 0.0;
        }
        double cos = Mth.clamp(a.dot(b) / (la * lb), -1.0, 1.0);
        return Math.acos(cos);
    }

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

    // climb to the ceiling and cruise level, dive straight at aim inside terminal range
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

    // swept proximity fuze
    public static Vec3 closestPointOnSegment(Vec3 a, Vec3 b, Vec3 point) {
        Vec3 seg = b.subtract(a);
        double lenSqr = seg.lengthSqr();
        if (lenSqr < EPSILON) {
            return a;
        }
        double t = Mth.clamp(point.subtract(a).dot(seg) / lenSqr, 0.0, 1.0);
        return a.add(seg.scale(t));
    }

    public static Vec3 slerp(Vec3 a, Vec3 b, double t) {
        double dot = Mth.clamp(a.dot(b), -1.0, 1.0);
        // component of b orthogonal to a, undefined when parallel
        Vec3 orthogonal = b.subtract(a.scale(dot));
        if (orthogonal.lengthSqr() < EPSILON) {
            return b;
        }
        orthogonal = orthogonal.normalize();
        double theta = Math.acos(dot) * t;
        return a.scale(Math.cos(theta)).add(orthogonal.scale(Math.sin(theta)));
    }
}
