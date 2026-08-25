package ru.reset.rzero.checkpoint;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import ru.reset.rzero.checkpoint.capture.CheckpointWriter;
import ru.reset.rzero.checkpoint.capture.ChunkCapture;
import ru.reset.rzero.checkpoint.data.CheckpointData;
import ru.reset.rzero.checkpoint.data.ChunkScope;
import ru.reset.rzero.checkpoint.data.EntitySnapshot;
import ru.reset.rzero.checkpoint.restore.ChunkRestorer;
import ru.reset.rzero.checkpoint.restore.EntityRestorer;
import ru.reset.rzero.checkpoint.restore.RollbackCoordinator;
import ru.reset.rzero.serial.RZBlobEncoder;

import java.util.List;
import java.util.UUID;

public final class CheckpointManager {

    public static final ThreadLocal<Boolean> isRestoringChunk = ChunkRestorer.isRestoringChunk;

    private CheckpointManager() {
    }


    public static void setCheckpoint(ServerPlayer player) {
        CheckpointWriter.setCheckpoint(player);
    }

    public static void setCheckpointHeadless(MinecraftServer server,
                                             ServerLevel anchorLevel,
                                             BlockPos anchorPos) {
        CheckpointWriter.setCheckpointHeadless(server, anchorLevel, anchorPos);
    }

    public static void setCheckpointHeadlessScoped(MinecraftServer server,
                                                   ServerLevel anchorLevel,
                                                   BlockPos anchorPos,
                                                   ChunkScope scope) {
        CheckpointWriter.setCheckpointHeadlessScoped(server, anchorLevel, anchorPos, scope);
    }

    public static void captureChunkForSnapshot(ServerLevel level, LevelChunk chunk, CheckpointData data) {
        ChunkCapture.capture(level, chunk, data);
    }

    public static void captureChunkForSnapshot(ServerLevel level,
                                               LevelChunk chunk,
                                               CheckpointData data,
                                               RZBlobEncoder.Session session) {
        ChunkCapture.capture(level, chunk, data, session);
    }

    public static void captureChunkForSnapshot(ServerLevel level,
                                               LevelChunk chunk,
                                               CheckpointData data,
                                               RZBlobEncoder.Session session,
                                               List<Entity> preloadedEntities) {
        ChunkCapture.capture(level, chunk, data, session, preloadedEntities);
    }


    public static void applyChunkRestore(ServerLevel level, LevelChunk chunk, CheckpointData data) {
        ChunkRestorer.apply(level, chunk, data);
    }

    public static void applyChunkRestore(ServerLevel level,
                                         LevelChunk chunk,
                                         CheckpointData data,
                                         boolean isDuringLoad) {
        ChunkRestorer.apply(level, chunk, data, isDuringLoad);
    }

    public static void spawnEntitiesForChunk(ServerLevel level, ChunkPos cPos, CheckpointData data) {
        EntityRestorer.spawnEntitiesForChunk(level, cPos, data);
    }

    public static void restoreCheckpoint(ServerPlayer player) {
        RollbackCoordinator.restoreCheckpoint(player);
    }

    public static boolean restoreCheckpointHeadless(MinecraftServer server) {
        return RollbackCoordinator.restoreCheckpointHeadless(server);
    }

    public static void flushSyncPendingRestores(MinecraftServer server) {
        RollbackCoordinator.flushSyncPendingRestores(server);
    }


    public static EntitySnapshot getSnapshot(UUID uuid, CheckpointData data) {
        for (EntitySnapshot snap : data.entities) {
            if (snap.uuid.equals(uuid)) {
                return snap;
            }
        }
        return null;
    }
}
