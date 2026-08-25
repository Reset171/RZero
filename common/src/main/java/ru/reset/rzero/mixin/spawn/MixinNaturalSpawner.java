package ru.reset.rzero.mixin.spawn;

import ru.reset.rzero.RZero;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.reset.rzero.access.IRZeroServerLevel;
import ru.reset.rzero.config.RZeroCheckpointPolicy;
import ru.reset.rzero.engine.RZeroRandomMask;
import ru.reset.rzero.engine.SpawnEngine;
import ru.reset.rzero.runtime.RZeroRuntime;

@Mixin(NaturalSpawner.class)
public class MixinNaturalSpawner {
    @Unique
    private static final ThreadLocal<Boolean> rzero$spawnMaskActive = ThreadLocal.withInitial(() -> false);

    @Inject(
        method = "spawnForChunk(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/NaturalSpawner$SpawnState;ZZZ)V",
        at = @At("HEAD")
    )
    private static void rzero$pushDet(
            ServerLevel level, LevelChunk chunk, NaturalSpawner.SpawnState state,
            boolean spawnFriendlies, boolean spawnEnemies, boolean spawnPassives,
            CallbackInfo ci) {
        RZeroCheckpointPolicy.NaturalSpawn policy = RZeroRuntime.checkpointPolicy().determinism().naturalSpawn();
        if (!policy.enabled() || !policy.useSpawnEngine()) {
            rzero$spawnMaskActive.set(false);
            return;
        }

        if (rzero$spawnMaskActive.get()) {
            RandomSource restored = RZeroRandomMask.resetLevel(level);
            if (restored != null) {
                level.random = restored;
            }
            SpawnEngine.close();
            RZero.LOGGER.warn("[RZero] Recovered leaked NaturalSpawner RNG mask before applying a new one.");
        }

        long gameTime = level.getGameTime();
        long chunkKey = chunk.getPos().toLong();

        long seed = SpawnEngine.derive(level, chunkKey, gameTime, SpawnEngine.SALT_NATURAL_SPAWN);

        SpawnEngine.open(new SpawnEngine.Context(SpawnEngine.epochOf(gameTime), gameTime, chunkKey, seed));
        ((IRZeroServerLevel) level).rzero$pushDeterministicRandom(new LegacyRandomSource(seed));
        rzero$spawnMaskActive.set(true);
    }

    @Inject(
        method = "spawnForChunk(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/NaturalSpawner$SpawnState;ZZZ)V",
        at = @At("RETURN")
    )
    private static void rzero$popDet(
            ServerLevel level, LevelChunk chunk, NaturalSpawner.SpawnState state,
            boolean spawnFriendlies, boolean spawnEnemies, boolean spawnPassives,
            CallbackInfo ci) {
        if (!rzero$spawnMaskActive.get()) {
            return;
        }
        ((IRZeroServerLevel) level).rzero$popRandom();
        SpawnEngine.close();
        rzero$spawnMaskActive.set(false);
    }

    @Redirect(
        method = "spawnForChunk(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/NaturalSpawner$SpawnState;ZZZ)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/NaturalSpawner$SpawnState;canSpawnForCategory(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/world/level/ChunkPos;)Z")
    )
    private static boolean rzero$localCap(
            NaturalSpawner.SpawnState instance, MobCategory category, ChunkPos pos,
            ServerLevel level, LevelChunk chunk, NaturalSpawner.SpawnState state,
            boolean spawnFriendlies, boolean spawnEnemies, boolean spawnPassives) {
        RZeroCheckpointPolicy.NaturalSpawn policy = RZeroRuntime.checkpointPolicy().determinism().naturalSpawn();
        if (!policy.enabled() || !policy.localCap()) {
            return ((MixinSpawnStateAccessor) instance).rzero$invokeCanSpawnForCategory(category, pos);
        }
        return SpawnEngine.canSpawnLocally(level, pos, category);
    }
}
