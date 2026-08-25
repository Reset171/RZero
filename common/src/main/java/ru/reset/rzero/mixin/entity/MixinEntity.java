package ru.reset.rzero.mixin.entity;

import ru.reset.rzero.runtime.MobRamCache;
import ru.reset.rzero.runtime.SnapshotRegistry;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.reset.rzero.access.IRZeroSpawnStamp;
import ru.reset.rzero.engine.SpawnEngine;
import ru.reset.rzero.runtime.RZeroRuntime;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(Entity.class)
public abstract class MixinEntity implements ru.reset.rzero.access.IRZeroEntityRandom, IRZeroSpawnStamp {
    @Shadow @Final private static AtomicInteger ENTITY_COUNTER;

    @Mutable
    @Shadow @Final protected RandomSource random;

    @Override
    public void rzero$setRandom(RandomSource value) {
        this.random = value;
    }


    @Unique
    private static int rzero$getEntityCounter() {
        return ENTITY_COUNTER.get();
    }

    @Unique
    private static void rzero$setEntityCounter(int val) {
        ENTITY_COUNTER.set(val);
    }


    @Unique
    private boolean rzero$spawnStamped;
    @Unique
    private long rzero$spawnTick;
    @Unique
    private long rzero$spawnEpoch;
    @Unique
    private long rzero$spawnSeed;
    @Unique
    private long rzero$spawnChunk;

    @Override
    public void rzero$stampSpawn(long gameTime, long epoch, long seed, long chunkKey) {
        this.rzero$spawnStamped = true;
        this.rzero$spawnTick = gameTime;
        this.rzero$spawnEpoch = epoch;
        this.rzero$spawnSeed = seed;
        this.rzero$spawnChunk = chunkKey;
    }

    @Override
    public boolean rzero$hasSpawnStamp() {
        return this.rzero$spawnStamped;
    }

    @Override
    public long rzero$getSpawnTick() {
        return this.rzero$spawnTick;
    }

    @Override
    public long rzero$getSpawnEpoch() {
        return this.rzero$spawnEpoch;
    }

    @Override
    public long rzero$getSpawnSeed() {
        return this.rzero$spawnSeed;
    }

    @Override
    public long rzero$getSpawnChunk() {
        return this.rzero$spawnChunk;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void rzero$stampNaturalSpawn(EntityType<?> type, Level level, CallbackInfo ci) {
        if (!((Object) this instanceof Mob)) return;
        if (!RZeroRuntime.checkpointPolicy().determinism().naturalSpawn().stampSpawnOrigin()) return;
        SpawnEngine.Context ctx = SpawnEngine.current();
        if (ctx == null) return;
        this.rzero$stampSpawn(ctx.gameTime(), ctx.epoch(), ctx.seed(), ctx.chunkKey());
    }

    @Inject(method = "getEncodeId", at = @At("HEAD"), cancellable = true)
    private void rzero$getEncodeIdFishingHook(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<String> cir) {
        if ((Object) this instanceof net.minecraft.world.entity.projectile.FishingHook hook) {
            net.minecraft.resources.ResourceLocation loc = net.minecraft.world.entity.EntityType.getKey(hook.getType());
            if (loc != null) {
                cir.setReturnValue(loc.toString());
            }
        }
    }


    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void rzero$captureMobRamOnUnload(Entity.RemovalReason reason, CallbackInfo ci) {
        if (!RZeroRuntime.checkpointPolicy().rollback().entities().mobRamCache()) {
            return;
        }
        if (reason != Entity.RemovalReason.UNLOADED_TO_CHUNK
                && reason != Entity.RemovalReason.UNLOADED_WITH_PLAYER) {
            return;
        }
        Entity self = (Entity)(Object) this;
        if (!(self instanceof Mob mob)) return;
        if (!(mob.level() instanceof ServerLevel sl)) return;
        if (!SnapshotRegistry.allowedSnapshotEntities.contains(mob.getUUID())) return;
        try {
            ru.reset.rzero.checkpoint.data.MobRamLive live = ru.reset.rzero.checkpoint.data.MobRamLive.captureFrom(mob);
            live.capturedAtTick = sl.getServer() != null ? sl.getServer().getTickCount() : 0L;
            ConcurrentHashMap<java.util.UUID, ru.reset.rzero.checkpoint.data.MobRamLive> dimCache =
                    MobRamCache.mobRamCache.computeIfAbsent(
                            sl.dimension(), k -> new ConcurrentHashMap<>());
            dimCache.put(mob.getUUID(), live);
        } catch (Throwable ignored) {
        }
    }


    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void rzero$saveRngState(net.minecraft.nbt.CompoundTag tag, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.nbt.CompoundTag> cir) {
        if (this.random instanceof ru.reset.rzero.access.IRZeroRandomState rState) {
            long[] state = rState.rzero$getState();
            if (state != null) {
                tag.putLongArray("RZeroRngState", state);
            }
        }
        if (this.rzero$spawnStamped) {
            net.minecraft.nbt.CompoundTag stamp = new net.minecraft.nbt.CompoundTag();
            stamp.putLong("tick", this.rzero$spawnTick);
            stamp.putLong("epoch", this.rzero$spawnEpoch);
            stamp.putLong("seed", this.rzero$spawnSeed);
            stamp.putLong("chunk", this.rzero$spawnChunk);
            tag.put("RZeroSpawnStamp", stamp);
        }
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void rzero$loadRngState(net.minecraft.nbt.CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("RZeroRngState", 12)) {
            if (this.random instanceof ru.reset.rzero.access.IRZeroRandomState rState) {
                rState.rzero$setState(tag.getLongArray("RZeroRngState"));
            }
        }
        if (tag.contains("RZeroSpawnStamp", 10)) {
            net.minecraft.nbt.CompoundTag stamp = tag.getCompound("RZeroSpawnStamp");
            this.rzero$stampSpawn(
                    stamp.getLong("tick"),
                    stamp.getLong("epoch"),
                    stamp.getLong("seed"),
                    stamp.getLong("chunk"));
        }
    }

    @org.spongepowered.asm.mixin.injection.Redirect(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;create()Lnet/minecraft/util/RandomSource;")
    )
    private RandomSource rzero$redirectEntityRandom() {
        RandomSource det = ru.reset.rzero.engine.RZeroRandomMask.peek();
        if (det != null) {
            return new net.minecraft.world.level.levelgen.LegacyRandomSource(det.nextLong());
        }
        return RandomSource.create();
    }
}
