package ru.reset.rzero.event;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import ru.reset.rzero.checkpoint.CheckpointManager;
import ru.reset.rzero.checkpoint.data.CheckpointData;
import ru.reset.rzero.runtime.RZeroRuntime;
import ru.reset.rzero.runtime.RestoreQueues;
import ru.reset.rzero.runtime.SnapshotRegistry;

public final class ChunkEvents {

    private static final long WINDOW_TICKS = 20L;

    private ChunkEvents() {
    }

    public static void onChunkLoad(ServerLevel serverLevel, LevelChunk chunk) {
        long chunkKey = chunk.getPos().toLong();
        SnapshotRegistry.loadedChunksFor(serverLevel.dimension()).add(chunkKey);

        CheckpointData activeSnapshot = SnapshotRegistry.activeSnapshots.get(serverLevel.dimension());
        if (activeSnapshot == null) {
            return;
        }
        long now = serverLevel.getServer().overworld().getGameTime();

        if (activeSnapshot.pendingBlockRollbacks.contains(chunkKey)) {
            activeSnapshot.pendingBlockRollbacks.remove(chunkKey);
            activeSnapshot.setDirty();
            if (activeSnapshot.sectionSnapshots.containsKey(chunkKey)) {
                CheckpointManager.applyChunkRestore(serverLevel, chunk, activeSnapshot, true);
                RestoreQueues.chunksPendingEntityRestore.add(chunk);
            }
            Long2LongMap rollbacks = RestoreQueues.rollbacksFor(serverLevel.dimension());
            rollbacks.put(chunkKey, now + WINDOW_TICKS);
            return;
        }

        if (!RZeroRuntime.isRestoring && !activeSnapshot.sectionSnapshots.containsKey(chunkKey)) {
            CheckpointManager.captureChunkForSnapshot(serverLevel, chunk, activeSnapshot);
            activeSnapshot.setDirty();
            Long2LongMap windows = RestoreQueues.windowsFor(serverLevel.dimension());
            windows.put(chunkKey, now + WINDOW_TICKS);
        }
    }

    public static void onChunkUnload(ServerLevel serverLevel, LevelChunk chunk) {
        LongSet chunks = SnapshotRegistry.loadedChunks.get(serverLevel.dimension());
        if (chunks != null) {
            chunks.remove(chunk.getPos().toLong());
        }
    }
}
