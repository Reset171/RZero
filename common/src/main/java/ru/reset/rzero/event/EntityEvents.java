package ru.reset.rzero.event;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import ru.reset.rzero.checkpoint.capture.EntityCapture;
import ru.reset.rzero.checkpoint.data.CheckpointData;
import ru.reset.rzero.checkpoint.data.EntitySnapshot;
import ru.reset.rzero.config.RZeroCheckpointPolicy;
import ru.reset.rzero.runtime.MobRamCache;
import ru.reset.rzero.runtime.RZeroRuntime;
import ru.reset.rzero.runtime.RestoreQueues;
import ru.reset.rzero.runtime.SnapshotRegistry;

import java.util.ArrayList;
import java.util.List;

public final class EntityEvents {

    private EntityEvents() {
    }

    public static void onEntityJoin(Entity entity, ServerLevel world) {
        if (entity instanceof ServerPlayer) {
            return;
        }

        ResourceKey<Level> dim = world.dimension();
        long chunkKey = entity.chunkPosition().toLong();
        CheckpointData activeSnapshot = SnapshotRegistry.activeSnapshots.get(dim);
        RZeroCheckpointPolicy policy = RZeroRuntime.effectivePolicy(activeSnapshot);

        if (entity instanceof Mob mob && policy.rollback().entities().mobRamCache()) {
            MobRamCache.applyOnJoin(mob, world);
        }

        Long2LongMap rollbacks = RestoreQueues.rollbacksFor(dim);
        Long2LongMap windows = RestoreQueues.windowsFor(dim);

        if (activeSnapshot == null) {
            return;
        }
        if (!policy.rollback().entities().presence()) {
            return;
        }
        if (SnapshotRegistry.allowedSnapshotEntities.contains(entity.getUUID())) {
            return;
        }

        if (rollbacks.containsKey(chunkKey)) {
            RestoreQueues.doomedEntities.add(entity);
            return;
        }

        if (RZeroRuntime.isRestoring) {
            return;
        }

        long windowEnd = windows.getOrDefault(chunkKey, -1L);
        if (windowEnd == -1L || world.getServer().overworld().getGameTime() > windowEnd) {
            return;
        }

        EntitySnapshot snap = EntityCapture.buildSnapshot(entity, chunkKey);
        if (snap == null) {
            return;
        }
        activeSnapshot.entities.add(snap);
        List<EntitySnapshot> list = activeSnapshot.entitiesByChunk.get(chunkKey);
        if (list == null) {
            list = new ArrayList<>();
            activeSnapshot.entitiesByChunk.put(chunkKey, list);
        }
        list.add(snap);
        SnapshotRegistry.allowedSnapshotEntities.add(entity.getUUID());
        activeSnapshot.setDirty();
    }
}
