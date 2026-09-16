package net.washedupplayz.magicmissiles.missile;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry of the {@link MissileSpec}s a launcher can fire.
 *
 * <p>Specs are looked up by id when a missile is loaded from disk, so ids are
 * persisted data — renaming one silently retargets every saved missile using it.
 * An unknown id falls back to {@link #STANDARD} rather than dropping the missile.
 */
public final class MissileSpecs {
    private static final Map<String, MissileSpec> BY_ID = new LinkedHashMap<>();

    /**
     * The general-purpose missile.
     *
     * <p>Geometry scales with speed. At the original 1.8 blocks/tick the terminal
     * range was 48 and the lookahead 24; both are scaled by the speed increase so
     * the flight profile keeps the same shape in time rather than in blocks.
     * Turn radius is 31.8 blocks against a 128-block terminal range, a dive margin
     * of 4.0 — the same margin the original tuning had.
     */
    public static final MissileSpec STANDARD = register(new MissileSpec(
            "standard",
            5.0,                            // cruise speed, blocks/tick (100 m/s, mach 0.29)
            Math.toRadians(9.0),            // max turn per tick
            24.0,                           // seeker range
            Math.cos(Math.toRadians(60.0)), // seeker half-cone
            2,                              // ticks between seeker sweeps
            128.0,                          // terminal dive range
            64.0,                           // cruise waypoint lookahead
            2.0,                            // proximity fuze radius
            2,                              // ticks between network corrections
            1200,                           // fuel: 60 s of flight
            3.0f,                           // explosion power
            6.0f));                         // direct hit damage

    private MissileSpecs() {}

    private static MissileSpec register(MissileSpec spec) {
        BY_ID.put(spec.id(), spec);
        return spec;
    }

    /** Never null — an unrecognised id resolves to {@link #STANDARD}. */
    public static MissileSpec byId(String id) {
        return BY_ID.getOrDefault(id, STANDARD);
    }
}
