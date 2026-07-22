package net.washedupplayz.magicmissiles.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

/** Verifies the turn-rate–limited steering used by missile guidance. */
class GuidanceMathTest {
    private static final double EPS = 1.0e-6;

    @Test
    void angleBetweenPerpendicularVectorsIsNinetyDegrees() {
        double angle = GuidanceMath.angleBetween(new Vec3(1, 0, 0), new Vec3(0, 0, 1));
        assertEquals(Math.toRadians(90.0), angle, 1.0e-4);
    }

    @Test
    void angleBetweenIdenticalVectorsIsZero() {
        double angle = GuidanceMath.angleBetween(new Vec3(0, 1, 0), new Vec3(0, 3, 0));
        assertEquals(0.0, angle, 1.0e-4);
    }

    @Test
    void angleBetweenIsZeroForZeroLengthVector() {
        assertEquals(0.0, GuidanceMath.angleBetween(Vec3.ZERO, new Vec3(1, 0, 0)), EPS);
    }

    @Test
    void steerRotatesByAtMostMaxTurn() {
        double maxTurn = Math.toRadians(10.0);
        Vec3 velocity = new Vec3(2, 0, 0);              // heading +X at speed 2
        Vec3 desired = new Vec3(0, 0, 1);               // want to face +Z (90° away)

        Vec3 result = GuidanceMath.steer(velocity, desired, maxTurn, 2.0);

        // Turned toward +Z but only by the max-turn budget, keeping speed.
        double turned = GuidanceMath.angleBetween(velocity, result);
        assertEquals(maxTurn, turned, 1.0e-4);
        assertEquals(2.0, result.length(), 1.0e-4);
        assertTrue(result.z > 0, "should have rotated toward the target");
    }

    @Test
    void steerSnapsToDesiredWhenWithinMaxTurn() {
        double maxTurn = Math.toRadians(30.0);
        Vec3 velocity = new Vec3(1, 0, 0);
        Vec3 desired = new Vec3(Math.cos(Math.toRadians(10.0)), 0, Math.sin(Math.toRadians(10.0)));

        Vec3 result = GuidanceMath.steer(velocity, desired, maxTurn, 1.0);

        double off = GuidanceMath.angleBetween(result, desired);
        assertEquals(0.0, off, 1.0e-4);
        assertEquals(1.0, result.length(), 1.0e-4);
    }

    @Test
    void repeatedSteeringConvergesOnTarget() {
        double maxTurn = Math.toRadians(9.0);
        Vec3 velocity = new Vec3(1.6, 0, 0);
        Vec3 desired = new Vec3(-1, 0, 0);              // 180° reversal

        for (int i = 0; i < 40; i++) {
            velocity = GuidanceMath.steer(velocity, desired, maxTurn, 1.6);
        }

        assertEquals(0.0, GuidanceMath.angleBetween(velocity, desired), 1.0e-3);
        assertEquals(1.6, velocity.length(), 1.0e-4);
    }

    @Test
    void slerpMidpointBisectsTheArc() {
        Vec3 mid = GuidanceMath.slerp(new Vec3(1, 0, 0), new Vec3(0, 0, 1), 0.5);
        assertEquals(Math.toRadians(45.0), GuidanceMath.angleBetween(new Vec3(1, 0, 0), mid), 1.0e-4);
        assertEquals(1.0, mid.length(), 1.0e-4);
    }
}
