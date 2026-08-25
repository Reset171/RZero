package ru.reset.rzero.checkpoint.data;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.pathfinder.Path;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class MobRamLive {
    public final Map<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> memories = new LinkedHashMap<>();

    public boolean hasLookControl;
    public double lookX, lookY, lookZ;
    public float lookYMaxRotSpeed, lookXMaxRotAngle;
    public int lookAtCooldown;

    public boolean hasMoveControl;
    public double moveX, moveY, moveZ, moveSpeedModifier;
    public float moveStrafeForwards, moveStrafeRight;
    public MoveControl.Operation moveOperation;

    public UUID targetUuid;
    public boolean hasPath;
    public int pathTargetX, pathTargetY, pathTargetZ;
    public double pathSpeed;
    public long[] rngState;
    public int tickCount;

    public boolean hasLivingTimers;
    public int hurtTime, deathTime, attackStrengthTicker, noActionTime, noJumpDelay, swingTime, useItemRemaining, lastHurtByMobTimestamp, portalCooldown;
    public float lastHurt;
    public boolean swinging;

    public long capturedAtTick;

    public static MobRamLive captureFrom(Mob mob) {
        MobRamLive live = new MobRamLive();
        if (mob.getBrain() != null && mob.getBrain().memories != null) {
            live.memories.putAll(mob.getBrain().memories);
        }
        if (mob.getLookControl() != null) {
            live.hasLookControl = true;
            live.lookX = mob.getLookControl().wantedX;
            live.lookY = mob.getLookControl().wantedY;
            live.lookZ = mob.getLookControl().wantedZ;
            live.lookYMaxRotSpeed = mob.getLookControl().yMaxRotSpeed;
            live.lookXMaxRotAngle = mob.getLookControl().xMaxRotAngle;
            live.lookAtCooldown = mob.getLookControl().lookAtCooldown;
        }
        if (mob.getMoveControl() != null) {
            live.hasMoveControl = true;
            live.moveX = mob.getMoveControl().wantedX;
            live.moveY = mob.getMoveControl().wantedY;
            live.moveZ = mob.getMoveControl().wantedZ;
            live.moveSpeedModifier = mob.getMoveControl().speedModifier;
            live.moveStrafeForwards = mob.getMoveControl().strafeForwards;
            live.moveStrafeRight = mob.getMoveControl().strafeRight;
            live.moveOperation = mob.getMoveControl().operation;
        }
        if (mob.getTarget() != null && !mob.getTarget().isRemoved()) {
            live.targetUuid = mob.getTarget().getUUID();
        }
        Path path = mob.getNavigation().getPath();
        if (path != null && path.getTarget() != null) {
            live.hasPath = true;
            live.pathTargetX = path.getTarget().getX();
            live.pathTargetY = path.getTarget().getY();
            live.pathTargetZ = path.getTarget().getZ();
            try {
                live.pathSpeed = mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
            } catch (Throwable ignored) {
                live.pathSpeed = 1.0;
            }
        }
        try {
            if (mob.getRandom() instanceof ru.reset.rzero.access.IRZeroRandomState rState) {
                live.rngState = rState.rzero$getState();
            }
        } catch (Throwable ignored) {}
        live.tickCount = mob.tickCount;

        try {
            ru.reset.rzero.mixin.entity.MixinEntityAccessor eacc =
                    (ru.reset.rzero.mixin.entity.MixinEntityAccessor)(Object) mob;
            live.portalCooldown = eacc.rzero$getPortalCooldown();
        } catch (Throwable ignored) {}
        try {
            ru.reset.rzero.mixin.entity.MixinLivingEntityAccessor lacc =
                    (ru.reset.rzero.mixin.entity.MixinLivingEntityAccessor)(Object) mob;
            live.hurtTime = lacc.rzero$getHurtTime();
            live.deathTime = lacc.rzero$getDeathTime();
            live.lastHurt = lacc.rzero$getLastHurt();
            live.attackStrengthTicker = lacc.rzero$getAttackStrengthTicker();
            live.noActionTime = lacc.rzero$getNoActionTime();
            live.noJumpDelay = lacc.rzero$getNoJumpDelay();
            live.swingTime = lacc.rzero$getSwingTime();
            live.swinging = lacc.rzero$getSwinging();
            live.useItemRemaining = lacc.rzero$getUseItemRemaining();
            live.lastHurtByMobTimestamp = lacc.rzero$getLastHurtByMobTimestamp();
            live.hasLivingTimers = true;
        } catch (Throwable ignored) {}

        return live;
    }

    public static MobRamLive fromSnapshots(EntityRAMSnapshot ram, EntitySnapshot entity) {
        MobRamLive live = new MobRamLive();
        if (ram != null) {
            if (ram.hasLookControl) {
                live.hasLookControl = true;
                live.lookX = ram.lookX;
                live.lookY = ram.lookY;
                live.lookZ = ram.lookZ;
                live.lookYMaxRotSpeed = ram.lookYMaxRotSpeed;
                live.lookXMaxRotAngle = ram.lookXMaxRotAngle;
                live.lookAtCooldown = ram.lookAtCooldown;
            }
            if (ram.hasMoveControl) {
                live.hasMoveControl = true;
                live.moveX = ram.moveX;
                live.moveY = ram.moveY;
                live.moveZ = ram.moveZ;
                live.moveSpeedModifier = ram.moveSpeedModifier;
                live.moveStrafeForwards = ram.moveStrafeForwards;
                live.moveStrafeRight = ram.moveStrafeRight;
                live.moveOperation = ram.moveOperation;
            }
        }
        if (entity != null) {
            live.targetUuid = entity.targetUuid;
            live.hasPath = entity.hasPath;
            live.pathTargetX = entity.pathTargetX;
            live.pathTargetY = entity.pathTargetY;
            live.pathTargetZ = entity.pathTargetZ;
            live.pathSpeed = entity.pathSpeed;
            live.rngState = entity.rngState;
            live.tickCount = entity.tickCount;
            if (entity.hasLivingTimers) {
                live.hasLivingTimers = true;
                live.hurtTime = entity.hurtTime;
                live.deathTime = entity.deathTime;
                live.lastHurt = entity.lastHurt;
                live.attackStrengthTicker = entity.attackStrengthTicker;
                live.noActionTime = entity.noActionTime;
                live.noJumpDelay = entity.noJumpDelay;
                live.swingTime = entity.swingTime;
                live.swinging = entity.swinging;
                live.useItemRemaining = entity.useItemRemaining;
                live.lastHurtByMobTimestamp = entity.lastHurtByMobTimestamp;
                live.portalCooldown = entity.portalCooldown;
            }
        }
        return live;
    }

    public void applyTo(Mob mob, ServerLevel level) {
        if (mob.getBrain() != null && mob.getBrain().memories != null && !memories.isEmpty()) {
            for (Map.Entry<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> e : memories.entrySet()) {
                if (!mob.getBrain().memories.containsKey(e.getKey())) continue;
                Optional<? extends ExpirableValue<?>> v = e.getValue();
                if (v == null) continue;
                if (v.isPresent()) {
                    Object inner = v.get().getValue();
                    if (inner instanceof LivingEntity le && le.isRemoved()) {
                        @SuppressWarnings({"unchecked", "rawtypes"})
                        Map raw = mob.getBrain().memories;
                        raw.put(e.getKey(), Optional.empty());
                        continue;
                    }
                }
                @SuppressWarnings({"unchecked", "rawtypes"})
                Map raw = mob.getBrain().memories;
                raw.put(e.getKey(), v);
            }
        }
        if (hasLookControl && mob.getLookControl() != null) {
            mob.getLookControl().wantedX = lookX;
            mob.getLookControl().wantedY = lookY;
            mob.getLookControl().wantedZ = lookZ;
            mob.getLookControl().yMaxRotSpeed = lookYMaxRotSpeed;
            mob.getLookControl().xMaxRotAngle = lookXMaxRotAngle;
            mob.getLookControl().lookAtCooldown = lookAtCooldown;
        }
        if (hasMoveControl && mob.getMoveControl() != null) {
            mob.getMoveControl().wantedX = moveX;
            mob.getMoveControl().wantedY = moveY;
            mob.getMoveControl().wantedZ = moveZ;
            mob.getMoveControl().speedModifier = moveSpeedModifier;
            mob.getMoveControl().strafeForwards = moveStrafeForwards;
            mob.getMoveControl().strafeRight = moveStrafeRight;
            if (moveOperation != null) {
                mob.getMoveControl().operation = moveOperation;
            }
        }
        if (targetUuid != null) {
            Entity t = level.getEntity(targetUuid);
            if (t == null && level.getServer() != null) {
                t = level.getServer().getPlayerList().getPlayer(targetUuid);
            }
            if (t instanceof LivingEntity le && !le.isRemoved()) {
                mob.setTarget(le);
            }
        }
        if (hasPath) {
            mob.getNavigation().moveTo(pathTargetX, pathTargetY, pathTargetZ, pathSpeed > 0 ? pathSpeed : 1.0);
        }
        if (rngState != null) {
            try {
                if (mob.getRandom() instanceof ru.reset.rzero.access.IRZeroRandomState rState) {
                    rState.rzero$setState(rngState);
                }
            } catch (Throwable ignored) {}
        }
        mob.tickCount = tickCount;

        try {
            ((ru.reset.rzero.mixin.entity.MixinEntityAccessor)(Object) mob).rzero$setPortalCooldown(portalCooldown);
        } catch (Throwable ignored) {}
        if (hasLivingTimers) {
            try {
                ru.reset.rzero.mixin.entity.MixinLivingEntityAccessor lacc =
                        (ru.reset.rzero.mixin.entity.MixinLivingEntityAccessor)(Object) mob;
                lacc.rzero$setHurtTime(hurtTime);
                lacc.rzero$setDeathTime(deathTime);
                lacc.rzero$setDead(mob.getHealth() <= 0.0f);
                lacc.rzero$setLastHurt(lastHurt);
                lacc.rzero$setAttackStrengthTicker(attackStrengthTicker);
                lacc.rzero$setNoActionTime(noActionTime);
                lacc.rzero$setNoJumpDelay(noJumpDelay);
                lacc.rzero$setSwingTime(swingTime);
                lacc.rzero$setSwinging(swinging);
                lacc.rzero$setUseItemRemaining(useItemRemaining);
                lacc.rzero$setLastHurtByMobTimestamp(lastHurtByMobTimestamp);
            } catch (Throwable ignored) {}
        }
    }

}
