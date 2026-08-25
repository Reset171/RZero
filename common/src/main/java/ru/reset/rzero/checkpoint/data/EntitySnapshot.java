package ru.reset.rzero.checkpoint.data;

import net.minecraft.nbt.CompoundTag;
import ru.reset.rzero.serial.RZBlob;

import org.jetbrains.annotations.Nullable;
import java.util.UUID;

public class EntitySnapshot {
    public int entityId;
    public UUID uuid;
    public RZBlob blob;
    public long chunkKey;
    public double posX, posY, posZ;
    public UUID targetUuid;
    public long[] rngState;
    public boolean firstTick;
    public int tickCount;
    public boolean hasPath;
    public int pathTargetX, pathTargetY, pathTargetZ;
    public double pathSpeed;

    public boolean hasLivingTimers;
    public int hurtTime;
    public int deathTime;
    public float lastHurt;
    public int attackStrengthTicker;
    public int noActionTime;
    public int noJumpDelay;
    public int swingTime;
    public boolean swinging;
    public int useItemRemaining;
    public int lastHurtByMobTimestamp;
    public int portalCooldown;
    public int forcedAgeTimer;


    public CompoundTag decodeNbt() {
        return blob != null ? blob.toCompound() : new CompoundTag();
    }

    public void setNbt(CompoundTag tag) {
        this.blob = RZBlob.of(tag);
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("eId", entityId);
        tag.putUUID("uuid", uuid);
        tag.put("nbt", decodeNbt());
        tag.putLong("ck", chunkKey);
        tag.putDouble("px", posX);
        tag.putDouble("py", posY);
        tag.putDouble("pz", posZ);
        if (targetUuid != null) {
            tag.putUUID("targetUuid", targetUuid);
        }
        if (rngState != null) {
            tag.putLongArray("rngState", rngState);
        }
        tag.putInt("tickCount", tickCount);
        if (hasPath) {
            tag.putBoolean("hasPath", true);
            tag.putInt("pTX", pathTargetX);
            tag.putInt("pTY", pathTargetY);
            tag.putInt("pTZ", pathTargetZ);
            tag.putDouble("pSpeed", pathSpeed);
        }
        if (hasLivingTimers) {
            tag.putBoolean("livT", true);
            tag.putInt("ht", hurtTime);
            tag.putInt("dt", deathTime);
            tag.putFloat("lh", lastHurt);
            tag.putInt("at", attackStrengthTicker);
            tag.putInt("nat", noActionTime);
            tag.putInt("njd", noJumpDelay);
            tag.putInt("st", swingTime);
            tag.putBoolean("sw", swinging);
            tag.putInt("uir", useItemRemaining);
            tag.putInt("lhmt", lastHurtByMobTimestamp);
            tag.putInt("pc", portalCooldown);
            tag.putInt("fat", forcedAgeTimer);
        }
        return tag;
    }

    public static EntitySnapshot fromNBT(CompoundTag tag) {
        EntitySnapshot es = new EntitySnapshot();
        es.entityId = tag.getInt("eId");
        es.uuid = tag.getUUID("uuid");
        es.blob = RZBlob.of(tag.getCompound("nbt"));
        if (tag.contains("ck")) {
            es.chunkKey = tag.getLong("ck");
            es.posX = tag.getDouble("px");
            es.posY = tag.getDouble("py");
            es.posZ = tag.getDouble("pz");
        } else {
            CompoundTag nbt = tag.getCompound("nbt");
            net.minecraft.nbt.ListTag pos = nbt.getList("Pos", 6);
            if (pos.size() == 3) {
                es.posX = pos.getDouble(0);
                es.posY = pos.getDouble(1);
                es.posZ = pos.getDouble(2);
                es.chunkKey = net.minecraft.world.level.ChunkPos.asLong(
                        net.minecraft.util.Mth.floor(es.posX) >> 4,
                        net.minecraft.util.Mth.floor(es.posZ) >> 4);
            }
        }
        if (tag.hasUUID("targetUuid")) {
            es.targetUuid = tag.getUUID("targetUuid");
        }
        if (tag.contains("rngState")) {
            es.rngState = tag.getLongArray("rngState");
        }
        es.tickCount = tag.getInt("tickCount");
        if (tag.getBoolean("hasPath")) {
            es.hasPath = true;
            es.pathTargetX = tag.getInt("pTX");
            es.pathTargetY = tag.getInt("pTY");
            es.pathTargetZ = tag.getInt("pTZ");
            es.pathSpeed = tag.getDouble("pSpeed");
        }
        if (tag.getBoolean("livT")) {
            es.hasLivingTimers = true;
            es.hurtTime = tag.getInt("ht");
            es.deathTime = tag.getInt("dt");
            es.lastHurt = tag.getFloat("lh");
            es.attackStrengthTicker = tag.getInt("at");
            es.noActionTime = tag.getInt("nat");
            es.noJumpDelay = tag.getInt("njd");
            es.swingTime = tag.getInt("st");
            es.swinging = tag.getBoolean("sw");
            es.useItemRemaining = tag.getInt("uir");
            es.lastHurtByMobTimestamp = tag.getInt("lhmt");
            es.portalCooldown = tag.getInt("pc");
            es.forcedAgeTimer = tag.getInt("fat");
        }
        return es;
    }

