package ru.reset.rzero.runtime;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import ru.reset.rzero.checkpoint.player.PlayerData;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class RestoreQueues {

    public static final ConcurrentLinkedQueue<LevelChunk> chunksReadyForRestore =
            new ConcurrentLinkedQueue<>();

    public static final ConcurrentLinkedQueue<LevelChunk> chunksPendingEntityRestore =
            new ConcurrentLinkedQueue<>();

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
    }
}
