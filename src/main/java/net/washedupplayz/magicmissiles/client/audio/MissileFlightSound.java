package net.washedupplayz.magicmissiles.client.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.washedupplayz.magicmissiles.client.MissileGhost;
import net.washedupplayz.magicmissiles.registry.ModSounds;

// launch clip played once, following the missile at its retarded position
public class MissileFlightSound extends AbstractTickableSoundInstance {
    private final MissileGhost ghost;
    private final double reference;
    private final double max;
    // ghost clock, keeps running after removal
    private long now;

    public MissileFlightSound(MissileGhost ghost, int delay, double reference, double max) {
        super(ModSounds.MISSILE_LAUNCH.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.ghost = ghost;
        this.reference = reference;
        this.max = max;
        this.now = ghost.age() + delay;
        this.looping = false;
        // own curve, see AudioDirector
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.volume = 0.0f;
        this.pitch = 1.0f;
        this.x = ghost.x;
        this.y = ghost.y;
        this.z = ghost.z;
    }

    // volume is only known after the first tick
    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        now = ghost.isRemoved() ? now + 1 : ghost.age();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            this.volume = 0.0f;
            return;
        }

        Vec3 listener = player.position();
        // removed and its last emission heard
        if (ghost.isRemoved() && ghost.emissions().allArrived(now, listener)) {
            stop();
            return;
        }
        Vec3 heard = ghost.emissions().retardedPosition(now, listener);
        if (heard == null) {
            this.volume = 0.0f;
            return;
        }

        this.x = heard.x;
        this.y = heard.y;
        this.z = heard.z;
        this.volume = Acoustics.distanceGain(heard.distanceTo(listener), reference, max);
        this.pitch = Acoustics.dopplerPitch(heard, ghost.vel(), listener, player.getDeltaMovement());
    }
}
