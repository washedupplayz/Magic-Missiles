package net.washedupplayz.magicmissiles.missile;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

/**
 * Authoritative server-side flight state for one missile. Deliberately <em>not</em>
 * an {@link net.minecraft.world.entity.Entity}: it lives in {@link MissileManager}
 * and is ticked every server tick regardless of whether the chunk it is over is
 * loaded, so a missile can cruise across unloaded terrain without freezing or
 * forcing chunk loads. It is a plain mutable data holder — all behaviour lives in
 * the manager.
 */
public final class MissileState {
    public long id;

    /** Which {@link MissileSpec} governs this missile's flight. Persisted by id. */
    public String specId = MissileSpecs.STANDARD.id();

    public double x, y, z;
    public double vx, vy, vz;

    /** Speed the guidance keeps the missile at (set from the launch velocity). */
    public double cruiseSpeed;
    public int fuelTicks;
    public float explosionPower;

    @Nullable
    public UUID ownerUuid;
    @Nullable
    public UUID targetUuid;

    /**
     * Last position at which the target was resolvable. Lets the missile keep
     * flying toward where the target was even while the target sits in an unloaded
     * chunk (where {@link net.minecraft.server.level.ServerLevel#getEntity} can't
     * find it).
     */
    public boolean hasLastTarget;
    public double lastTx, lastTy, lastTz;

    public int acquireCooldown;

    public MissileSpec spec() {
        return MissileSpecs.byId(specId);
    }

    public Vec3 pos() {
        return new Vec3(x, y, z);
    }

    public Vec3 vel() {
        return new Vec3(vx, vy, vz);
    }

    public void setPos(Vec3 p) {
        this.x = p.x;
        this.y = p.y;
        this.z = p.z;
    }

    public void setVel(Vec3 v) {
        this.vx = v.x;
        this.vy = v.y;
        this.vz = v.z;
    }

    public void rememberTarget(Vec3 aim) {
        this.hasLastTarget = true;
        this.lastTx = aim.x;
        this.lastTy = aim.y;
        this.lastTz = aim.z;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Id", id);
        tag.putString("Spec", specId);
        tag.putDouble("X", x);
        tag.putDouble("Y", y);
        tag.putDouble("Z", z);
        tag.putDouble("VX", vx);
        tag.putDouble("VY", vy);
        tag.putDouble("VZ", vz);
        tag.putDouble("Cruise", cruiseSpeed);
        tag.putInt("Fuel", fuelTicks);
        tag.putFloat("Power", explosionPower);
        if (ownerUuid != null) {
            tag.putUUID("Owner", ownerUuid);
        }
        if (targetUuid != null) {
            tag.putUUID("Target", targetUuid);
        }
        if (hasLastTarget) {
            tag.putBoolean("HasLastTarget", true);
            tag.putDouble("LTX", lastTx);
            tag.putDouble("LTY", lastTy);
            tag.putDouble("LTZ", lastTz);
        }
        return tag;
    }

    public static MissileState load(CompoundTag tag) {
        MissileState m = new MissileState();
        m.id = tag.getLong("Id");
        // missing on missiles saved before specs existed; byId falls back to STANDARD
        m.specId = tag.getString("Spec");
        m.x = tag.getDouble("X");
        m.y = tag.getDouble("Y");
        m.z = tag.getDouble("Z");
        m.vx = tag.getDouble("VX");
        m.vy = tag.getDouble("VY");
        m.vz = tag.getDouble("VZ");
        m.cruiseSpeed = tag.getDouble("Cruise");
        m.fuelTicks = tag.getInt("Fuel");
        m.explosionPower = tag.getFloat("Power");
        if (tag.hasUUID("Owner")) {
            m.ownerUuid = tag.getUUID("Owner");
        }
        if (tag.hasUUID("Target")) {
            m.targetUuid = tag.getUUID("Target");
        }
        if (tag.getBoolean("HasLastTarget")) {
            m.hasLastTarget = true;
            m.lastTx = tag.getDouble("LTX");
            m.lastTy = tag.getDouble("LTY");
            m.lastTz = tag.getDouble("LTZ");
        }
        return m;
    }
}
