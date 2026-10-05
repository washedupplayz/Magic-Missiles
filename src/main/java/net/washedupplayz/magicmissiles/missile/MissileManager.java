package net.washedupplayz.magicmissiles.missile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.washedupplayz.magicmissiles.MagicMissiles;
import net.washedupplayz.magicmissiles.network.MissileRemovePayload;
import net.washedupplayz.magicmissiles.network.MissileSpawnPayload;
import net.washedupplayz.magicmissiles.network.MissileUpdatePayload;
import net.washedupplayz.magicmissiles.network.ModNetwork;
import net.washedupplayz.magicmissiles.util.GuidanceMath;

// missiles are not entities, so they tick regardless of chunk loading
public class MissileManager extends SavedData {
    private static final String DATA_NAME = MagicMissiles.MOD_ID + "_missiles";

    // per-missile numbers live on MissileSpec

    private static final double MIN_CRUISE_SPEED = 0.1;
    // above build height, nothing to hit over unloaded chunks
    private static final double CRUISE_CEILING_MARGIN = 16.0;
    private static final Holder<SoundEvent> SILENT = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.EMPTY);

    private final Map<Long, MissileState> missiles = new LinkedHashMap<>();
    private long nextId = 1L;
    private long tickCounter;

    private ServerLevel level;

    public MissileManager() {}

    public static MissileManager get(ServerLevel level) {
        MissileManager manager = level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(MissileManager::new, (tag, provider) -> load(tag)), DATA_NAME);
        manager.level = level;
        return manager;
    }

    private static MissileManager load(CompoundTag tag) {
        MissileManager manager = new MissileManager();
        manager.nextId = tag.getLong("NextId");
        ListTag list = tag.getList("Missiles", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            MissileState m = MissileState.load(list.getCompound(i));
            manager.missiles.put(m.id, m);
        }
        return manager;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putLong("NextId", nextId);
        ListTag list = new ListTag();
        for (MissileState m : missiles.values()) {
            list.add(m.save());
        }
        tag.put("Missiles", list);
        return tag;
    }

    public long launch(Vec3 pos, Vec3 direction, MissileSpec spec,
                       @Nullable LivingEntity owner, @Nullable LivingEntity target) {
        MissileState m = new MissileState();
        m.id = nextId++;
        m.specId = spec.id();
        m.setPos(pos);
        // speed from the spec, heading from the launcher
        Vec3 heading = direction.lengthSqr() < 1.0e-8 ? new Vec3(0.0, 1.0, 0.0) : direction.normalize();
        m.setVel(heading.scale(spec.cruiseSpeed()));
        m.cruiseSpeed = Math.max(spec.cruiseSpeed(), MIN_CRUISE_SPEED);
        m.fuelTicks = spec.fuelTicks();
        m.explosionPower = spec.explosionPower();
        m.ownerUuid = owner == null ? null : owner.getUUID();
        if (target != null) {
            m.targetUuid = target.getUUID();
            m.rememberTarget(aimPoint(target));
        }

        missiles.put(m.id, m);
        setDirty();
        ModNetwork.broadcast(level, new MissileSpawnPayload(m.id, m.x, m.y, m.z, m.vx, m.vy, m.vz));
        return m.id;
    }

    public void resyncTo(ServerPlayer player) {
        for (MissileState m : missiles.values()) {
            ModNetwork.sendTo(player, new MissileSpawnPayload(m.id, m.x, m.y, m.z, m.vx, m.vy, m.vz));
        }
    }

    public void tick(ServerLevel level) {
        this.level = level;
        if (missiles.isEmpty()) {
            return;
        }
        tickCounter++;
        // copy, missiles are removed mid-iteration
        for (MissileState m : new ArrayList<>(missiles.values())) {
            if (!tickMissile(m)) {
                missiles.remove(m.id);
            }
        }
        setDirty();
    }

    private boolean tickMissile(MissileState m) {
        MissileSpec spec = m.spec();
        Vec3 pos = m.pos();
        Vec3 vel = m.vel();
        if (m.cruiseSpeed < MIN_CRUISE_SPEED) {
            m.cruiseSpeed = Math.max(vel.length(), MIN_CRUISE_SPEED);
        }

        // last known fix, so it flies on while the target is unloaded
        LivingEntity target = resolveTarget(m);
        Vec3 aim = null;
        if (target != null) {
            aim = aimPoint(target);
            m.rememberTarget(aim);
        } else if (m.hasLastTarget) {
            aim = new Vec3(m.lastTx, m.lastTy, m.lastTz);
        }

        boolean loaded = level.isLoaded(BlockPos.containing(pos));

        // seeker needs loaded entities
        if (m.targetUuid == null && loaded && --m.acquireCooldown <= 0) {
            m.acquireCooldown = spec.acquireInterval();
            LivingEntity acquired = acquireTarget(m, vel, spec);
            if (acquired != null) {
                m.targetUuid = acquired.getUUID();
                aim = aimPoint(acquired);
                m.rememberTarget(aim);
            }
        }

        // holds heading without a target
        Vec3 desired = aim != null ? loftedDesire(pos, aim, spec) : vel;
        vel = GuidanceMath.steer(vel, desired, spec.maxTurnRad(), m.cruiseSpeed);
        Vec3 nextPos = pos.add(vel);

        // no collision over unloaded chunks
        if (loaded) {
            // swept fuze up to any block hit, a fast missile cannot step past its target
            Vec3 impact = clipBlocks(pos, nextPos);
            Vec3 end = impact != null ? impact : nextPos;
            if (aim != null) {
                Vec3 closest = GuidanceMath.closestPointOnSegment(pos, end, aim);
                if (closest.distanceToSqr(aim) <= spec.proximityFuze() * spec.proximityFuze()) {
                    detonate(m, closest, target, true);
                    return false;
                }
            }
            if (impact != null) {
                detonate(m, impact, target, true);
                return false;
            }
        }

        m.setPos(nextPos);
        m.setVel(vel);

        if (--m.fuelTicks <= 0) {
            // fuel out: explode over loaded ground, fizzle otherwise
            detonate(m, m.pos(), target, loaded);
            return false;
        }

        if (tickCounter % spec.networkUpdateInterval() == 0) {
            ModNetwork.broadcast(level, new MissileUpdatePayload(m.id, m.x, m.y, m.z, m.vx, m.vy, m.vz));
        }
        return true;
    }

    private Vec3 loftedDesire(Vec3 pos, Vec3 aim, MissileSpec spec) {
        double ceiling = level.getMaxBuildHeight() + CRUISE_CEILING_MARGIN;
        return GuidanceMath.loftedDirection(pos, aim, ceiling, spec.terminalRange(), spec.cruiseLookahead());
    }

    @Nullable
    private LivingEntity resolveTarget(MissileState m) {
        if (m.targetUuid == null) {
            return null;
        }
        Entity entity = level.getEntity(m.targetUuid);
        if (entity == null) {
            return null; // maybe just unloaded, keep the id
        }
        if (entity instanceof LivingEntity living && living.isAlive() && !living.getUUID().equals(m.ownerUuid)) {
            return living;
        }
        m.targetUuid = null; // dead or invalid
        return null;
    }

    @Nullable
    private LivingEntity acquireTarget(MissileState m, Vec3 vel, MissileSpec spec) {
        if (vel.lengthSqr() < MIN_CRUISE_SPEED * MIN_CRUISE_SPEED) {
            return null;
        }
        Vec3 forward = vel.normalize();
        Vec3 pos = m.pos();
        AABB searchBox = new AABB(pos, pos).inflate(spec.seekerRange());
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                candidate -> candidate.isAlive() && !(candidate instanceof Player)
                        && !candidate.getUUID().equals(m.ownerUuid));

        LivingEntity best = null;
        double bestDistSqr = spec.seekerRange() * spec.seekerRange();
        for (LivingEntity candidate : candidates) {
            Vec3 toTarget = aimPoint(candidate).subtract(pos);
            double distSqr = toTarget.lengthSqr();
            if (distSqr > bestDistSqr) {
                continue;
            }
            if (forward.dot(toTarget.normalize()) < spec.seekerConeCos()) {
                continue;
            }
            if (!hasLineOfSight(pos, aimPoint(candidate))) {
                continue;
            }
            best = candidate;
            bestDistSqr = distSqr;
        }
        return best;
    }

    private boolean hasLineOfSight(Vec3 start, Vec3 end) {
        HitResult result = level.clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        return result.getType() == HitResult.Type.MISS || result.getLocation().distanceToSqr(end) < 1.0;
    }

    @Nullable
    private Vec3 clipBlocks(Vec3 from, Vec3 to) {
        HitResult result = level.clip(new ClipContext(
                from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        return result.getType() == HitResult.Type.MISS ? null : result.getLocation();
    }

    private static Vec3 aimPoint(Entity target) {
        return target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
    }

    private void detonate(MissileState m, Vec3 at, @Nullable LivingEntity directHit, boolean explode) {
        if (explode) {
            // rare synchronous load, impacts land near the loaded target
            level.getChunkAt(BlockPos.containing(at));

            Entity owner = m.ownerUuid == null ? null : level.getEntity(m.ownerUuid);
            if (directHit != null) {
                directHit.hurt(level.damageSources().explosion(null, owner), m.spec().directHitDamage());
            }
            // silent, the client schedules the boom from the remove payload
            level.explode(null, Explosion.getDefaultDamageSource(level, null), null,
                    at.x, at.y, at.z, m.explosionPower, false, Level.ExplosionInteraction.TNT,
                    ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER, SILENT);
        }
        ModNetwork.broadcast(level, new MissileRemovePayload(m.id, at.x, at.y, at.z, explode));
    }
}
