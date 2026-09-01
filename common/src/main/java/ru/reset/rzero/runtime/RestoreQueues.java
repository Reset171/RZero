package ru.reset.rzero.runtime;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import ru.reset.rzero.checkpoint.data.OpenMenuSnapshot;
import ru.reset.rzero.checkpoint.player.PlayerData;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class RestoreQueues {

    public record PathRestore(Mob mob, double x, double y, double z, double speed) {}

    public record ChunkResend(ServerLevel level, int chunkX, int chunkZ, int dueTick) {}

    public static final ConcurrentLinkedQueue<LevelChunk> chunksReadyForRestore =
            new ConcurrentLinkedQueue<>();

    public static final ConcurrentLinkedQueue<LevelChunk> chunksPendingEntityRestore =
            new ConcurrentLinkedQueue<>();

    public static final ConcurrentLinkedQueue<PathRestore> pendingPathRestores =
            new ConcurrentLinkedQueue<>();

    public static final ConcurrentLinkedQueue<ChunkResend> pendingChunkResends =
            new ConcurrentLinkedQueue<>();

    private static final it.unimi.dsi.fastutil.longs.LongOpenHashSet queuedResendKeys =
            new it.unimi.dsi.fastutil.longs.LongOpenHashSet();

    public static void enqueueChunkResend(ServerLevel level, int chunkX, int chunkZ, int dueTick) {
        if (queuedResendKeys.add(ChunkPos.asLong(chunkX, chunkZ))) {
            pendingChunkResends.add(new ChunkResend(level, chunkX, chunkZ, dueTick));
        }
    }

    public static ChunkResend pollChunkResend(int currentTick) {
        ChunkResend resend = pendingChunkResends.peek();
        if (resend == null || resend.dueTick() > currentTick) return null;
        pendingChunkResends.poll();
        queuedResendKeys.remove(ChunkPos.asLong(resend.chunkX(), resend.chunkZ()));
        return resend;
    }

    public static final ConcurrentHashMap<UUID, OpenMenuSnapshot> pendingMenuRestores =
            new ConcurrentHashMap<>();

    public static void enqueueMenuRestore(UUID playerId, OpenMenuSnapshot snapshot, int currentTick) {
        snapshot.enqueuedAtTick = currentTick;
        pendingMenuRestores.put(playerId, snapshot);
    }

    public static final HashMap<UUID, Entity> entityRemapCache =
            new HashMap<>();

    public static final java.util.IdentityHashMap<Entity, Entity> entityRemapByIdentity =
            new java.util.IdentityHashMap<>();

    public static final ConcurrentLinkedQueue<Entity> doomedEntities = new ConcurrentLinkedQueue<>();

    public static final ConcurrentHashMap<UUID, UUID> pendingRides = new ConcurrentHashMap<>();

    public static final ConcurrentHashMap<UUID, PlayerData> pendingOfflineRollbacks =
            new ConcurrentHashMap<>();

    public static final ConcurrentHashMap<ResourceKey<Level>, Long2LongMap> pendingEntityRollbacks =
            new ConcurrentHashMap<>();

    public static final ConcurrentHashMap<ResourceKey<Level>, Long2LongMap> chunkCaptureWindows =
            new ConcurrentHashMap<>();

    public static UUID pendingDeathRollback = null;

    private RestoreQueues() {
    }

    private static Long2LongMap syncMap() {
        return Long2LongMaps.synchronize(new Long2LongOpenHashMap());
    }

    public static Long2LongMap rollbacksFor(ResourceKey<Level> dim) {
        return pendingEntityRollbacks.computeIfAbsent(dim, k -> syncMap());
    }

    public static Long2LongMap windowsFor(ResourceKey<Level> dim) {
        return chunkCaptureWindows.computeIfAbsent(dim, k -> syncMap());
    }

    public static void clearForRestore() {
        pendingEntityRollbacks.clear();
        chunksReadyForRestore.clear();
        pendingPathRestores.clear();
        pendingChunkResends.clear();
        pendingMenuRestores.clear();
        queuedResendKeys.clear();
        entityRemapCache.clear();
        entityRemapByIdentity.clear();
    }
}
