package net.washedupplayz.magicmissiles.missile;

import java.util.LinkedHashMap;
import java.util.Map;

// ids are persisted, renaming one retargets saved missiles
public final class MissileSpecs {
    private static final Map<String, MissileSpec> BY_ID = new LinkedHashMap<>();

    // turn radius 31.8 against 128 terminal range, dive margin 4.0
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

    public static MissileSpec byId(String id) {
        return BY_ID.getOrDefault(id, STANDARD);
    }
}
