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

// every sound uses Attenuation.NONE with gain from Acoustics, the engine curve is linear
public final class AudioDirector {
    // full gain inside, inverse distance beyond
    private static final double LAUNCH_REFERENCE = 24.0;
    private static final double IMPACT_REFERENCE = 64.0;

    // silent beyond
    private static final double LAUNCH_MAX = 1536.0;
    private static final double IMPACT_MAX = 4096.0;

    private AudioDirector() {}

    public static void onLaunch(MissileGhost ghost) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        double distance = player.position().distanceTo(ghost.pos());
        if (distance >= LAUNCH_MAX) {
            return;
        }
        int delay = Acoustics.delayTicks(distance);
        minecraft.getSoundManager().playDelayed(
                new MissileFlightSound(ghost, delay, LAUNCH_REFERENCE, LAUNCH_MAX), delay);
    }

    public static void onImpact(Vec3 at) {
        fireDelayed(ModSounds.MISSILE_IMPACT.get(), at, IMPACT_REFERENCE, IMPACT_MAX);
    }

    // gain fixed at emission, the wavefront does not follow the listener
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
