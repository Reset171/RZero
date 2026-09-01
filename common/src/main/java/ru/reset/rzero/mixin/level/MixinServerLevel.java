package ru.reset.rzero.mixin.level;

import ru.reset.rzero.RZero;
import ru.reset.rzero.runtime.RZeroRuntime;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.reset.rzero.RZero;
import ru.reset.rzero.access.IRZeroEntitySectionManager;
import ru.reset.rzero.access.IRZeroServerLevel;
import ru.reset.rzero.engine.RZeroRandomMask;
import ru.reset.rzero.mixin.random.MixinLevelRandomAccessor;

import java.util.UUID;
import java.util.function.BooleanSupplier;

@Mixin(ServerLevel.class)
public abstract class MixinServerLevel implements IRZeroServerLevel {
    @Shadow @Final private PersistentEntitySectionManager<Entity> entityManager;
    @Shadow @Final private ObjectLinkedOpenHashSet<BlockEventData> blockEvents;

    @Unique
    private static final ThreadLocal<Boolean> rzero$tickChunkMaskActive = ThreadLocal.withInitial(() -> false);

    @Unique
    private static final ThreadLocal<Boolean> rzero$tickBlockMaskActive = ThreadLocal.withInitial(() -> false);

    @Unique
    private static final ThreadLocal<Boolean> rzero$entityTickMaskActive = ThreadLocal.withInitial(() -> false);

    @Unique
    private static final ThreadLocal<Boolean> rzero$customSpawnerMaskActive = ThreadLocal.withInitial(() -> false);

    @Unique
    private static final ThreadLocal<net.minecraft.world.level.levelgen.SingleThreadedRandomSource> rzero$REUSABLE_RANDOM =
            ThreadLocal.withInitial(() -> new net.minecraft.world.level.levelgen.SingleThreadedRandomSource(0L));

    @Unique
    private static final java.util.concurrent.ConcurrentHashMap<net.minecraft.world.entity.EntityType<?>, Long> rzero$TYPE_SALTS =
            new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public ObjectLinkedOpenHashSet<BlockEventData> rzero$getBlockEvents() {
        return this.blockEvents;
    }

    @Override
    public void rzero$deepRemoveEntity(Entity entity) {
        entity.remove(Entity.RemovalReason.CHANGED_DIMENSION);
        ((ServerLevel) (Object) this).getChunkSource().removeEntity(entity);
    }

    @Override
    public boolean rzero$surgicalSpawn(Entity entity) {
        return this.entityManager.addNewEntity(entity);
    }

    @Override
    public void rzero$eradicateGhostEntity(UUID uuid) {
        Entity entity = ((ServerLevel) (Object) this).getEntity(uuid);
        if (entity != null) {
            ((ServerLevel) (Object) this).getChunkSource().removeEntity(entity);
        }
        ((IRZeroEntitySectionManager) (Object) this.entityManager).rzero$eradicate(uuid);
    }

