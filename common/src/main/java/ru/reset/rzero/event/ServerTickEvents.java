package ru.reset.rzero.event;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
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
        int tickCount = server.getTickCount();
        ru.reset.rzero.util.RZBenchmark.tick(tickCount);
        boolean queuesEmpty = RestoreQueues.chunksReadyForRestore.isEmpty()
                && RestoreQueues.chunksPendingEntityRestore.isEmpty()
                && RestoreQueues.pendingPathRestores.isEmpty()
                && RestoreQueues.pendingChunkResends.isEmpty()
                && RestoreQueues.pendingMenuRestores.isEmpty()
                && SnapshotRegistry.allPendingBlockRollbacksEmpty();
        if (queuesEmpty) {
            ru.reset.rzero.util.RZBenchmark.endAndLog();
        }

        MobRamCache.evictExpired(currentTick);
        promoteLoadedChunks(server);
        discardDoomedEntities();
        drainEntityRestoreQueue();
        processMenuRestores(server, tickCount);
        drainBlockRestoreQueue(currentTick);
        drainPathRestores();
        drainChunkResends(server);
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

    public static void drainEntityRestoreQueue() {
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

    private static void drainPathRestores() {
        RestoreQueues.PathRestore pending;
        int budget = 32;
        while (budget-- > 0 && (pending = RestoreQueues.pendingPathRestores.poll()) != null) {
            Mob mob = pending.mob();
            if (mob.isRemoved()) {
                continue;
            }
            mob.getNavigation().moveTo(pending.x(), pending.y(), pending.z(), pending.speed());
        }
    }

    private static final int MENU_RESTORE_TIMEOUT_TICKS = 40;

    private static void drainChunkResends(MinecraftServer server) {
        int tickCount = server.getTickCount();
        RestoreQueues.ChunkResend resend;
        while ((resend = RestoreQueues.pollChunkResend(tickCount)) != null) {
            ServerLevel level = resend.level();
            LevelChunk chunk = level.getChunkSource().getChunkNow(resend.chunkX(), resend.chunkZ());
            if (chunk == null) {
                continue;
            }
            net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket packet =
                    new net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket(
                            chunk, level.getLightEngine(), null, null);
            ChunkPos cPos = new ChunkPos(resend.chunkX(), resend.chunkZ());
            for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(cPos, false)) {
                player.connection.send(packet);
            }
        }
    }

    private static void processMenuRestores(MinecraftServer server, int tickCount) {
        if (RestoreQueues.pendingMenuRestores.isEmpty()) {
            return;
        }
        var it = RestoreQueues.pendingMenuRestores.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.isRemoved()) {
                it.remove();
                continue;
            }
            var snapshot = entry.getValue();
            if (snapshot == null) {
                it.remove();
                continue;
            }
            ServerLevel level = player.serverLevel();
            if (snapshot.isAnchorReady(level)) {
                long t0 = System.nanoTime();
                try {
                    snapshot.restoreMenuReopen(player, level, player.registryAccess());
                } catch (Throwable t) {
                    ru.reset.rzero.RZero.LOGGER.warn("[RZero] Menu reopen failed: {}", t.getMessage());
                }
                ru.reset.rzero.util.RZBenchmark.accum(ru.reset.rzero.util.RZBenchmark.Phase.MENU_REOPEN, t0);
                it.remove();
            } else if (snapshot.menuRestoreAttempts++ > MENU_RESTORE_TIMEOUT_TICKS) {
                ru.reset.rzero.RZero.LOGGER.warn("[RZero] Menu reopen abandoned: anchor never appeared for {}",
                        player.getName().getString());
                it.remove();
            }
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
