package net.washedupplayz.magicmissiles.client.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class AcousticsTest {
    private static final Vec3 STILL = Vec3.ZERO;

    @Test
    void speedOfSoundIsThreeFortyThreeMetresPerSecond() {
        assertEquals(17.15, Acoustics.SPEED_OF_SOUND, 1.0e-9);
    }

    @Test
    void delayMatchesDistanceOverSpeed() {
        assertEquals(0, Acoustics.delayTicks(0.0));
        assertEquals(1, Acoustics.delayTicks(17.15));
        // a detonation at the old 2160-block range lands just over six seconds late
        assertEquals(126, Acoustics.delayTicks(2160.0));
    }

    @Test
    void delayIsNeverNegative() {
        assertEquals(0, Acoustics.delayTicks(-500.0));
    }

    @Test
    void gainIsFullInsideTheReferenceDistance() {
        assertEquals(1.0f, Acoustics.distanceGain(0.0, 16.0, 2000.0), 1.0e-6);
        assertEquals(1.0f, Acoustics.distanceGain(8.0, 16.0, 2000.0), 1.0e-6);
    }

    @Test
    void gainFollowsInverseDistance() {
        // doubling the distance halves the gain, unlike the engine's linear ramp
        float near = Acoustics.distanceGain(100.0, 16.0, 4000.0);
        float far = Acoustics.distanceGain(200.0, 16.0, 4000.0);
        assertEquals(near / 2.0f, far, 1.0e-4);
    }

    @Test
    void gainReachesSilenceAtTheCutoff() {
        assertEquals(0.0f, Acoustics.distanceGain(2000.0, 16.0, 2000.0), 1.0e-6);
        assertEquals(0.0f, Acoustics.distanceGain(9999.0, 16.0, 2000.0), 1.0e-6);
    }

    @Test
    void gainDecreasesMonotonically() {
        float previous = Float.MAX_VALUE;
        for (double d = 0.0; d <= 2000.0; d += 5.0) {
            float g = Acoustics.distanceGain(d, 16.0, 2000.0);
            assertTrue(g <= previous + 1.0e-6, "gain rose at " + d);
            previous = g;
        }
    }

    @Test
    void approachingSourceRaisesPitch() {
        // missile at 5 blocks/tick closing head-on from -100 on the x axis
        float pitch = Acoustics.dopplerPitch(
                new Vec3(-100, 0, 0), new Vec3(5, 0, 0), STILL, STILL);
        assertEquals(17.15 / (17.15 - 5.0), pitch, 1.0e-4);
        assertTrue(pitch > 1.0f);
    }

    @Test
    void recedingSourceLowersPitch() {
        float pitch = Acoustics.dopplerPitch(
                new Vec3(-100, 0, 0), new Vec3(-5, 0, 0), STILL, STILL);
        assertEquals(17.15 / (17.15 + 5.0), pitch, 1.0e-4);
        assertTrue(pitch < 1.0f);
    }

    @Test
    void crossingSourceIsUnshifted() {
        // moving perpendicular to the line of sight: no radial component
        float pitch = Acoustics.dopplerPitch(
                new Vec3(-100, 0, 0), new Vec3(0, 0, 5), STILL, STILL);
        assertEquals(1.0f, pitch, 1.0e-4);
    }

    @Test
    void listenerMotionCountsToo() {
        // listener flying at the stationary source hears it raised
        float pitch = Acoustics.dopplerPitch(
                new Vec3(-100, 0, 0), STILL, STILL, new Vec3(-5, 0, 0));
        assertTrue(pitch > 1.0f, "closing listener should raise pitch, got " + pitch);
    }

    @Test
    void pitchStaysInsideTheEngineClamp() {
        // even at absurd closing speeds the engine would clamp to [0.5, 2.0]
        float fast = Acoustics.dopplerPitch(
                new Vec3(-100, 0, 0), new Vec3(500, 0, 0), STILL, STILL);
        assertTrue(fast <= 2.0f && fast >= 0.5f, "out of clamp: " + fast);
    }

    @Test
    void coincidentPositionsAreUnshifted() {
        assertEquals(1.0f, Acoustics.dopplerPitch(STILL, new Vec3(5, 0, 0), STILL, STILL), 1.0e-6);
    }
}
