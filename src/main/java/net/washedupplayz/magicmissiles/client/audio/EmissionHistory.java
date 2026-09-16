package net.washedupplayz.magicmissiles.client.audio;

import net.minecraft.world.phys.Vec3;

/**
 * Recent positions of a moving sound source, used to find where it was when the
 * sound arriving now was emitted.
 *
 * <p>A missile kilometres away is heard several seconds late, so the sound should
 * come from where it was then, not from where it is. Solving for that retarded
 * position is what makes a distant fast mover sit correctly in the world instead of
 * sliding around with the object.
 *
 * <p>Fixed-size ring buffer, oldest entries overwritten. It is separate from the
 * render trail, which stores nozzle positions and is sized for how the contrail
 * should look rather than for how far sound travels.
 */
public final class EmissionHistory {
    /** Covers roughly {@code capacity * SPEED_OF_SOUND} blocks of delay. */
    public static final int DEFAULT_CAPACITY = 160;

    private final double[] xs;
    private final double[] ys;
    private final double[] zs;
    private final long[] ticks;
    private int size;
    private int next;

    public EmissionHistory() {
        this(DEFAULT_CAPACITY);
    }

    public EmissionHistory(int capacity) {
        this.xs = new double[capacity];
        this.ys = new double[capacity];
        this.zs = new double[capacity];
        this.ticks = new long[capacity];
    }

    public void record(long tick, Vec3 pos) {
        xs[next] = pos.x;
        ys[next] = pos.y;
        zs[next] = pos.z;
        ticks[next] = tick;
        next = (next + 1) % xs.length;
        if (size < xs.length) {
            size++;
        }
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /** Newest first: 0 is the most recent sample. */
    private int index(int age) {
        return Math.floorMod(next - 1 - age, xs.length);
    }

    private Vec3 at(int age) {
        int i = index(age);
        return new Vec3(xs[i], ys[i], zs[i]);
    }

    /**
     * Where the source was when the wavefront reaching {@code listener} at
     * {@code now} left it.
     *
     * <p>Solves {@code |P(t) - L| = c (now - t)} by walking back through the
     * samples for the sign change and interpolating across it. The crossing is
     * unique while the source stays below the speed of sound, which the guidance
     * model guarantees.
     *
     * <p>Returns null when no crossing exists, meaning nothing emitted so far has
     * reached the listener yet. That is the correct answer just after launch — the
     * sound is still in the air — and it gives the motor loop its propagation delay
     * for free. It also covers a listener so distant that the emission predates the
     * buffer, which is further than anything stays audible.
     */
    public Vec3 retardedPosition(long now, Vec3 listener) {
        if (size == 0) {
            return null;
        }
        double previous = residual(0, now, listener);
        if (previous >= 0.0) {
            return at(0);
        }
        for (int age = 1; age < size; age++) {
            double current = residual(age, now, listener);
            if (current >= 0.0) {
                double span = current - previous;
                double alpha = span < 1.0e-9 ? 0.0 : -previous / span;
                return at(age - 1).lerp(at(age), alpha);
            }
            previous = current;
        }
        return null;
    }

    /** {@code c (now - t) - |P(t) - L|}: negative before the wavefront, positive after. */
    private double residual(int age, long now, Vec3 listener) {
        int i = index(age);
        double dx = xs[i] - listener.x;
        double dy = ys[i] - listener.y;
        double dz = zs[i] - listener.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return Acoustics.SPEED_OF_SOUND * (now - ticks[i]) - distance;
    }
}
