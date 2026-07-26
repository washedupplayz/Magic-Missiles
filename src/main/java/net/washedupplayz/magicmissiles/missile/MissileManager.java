package net.washedupplayz.magicmissiles.missile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
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
import net.washedupplayz.magicmissiles.registry.ModSounds;
import net.washedupplayz.magicmissiles.util.GuidanceMath;

/**
 * Per-{@link ServerLevel} authoritative simulation of all in-flight missiles.
 *
 * <p>Missiles are plain {@link MissileState} numbers, not entities, so they tick
 * every server tick independent of chunk loading — a missile never freezes and
 * never forces a chunk to load while cruising. Guidance reuses {@link GuidanceMath}.
 *
 * <p>Flight is lofted: the missile climbs to a cruise ceiling above the world's
 * build height and only dives once it is within terminal range of its target. That
 * keeps it above any terrain while it crosses unloaded chunks (where we cannot test
 * collision), so "free flight over unloaded chunks" is safe. Block collision is
 * only evaluated where chunks are loaded; the decisive terminal impact happens near
 * the target, which is loaded.
 *
 * <p>State persists via {@link SavedData} so missiles survive a restart, and every
 * launch/update/impact is broadcast to clients (see {@link ModNetwork}) which render
 * the missile as a long-range ghost.
 */
public class MissileManager extends SavedData {
    private static final String DATA_NAME = MagicMissiles.MOD_ID + "_missiles";

    // --- Guidance tuning ---
    private static final double SEEKER_RANGE = 24.0;
    private static final double SEEKER_CONE_COS = Math.cos(Math.toRadians(60.0));
    private static final double MAX_TURN_RAD = Math.toRadians(9.0);
    private static final int ACQUIRE_INTERVAL = 4;
    private static final double MIN_CRUISE_SPEED = 0.1;

    // --- Long-range flight ---
    /** Blocks above the world's max build height to cruise at (nothing to hit up there). */
    private static final double CRUISE_CEILING_MARGIN = 16.0;
    /** Horizontal distance to the target at which the missile begins its terminal dive. */
    private static final double TERMINAL_RANGE = 48.0;
    /** How far ahead the cruise waypoint is projected each tick. */
    private static final double CRUISE_LOOKAHEAD = 24.0;
    /** Proximity fuze radius around the aim point. */
    private static final double PROXIMITY_FUZE = 2.0;
    /** Ticks between network position corrections. */
    private static final int NETWORK_UPDATE_INTERVAL = 4;

    private static final int DEFAULT_FUEL_TICKS = 1200; // ~60s: long enough to watch cross-terrain flights
    private static final float DEFAULT_EXPLOSION_POWER = 3.0f;
    private static final float DIRECT_HIT_DAMAGE = 6.0f;

    private final Map<Long, MissileState> missiles = new LinkedHashMap<>();
    private long nextId = 1L;
    private long tickCounter;

    /** Not persisted; refreshed on every {@link #get}. */
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

    // ------------------------------------------------------------------
    // Launch
    // ------------------------------------------------------------------

