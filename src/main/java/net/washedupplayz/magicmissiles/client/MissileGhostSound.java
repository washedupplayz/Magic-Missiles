package net.washedupplayz.magicmissiles.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.washedupplayz.magicmissiles.registry.ModSounds;

/**
 * Client-side launch/flight sound tied to a missile ghost. It follows the ghost as
 * it flies and stops when the ghost is removed (detonation or fizzle), so the clip
 * never outlives the rocket.
 *
 * <p>This is the Phase-2 placeholder: vanilla attenuation still caps audibility at
 * {@code ~16 × volume} blocks. The full distance model — launch-boom delay by
 * distance/speed-of-sound, Doppler, sonic pass-by — arrives with the audio director
 * in Phase 3.
 */
public class MissileGhostSound extends AbstractTickableSoundInstance {
    private final MissileGhost ghost;

    public MissileGhostSound(MissileGhost ghost) {
        super(ModSounds.MISSILE_LAUNCH.get(), SoundSource.BLOCKS, RandomSource.create());
        this.ghost = ghost;
        this.x = ghost.x;
        this.y = ghost.y;
        this.z = ghost.z;
        this.volume = 1.0f;
        this.pitch = 1.0f;
        this.looping = false;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
    }

    @Override
    public void tick() {
        if (ghost.isRemoved()) {
            stop();
            return;
        }
        this.x = ghost.x;
        this.y = ghost.y;
        this.z = ghost.z;
    }
}
