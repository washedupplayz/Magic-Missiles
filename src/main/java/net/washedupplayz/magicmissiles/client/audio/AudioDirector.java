package net.washedupplayz.magicmissiles.client.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.washedupplayz.magicmissiles.client.MissileGhost;
import net.washedupplayz.magicmissiles.registry.ModSounds;

/**
 * Turns missile events into acoustic events.
 *
 * <p>The old behaviour played a file the instant something happened, at whatever
 * range vanilla's attenuation allowed. Here an event is a wavefront: it leaves the
 * place it happened, takes {@code distance / 17.15} ticks to arrive, and carries
 * the energy it had when it left.
 *
 * <p>Every sound uses {@link SoundInstance.Attenuation#NONE} and supplies its own
 * gain from {@link Acoustics}. The engine's own model is linear — half volume at
 * half the cutoff, then an abrupt stop — which is audibly wrong over the distances
 * missiles cover. Turning it off keeps the source positional, so direction and
 * panning still work; only the distance curve becomes ours.
 */
public final class AudioDirector {
    /** Radius inside which a sound is at full gain, then inverse-distance beyond. */
    private static final double LAUNCH_REFERENCE = 24.0;
    private static final double IMPACT_REFERENCE = 64.0;
    private static final double MOTOR_REFERENCE = 24.0;

    /** Range at which each sound stops being worth a channel. */
    private static final double LAUNCH_MAX = 1536.0;
    private static final double IMPACT_MAX = 4096.0;
    private static final double MOTOR_MAX = 2048.0;

    private AudioDirector() {}

    /** Ignition, heard from where the missile left the rail. */
    public static void onLaunch(Vec3 at) {
        fireDelayed(ModSounds.MISSILE_LAUNCH.get(), at, LAUNCH_REFERENCE, LAUNCH_MAX);
    }

    /** Detonation. At full range this lands several seconds after the flash. */
    public static void onImpact(Vec3 at) {
        fireDelayed(ModSounds.MISSILE_IMPACT.get(), at, IMPACT_REFERENCE, IMPACT_MAX);
    }

    /** Motor loop for the life of the ghost; silent until its wavefront arrives. */
    public static void startMotor(MissileGhost ghost) {
        Minecraft.getInstance().getSoundManager()
                .play(new MissileMotorSound(ghost, MOTOR_REFERENCE, MOTOR_MAX));
    }

    /**
     * Schedule a one-shot from a fixed point.
     *
     * <p>Distance and gain are measured once, at emission. A wavefront leaves with
     * the energy it has and does not care where the listener goes afterwards, so
     * recomputing on arrival would be wrong as well as more work.
     */
    private static void fireDelayed(SoundEvent event, Vec3 at, double reference, double max) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        double distance = player.position().distanceTo(at);
        float gain = Acoustics.distanceGain(distance, reference, max);
        if (gain <= 0.0f) {
            return;
        }
        SimpleSoundInstance instance = new SimpleSoundInstance(
                event.getLocation(),
                SoundSource.BLOCKS,
                gain,
                1.0f,
                SoundInstance.createUnseededRandom(),
                false,
                0,
                SoundInstance.Attenuation.NONE,
                at.x, at.y, at.z,
                false);
        minecraft.getSoundManager().playDelayed(instance, Acoustics.delayTicks(distance));
    }
}
