package net.washedupplayz.magicmissiles.client.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.washedupplayz.magicmissiles.client.MissileGhost;
import net.washedupplayz.magicmissiles.registry.ModSounds;

/**
 * The rocket motor, running for as long as the ghost exists.
 *
 * <p>Replaces the one-shot that used to play at launch: a 60 s burn wants a loop,
 * not a five-second clip. The engine re-reads volume, pitch and position from a
 * tickable instance every tick, which is what makes the rest of this possible.
 *
 * <p>The sound is placed at the missile's <em>retarded</em> position — where it was
 * when the sound arriving now left it — rather than where it is. For a fast source
 * kilometres away those are very different places, and using the current position
 * is what makes distant movers sound like they are sliding around the listener.
 * Before the first wavefront arrives there is no retarded position at all, so the
 * loop simply stays silent, which is the propagation delay handled for free.
 */
public class MissileMotorSound extends AbstractTickableSoundInstance {
    private final MissileGhost ghost;
    private final double reference;
    private final double max;

    public MissileMotorSound(MissileGhost ghost, double reference, double max) {
        super(ModSounds.MISSILE_LAUNCH.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.ghost = ghost;
        this.reference = reference;
        this.max = max;
        this.looping = true;
        this.delay = 0;
        // our own curve, see AudioDirector
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.volume = 0.0f;
        this.pitch = 1.0f;
        this.x = ghost.x;
        this.y = ghost.y;
        this.z = ghost.z;
    }

    /** Starts out of earshot and fades up as the wavefront arrives, so it must. */
    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (ghost.isRemoved()) {
            stop();
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            this.volume = 0.0f;
            return;
        }

        Vec3 listener = player.position();
        Vec3 heard = ghost.emissions().retardedPosition(ghost.age(), listener);
        if (heard == null) {
            this.volume = 0.0f; // still in the air
            return;
        }

        this.x = heard.x;
        this.y = heard.y;
        this.z = heard.z;
        this.volume = Acoustics.distanceGain(heard.distanceTo(listener), reference, max);
        this.pitch = Acoustics.dopplerPitch(heard, ghost.vel(), listener, player.getDeltaMovement());
    }
}