    public void captureLivingTimers(net.minecraft.world.entity.Entity entity) {
        try {
            ru.reset.rzero.mixin.entity.MixinEntityAccessor eacc =
                    (ru.reset.rzero.mixin.entity.MixinEntityAccessor)(Object) entity;
            this.portalCooldown = eacc.rzero$getPortalCooldown();
        } catch (Throwable ignored) {}
        if (entity instanceof net.minecraft.world.entity.LivingEntity) {
            try {
                ru.reset.rzero.mixin.entity.MixinLivingEntityAccessor lacc =
                        (ru.reset.rzero.mixin.entity.MixinLivingEntityAccessor)(Object) entity;
                this.hurtTime = lacc.rzero$getHurtTime();
                this.deathTime = lacc.rzero$getDeathTime();
                this.lastHurt = lacc.rzero$getLastHurt();
                this.attackStrengthTicker = lacc.rzero$getAttackStrengthTicker();
                this.noActionTime = lacc.rzero$getNoActionTime();
                this.noJumpDelay = lacc.rzero$getNoJumpDelay();
                this.swingTime = lacc.rzero$getSwingTime();
                this.swinging = lacc.rzero$getSwinging();
                this.useItemRemaining = lacc.rzero$getUseItemRemaining();
                this.lastHurtByMobTimestamp = lacc.rzero$getLastHurtByMobTimestamp();
                this.hasLivingTimers = true;
            } catch (Throwable ignored) {}
        }
        if (entity instanceof net.minecraft.world.entity.AgeableMob) {
            try {
                this.forcedAgeTimer =
                        ((ru.reset.rzero.mixin.entity.MixinAgeableMob)(Object) entity).rzero$getForcedAgeTimer();
            } catch (Throwable ignored) {}
        }
    }

    public void applyLivingTimers(net.minecraft.world.entity.Entity entity) {
        try {
            ru.reset.rzero.mixin.entity.MixinEntityAccessor eacc = (ru.reset.rzero.mixin.entity.MixinEntityAccessor)(Object) entity;
            eacc.rzero$setPortalCooldown(portalCooldown);
        } catch (Throwable ignored) {}
        if (!hasLivingTimers) return;
        if (entity instanceof net.minecraft.world.entity.LivingEntity) {
            try {
                ru.reset.rzero.mixin.entity.MixinLivingEntityAccessor lacc =
                        (ru.reset.rzero.mixin.entity.MixinLivingEntityAccessor)(Object) entity;
                lacc.rzero$setHurtTime(hurtTime);
                lacc.rzero$setDeathTime(deathTime);
                lacc.rzero$setDead(((net.minecraft.world.entity.LivingEntity)entity).getHealth() <= 0.0f);
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
        if (entity instanceof net.minecraft.world.entity.AgeableMob) {
            try {
                ((ru.reset.rzero.mixin.entity.MixinAgeableMob)(Object) entity).rzero$setForcedAgeTimer(forcedAgeTimer);
            } catch (Throwable ignored) {}
        }
    }
}
