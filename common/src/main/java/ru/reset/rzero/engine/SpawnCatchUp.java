package ru.reset.rzero.engine;

import ru.reset.rzero.RZero;
import ru.reset.rzero.runtime.RZeroRuntime;

import ru.reset.rzero.access.IRZeroServerLevel;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import ru.reset.rzero.checkpoint.data.PendingSpawnLedger;

public final class SpawnCatchUp {

    private SpawnCatchUp() {}

    public static void onChunkStartTicking(ServerLevel level, LevelChunk chunk) {
        var policy = RZeroRuntime.checkpointPolicy().determinism().naturalSpawn();
        if (RZeroRuntime.isRestoring || !policy.enabled() || !policy.useSpawnEngine()) {
            return;
        }
        if (!policy.catchUp() && !policy.monsterCatchUp()) {
            return;
        }

        long chunkKey = chunk.getPos().toLong();
        long nowEpoch = SpawnEngine.epochOf(level.getGameTime());

        PendingSpawnLedger ledger;
        try {
            ledger = PendingSpawnLedger.get(level);
        } catch (Throwable t) {
            RZero.LOGGER.warn("[RZero] Spawn ledger unavailable for {}: {}",
                    level.dimension().location(), t.getMessage());
            return;
        }

        if (policy.catchUp()) {
            long from = ledger.catchUpFrom(chunkKey, nowEpoch);
            for (long epoch = from; epoch <= nowEpoch; epoch++) {
                replayEpoch(level, chunk, chunkKey, epoch, SpawnEngine.CREATURE_EPOCH_TICKS,
                        MobCategory.CREATURE, SpawnEngine.SALT_CATCHUP);
            }
        }

        if (policy.monsterCatchUp()) {
            long day = SpawnEngine.monsterEpochOf(level.getGameTime());
            if (ledger.monsterOwesReplay(chunkKey, day)) {
                replayEpoch(level, chunk, chunkKey, day, SpawnEngine.MONSTER_EPOCH_TICKS,
                        MobCategory.MONSTER, SpawnEngine.SALT_CATCHUP_MONSTER);
            }
        }
    }

    private static void replayEpoch(ServerLevel level, LevelChunk chunk, long chunkKey, long epoch,
                                   long epochTicks, MobCategory category, long salt) {
        long seed = SpawnEngine.derive(level, chunkKey, epoch, salt);
        SpawnEngine.Context ctx = new SpawnEngine.Context(
                epoch, epoch * epochTicks, chunkKey, seed);

        IRZeroServerLevel det = (IRZeroServerLevel) level;
        SpawnEngine.open(ctx);
        det.rzero$pushDeterministicRandom(new LegacyRandomSource(seed));
        try {
            if (SpawnEngine.canSpawnLocally(level, chunk.getPos(), category)) {
                NaturalSpawner.spawnCategoryForChunk(
                        category, level, chunk,
                        (type, pos, chunkAccess) -> true,
                        (mob, chunkAccess) -> SpawnEngine.afterSpawn(mob));
            }
        } catch (Throwable t) {
            RZero.LOGGER.warn("[RZero] Catch-up spawn failed for chunk {} epoch {} ({}): {}",
                    chunkKey, epoch, category, t.getMessage());
        } finally {
            det.rzero$popRandom();
            SpawnEngine.close();
        }
    }
}
