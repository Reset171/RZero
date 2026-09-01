package ru.reset.rzero.checkpoint.restore;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardSaveData;
import ru.reset.rzero.ModGameRules;
import ru.reset.rzero.RZero;
import ru.reset.rzero.access.IRZeroRandomState;
import ru.reset.rzero.access.IRZeroServerLevel;
import ru.reset.rzero.api.DevHooks;
import ru.reset.rzero.checkpoint.data.CheckpointData;
import ru.reset.rzero.checkpoint.data.EntitySnapshot;
import ru.reset.rzero.checkpoint.data.PendingSpawnLedger;
import ru.reset.rzero.config.RZeroCheckpointPolicy;
import ru.reset.rzero.adaptive.AdaptiveSaveEngine;
import ru.reset.rzero.engine.EntityIdCounter;
import ru.reset.rzero.network.RollbackChatPacket;
import ru.reset.rzero.platform.Services;
import ru.reset.rzero.runtime.MobRamCache;
import ru.reset.rzero.runtime.RZeroRuntime;
import ru.reset.rzero.runtime.RestoreQueues;
import ru.reset.rzero.runtime.SnapshotRegistry;
import ru.reset.rzero.util.DetOrder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RollbackCoordinator {

    private static final long ENTITY_ROLLBACK_WINDOW = 20L;

    private RollbackCoordinator() {
    }

    public static void restoreCheckpoint(ServerPlayer player) {
        restore(player.server, player);
    }

    public static boolean restoreCheckpointHeadless(MinecraftServer server) {
        return restore(server, null);
    }

    private static boolean restore(MinecraftServer server, ServerPlayer player) {
        RZero.logInfo("[RZero] Initiating timeline rollback{}...",
                player == null ? " (headless)" : "");
        ru.reset.rzero.util.RZBenchmark.begin("ROLLBACK RESTORE", server.getTickCount());

        CheckpointData targetPlayerData = null;
        ServerLevel targetLevel = null;
        for (var entry : DetOrder.sortedEntries(
                SnapshotRegistry.activeSnapshots, k -> k.location().toString())) {
            if (entry.getValue().adaptiveState != null) {
                targetPlayerData = entry.getValue();
                targetLevel = server.getLevel(entry.getKey());
                break;
            }
        }
        if (targetPlayerData == null || targetLevel == null) {
            RZero.LOGGER.warn("[RZero] Rollback aborted: No timeline origin found in any dimension.");
            ru.reset.rzero.util.RZBenchmark.endAndLog();
            return false;
        }

        RZeroCheckpointPolicy targetPolicy = RZeroRuntime.effectivePolicy(targetPlayerData);
        RZeroRuntime.setActiveCheckpointPolicy(targetPolicy);

        RZeroRuntime.isRestoring = true;
        RZeroRuntime.wasRestoredThisTick = true;

        AdaptiveSaveEngine.restoreAdaptiveState(targetPlayerData.adaptiveState);
        if (targetPolicy.rollback().entities().entityId()) {
            EntityIdCounter.set(targetPlayerData.adaptiveState.entityIdCounter);
            bumpEntityIdCounterAboveSnapshots();
        }

        if (player != null) {
            notifyPlayers(server, targetPlayerData);
        }
        DevHooks.fireProfileStart(server, "restore");

        try {
            long t0 = System.nanoTime();
            prepareSnapshots(server);
            ru.reset.rzero.util.RZBenchmark.accum(ru.reset.rzero.util.RZBenchmark.Phase.PREPARE, t0);
            if (player != null) {
                PlayerRestorer.restoreAll(server, targetPlayerData, targetLevel);
            }
            long t1 = System.nanoTime();
            restoreGlobals(server, targetPlayerData, targetPolicy);
            ru.reset.rzero.util.RZBenchmark.accum(ru.reset.rzero.util.RZBenchmark.Phase.SERVER_GLOBALS, t1);
            long t2 = System.nanoTime();
            restoreScoreboard(server, targetPlayerData, targetPolicy);
            ru.reset.rzero.util.RZBenchmark.accum(ru.reset.rzero.util.RZBenchmark.Phase.SCOREBOARD, t2);

            int chunksProcessed = 0;
            int chunksQueued = 0;
            for (ServerLevel level : server.getAllLevels()) {
                CheckpointData data = SnapshotRegistry.activeSnapshots.get(level.dimension());
                if (data == null) {
                    continue;
                }
                long t3 = System.nanoTime();
                WorldStateRestorer.restore(level, data);
                ru.reset.rzero.util.RZBenchmark.accum(ru.reset.rzero.util.RZBenchmark.Phase.WORLD_STATE, t3);
                int[] counts = restoreChunks(level, data);
                chunksProcessed += counts[0];
                chunksQueued += counts[1];
                data.setDirty();
                long t4 = System.nanoTime();
                dropStaleBlockEvents(level, data);
                ru.reset.rzero.util.RZBenchmark.accum(ru.reset.rzero.util.RZBenchmark.Phase.DROP_STALE_EVENTS, t4);
                long t5 = System.nanoTime();
                removeSnapshotEntities(level, data);
                ru.reset.rzero.util.RZBenchmark.accum(ru.reset.rzero.util.RZBenchmark.Phase.REMOVE_SNAPSHOT_ENTITIES, t5);
            }

            ru.reset.rzero.event.ServerTickEvents.drainEntityRestoreQueue();

            if (player != null) {
                long tOffline = System.nanoTime();
                PlayerRestorer.restoreOfflineFiles(server, targetPlayerData);
                ru.reset.rzero.util.RZBenchmark.accum(ru.reset.rzero.util.RZBenchmark.Phase.OFFLINE_FILES, tOffline);
            }

            AdaptiveSaveEngine.resetAutoSaveTimer(server);
            DevHooks.fireProfileEnd(server, "restore", chunksProcessed, chunksQueued);
            DevHooks.fireCheckpointLoaded(server);
            return true;
        } finally {
            RZeroRuntime.isRestoring = false;
        }
    }

    private static void bumpEntityIdCounterAboveSnapshots() {
        int maxSnapId = 0;
        for (CheckpointData d : DetOrder.commutativeValues(SnapshotRegistry.activeSnapshots)) {
            for (EntitySnapshot es : d.entities) {
                if (es.entityId > maxSnapId) {
                    maxSnapId = es.entityId;
                }
            }
        }
        EntityIdCounter.set(maxSnapId + 1);
    }

    private static void notifyPlayers(MinecraftServer server, CheckpointData data) {
        long gameTime = data.worldState != null ? data.worldState.gameTime : server.overworld().getGameTime();
        long dayTime = data.worldState != null ? data.worldState.dayTime : server.overworld().getDayTime();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ru.reset.rzero.checkpoint.player.PlayerData pd = data.playersData.get(p.getUUID());
            if (pd != null) {
                Services.PLATFORM.sendToPlayer(p, new RollbackChatPacket(pd.x, pd.y, pd.z, pd.yRot, pd.xRot, gameTime, dayTime));
            } else {
                net.minecraft.core.BlockPos spawn = p.serverLevel().getSharedSpawnPos();
                Services.PLATFORM.sendToPlayer(p, new RollbackChatPacket(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 0f, 0f, gameTime, dayTime));
            }
        }
        if (!server.overworld().getGameRules().getBoolean(ModGameRules.RULE_PLAY_ROLLBACK_SOUND)) {
            return;
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.connection.send(new ClientboundSoundPacket(
                    Holder.direct(RZero.RBD_SOUND), SoundSource.MASTER,
                    p.getX(), p.getY(), p.getZ(), 1.0f, 1.0f,
                    p.serverLevel().getRandom().nextLong()));
        }
    }

    private static void prepareSnapshots(MinecraftServer server) {
        SnapshotRegistry.allowedSnapshotEntities.clear();
        MobRamCache.clear();
        clearMonsterCatchUpProgress(server);

        for (CheckpointData d : DetOrder.commutativeValues(SnapshotRegistry.activeSnapshots)) {
            if (d.asyncSession != null) {
                try {
                    d.asyncSession.awaitAll();
                } finally {
                    d.asyncSession = null;
                }
            }
            if (RZeroRuntime.effectivePolicy(d).rollback().entities().presence()) {
                for (EntitySnapshot snap : d.entities) {
                    SnapshotRegistry.allowedSnapshotEntities.add(snap.uuid);
                }
            }
            d.pendingBlockRollbacks.clear();
        }
        RestoreQueues.clearForRestore();
    }

    private static void clearMonsterCatchUpProgress(MinecraftServer server) {
        if (!RZeroRuntime.checkpointPolicy().determinism().naturalSpawn().monsterCatchUp()) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            try {
                PendingSpawnLedger.get(level).clearMonsterProgress();
            } catch (Throwable t) {
                RZero.LOGGER.warn("[RZero] Could not reset monster catch-up for {}: {}",
                        level.dimension().location(), t.getMessage());
            }
        }
    }

    private static void restoreGlobals(MinecraftServer server,
                                       CheckpointData data,
                                       RZeroCheckpointPolicy policy) {
        if (data.serverGlobals == null) {
            return;
        }
        try {
            data.serverGlobals.restore(server, server.registryAccess(),
                    policy.rollback().world().serverGlobals());
        } catch (Exception e) {
            RZero.LOGGER.warn("[RZero] ServerGlobals restore failed: {}", e.getMessage());
        }
    }

    private static void restoreScoreboard(MinecraftServer server,
                                          CheckpointData data,
                                          RZeroCheckpointPolicy policy) {
        if (data.scoreboardTag == null || !policy.rollback().world().scoreboard()) {
            return;
        }
        try {
            Scoreboard sb = server.getScoreboard();
            for (Objective o : new ArrayList<>(sb.getObjectives())) {
                sb.removeObjective(o);
            }
            for (PlayerTeam t : new ArrayList<>(sb.getPlayerTeams())) {
                sb.removePlayerTeam(t);
            }
            new ScoreboardSaveData(sb).load(data.scoreboardTag, server.registryAccess());
        } catch (Exception e) {
            RZero.LOGGER.warn("[RZero] Scoreboard restore failed: {}", e.getMessage());
        }
    }

    private static int[] restoreChunks(ServerLevel level, CheckpointData data) {
        RZeroCheckpointPolicy policy = RZeroRuntime.effectivePolicy(data);
        boolean hasChunkRestoreWork = policy.rollback().blocks()
                || policy.rollback().blockEntities()
                || policy.rollback().blockTicks()
                || policy.rollback().fluidTicks()
                || policy.rollback().blockEvents()
                || policy.rollback().pois()
                || policy.rollback().entities().presence();
        if (!hasChunkRestoreWork) {
            return new int[]{0, 0};
        }

        int processed = 0;
        int queued = 0;

        long[] chunkKeys = data.sectionSnapshots.keySet().toLongArray();
        Arrays.sort(chunkKeys);

        for (long chunkKey : chunkKeys) {
            ChunkPos cPos = new ChunkPos(chunkKey);
            Long2LongMap rollbacks = RestoreQueues.rollbacksFor(level.dimension());
            long deadline = level.getServer().getTickCount() + ENTITY_ROLLBACK_WINDOW;

            if (level.hasChunk(cPos.x, cPos.z)) {
                processed++;
                LevelChunk chunk = level.getChunk(cPos.x, cPos.z);
                ChunkRestorer.apply(level, chunk, data);
                RestoreQueues.chunksPendingEntityRestore.add(chunk);
            } else {
                queued++;
                data.pendingBlockRollbacks.add(chunkKey);
            }
            rollbacks.put(chunkKey, deadline);
        }
        return new int[]{processed, queued};
    }

    private static void dropStaleBlockEvents(ServerLevel level, CheckpointData data) {
        var currentEvents = ((IRZeroServerLevel) level).rzero$getBlockEvents();
        currentEvents.removeIf(be ->
                data.sectionSnapshots.containsKey(new ChunkPos(be.pos()).toLong())
                        && level.hasChunk(be.pos().getX() >> 4, be.pos().getZ() >> 4));
    }

    private static void removeSnapshotEntities(ServerLevel level, CheckpointData data) {
        var entitiesPolicy = RZeroRuntime.effectivePolicy(data).rollback().entities();
        if (!entitiesPolicy.presence()) {
            return;
        }
        boolean rollbackItems = entitiesPolicy.droppedItems();
        boolean rollbackOrbs = entitiesPolicy.experienceOrbs();
        List<Entity> toRemove = new ArrayList<>();
        for (Entity e : level.getAllEntities()) {
            if (e == null || e instanceof ServerPlayer) {
                continue;
            }
            if (!rollbackItems && e instanceof net.minecraft.world.entity.item.ItemEntity) {
                continue;
            }
            if (!rollbackOrbs && e instanceof net.minecraft.world.entity.ExperienceOrb) {
                continue;
            }
            long chunkKey = e.chunkPosition().toLong();
            if (SnapshotRegistry.allowedSnapshotEntities.contains(e.getUUID())
                    || data.sectionSnapshots.containsKey(chunkKey)) {
                toRemove.add(e);
            }
        }
        for (Entity e : toRemove) {
            ((IRZeroServerLevel) level).rzero$deepRemoveEntity(e);
        }
    }

    public static void flushSyncPendingRestores(MinecraftServer server) {
        for (var entry : DetOrder.sortedEntries(
                SnapshotRegistry.activeSnapshots, k -> k.location().toString())) {
            ServerLevel level = server.getLevel(entry.getKey());
            if (level == null) {
                continue;
            }
            CheckpointData data = entry.getValue();

            long[] keys;
            synchronized (data.pendingBlockRollbacks) {
                keys = data.pendingBlockRollbacks.toLongArray();
            }
            Arrays.sort(keys);
            for (long chunkKey : keys) {
                ChunkPos cPos = new ChunkPos(chunkKey);
                LevelChunk chunk = level.getChunk(cPos.x, cPos.z);
                ChunkRestorer.apply(level, chunk, data);
                EntityRestorer.spawnEntitiesForChunk(level, cPos, data);
                data.pendingBlockRollbacks.remove(chunkKey);
            }
            data.setDirty();
        }

        LevelChunk c;
        while ((c = RestoreQueues.chunksReadyForRestore.poll()) != null) {
            ServerLevel l = (ServerLevel) c.getLevel();
            CheckpointData d = SnapshotRegistry.activeSnapshots.get(l.dimension());
            if (d != null && d.sectionSnapshots.containsKey(c.getPos().toLong())) {
                ChunkRestorer.apply(l, c, d);
                EntityRestorer.spawnEntitiesForChunk(l, c.getPos(), d);
            }
        }
        while ((c = RestoreQueues.chunksPendingEntityRestore.poll()) != null) {
            ServerLevel l = (ServerLevel) c.getLevel();
            CheckpointData d = SnapshotRegistry.activeSnapshots.get(l.dimension());
            if (d != null) {
                EntityRestorer.spawnEntitiesForChunk(l, c.getPos(), d);
            }
        }

        for (var entry : DetOrder.sortedEntries(
                SnapshotRegistry.activeSnapshots, k -> k.location().toString())) {
            ServerLevel level = server.getLevel(entry.getKey());
            if (level == null) {
                continue;
            }
            CheckpointData data = entry.getValue();
            if (RZeroRuntime.effectivePolicy(data).rollback().world().levelRng()
                    && data.worldState != null && data.worldState.rngState != null
                    && level.getRandom() instanceof IRZeroRandomState rState) {
                rState.rzero$setState(data.worldState.rngState);
            }
        }
    }
}
