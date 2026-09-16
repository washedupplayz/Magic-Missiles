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

    // --- Lofted cruise-then-dive trajectory ---
    private static final double CEILING = 336.0;
    private static final double TERMINAL_RANGE = 48.0;
    private static final double LOOKAHEAD = 24.0;

    @Test
    void loftClimbsAndHeadsDownrangeWhenFarAndBelowCeiling() {
        Vec3 dir = GuidanceMath.loftedDirection(
                new Vec3(0, 100, 0), new Vec3(1000, 64, 0), CEILING, TERMINAL_RANGE, LOOKAHEAD);
        assertTrue(dir.y > 0, "should climb toward the ceiling");
        assertTrue(dir.x > 0, "should head toward the target's ground track");
        assertEquals(0.0, dir.z, EPS);
        assertEquals(CEILING - 100.0, dir.y, EPS); // aims exactly at ceiling altitude
    }

    @Test
    void loftFliesLevelWhenAlreadyAtCeiling() {
        Vec3 dir = GuidanceMath.loftedDirection(
                new Vec3(0, CEILING, 0), new Vec3(1000, 64, 0), CEILING, TERMINAL_RANGE, LOOKAHEAD);
        assertEquals(0.0, dir.y, EPS);
        assertTrue(dir.x > 0);
    }

    @Test
    void loftDescendsToCeilingWhenAboveIt() {
        Vec3 dir = GuidanceMath.loftedDirection(
                new Vec3(0, 400, 0), new Vec3(1000, 64, 0), CEILING, TERMINAL_RANGE, LOOKAHEAD);
        assertTrue(dir.y < 0, "should sink back down to the ceiling");
    }

    @Test
    void loftDivesStraightAtTargetWithinTerminalRange() {
        Vec3 pos = new Vec3(0, 100, 0);
        Vec3 aim = new Vec3(10, 64, 0); // 10 blocks away horizontally, below
        Vec3 dir = GuidanceMath.loftedDirection(pos, aim, CEILING, TERMINAL_RANGE, LOOKAHEAD);
        assertEquals(aim.subtract(pos).x, dir.x, EPS);
        assertEquals(aim.subtract(pos).y, dir.y, EPS);
        assertEquals(aim.subtract(pos).z, dir.z, EPS);
        assertTrue(dir.y < 0, "dive has a downward component toward the target");
    }

    // --- Proximity fuze segment math ---

    @Test
    void closestPointProjectsOntoTheSegment() {
        Vec3 closest = GuidanceMath.closestPointOnSegment(
                new Vec3(0, 0, 0), new Vec3(10, 0, 0), new Vec3(3, 4, 0));
        assertEquals(3.0, closest.x, 1.0e-6);
        assertEquals(0.0, closest.y, 1.0e-6);
    }

    @Test
    void closestPointClampsToSegmentEnds() {
        Vec3 a = new Vec3(0, 0, 0);
        Vec3 b = new Vec3(10, 0, 0);
        assertEquals(0.0, GuidanceMath.closestPointOnSegment(a, b, new Vec3(-50, 0, 0)).x, 1.0e-6);
        assertEquals(10.0, GuidanceMath.closestPointOnSegment(a, b, new Vec3(50, 0, 0)).x, 1.0e-6);
    }

    @Test
    void closestPointHandlesZeroLengthSegment() {
        Vec3 a = new Vec3(2, 3, 4);
        assertEquals(a, GuidanceMath.closestPointOnSegment(a, a, new Vec3(9, 9, 9)));
    }

    @Test
    void sweptFuzeCatchesATargetAnEndpointTestWouldMiss() {
        // 5 blocks/tick straight past a target sitting 1 block off the track:
        // both endpoints are outside a 2-block fuze, the closest approach is inside.
        Vec3 from = new Vec3(-2.5, 0, 0);
        Vec3 to = new Vec3(2.5, 0, 0);
        Vec3 target = new Vec3(0, 1, 0);
        double fuzeSqr = 2.0 * 2.0;

        assertTrue(from.distanceToSqr(target) > fuzeSqr, "start must be outside the fuze");
        assertTrue(to.distanceToSqr(target) > fuzeSqr, "endpoint must be outside the fuze");
        assertTrue(GuidanceMath.closestPointOnSegment(from, to, target).distanceToSqr(target) <= fuzeSqr,
                "swept test must detonate");
    }
}
