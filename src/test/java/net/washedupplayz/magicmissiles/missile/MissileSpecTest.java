package net.washedupplayz.magicmissiles.missile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Guards the coupled numbers on the shipped missile specs. */
class MissileSpecTest {
    @Test
    void turnRadiusFollowsFromSpeedAndTurnRate() {
        assertEquals(5.0 / Math.toRadians(9.0), MissileSpecs.STANDARD.turnRadius(), 1.0e-6);
    }

    @Test
    void terminalRangeLeavesRoomForTheDive() {
        // below about 2 the missile cannot turn tightly enough and circles the target
        assertTrue(MissileSpecs.STANDARD.diveMargin() > 3.0,
                "dive margin too tight: " + MissileSpecs.STANDARD.diveMargin());
    }

    @Test
    void seekerSweepsOftenEnoughToSeeItsRange() {
        MissileSpec spec = MissileSpecs.STANDARD;
        double travelBetweenSweeps = spec.cruiseSpeed() * spec.acquireInterval();
        assertTrue(travelBetweenSweeps < spec.seekerRange() * 0.5,
                "seeker sweeps too far apart: " + travelBetweenSweeps);
    }

    @Test
    void fuzeIsNotOutrunByOneTickOfFlight() {
        // the swept fuze handles the miss, but a step far beyond the fuze diameter
        // still means the missile only sees a target for a single tick
        MissileSpec spec = MissileSpecs.STANDARD;
        assertTrue(spec.cruiseSpeed() <= spec.proximityFuze() * 4.0,
                "step dwarfs the fuze: " + spec.cruiseSpeed());
    }

    @Test
    void staysBelowMachOne() {
        // 343 m/s over 20 ticks; the guidance model assumes flight below mach 1
        assertTrue(MissileSpecs.STANDARD.cruiseSpeed() < 343.0 / 20.0);
    }

    @Test
    void deadReckoningGapStaysSmall() {
        assertTrue(MissileSpecs.STANDARD.deadReckonGap() <= 12.0,
                "clients extrapolate too far: " + MissileSpecs.STANDARD.deadReckonGap());
    }

    @Test
    void unknownSpecIdFallsBackToStandard() {
        assertSame(MissileSpecs.STANDARD, MissileSpecs.byId("no-such-spec"));
        assertSame(MissileSpecs.STANDARD, MissileSpecs.byId(""));
    }
}
