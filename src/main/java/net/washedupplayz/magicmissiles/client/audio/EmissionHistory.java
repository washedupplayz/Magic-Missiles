package net.washedupplayz.magicmissiles.client.audio;

import net.minecraft.world.phys.Vec3;

// ring buffer of past source positions, for the retarded position
public final class EmissionHistory {
    // ~2700 blocks of delay
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

    // age 0 is the newest sample
    private int index(int age) {
        return Math.floorMod(next - 1 - age, xs.length);
    }

    private Vec3 at(int age) {
        int i = index(age);
        return new Vec3(xs[i], ys[i], zs[i]);
    }

    // solves |P(t) - L| = c (now - t), unique below the speed of sound
    // null while nothing emitted has reached the listener yet
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

    // true once the newest sample has been heard
    public boolean allArrived(long now, Vec3 listener) {
        return size > 0 && residual(0, now, listener) >= 0.0;
    }

    // negative before the wavefront, positive after
    private double residual(int age, long now, Vec3 listener) {
        int i = index(age);
        double dx = xs[i] - listener.x;
        double dy = ys[i] - listener.y;
        double dz = zs[i] - listener.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return Acoustics.SPEED_OF_SOUND * (now - ticks[i]) - distance;
    }
}
