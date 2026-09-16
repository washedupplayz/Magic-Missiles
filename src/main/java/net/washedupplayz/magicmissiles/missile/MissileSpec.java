package net.washedupplayz.magicmissiles.missile;

/**
 * Flight and guidance characteristics of one kind of missile.
 *
 * <p>These were global constants on {@link MissileManager}, which meant every
 * missile in the world flew identically. They live here so a launcher can pick a
 * spec at launch and the simulation reads its numbers per missile — a slow
 * heavy-warhead missile and a fast interceptor can share one manager.
 *
 * <p>Several fields are coupled and cannot be tuned independently:
 *
 * <ul>
 * <li>Turn radius follows from {@code cruiseSpeed} and {@code maxTurnRad}. A full
 *     circle takes {@code 360 / degrees-per-tick} ticks, so
 *     {@code radius = ticks * cruiseSpeed / 2pi}. {@link #turnRadius()} computes it.
 * <li>{@code terminalRange} must stay comfortably above that radius or the missile
 *     cannot turn tightly enough to dive onto a target and will circle it instead.
 *     {@link #diveMargin()} is the ratio; keep it near 4.
 * <li>{@code acquireInterval} times {@code cruiseSpeed} is how far the missile
 *     travels between seeker sweeps, which wants to stay well inside
 *     {@code seekerRange}.
 * </ul>
 */
public record MissileSpec(
        String id,
        double cruiseSpeed,
        double maxTurnRad,
        double seekerRange,
        double seekerConeCos,
        int acquireInterval,
        double terminalRange,
        double cruiseLookahead,
        double proximityFuze,
        int networkUpdateInterval,
        int fuelTicks,
        float explosionPower,
        float directHitDamage) {

    /** Turn radius in blocks: a full circle takes {@code 2pi / maxTurnRad} ticks. */
    public double turnRadius() {
        return cruiseSpeed / maxTurnRad;
    }

    /** How much room the terminal dive has: {@code terminalRange / turnRadius}. */
    public double diveMargin() {
        return terminalRange / turnRadius();
    }

    /** Blocks the missile covers between network corrections. */
    public double deadReckonGap() {
        return cruiseSpeed * networkUpdateInterval;
    }

    /** Powered range in blocks. */
    public double range() {
        return cruiseSpeed * fuelTicks;
    }
}
