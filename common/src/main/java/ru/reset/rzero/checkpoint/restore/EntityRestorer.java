package ru.reset.rzero.checkpoint.restore;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import ru.reset.rzero.RZero;
import ru.reset.rzero.access.IRZeroRandomState;
import ru.reset.rzero.access.IRZeroServerLevel;
import ru.reset.rzero.checkpoint.capture.EntityCapture;
import ru.reset.rzero.checkpoint.data.CheckpointData;
import ru.reset.rzero.checkpoint.data.EntityRAMSnapshot;
import ru.reset.rzero.checkpoint.data.EntitySnapshot;
import ru.reset.rzero.config.RZeroCheckpointPolicy;
import ru.reset.rzero.runtime.RZeroRuntime;
import ru.reset.rzero.runtime.RestoreQueues;
import ru.reset.rzero.runtime.SnapshotRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class EntityRestorer {

    private EntityRestorer() {
    }

    public static void spawnEntitiesForChunk(ServerLevel level, ChunkPos cPos, CheckpointData data) {
        RZeroCheckpointPolicy.Entities policy = RZeroRuntime.effectivePolicy(data).rollback().entities();
        if (!policy.presence()) {
            return;
        }

        long t0 = System.nanoTime();
        removeForeignEntities(level, cPos, policy);
        ru.reset.rzero.util.RZBenchmark.accum(ru.reset.rzero.util.RZBenchmark.Phase.REMOVE_FOREIGN, t0);

        long targetChunkKey = cPos.toLong();
        List<EntitySnapshot> chunkEntities = data.getEntitiesForChunk(targetChunkKey);
        if (chunkEntities.isEmpty()) {
            return;
        }

        Map<UUID, Mob> restoredMobs = new HashMap<>();
        List<EntitySnapshot> spawnedSnaps = new ArrayList<>();

        boolean rollbackItems = policy.droppedItems();
        boolean rollbackOrbs = policy.experienceOrbs();
        long t1 = System.nanoTime();
        for (EntitySnapshot snap : chunkEntities) {
            CompoundTag rawNbt = snap.decodeNbt();
            String idStr = rawNbt.getString("id");
            if (!rollbackItems && "minecraft:item".equals(idStr)) {
                continue;
            }
            if (!rollbackOrbs && "minecraft:experience_orb".equals(idStr)) {
                continue;
            }
            spawnOrLoad(level, snap, restoredMobs, policy);
            spawnedSnaps.add(snap);
        }
        ru.reset.rzero.util.RZBenchmark.accum(ru.reset.rzero.util.RZBenchmark.Phase.SPAWN_NBT, t1);
        ru.reset.rzero.util.RZBenchmark.addMobs(restoredMobs.size());

        for (EntitySnapshot snap : spawnedSnaps) {
            Mob mob = restoredMobs.get(snap.uuid);
            if (mob != null) {
                relinkMob(level, mob, snap, data.entityRamSnapshots.get(snap.uuid), policy);
            }
        }
    }

    private static void removeForeignEntities(ServerLevel level, ChunkPos cPos, RZeroCheckpointPolicy.Entities policy) {
        boolean rollbackItems = policy.droppedItems();
        boolean rollbackOrbs = policy.experienceOrbs();
        AABB box = new AABB(cPos.getMinBlockX(), level.getMinBuildHeight(), cPos.getMinBlockZ(),
                cPos.getMaxBlockX(), level.getMaxBuildHeight(), cPos.getMaxBlockZ());
        List<Entity> existing = level.getEntities((Entity) null, box, e -> !(e instanceof ServerPlayer));
        for (Entity e : existing) {
            if (!rollbackItems && e instanceof net.minecraft.world.entity.item.ItemEntity) {
                continue;
            }
            if (!rollbackOrbs && e instanceof net.minecraft.world.entity.ExperienceOrb) {
                continue;
            }
            if (!SnapshotRegistry.allowedSnapshotEntities.contains(e.getUUID())) {
                ((IRZeroServerLevel) level).rzero$deepRemoveEntity(e);
            }
        }
    }

    private static long resolveChunkKey(EntitySnapshot snap) {
        if (snap.chunkKey != 0L || snap.blob == null) {
            return snap.chunkKey;
        }
        CompoundTag legacy = snap.decodeNbt();
        net.minecraft.nbt.ListTag posTag = legacy.getList("Pos", 6);
        if (posTag.size() != 3) {
            return snap.chunkKey;
        }
        return ChunkPos.asLong(
                (int) Math.floor(posTag.getDouble(0)) >> 4,
                (int) Math.floor(posTag.getDouble(2)) >> 4);
    }

    private static void spawnOrLoad(ServerLevel level,
                                    EntitySnapshot snap,
                                    Map<UUID, Mob> restoredMobs,
                                    RZeroCheckpointPolicy.Entities policy) {
        CompoundTag nbtToLoad = filterNbtForPolicy(snap.decodeNbt(), policy);
        Entity existing = level.getEntity(snap.uuid);

        if (existing != null) {
            existing.load(nbtToLoad);
            applyIdentity(existing, snap, policy);
            if (existing instanceof Mob m) {
                restoredMobs.put(snap.uuid, m);
            }
            return;
        }

        ((IRZeroServerLevel) level).rzero$eradicateGhostEntity(snap.uuid);
        boolean wasLoading = ru.reset.rzero.engine.BrainConstructionGate.loadingEntity;
        ru.reset.rzero.engine.BrainConstructionGate.loadingEntity = policy.brainRam();
        try {
            EntityType.loadEntityRecursive(nbtToLoad, level, e -> {
                if (e == null) {
                    return null;
                }
                if (e.getUUID().equals(snap.uuid)) {
                    assignEntityId(level, e, snap, policy);
                    applyIdentity(e, snap, policy);
                }
                try {
                    boolean added = ((IRZeroServerLevel) level).rzero$surgicalSpawn(e);
                    if (!added) {
                        RZero.LOGGER.warn(
                                "[RZero-TAS-Audit] surgicalSpawn REJECTED for uuid={} (entityManager says UUID still known); discarding orphan",
                                e.getUUID());
                        e.discard();
                    } else if (e instanceof Mob m && e.getUUID().equals(snap.uuid)) {
                        restoredMobs.put(snap.uuid, m);
                    }
                } catch (IllegalStateException ex) {
                    RZero.LOGGER.warn("[RZero] Suppressed Entity tracking collision for {}: {}",
                            e.getUUID(), ex.getMessage());
                    e.discard();
                }
                return e;
            });
        } finally {
            ru.reset.rzero.engine.BrainConstructionGate.loadingEntity = wasLoading;
        }
    }

    private static CompoundTag filterNbtForPolicy(CompoundTag tag, RZeroCheckpointPolicy.Entities policy) {
        if (policy.passengers() && policy.rngState()) {
            return tag;
        }
        CompoundTag copy = tag.copy();
        if (!policy.passengers()) {
            copy.remove("Passengers");
        }
        if (!policy.rngState()) {
            copy.remove("RZeroRngState");
        }
        return copy;
    }

    private static void assignEntityId(ServerLevel level,
                                       Entity e,
                                       EntitySnapshot snap,
                                       RZeroCheckpointPolicy.Entities policy) {
        if (!policy.entityId()) {
            return;
        }
        Entity idHolder = level.getEntity(snap.entityId);
        if (idHolder == null || idHolder.getUUID().equals(snap.uuid)) {
            e.setId(snap.entityId);
            return;
        }
        RZero.LOGGER.warn(
                "[RZero-TAS-Audit] ID Collision! UUID:{} forced to keep NEW_ID:{} because RESTORED_ID:{} is occupied by UUID:{}",
                snap.uuid, e.getId(), snap.entityId, idHolder.getUUID());
    }

    private static void applyIdentity(Entity entity,
                                      EntitySnapshot snap,
                                      RZeroCheckpointPolicy.Entities policy) {
        if (policy.tickCount()) {
            entity.tickCount = snap.tickCount;
        }
        if (policy.rngState()
                && snap.rngState != null
                && entity.getRandom() instanceof IRZeroRandomState rState) {
            rState.rzero$setState(snap.rngState);
        }
        if (policy.livingTimers()) {
            snap.applyLivingTimers(entity);
        }
    }

    private static void relinkMob(ServerLevel level,
                                  Mob mob,
                                  EntitySnapshot snap,
                                  EntityRAMSnapshot ram,
                                  RZeroCheckpointPolicy.Entities policy) {
        if (policy.brainRam() && ram != null) {
            BrainRestorer.restore(mob, ram, snap);
            long t0 = System.nanoTime();
            EntityCapture.applyMobRam(mob, ram);
            ru.reset.rzero.util.RZBenchmark.accum(ru.reset.rzero.util.RZBenchmark.Phase.APPLY_MOB_RAM, t0);
        }

        if (policy.target() && snap.targetUuid != null) {
            Entity target = RestoreQueues.entityRemapCache.get(snap.targetUuid);
            if (target != null && target.isRemoved()) {
                target = null;
            }
            if (target == null) {
                target = level.getEntity(snap.targetUuid);
                if (target == null) {
                    target = level.getServer().getPlayerList().getPlayer(snap.targetUuid);
                }
                if (target != null && !target.isRemoved()) {
                    RestoreQueues.entityRemapCache.put(snap.targetUuid, target);
                }
            }
            if (target instanceof LivingEntity le) {
                mob.setTarget(le);
            }
        }

        if (policy.navigation() && snap.hasPath) {
            RestoreQueues.pendingPathRestores.add(new RestoreQueues.PathRestore(
                    mob, snap.pathTargetX, snap.pathTargetY, snap.pathTargetZ,
                    snap.pathSpeed > 0 ? snap.pathSpeed : 1.0));
        }
    }
}