    @Override
    public void rzero$eradicateAllGhostsExcept(java.util.Set<UUID> keep) {
        for (Entity e : ((ServerLevel) (Object) this).getAllEntities()) {
            if (!keep.contains(e.getUUID())) {
                ((ServerLevel) (Object) this).getChunkSource().removeEntity(e);
            }
        }
        ((IRZeroEntitySectionManager) (Object) this.entityManager).rzero$eradicateAllExcept(keep);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        if (RZeroRuntime.isRestoring) {
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void onTickEnd(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
    }

    @Override
    public void rzero$pushDeterministicRandom(RandomSource random) {
        ServerLevel level = (ServerLevel) (Object) this;
        RZeroRandomMask.push(level, level.getRandom(), random);
        ((MixinLevelRandomAccessor)(Object)this).rzero$setRandom(random);
    }

    @Override
    public void rzero$popRandom() {
        ServerLevel level = (ServerLevel) (Object) this;
        RandomSource old = RZeroRandomMask.pop(level);
        if (old != null) {
            ((MixinLevelRandomAccessor)(Object)this).rzero$setRandom(old);
        }
    }

    @Unique
    private void rzero$recoverLeakedMask(ServerLevel level, String scopeName) {
        RandomSource restored = RZeroRandomMask.resetLevel(level);
        if (restored != null) {
            ((MixinLevelRandomAccessor)(Object)this).rzero$setRandom(restored);
        }
        RZero.LOGGER.warn("[RZero] Recovered leaked {} RNG mask before applying a new one.", scopeName);
    }

    @Unique
    private void rzero$pushScope(long seed) {
        net.minecraft.world.level.levelgen.SingleThreadedRandomSource rand = rzero$REUSABLE_RANDOM.get();
        rand.setSeed(seed);
        this.rzero$pushDeterministicRandom(rand);
    }

    @Unique
    private static long rzero$typeSalt(net.minecraft.world.entity.EntityType<?> type) {
        return rzero$TYPE_SALTS.computeIfAbsent(type,
                t -> (long) net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(t).hashCode());
    }

    @Unique
    private static long rzero$mixSeed(ServerLevel level, long salt) {
        long raw = level.getSeed() ^ level.getGameTime() ^ salt;
        return net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(raw);
    }

    @Unique
    private static long rzero$entitySeed(ServerLevel level, Entity entity) {
        long uuidSalt = entity.getUUID().getMostSignificantBits() ^ entity.getUUID().getLeastSignificantBits();
        long idSalt = ((long) entity.getId()) << 32;
        long typeSalt = rzero$typeSalt(entity.getType());
        return rzero$mixSeed(level, uuidSalt ^ idSalt ^ typeSalt ^ 0x13579BDF2468ACE0L);
    }

    @Inject(method = "tickChunk", at = @At("HEAD"))
    private void rzero$pushDetChunk(net.minecraft.world.level.chunk.LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        if (!RZeroRuntime.naturalSpawnPolicy().enabled()) {
            return;
        }
        ServerLevel level = (ServerLevel) (Object) this;
        if (rzero$tickChunkMaskActive.get()) {
            rzero$recoverLeakedMask(level, "tickChunk");
        }

        long seed = rzero$mixSeed(level, chunk.getPos().toLong() ^ 0x12345678L);
        this.rzero$pushScope(seed);
        rzero$tickChunkMaskActive.set(true);
    }

    @Inject(method = "tickChunk", at = @At("RETURN"))
    private void rzero$popDetChunk(net.minecraft.world.level.chunk.LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        this.rzero$popRandom();
        rzero$tickChunkMaskActive.set(false);
    }

    @Inject(method = "tickBlock", at = @At("HEAD"))
    private void rzero$pushDetScheduledBlockTick(net.minecraft.core.BlockPos pos, Block block, CallbackInfo ci) {
        if (!RZeroRuntime.naturalSpawnPolicy().enabled()) {
            return;
        }
        ServerLevel level = (ServerLevel) (Object) this;
        if (rzero$tickBlockMaskActive.get()) {
            rzero$recoverLeakedMask(level, "tickBlock");
        }

        long blockSalt = (long) net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).hashCode() << 32;
        long seed = rzero$mixSeed(level, pos.asLong() ^ blockSalt ^ 0x5F3759DFL);
        this.rzero$pushScope(seed);
        rzero$tickBlockMaskActive.set(true);
    }

    @Inject(method = "tickBlock", at = @At("RETURN"))
    private void rzero$popDetScheduledBlockTick(net.minecraft.core.BlockPos pos, Block block, CallbackInfo ci) {
        this.rzero$popRandom();
        rzero$tickBlockMaskActive.set(false);
    }

    @Inject(method = "tickNonPassenger", at = @At("HEAD"))
    private void rzero$pushDetEntityTick(Entity entity, CallbackInfo ci) {
        if (!RZeroRuntime.mobAiPolicy().brainRng()) {
            return;
        }
        ServerLevel level = (ServerLevel) (Object) this;
        if (rzero$entityTickMaskActive.get()) {
            rzero$recoverLeakedMask(level, "entityTick");
        }

        this.rzero$pushScope(rzero$entitySeed(level, entity));
        rzero$entityTickMaskActive.set(true);
    }

    @Inject(method = "tickNonPassenger", at = @At("RETURN"))
    private void rzero$popDetEntityTick(Entity entity, CallbackInfo ci) {
        this.rzero$popRandom();
        rzero$entityTickMaskActive.set(false);
    }

    @Inject(method = "tickCustomSpawners", at = @At("HEAD"))
    private void rzero$pushDetCustomSpawners(boolean spawnEnemies, boolean spawnFriendlies, CallbackInfo ci) {
        if (!RZeroRuntime.checkpointPolicy().determinism().spawns().wanderingTrader()) {
            return;
        }
        ServerLevel level = (ServerLevel) (Object) this;
        if (rzero$customSpawnerMaskActive.get()) {
            rzero$recoverLeakedMask(level, "customSpawner");
        }

        long flags = (spawnEnemies ? 1L : 0L) | (spawnFriendlies ? 2L : 0L);
        this.rzero$pushScope(rzero$mixSeed(level, flags ^ 0x6A09E667F3BCC909L));
        rzero$customSpawnerMaskActive.set(true);
    }

    @Inject(method = "tickCustomSpawners", at = @At("RETURN"))
    private void rzero$popDetCustomSpawners(boolean spawnEnemies, boolean spawnFriendlies, CallbackInfo ci) {
        this.rzero$popRandom();
        rzero$customSpawnerMaskActive.set(false);
    }
}
