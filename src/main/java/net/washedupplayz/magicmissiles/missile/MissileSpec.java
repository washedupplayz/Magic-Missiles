package net.washedupplayz.magicmissiles.missile;

// coupled fields:
// terminalRange must stay well above turnRadius or the missile circles, keep diveMargin near 4
// acquireInterval * cruiseSpeed must stay well inside seekerRange
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
    public double turnRadius() {
        return cruiseSpeed / maxTurnRad;
    }

    public double diveMargin() {
        return terminalRange / turnRadius();
    }

    public double deadReckonGap() {
        return cruiseSpeed * networkUpdateInterval;
    }

    public double range() {
        return cruiseSpeed * fuelTicks;
    }
}
