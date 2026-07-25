package net.washedupplayz.magicmissiles.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.washedupplayz.magicmissiles.entity.MissileEntity;
import net.washedupplayz.magicmissiles.registry.ModSounds;

/**
 * Client-side launch sound tied to a missile's lifetime. It starts when the missile
 * spawns and stops automatically the moment the missile is removed (e.g. when it
 * detonates), so a long launch/startup clip never outlives the rocket. Anchored to
 * the launch position rather than following the missile.
 */
public class MissileLaunchSound extends AbstractTickableSoundInstance {
    private final MissileEntity missile;

    public MissileLaunchSound(MissileEntity missile) {
        super(ModSounds.MISSILE_LAUNCH.get(), SoundSource.BLOCKS, RandomSource.create());
        this.missile = missile;
        this.x = missile.getX();
        this.y = missile.getY();
        this.z = missile.getZ();
        this.volume = 1.0f;
        this.pitch = 1.0f;
        this.looping = false;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
    }

    @Override
    public void tick() {
        if (this.missile.isRemoved()) {
            this.stop();
        }
    }
}
