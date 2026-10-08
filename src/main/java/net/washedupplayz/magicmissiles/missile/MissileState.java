package net.washedupplayz.magicmissiles.missile;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

public final class MissileState {
    public long id;

    public String specId = MissileSpecs.STANDARD.id();

    public double x, y, z;
    public double vx, vy, vz;

    public double cruiseSpeed;
    public int fuelTicks;
    public float explosionPower;

    @Nullable
    public UUID ownerUuid;
    @Nullable
    public UUID targetUuid;

    // last resolvable target position, flown at while the target is unloaded
    public boolean hasLastTarget;
    public double lastTx, lastTy, lastTz;

    public int acquireCooldown;

    // coordinate target, the seeker stays off
    public boolean fixedTarget;
    // NaN for the default ceiling
    public double cruiseAltitude = Double.NaN;

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
        if (fixedTarget) {
            tag.putBoolean("Fixed", true);
        }
        if (!Double.isNaN(cruiseAltitude)) {
            tag.putDouble("CruiseAlt", cruiseAltitude);
        }
        return tag;
    }

    public static MissileState load(CompoundTag tag) {
        MissileState m = new MissileState();
        m.id = tag.getLong("Id");
        // absent on pre-spec saves, byId falls back to STANDARD
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
        m.fixedTarget = tag.getBoolean("Fixed");
        m.cruiseAltitude = tag.contains("CruiseAlt") ? tag.getDouble("CruiseAlt") : Double.NaN;
        return m;
    }
}