    /**
     * Register a new missile and announce it to clients.
     *
     * @param owner  the launcher, excluded from targeting/collision (nullable)
     * @param target a designated target to home toward (nullable — otherwise the
     *               onboard seeker acquires one)
     * @return the assigned missile id
     */
    public long launch(Vec3 pos, Vec3 velocity, @Nullable LivingEntity owner, @Nullable LivingEntity target) {
        MissileState m = new MissileState();
        m.id = nextId++;
        m.setPos(pos);
        m.setVel(velocity);
        m.cruiseSpeed = Math.max(velocity.length(), MIN_CRUISE_SPEED);
        m.fuelTicks = DEFAULT_FUEL_TICKS;
        m.explosionPower = DEFAULT_EXPLOSION_POWER;
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

    /** Resend spawns for every active missile to a player who just joined this level. */
    public void resyncTo(ServerPlayer player) {
        for (MissileState m : missiles.values()) {
            ModNetwork.sendTo(player, new MissileSpawnPayload(m.id, m.x, m.y, m.z, m.vx, m.vy, m.vz));
        }
    }

    // ------------------------------------------------------------------
    // Tick
    // ------------------------------------------------------------------

    public void tick(ServerLevel level) {
        this.level = level;
        if (missiles.isEmpty()) {
            return;
        }
        tickCounter++;
        // Copy the values so a detonation can remove from the map mid-iteration.
        for (MissileState m : new ArrayList<>(missiles.values())) {
            if (!tickMissile(m)) {
                missiles.remove(m.id);
            }
        }
        setDirty();
    }

    /** @return {@code true} to keep the missile flying, {@code false} once it is gone. */
    private boolean tickMissile(MissileState m) {
        Vec3 pos = m.pos();
        Vec3 vel = m.vel();
        if (m.cruiseSpeed < MIN_CRUISE_SPEED) {
            m.cruiseSpeed = Math.max(vel.length(), MIN_CRUISE_SPEED);
        }

        // Resolve target and keep a fix on where it was, so we can fly toward it
        // even while it sits in an unloaded chunk.
        LivingEntity target = resolveTarget(m);
        Vec3 aim = null;
        if (target != null) {
            aim = aimPoint(target);
            m.rememberTarget(aim);
        } else if (m.hasLastTarget) {
            aim = new Vec3(m.lastTx, m.lastTy, m.lastTz);
        }

        boolean loaded = level.isLoaded(BlockPos.containing(pos));

        // Onboard seeker: only meaningful where entities are loaded.
        if (m.targetUuid == null && loaded && --m.acquireCooldown <= 0) {
            m.acquireCooldown = ACQUIRE_INTERVAL;
            LivingEntity acquired = acquireTarget(m, vel);
            if (acquired != null) {
                m.targetUuid = acquired.getUUID();
                aim = aimPoint(acquired);
                m.rememberTarget(aim);
            }
        }

        // Steer toward the lofted waypoint (or hold heading with no target).
        Vec3 desired = aim != null ? loftedDesire(pos, aim) : vel;
        vel = GuidanceMath.steer(vel, desired, MAX_TURN_RAD, m.cruiseSpeed);
        Vec3 nextPos = pos.add(vel);

        // Collision only where the chunk is loaded; free flight otherwise.
        if (loaded) {
            Vec3 impact = clipBlocks(pos, nextPos);
            if (impact != null) {
                detonate(m, impact, target, true);
                return false;
            }
            if (aim != null && nextPos.distanceToSqr(aim) <= PROXIMITY_FUZE * PROXIMITY_FUZE) {
                detonate(m, nextPos, target, true);
                return false;
            }
        }

        m.setPos(nextPos);
        m.setVel(vel);

        if (--m.fuelTicks <= 0) {
            // Fuel-out: explode if we're over loaded ground, otherwise fizzle silently.
            detonate(m, m.pos(), target, loaded);
            return false;
        }

        if (tickCounter % NETWORK_UPDATE_INTERVAL == 0) {
            ModNetwork.broadcast(level, new MissileUpdatePayload(m.id, m.x, m.y, m.z, m.vx, m.vy, m.vz));
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Guidance helpers
    // ------------------------------------------------------------------

    private Vec3 loftedDesire(Vec3 pos, Vec3 aim) {
        double ceiling = level.getMaxBuildHeight() + CRUISE_CEILING_MARGIN;
        return GuidanceMath.loftedDirection(pos, aim, ceiling, TERMINAL_RANGE, CRUISE_LOOKAHEAD);
    }

    /** Resolve the locked target; keep the id if merely unloaded, drop it if dead/invalid. */
    @Nullable
    private LivingEntity resolveTarget(MissileState m) {
        if (m.targetUuid == null) {
            return null;
        }
        Entity entity = level.getEntity(m.targetUuid);
        if (entity == null) {
            return null; // possibly just unloaded — keep the id and fly on last-known
        }
        if (entity instanceof LivingEntity living && living.isAlive() && !living.getUUID().equals(m.ownerUuid)) {
            return living;
        }
        m.targetUuid = null; // dead / invalid
        return null;
    }

    /** Onboard seeker: nearest living target in the forward cone with line of sight. */
    @Nullable
    private LivingEntity acquireTarget(MissileState m, Vec3 vel) {
        if (vel.lengthSqr() < MIN_CRUISE_SPEED * MIN_CRUISE_SPEED) {
            return null;
        }
        Vec3 forward = vel.normalize();
        Vec3 pos = m.pos();
        AABB searchBox = new AABB(pos, pos).inflate(SEEKER_RANGE);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                candidate -> candidate.isAlive() && !(candidate instanceof Player)
                        && !candidate.getUUID().equals(m.ownerUuid));

        LivingEntity best = null;
        double bestDistSqr = SEEKER_RANGE * SEEKER_RANGE;
        for (LivingEntity candidate : candidates) {
            Vec3 toTarget = aimPoint(candidate).subtract(pos);
            double distSqr = toTarget.lengthSqr();
            if (distSqr > bestDistSqr) {
                continue;
            }
            if (forward.dot(toTarget.normalize()) < SEEKER_CONE_COS) {
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

    /** @return the block-hit location along {@code from → to}, or {@code null} for a clear path. */
    @Nullable
    private Vec3 clipBlocks(Vec3 from, Vec3 to) {
        HitResult result = level.clip(new ClipContext(
                from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        return result.getType() == HitResult.Type.MISS ? null : result.getLocation();
    }

    private static Vec3 aimPoint(Entity target) {
        return target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
    }

    // ------------------------------------------------------------------
    // Terminal
    // ------------------------------------------------------------------

    private void detonate(MissileState m, Vec3 at, @Nullable LivingEntity directHit, boolean explode) {
        if (explode) {
            // Make sure the target chunk's blocks are present, then blast. Impacts land
            // near the target (loaded), so this is a rare, cheap synchronous load.
            level.getChunkAt(BlockPos.containing(at));

            Entity owner = m.ownerUuid == null ? null : level.getEntity(m.ownerUuid);
            if (directHit != null) {
                directHit.hurt(level.damageSources().explosion(null, owner), DIRECT_HIT_DAMAGE);
            }
            // Volume 4.0 => ~64-block broadcast, matching the blast radius audibility.
            level.playSound(null, at.x, at.y, at.z,
                    ModSounds.MISSILE_IMPACT.get(), SoundSource.BLOCKS, 4.0f, 1.0f);
            level.explode(null, at.x, at.y, at.z, m.explosionPower, Level.ExplosionInteraction.TNT);
        }
        ModNetwork.broadcast(level, new MissileRemovePayload(m.id, at.x, at.y, at.z, explode));
    }
}
