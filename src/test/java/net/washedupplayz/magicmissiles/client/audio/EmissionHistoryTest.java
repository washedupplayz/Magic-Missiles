package net.washedupplayz.magicmissiles.client.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class EmissionHistoryTest {

    /** Straight flight along +X at {@code speed}, one sample per tick, ending at tick {@code now}. */
    private static EmissionHistory flightAlongX(double speed, long now, int samples) {
        EmissionHistory history = new EmissionHistory(256);
        for (long t = now - samples + 1; t <= now; t++) {
            history.record(t, new Vec3(speed * t, 0, 0));
        }
        return history;
    }

    @Test
    void emptyHistoryHasNoSolution() {
        assertNull(new EmissionHistory().retardedPosition(0L, Vec3.ZERO));
    }

    @Test
    void listenerOnTopOfTheSourceHearsItWhereItIs() {
        EmissionHistory history = flightAlongX(5.0, 100L, 50);
        Vec3 heard = history.retardedPosition(100L, new Vec3(500, 0, 0));
        assertEquals(500.0, heard.x, 0.5);
    }

    @Test
    void distantListenerHearsAnOlderPosition() {
        // source at x = 5t, listener parked at the origin. At tick 100 the source is
        // at x = 500, but the sound arriving now left earlier and from further back.
        EmissionHistory history = flightAlongX(5.0, 100L, 120);
        Vec3 heard = history.retardedPosition(100L, Vec3.ZERO);
        assertTrue(heard.x < 500.0, "should lag the true position, got " + heard.x);

        // solve |5 t| = c (100 - t)  ->  t = 100c / (c + 5)
        double c = Acoustics.SPEED_OF_SOUND;
        double expected = 5.0 * (100.0 * c / (c + 5.0));
        assertEquals(expected, heard.x, 1.0);
    }

    @Test
    void theSolutionSatisfiesThePropagationEquation() {
        EmissionHistory history = flightAlongX(5.0, 200L, 200);
        Vec3 listener = new Vec3(0, 0, 300);
        Vec3 heard = history.retardedPosition(200L, listener);

        // recover the emission tick from the position and check the travel time
        double emissionTick = heard.x / 5.0;
        double travel = heard.distanceTo(listener) / Acoustics.SPEED_OF_SOUND;
        assertEquals(200.0, emissionTick + travel, 1.0);
    }

    @Test
    void aStationarySourceIsHeardWhereItStands() {
        EmissionHistory history = new EmissionHistory(64);
        Vec3 spot = new Vec3(40, 70, -12);
        for (long t = 0; t <= 60; t++) {
            history.record(t, spot);
        }
        Vec3 heard = history.retardedPosition(60L, new Vec3(0, 70, 0));
        assertEquals(spot.x, heard.x, 1.0e-6);
        assertEquals(spot.z, heard.z, 1.0e-6);
    }

    @Test
    void silentUntilTheWavefrontArrives() {
        // a missile that launched one tick ago 500 blocks away cannot be audible yet
        EmissionHistory history = new EmissionHistory(64);
        history.record(0L, new Vec3(500, 0, 0));
        history.record(1L, new Vec3(505, 0, 0));
        assertNull(history.retardedPosition(1L, Vec3.ZERO));

        // by tick 30 the wavefront has covered 500 blocks and it becomes audible
        for (long t = 2; t <= 30; t++) {
            history.record(t, new Vec3(500 + 5.0 * t, 0, 0));
        }
        assertNotNull(history.retardedPosition(30L, Vec3.ZERO));
    }

    @Test
    void ringBufferOverwritesOldestFirst() {
        EmissionHistory history = new EmissionHistory(3);
        for (long t = 0; t < 10; t++) {
            history.record(t, new Vec3(t, 0, 0));
        }
        // only ticks 7, 8, 9 survive; a nearby listener hears the newest
        Vec3 heard = history.retardedPosition(9L, new Vec3(9, 0, 0));
        assertEquals(9.0, heard.x, 0.5);
    }
}
