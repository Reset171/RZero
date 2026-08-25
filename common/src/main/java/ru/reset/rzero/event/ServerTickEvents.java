package ru.reset.rzero.event;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import ru.reset.rzero.ModGameRules;
import ru.reset.rzero.api.DevHooks;
import ru.reset.rzero.checkpoint.CheckpointManager;
import ru.reset.rzero.checkpoint.data.CheckpointData;
import ru.reset.rzero.adaptive.AdaptiveSaveEngine;
import ru.reset.rzero.runtime.MobRamCache;
import ru.reset.rzero.runtime.RZeroRuntime;
import ru.reset.rzero.runtime.RestoreQueues;
import ru.reset.rzero.runtime.SnapshotRegistry;
import ru.reset.rzero.util.DetOrder;

import java.util.Map;
import java.util.UUID;

public final class ServerTickEvents {

    private static final long ENTITY_ROLLBACK_WINDOW = 20L;

    private ServerTickEvents() {
    }

    public static void onServerTickPost(MinecraftServer server) {
        RZeroRuntime.wasRestoredThisTick = false;
        DevHooks.fireServerTickPost(server);

        long currentTick = server.overworld().getGameTime();

        MobRamCache.evictExpired(currentTick);
        promoteLoadedChunks(server);
        discardDoomedEntities();
        drainEntityRestoreQueue();
        drainBlockRestoreQueue(currentTick);
        remountPendingRides(server);
        applyPendingDeathRollback(server);
        expireWindows(currentTick);

        ServerLevel overworld = server.overworld();
        if (!overworld.getGameRules().getBoolean(ModGameRules.RULE_AUTO_SAVE)) {
            return;
        }
        AdaptiveSaveEngine.tick(server, overworld, currentTick);
    }

    private static void promoteLoadedChunks(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            CheckpointData activeSnapshot = SnapshotRegistry.activeSnapshots.get(level.dimension());
            if (activeSnapshot == null || activeSnapshot.pendingBlockRollbacks.isEmpty()) {
                continue;
            }
            long[] pending = activeSnapshot.pendingBlockRollbacks.toLongArray();
            boolean changed = false;
            for (long chunkKey : pending) {
                ChunkPos cPos = new ChunkPos(chunkKey);
                LevelChunk lc = level.getChunkSource().getChunkNow(cPos.x, cPos.z);
                if (lc == null) {
                    continue;
                }
                activeSnapshot.pendingBlockRollbacks.remove(chunkKey);
                changed = true;
                RestoreQueues.chunksReadyForRestore.add(lc);
            }
            if (changed) {
                activeSnapshot.setDirty();
            }
        }
    }

    private static void discardDoomedEntities() {
        Entity doomed;
        while ((doomed = RestoreQueues.doomedEntities.poll()) != null) {
            doomed.discard();
        }
    }

    private static void drainEntityRestoreQueue() {
        LevelChunk chunk;
        while ((chunk = RestoreQueues.chunksPendingEntityRestore.poll()) != null) {
            ServerLevel level = (ServerLevel) chunk.getLevel();
            CheckpointData activeSnapshot = SnapshotRegistry.activeSnapshots.get(level.dimension());
            if (activeSnapshot != null) {
                CheckpointManager.spawnEntitiesForChunk(level, chunk.getPos(), activeSnapshot);
            }
        }
    }

    private static void drainBlockRestoreQueue(long currentTick) {
        LevelChunk chunk;
        while ((chunk = RestoreQueues.chunksReadyForRestore.poll()) != null) {
            ServerLevel level = (ServerLevel) chunk.getLevel();
            long chunkKey = chunk.getPos().toLong();
            CheckpointData activeSnapshot = SnapshotRegistry.activeSnapshots.get(level.dimension());
            if (activeSnapshot == null || !activeSnapshot.sectionSnapshots.containsKey(chunkKey)) {
                continue;
            }
            CheckpointManager.applyChunkRestore(level, chunk, activeSnapshot);
            RestoreQueues.chunksPendingEntityRestore.add(chunk);
            Long2LongMap rollbacks = RestoreQueues.rollbacksFor(level.dimension());
            rollbacks.put(chunkKey, currentTick + ENTITY_ROLLBACK_WINDOW);
        }
    }

    private static void remountPendingRides(MinecraftServer server) {
        if (RestoreQueues.pendingRides.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, UUID> entry :
                DetOrder.sortedEntries(RestoreQueues.pendingRides, UUID::toString)) {
            UUID playerUuid = entry.getKey();
            ServerPlayer p = server.getPlayerList().getPlayer(playerUuid);
            if (p == null) {
                RestoreQueues.pendingRides.remove(playerUuid);
                continue;
            }
            Entity vehicle = p.serverLevel().getEntity(entry.getValue());
            if (vehicle != null) {
                p.startRiding(vehicle, true);
                RestoreQueues.pendingRides.remove(playerUuid);
            }
        }
    }

    private static void applyPendingDeathRollback(MinecraftServer server) {
        if (RestoreQueues.pendingDeathRollback == null) {
            return;
        }
        ServerPlayer p = server.getPlayerList().getPlayer(RestoreQueues.pendingDeathRollback);
        if (p != null) {
            CheckpointManager.restoreCheckpoint(p);
        }
        RestoreQueues.pendingDeathRollback = null;
    }

    private static void expireWindows(long currentTick) {
        for (var inner : DetOrder.commutativeValues(RestoreQueues.pendingEntityRollbacks)) {
            inner.long2LongEntrySet().removeIf(entry -> currentTick >= entry.getLongValue());
        }
        for (var inner : DetOrder.commutativeValues(RestoreQueues.chunkCaptureWindows)) {
            inner.long2LongEntrySet().removeIf(entry -> currentTick >= entry.getLongValue());
        }
    }
}
