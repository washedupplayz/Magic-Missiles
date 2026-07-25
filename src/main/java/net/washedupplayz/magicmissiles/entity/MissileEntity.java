package net.washedupplayz.magicmissiles.entity;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.washedupplayz.magicmissiles.registry.ModEntities;
import net.washedupplayz.magicmissiles.registry.ModSounds;
import net.washedupplayz.magicmissiles.util.GuidanceMath;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A self-propelled, gravity-defying missile.
 *
 * <p>Flight is guided: the missile maintains its launch speed and steers toward a
 * locked target with a limited turn rate, so it curves in like a real missile. A
 * target can be designated at launch (e.g. by a radar-linked silo) or acquired
 * autonomously by the onboard seeker, which sweeps a forward cone for the nearest
 * valid target with line of sight. With no target it flies ballistically. It
 * detonates on impact or when its fuel runs out.
 */
public class MissileEntity extends Projectile implements GeoEntity {
    private static final int DEFAULT_FUEL_TICKS = 200; // ~10s of flight
    private static final float DEFAULT_EXPLOSION_POWER = 3.0f;
    private static final float DIRECT_HIT_DAMAGE = 6.0f;

    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.missile.fly");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    // --- Guidance tuning ---
    private static final double SEEKER_RANGE = 24.0;          // onboard acquisition range (blocks)
    private static final double SEEKER_CONE_COS = Math.cos(Math.toRadians(60.0)); // half-angle of seeker cone
    private static final double MAX_TURN_RAD = Math.toRadians(9.0); // maneuverability (per tick)
    private static final int ACQUIRE_INTERVAL = 4;            // ticks between acquisition sweeps
    private static final double MIN_CRUISE_SPEED = 0.1;

    /** Plume body colour: white-hot yellow fading to ember red over each particle's life. */
    private static final DustColorTransitionOptions EXHAUST_GRADIENT = new DustColorTransitionOptions(
            new Vector3f(1.0f, 0.9f, 0.55f),
            new Vector3f(0.65f, 0.12f, 0.03f),
            1.5f);

    private int fuelTicks = DEFAULT_FUEL_TICKS;
    private float explosionPower = DEFAULT_EXPLOSION_POWER;

    @Nullable
    private UUID targetUuid;
    private double cruiseSpeed;
    private int acquireCooldown;

    public MissileEntity(EntityType<? extends MissileEntity> type, Level level) {
        super(type, level);
    }

    public MissileEntity(Level level, LivingEntity owner) {
        this(ModEntities.MISSILE.get(), level);
        this.setOwner(owner);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // Guidance runs server-side; no synced data needed yet.
    }

    /** Designate a target the missile will home toward (overrides onboard search). */
    public void setTarget(@Nullable Entity target) {
        this.targetUuid = target == null ? null : target.getUUID();
    }

    public boolean hasTarget() {
        return this.targetUuid != null;
    }

    @Override
    public void tick() {
        super.tick();

        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            this.onHit(hit);
            if (this.isRemoved()) {
                return;
            }
        }

        if (!this.level().isClientSide) {
            guide();
        }

