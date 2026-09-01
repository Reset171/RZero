package ru.reset.rzero.event;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import ru.reset.rzero.RZero;
import ru.reset.rzero.RZeroConfig;
import ru.reset.rzero.api.DevHooks;
import ru.reset.rzero.checkpoint.data.CheckpointData;
import ru.reset.rzero.checkpoint.data.EntitySnapshot;
import ru.reset.rzero.checkpoint.data.RZeroAdaptiveData;
import ru.reset.rzero.config.RZeroCheckpointPolicy;
import ru.reset.rzero.adaptive.AdaptiveSaveEngine;
import ru.reset.rzero.engine.EntityIdCounter;
import ru.reset.rzero.runtime.MobRamCache;
import ru.reset.rzero.runtime.RZeroRuntime;
import ru.reset.rzero.runtime.RestoreQueues;
import ru.reset.rzero.runtime.SnapshotRegistry;
import ru.reset.rzero.serial.RZBlob;
import ru.reset.rzero.serial.RZBlobEncoder;
import ru.reset.rzero.util.DetOrder;

public final class ServerLifecycleEvents {

    private ServerLifecycleEvents() {
    }

    public static void onServerStopping() {
        RZBlobEncoder.shutdown();
        RZBlob.clearInternPool();
    }

    public static void onServerStarted(MinecraftServer server) {
        RZeroConfig.load();
        loadBrain();
        DevHooks.fireServerStarted(server);

        try {
            server.overworld().getDataStorage()
                    .computeIfAbsent(RZeroAdaptiveData.factory(), RZeroAdaptiveData.FILE_ID);
        } catch (Exception e) {
            RZero.LOGGER.error("[RZero] Failed to load RZeroAdaptiveData", e);
        }

        if (AdaptiveSaveEngine.currentInterval == 0 && AdaptiveSaveEngine.lastSaveTick == 0) {
            AdaptiveSaveEngine.lastSaveTick = server.overworld().getGameTime();
        }

        RestoreQueues.chunksReadyForRestore.clear();
        SnapshotRegistry.clear();
        MobRamCache.clear();
        RZeroRuntime.resetActiveCheckpointPolicyToConfig();

        long bootTick = server.overworld().getGameTime();
        for (ServerLevel level : server.getAllLevels()) {
            loadTimelineFor(level, bootTick);
        }
        restoreAdaptiveStateFromTimeline();
    }

    private static void loadTimelineFor(ServerLevel level, long bootTick) {
        String dataKey = "rzero_data_" + level.dimension().location().getPath();
        CheckpointData data = level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(CheckpointData::new, CheckpointData::load, null), dataKey);
        if (data.anchorId == null) {
            return;
        }

        SnapshotRegistry.activeSnapshots.put(level.dimension(), data);
        RZeroCheckpointPolicy policy = RZeroRuntime.effectivePolicy(data);
        if (policy.rollback().entities().presence()) {
            for (EntitySnapshot snap : data.entities) {
                SnapshotRegistry.allowedSnapshotEntities.add(snap.uuid);
            }
        }

        int seeded = 0;
        if (policy.rollback().entities().mobRamCache()) {
            seeded = MobRamCache.seedFrom(level, data, bootTick);
        }
        RZero.logInfo(
                "[RZero] Loaded timeline for dimension: {}. Pending persistent chunks: {}, seeded mob RAM: {}",
                level.dimension().location(), data.pendingBlockRollbacks.size(), seeded);
    }

    private static void restoreAdaptiveStateFromTimeline() {
        for (var entry : DetOrder.sortedEntries(
                SnapshotRegistry.activeSnapshots, k -> k.location().toString())) {
            CheckpointData cd = entry.getValue();
            if (cd.adaptiveState == null) {
                continue;
            }
            RZeroCheckpointPolicy policy = RZeroRuntime.effectivePolicy(cd);
            RZeroRuntime.setActiveCheckpointPolicy(cd.policy);
            AdaptiveSaveEngine.restoreAdaptiveState(cd.adaptiveState);
            if (policy.rollback().entities().entityId()) {
                EntityIdCounter.set(cd.adaptiveState.entityIdCounter);
            }
            return;
        }
        RZeroRuntime.resetActiveCheckpointPolicyToConfig();
    }

    public static void loadBrain() {
        RZero.logInfo("[RZero] Survival classifier initialized.");
    }
}
