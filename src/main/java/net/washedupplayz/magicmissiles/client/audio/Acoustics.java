package net.washedupplayz.magicmissiles.client.audio;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Propagation maths shared by every missile sound. Pure functions on numbers, no
 * Minecraft state, so the behaviour can be tested without a client.
 *
 * <p>Minecraft is one block per metre and twenty ticks per second, which fixes the
 * speed of sound at {@value #SPEED_OF_SOUND} blocks per tick. Everything here
 * follows from that.
 */
public final class Acoustics {
    /** 343 m/s expressed in blocks per tick. */
    public static final double SPEED_OF_SOUND = 343.0 / 20.0;

    private static final double EPSILON = 1.0e-6;
    /** The engine clamps pitch to this range in {@code SoundEngine.calculatePitch}. */
    private static final float MIN_PITCH = 0.5f;
    private static final float MAX_PITCH = 2.0f;

    private Acoustics() {}

    /** Ticks for a wavefront to cover {@code distance} blocks. */
    public static int delayTicks(double distance) {
        return (int) Math.round(Math.max(distance, 0.0) / SPEED_OF_SOUND);
    }

    /**
     * Inverse-distance gain, tapered to silence at {@code max}.
     *
     * <p>Vanilla's linear model holds half volume at half the cutoff and then stops
     * dead. Real sound loses most of its loudness early and trails off, which is
     * {@code reference / distance}. The last fifth of the range gets a cosine taper
     * so reaching the cutoff does not click.
     */
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

    /**
     * Doppler-shifted pitch for a source and listener in motion.
     *
     * <p>{@code f' = f (c - vl) / (c - vs)} with both velocities projected onto the
     * source-to-listener axis: {@code vs} is the source closing on the listener and
     * {@code vl} the listener receding from the source. Velocities are blocks per
     * tick to match {@link #SPEED_OF_SOUND}.
     */
    public static float dopplerPitch(Vec3 sourcePos, Vec3 sourceVel, Vec3 listenerPos, Vec3 listenerVel) {
        Vec3 toListener = listenerPos.subtract(sourcePos);
        double distance = toListener.length();
        if (distance < EPSILON) {
            return 1.0f;
        }
        Vec3 axis = toListener.scale(1.0 / distance);
        double vs = sourceVel.dot(axis);
        double vl = listenerVel.dot(axis);

        // Guard the denominator: at or past the speed of sound it collapses, and the
        // guidance model keeps missiles well below that anyway.
        double denominator = Math.max(SPEED_OF_SOUND - vs, SPEED_OF_SOUND * 0.1);
        double shift = (SPEED_OF_SOUND - vl) / denominator;
        return Mth.clamp((float) shift, MIN_PITCH, MAX_PITCH);
    }
}
