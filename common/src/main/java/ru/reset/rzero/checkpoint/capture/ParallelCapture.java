package ru.reset.rzero.checkpoint.capture;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import ru.reset.rzero.checkpoint.data.CheckpointData;
import ru.reset.rzero.checkpoint.data.ChunkScope;
import ru.reset.rzero.checkpoint.CheckpointManager;

public final class ParallelCapture {
    private ParallelCapture() {}

    public static void captureChunks(ServerLevel level,
                                     long[] chunkKeys,
                                     ChunkScope scope,
                                     CheckpointData data,
                                     ru.reset.rzero.serial.RZBlobEncoder.Session session,
                                     it.unimi.dsi.fastutil.longs.Long2ObjectMap<java.util.List<net.minecraft.world.entity.Entity>> entitiesByChunk) {
        for (long chunkKey : chunkKeys) {
            if (scope != null && !scope.includes(level.dimension(), chunkKey)) continue;
            ChunkPos cPos = new ChunkPos(chunkKey);
            if (level.hasChunk(cPos.x, cPos.z)) {
                LevelChunk chunk = level.getChunk(cPos.x, cPos.z);
                CheckpointManager.captureChunkForSnapshot(level, chunk, data, session, entitiesByChunk == null ? null : entitiesByChunk.get(chunkKey));
            }
        }
    }
}