        Vec3 motion = this.getDeltaMovement();
        this.updateRotation(motion);
        spawnTrail(motion);
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);

        if (--this.fuelTicks <= 0) {
            explode();
        }
    }

    /** Server-side homing: keep cruise speed and steer toward the current target. */
    private void guide() {
        Vec3 velocity = this.getDeltaMovement();
        if (this.cruiseSpeed < MIN_CRUISE_SPEED) {
            this.cruiseSpeed = Math.max(velocity.length(), MIN_CRUISE_SPEED);
        }

        Entity target = resolveTarget();
        if (target == null && --this.acquireCooldown <= 0) {
            this.acquireCooldown = ACQUIRE_INTERVAL;
            target = acquireTarget();
            setTarget(target);
        }

        if (target != null) {
            Vec3 aim = aimPoint(target).subtract(this.position());
            this.setDeltaMovement(GuidanceMath.steer(velocity, aim, MAX_TURN_RAD, this.cruiseSpeed));
        }
    }

    /** Resolve the locked target, dropping it if gone/dead/invalid. */
    @Nullable
    private Entity resolveTarget() {
        if (this.targetUuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity target = serverLevel.getEntity(this.targetUuid);
        if (target == null || !target.isAlive() || target == this.getOwner()) {
            this.targetUuid = null;
            return null;
        }
        return target;
    }

    /** Onboard seeker: nearest living target inside the forward cone with line of sight. */
    @Nullable
    private Entity acquireTarget() {
        Vec3 forward = this.getDeltaMovement();
        if (forward.lengthSqr() < MIN_CRUISE_SPEED * MIN_CRUISE_SPEED) {
            return null;
        }
        Vec3 forwardDir = forward.normalize();
        Entity owner = this.getOwner();

        AABB searchBox = this.getBoundingBox().inflate(SEEKER_RANGE);
        List<LivingEntity> candidates = this.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                candidate -> candidate.isAlive() && candidate != owner && !(candidate instanceof Player));

        Entity best = null;
        double bestDistSqr = SEEKER_RANGE * SEEKER_RANGE;
        for (LivingEntity candidate : candidates) {
            Vec3 toTarget = aimPoint(candidate).subtract(this.position());
            double distSqr = toTarget.lengthSqr();
            if (distSqr > bestDistSqr) {
                continue;
            }
            if (forwardDir.dot(toTarget.normalize()) < SEEKER_CONE_COS) {
                continue; // outside the seeker cone
            }
            if (!hasLineOfSight(candidate)) {
                continue;
            }
            best = candidate;
            bestDistSqr = distSqr;
        }
        return best;
    }

    private Vec3 aimPoint(Entity target) {
        return target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
    }

    private boolean hasLineOfSight(Entity target) {
        Vec3 start = this.position();
        Vec3 end = aimPoint(target);
        HitResult result = this.level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return result.getType() == HitResult.Type.MISS
                || result.getLocation().distanceToSqr(end) < 1.0;
    }

    /**
     * Emits a layered rocket exhaust: a blue-white hot core at the nozzle, an
     * orange flame, a white-hot to ember-red gradient plume body, and a lingering
     * smoke contrail. Particles are sampled along the segment covered this tick so
     * the plume stays continuous even at high missile speeds.
     */
    private void spawnTrail(Vec3 motion) {
        Level level = this.level();
        if (!level.isClientSide || motion.lengthSqr() < 1.0e-4) {
            return;
        }
        RandomSource rng = this.random;
        Vec3 forward = motion.normalize();
        double gap = motion.length();                       // ground covered this tick
        Vec3 nozzle = this.position().subtract(forward.scale(0.45));
        Vec3 exitVel = forward.scale(-(0.05 + gap * 0.15)); // slight backward exhaust velocity

        int samples = Math.max(2, (int) Math.ceil(gap / 0.3));
        for (int i = 0; i < samples; i++) {
            double t = (i + rng.nextDouble()) / samples;    // 0 at nozzle .. 1 a gap behind
            Vec3 p = nozzle.subtract(forward.scale(gap * t));

            if (t < 0.35) {                                 // hot core
                emit(level, ParticleTypes.SOUL_FIRE_FLAME, p, exitVel, rng, 0.02);
                emit(level, ParticleTypes.FLAME, p, exitVel.scale(0.8), rng, 0.03);
            }
            if (t > 0.2 && t < 0.7) {                       // luminous gradient body
                emit(level, EXHAUST_GRADIENT, p, exitVel.scale(0.6), rng, 0.06);
            }
            if (t > 0.45) {                                 // cooling smoke contrail
                emit(level, ParticleTypes.LARGE_SMOKE, p, exitVel.scale(0.3), rng, 0.06);
            }
        }
    }

    private static void emit(Level level, ParticleOptions particle, Vec3 pos, Vec3 velocity,
                             RandomSource rng, double spread) {
        level.addParticle(particle,
                pos.x + (rng.nextDouble() - 0.5) * spread,
                pos.y + (rng.nextDouble() - 0.5) * spread,
                pos.z + (rng.nextDouble() - 0.5) * spread,
                velocity.x, velocity.y, velocity.z);
    }

    private void updateRotation(Vec3 motion) {
        double horizontal = motion.horizontalDistance();
        this.setYRot((float) (Mth.atan2(motion.x, motion.z) * (180.0 / Math.PI)));
        this.setXRot((float) (Mth.atan2(motion.y, horizontal) * (180.0 / Math.PI)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide) {
            Entity target = result.getEntity();
            target.hurt(this.damageSources().explosion(this, this.getOwner()), DIRECT_HIT_DAMAGE);
        }
        explode();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (result.getType() == HitResult.Type.BLOCK) {
            explode();
        }
    }

    /** The missile should not lock onto or collide with its own launch platform's owner. */
    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && entity != this.getOwner();
    }

    private void explode() {
        if (this.isRemoved()) {
            return;
        }
        if (!this.level().isClientSide) {
            // Volume 4.0 => ~64-block broadcast range, matching the explosion itself,
            // so the impact is heard as far as the blast (playSound culls by volume*16).
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.MISSILE_IMPACT.get(), SoundSource.BLOCKS, 4.0f, 1.0f);
            this.level().explode(this, this.getX(), this.getY(), this.getZ(),
                    this.explosionPower, Level.ExplosionInteraction.TNT);
        }
        this.discard();
    }

    public void setExplosionPower(float power) {
        this.explosionPower = power;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("FuelTicks", this.fuelTicks);
        tag.putFloat("ExplosionPower", this.explosionPower);
        tag.putDouble("CruiseSpeed", this.cruiseSpeed);
        if (this.targetUuid != null) {
            tag.putUUID("Target", this.targetUuid);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("FuelTicks")) {
            this.fuelTicks = tag.getInt("FuelTicks");
        }
        if (tag.contains("ExplosionPower")) {
            this.explosionPower = tag.getFloat("ExplosionPower");
        }
        if (tag.contains("CruiseSpeed")) {
            this.cruiseSpeed = tag.getDouble("CruiseSpeed");
        }
        this.targetUuid = tag.hasUUID("Target") ? tag.getUUID("Target") : null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "fly", state -> state.setAndContinue(FLY)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
