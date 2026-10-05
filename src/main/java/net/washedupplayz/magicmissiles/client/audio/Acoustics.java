package net.washedupplayz.magicmissiles.client.audio;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class Acoustics {
    // 343 m/s in blocks per tick
    public static final double SPEED_OF_SOUND = 343.0 / 20.0;

    private static final double EPSILON = 1.0e-6;
    // engine pitch clamp
    private static final float MIN_PITCH = 0.5f;
    private static final float MAX_PITCH = 2.0f;

    private Acoustics() {}

    public static int delayTicks(double distance) {
        return (int) Math.round(Math.max(distance, 0.0) / SPEED_OF_SOUND);
    }

    // inverse distance, cosine taper over the last fifth so the cutoff does not click
    public static float distanceGain(double distance, double reference, double max) {
        if (distance >= max || max <= 0.0) {
            return 0.0f;
        }
        double gain = reference / Math.max(distance, reference);
        double taperStart = max * 0.8;
        if (distance > taperStart) {
            double t = (distance - taperStart) / (max - taperStart);
            gain *= 0.5 * (1.0 + Math.cos(Math.PI * t));
        }
        return (float) Mth.clamp(gain, 0.0, 1.0);
    }

    // f' = f (c - vl) / (c - vs), velocities along the source to listener axis
    public static float dopplerPitch(Vec3 sourcePos, Vec3 sourceVel, Vec3 listenerPos, Vec3 listenerVel) {
        Vec3 toListener = listenerPos.subtract(sourcePos);
        double distance = toListener.length();
        if (distance < EPSILON) {
            return 1.0f;
        }
        Vec3 axis = toListener.scale(1.0 / distance);
        double vs = sourceVel.dot(axis);
        double vl = listenerVel.dot(axis);

        // denominator collapses at the speed of sound
        double denominator = Math.max(SPEED_OF_SOUND - vs, SPEED_OF_SOUND * 0.1);
        double shift = (SPEED_OF_SOUND - vl) / denominator;
        return Mth.clamp((float) shift, MIN_PITCH, MAX_PITCH);
    }
}
