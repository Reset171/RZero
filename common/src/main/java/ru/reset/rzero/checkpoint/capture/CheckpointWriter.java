package ru.reset.rzero.checkpoint.capture;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.scores.ScoreboardSaveData;
import ru.reset.rzero.RZero;
import ru.reset.rzero.access.IRZeroRandomState;
import ru.reset.rzero.anchor.AnchorSelector;
import ru.reset.rzero.api.DevHooks;
import ru.reset.rzero.checkpoint.data.CheckpointData;
import ru.reset.rzero.checkpoint.data.ChunkScope;
import ru.reset.rzero.checkpoint.data.ServerGlobalsSnapshot;
import ru.reset.rzero.checkpoint.data.WorldSnapshot;
import ru.reset.rzero.checkpoint.player.AdvancementSnapshot;
import ru.reset.rzero.checkpoint.player.OfflinePlayerFiles;
import ru.reset.rzero.checkpoint.player.PlayerData;
import ru.reset.rzero.config.RZeroCheckpointPolicy;
import ru.reset.rzero.adaptive.AdaptiveSaveEngine;
import ru.reset.rzero.engine.EntityIdCounter;
import ru.reset.rzero.network.MarkChatPacket;
import ru.reset.rzero.platform.Services;
import ru.reset.rzero.runtime.MobRamCache;
import ru.reset.rzero.runtime.RZeroRuntime;
import ru.reset.rzero.runtime.RestoreQueues;
import ru.reset.rzero.runtime.SnapshotRegistry;
import ru.reset.rzero.serial.RZBlobEncoder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CheckpointWriter {

    private CheckpointWriter() {
    }

    public static void setCheckpoint(ServerPlayer player) {
        write(player.server, player.serverLevel(), player.getUUID(), player, null);
    }

    public static void setCheckpointHeadless(MinecraftServer server,
                                             ServerLevel anchorLevel,
                                             BlockPos anchorPos) {
        write(server, anchorLevel, uuidForAnchor(anchorLevel, anchorPos), null, null);
    }

    public static void setCheckpointHeadlessScoped(MinecraftServer server,
                                                   ServerLevel anchorLevel,
                                                   BlockPos anchorPos,
                                                   ChunkScope scope) {
        if (scope == null) {
            throw new IllegalArgumentException(
                    "scope must not be null; use setCheckpointHeadless for whole-world capture");
        }
        if (!scope.coversDim(anchorLevel.dimension())) {
            throw new IllegalArgumentException("scope dim " + scope.dim().location()
                    + " does not match anchor level dim " + anchorLevel.dimension().location());
        }
        write(server, anchorLevel, uuidForAnchor(anchorLevel, anchorPos), null, scope);
    }

    private static UUID uuidForAnchor(ServerLevel anchorLevel, BlockPos anchorPos) {
        return UUID.nameUUIDFromBytes(
                ("rzero-headless-" + anchorLevel.dimension().location() + "@" + anchorPos.asLong())
                        .getBytes(StandardCharsets.UTF_8));
    }

    private static Set<UUID> resolveAnchorIds(MinecraftServer server,
                                              UUID anchorId,
                                              ServerPlayer player) {
        if (player == null) {
            return anchorId == null ? Set.of() : Set.of(anchorId);
        }
        return AnchorSelector.resolveAnchors(server, server.overworld().getGameTime(), anchorId);
    }

    private static void write(MinecraftServer server,
                              ServerLevel anchorLevel,
                              UUID anchorId,
                              ServerPlayer player,
                              ChunkScope scope) {
        long startNanos = System.nanoTime();
        ru.reset.rzero.util.RZBenchmark.begin("CHECKPOINT CAPTURE", server.getTickCount());
        logIntent(anchorLevel, anchorId, player, scope);

        SnapshotRegistry.allowedSnapshotEntities.clear();
        RestoreQueues.chunkCaptureWindows.clear();
        SnapshotRegistry.activeSnapshots.clear();
        MobRamCache.clear();

        RZeroCheckpointPolicy policy = RZeroRuntime.settings().checkpointPolicy();
        RZeroRuntime.setActiveCheckpointPolicy(policy);

        Set<UUID> anchorIds = resolveAnchorIds(server, anchorId, player);
        UUID primaryAnchor = anchorIds.isEmpty() ? anchorId : anchorIds.iterator().next();

        int totalEntities = 0;
        int totalChunks = 0;
        RZBlobEncoder.Session globalSession = null;

        for (ServerLevel level : server.getAllLevels()) {
            if (scope != null && !scope.coversDim(level.dimension())) {
                continue;
            }
            CheckpointData data = loadOrCreate(level);
            data.anchorId = primaryAnchor;
            data.anchorIds.clear();
            data.anchorIds.addAll(anchorIds);
            data.scope = scope;
            data.policy = policy;

            boolean isAnchor = level == anchorLevel;
            if (isAnchor) {
                long t0 = System.nanoTime();
                capturePlayers(server, level, data);
                ru.reset.rzero.util.RZBenchmark.accum(ru.reset.rzero.util.RZBenchmark.Phase.CAPTURE_PLAYERS, t0);
                ru.reset.rzero.util.RZBenchmark.addPlayers(server.getPlayerList().getPlayers().size());
                long t1 = System.nanoTime();
                captureAnchorGlobals(server, data, scope);
                ru.reset.rzero.util.RZBenchmark.accum(ru.reset.rzero.util.RZBenchmark.Phase.CAPTURE_GLOBALS, t1);
            } else {
                data.adaptiveState = null;
                data.scoreboardTag = null;
            }

            long t2 = System.nanoTime();
            data.raidsTag = scope == null ? captureRaids(level) : null;
            data.dragonFightTag = scope == null ? captureDragonFight(level) : null;
            ru.reset.rzero.util.RZBenchmark.accum(ru.reset.rzero.util.RZBenchmark.Phase.CAPTURE_RAIDS, t2);
            long t3 = System.nanoTime();
            data.worldState = captureWorldState(level);
            ru.reset.rzero.util.RZBenchmark.accum(ru.reset.rzero.util.RZBenchmark.Phase.CAPTURE_WORLD_STATE, t3);

            long t4 = System.nanoTime();
            captureChunks(level, data, scope);
            ru.reset.rzero.util.RZBenchmark.accum(ru.reset.rzero.util.RZBenchmark.Phase.CAPTURE_CHUNKS, t4);
            ru.reset.rzero.util.RZBenchmark.addChunks(data.sectionSnapshots.size());

            totalEntities += data.entities.size();
            totalChunks += data.sectionSnapshots.size();
            if (globalSession == null && data.asyncSession != null) {
                globalSession = data.asyncSession;
            }

            data.setDirty();
            SnapshotRegistry.activeSnapshots.put(level.dimension(), data);
            DevHooks.SAVE_PROFILER.endSave(level, data.entities.size());
        }

        long mainThreadNanos = System.nanoTime() - startNanos;
        long mainThreadMillis = mainThreadNanos / 1_000_000L;
        final int finalEntities = totalEntities;
        final int finalChunks = totalChunks;

        if (globalSession != null) {
            globalSession.whenComplete(startNanos, stats -> {
                long totalMillis = (System.nanoTime() - startNanos) / 1_000_000L;
                long bgMillis = Math.max(0L, totalMillis - mainThreadMillis);
                RZero.logInfo("[RZero] Checkpoint saved: main thread: {} ms | background encode: {} ms | Total: {} ms (Entities: {}, Chunks: {}, Blobs: {})",
                        mainThreadMillis, bgMillis, totalMillis, finalEntities, finalChunks, stats.blobsCount());
                ru.reset.rzero.util.RZBenchmark.setAsyncNote(
                        String.format("Async Blobs: %.1f ms (Blobs: %d)", (double) bgMillis, stats.blobsCount()));
                server.execute(ru.reset.rzero.util.RZBenchmark::endAndLog);
            });
        } else {
            RZero.logInfo("[RZero] Checkpoint saved: main thread: {} ms | Total: {} ms (Entities: {}, Chunks: {})",
                    mainThreadMillis, mainThreadMillis, finalEntities, finalChunks);
            ru.reset.rzero.util.RZBenchmark.endAndLog();
        }

        DevHooks.fireCheckpointSaved(server);
    }

    private static void logIntent(ServerLevel anchorLevel,
                                 UUID anchorId,
                                 ServerPlayer player,
                                 ChunkScope scope) {
        if (player != null) {
            RZero.logInfo("[RZero] Creating checkpoint for player: {}",
                    player.getName().getString());
            Services.PLATFORM.sendToPlayer(player, new MarkChatPacket());
        } else if (scope != null) {
            RZero.logInfo(
                    "[RZero] Creating SCOPED headless timeline snapshot, anchor dim={} box=[{},{} .. {},{}]",
                    anchorLevel.dimension().location(),
                    scope.minChunkX(), scope.minChunkZ(), scope.maxChunkX(), scope.maxChunkZ());
        } else {
            RZero.logInfo("[RZero] Creating headless timeline snapshot, anchor dim={} id={}",
                    anchorLevel.dimension().location(), anchorId);
        }
    }

    private static CheckpointData loadOrCreate(ServerLevel level) {
        String dataKey = "rzero_data_" + level.dimension().location().getPath();
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(CheckpointData::new, CheckpointData::load, null), dataKey);
    }

    private static void capturePlayers(MinecraftServer server, ServerLevel level, CheckpointData data) {
        data.playersData.clear();
        data.rawPlayersNbt.clear();
        server.getPlayerList().saveAll();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            PlayerData pd = PlayerData.captureFrom(p, level.registryAccess());
            pd.advancements = AdvancementSnapshot.capture(p);
            data.playersData.put(p.getUUID(), pd);
        }
        OfflinePlayerFiles.backupInto(server, data.rawPlayersNbt);
    }

    private static void captureAnchorGlobals(MinecraftServer server, CheckpointData data, ChunkScope scope) {
        data.adaptiveState = AdaptiveSaveEngine.captureAdaptiveState();
        data.adaptiveState.entityIdCounter = EntityIdCounter.get();

        if (scope != null) {
            data.scoreboardTag = null;
            data.serverGlobals = null;
            return;
        }
        try {
            data.scoreboardTag = new ScoreboardSaveData(server.getScoreboard())
                    .save(new CompoundTag(), server.registryAccess());
        } catch (Exception e) {
            RZero.LOGGER.warn("[RZero] Scoreboard capture failed: {}", e.getMessage());
        }
        try {
            data.serverGlobals = ServerGlobalsSnapshot.capture(server, server.registryAccess());
        } catch (Exception e) {
            RZero.LOGGER.warn("[RZero] ServerGlobals capture failed: {}", e.getMessage());
        }
    }

    private static CompoundTag captureRaids(ServerLevel level) {
        try {
            return level.getRaids().save(new CompoundTag(), level.registryAccess());
        } catch (Exception e) {
            RZero.LOGGER.warn("[RZero] Raids capture for {} failed: {}",
                    level.dimension().location(), e.getMessage());
            return null;
        }
    }

    private static CompoundTag captureDragonFight(ServerLevel level) {
        EndDragonFight df = level.getDragonFight();
        if (df == null) {
            return null;
        }
        try {
            EndDragonFight.Data fd = df.saveData();
            return EndDragonFight.Data.CODEC.encodeStart(NbtOps.INSTANCE, fd).result()
                    .map(t -> {
                        CompoundTag wrap = new CompoundTag();
                        wrap.put("data", t);
                        return wrap;
                    })
                    .orElse(null);
        } catch (Exception e) {
            RZero.LOGGER.warn("[RZero] DragonFight capture failed: {}", e.getMessage());
            return null;
        }
    }

    private static WorldSnapshot captureWorldState(ServerLevel level) {
        WorldSnapshot ws = new WorldSnapshot();
        ws.dayTime = level.getDayTime();
        ws.gameTime = level.getGameTime();
        if (level.getLevelData() instanceof ServerLevelData sld) {
            ws.isRaining = level.isRaining();
            ws.isThundering = level.isThundering();
            ws.rainTime = sld.getRainTime();
            ws.thunderTime = sld.getThunderTime();
            ws.clearWeatherTime = sld.getClearWeatherTime();
        }
        if (level.getRandom() instanceof IRZeroRandomState rState) {
            ws.rngState = rState.rzero$getState();
        }
        return ws;
    }

    private static void captureChunks(ServerLevel level, CheckpointData data, ChunkScope scope) {
        data.entities.clear();
        data.entitiesByChunk.clear();
        data.entityRamSnapshots.clear();
        data.sectionSnapshots.clear();
        data.chunkBlockEntities.clear();
        data.chunkBlockTicks.clear();
        data.chunkFluidTicks.clear();
        data.blockEvents.clear();

        DevHooks.SAVE_PROFILER.beginSave();
        RZBlobEncoder.Session session = RZBlobEncoder.newSession();
        data.asyncSession = session;

        it.unimi.dsi.fastutil.longs.Long2ObjectMap<List<Entity>> entitiesByChunk = groupEntitiesByChunk(level);

        LongSet activeChunkKeys = SnapshotRegistry.loadedChunks.get(level.dimension());
        if (activeChunkKeys != null) {
            long[] keysArray;
            synchronized (activeChunkKeys) {
                keysArray = activeChunkKeys.toLongArray();
            }
            ParallelCapture.captureChunks(level, keysArray, scope, data, session, entitiesByChunk);
        }

        if (scope == null) {
            return;
        }
        for (int cx = scope.minChunkX(); cx <= scope.maxChunkX(); cx++) {
            for (int cz = scope.minChunkZ(); cz <= scope.maxChunkZ(); cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                long key = ChunkPos.asLong(cx, cz);
                if (data.sectionSnapshots.containsKey(key)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(cx, cz);
                ChunkCapture.capture(level, chunk, data, session, entitiesByChunk.get(key));
            }
        }
    }

    private static it.unimi.dsi.fastutil.longs.Long2ObjectMap<List<Entity>> groupEntitiesByChunk(ServerLevel level) {
        DevHooks.SAVE_PROFILER.beginPhase("entityGet");
        it.unimi.dsi.fastutil.longs.Long2ObjectMap<List<Entity>> byChunk =
                new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>();
        for (Entity e : level.getAllEntities()) {
            if (e instanceof ServerPlayer) {
                continue;
            }
            long chunkKey = ChunkPos.asLong(Mth.floor(e.getX()) >> 4, Mth.floor(e.getZ()) >> 4);
            List<Entity> list = byChunk.get(chunkKey);
            if (list == null) {
                list = new ArrayList<>();
                byChunk.put(chunkKey, list);
            }
            list.add(e);
        }
        DevHooks.SAVE_PROFILER.endPhase("entityGet");
        return byChunk;
    }
}
