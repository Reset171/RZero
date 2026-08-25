package ru.reset.rzero.checkpoint.restore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluid;
import ru.reset.rzero.block.SectionSnapshot;
import ru.reset.rzero.checkpoint.codec.ScheduledTickCodec;
import ru.reset.rzero.checkpoint.data.CheckpointData;
import ru.reset.rzero.config.RZeroCheckpointPolicy;
import ru.reset.rzero.mixin.level.MixinLevelTicksSchedule;
import ru.reset.rzero.runtime.RZeroRuntime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ChunkRestorer {

    private static final int UPDATE_NEIGHBORS_AND_CLIENTS = 3;

    public static final ThreadLocal<Boolean> isRestoringChunk = ThreadLocal.withInitial(() -> false);

    private ChunkRestorer() {
    }

    public static void apply(ServerLevel level, LevelChunk chunk, CheckpointData data) {
        apply(level, chunk, data, false);
    }

    public static void apply(ServerLevel level, LevelChunk chunk, CheckpointData data, boolean isDuringLoad) {
        RZeroCheckpointPolicy policy = RZeroRuntime.effectivePolicy(data);
        ChunkPos cPos = chunk.getPos();
        long chunkKey = cPos.toLong();

        boolean restoreBlockTicksEnabled = policy.rollback().blockTicks();
        boolean restoreFluidTicksEnabled = policy.rollback().fluidTicks();
        boolean restoreBlocksEnabled = policy.rollback().blocks();
        boolean restoreBlockEntitiesEnabled = policy.rollback().blockEntities();
        boolean restorePoisEnabled = policy.rollback().pois();
        boolean restoreBlockEventsEnabled = policy.rollback().blockEvents();

        if (restoreBlockTicksEnabled || restoreFluidTicksEnabled) {
            clearScheduledTicks(level, cPos);
        }
        Set<BlockPos> forceUpdatePos = restoreBlockEntitiesEnabled ? clearBlockEntities(chunk) : new HashSet<>();

        boolean needsUpdate = false;
        if (restoreBlocksEnabled) {
            needsUpdate = applySections(level, chunk, data, chunkKey, isDuringLoad);
        }
        if (restoreBlockEntitiesEnabled) {
            needsUpdate |= restoreBlockEntities(level, chunk, data, chunkKey, forceUpdatePos);
        }

        if (needsUpdate) {
            chunk.setUnsaved(true);
        }
        if (!isDuringLoad && !forceUpdatePos.isEmpty()) {
            pushUpdatesToClients(level, forceUpdatePos);
        }

        if (restoreBlockTicksEnabled || restoreFluidTicksEnabled) {
            restoreScheduledTicks(level, data, chunkKey, restoreBlockTicksEnabled, restoreFluidTicksEnabled);
        }

        if (!isDuringLoad && restorePoisEnabled && data.chunkPois.containsKey(chunkKey)) {
            ru.reset.rzero.checkpoint.capture.PoiCapture.restore(level, cPos, data.chunkPois.get(chunkKey));
        }
        if (restoreBlockEventsEnabled) {
            replayBlockEvents(level, data, cPos);
        }
    }

    private static void clearScheduledTicks(ServerLevel level, ChunkPos cPos) {
        BoundingBox box = new BoundingBox(
                cPos.getMinBlockX(), level.getMinBuildHeight(), cPos.getMinBlockZ(),
                cPos.getMaxBlockX(), level.getMaxBuildHeight(), cPos.getMaxBlockZ());
        level.getBlockTicks().clearArea(box);
        level.getFluidTicks().clearArea(box);
    }

    private static Set<BlockPos> clearBlockEntities(LevelChunk chunk) {
        Set<BlockPos> forceUpdatePos = new HashSet<>();
        List<BlockPos> existingBEs = new ArrayList<>(chunk.getBlockEntitiesPos());
        for (BlockPos bePos : existingBEs) {
            if (chunk.getBlockEntity(bePos) != null) {
                forceUpdatePos.add(bePos.immutable());
                chunk.removeBlockEntity(bePos);
            }
        }
        chunk.clearAllBlockEntities();
        return forceUpdatePos;
    }

    private static boolean applySections(ServerLevel level,
                                         LevelChunk chunk,
                                         CheckpointData data,
                                         long chunkKey,
                                         boolean isDuringLoad) {
        SectionSnapshot[] sections = data.sectionSnapshots.get(chunkKey);
        if (sections == null) {
            return false;
        }
        ChunkPos cPos = chunk.getPos();
        boolean needsUpdate = false;

        isRestoringChunk.set(true);
        try {
            for (int idx = 0; idx < sections.length; idx++) {
                SectionSnapshot snap = sections[idx];
                if (snap == null) {
                    continue;
                }
                LevelChunkSection live = chunk.getSection(idx);
                int yBase = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(idx));
                int changed = snap.applyDiffTo(live, chunk, level,
                        cPos.getMinBlockX(), yBase, cPos.getMinBlockZ(), isDuringLoad);
                if (changed > 0) {
                    needsUpdate = true;
                }
            }
        } finally {
            isRestoringChunk.set(false);
        }
        return needsUpdate;
    }

    private static boolean restoreBlockEntities(ServerLevel level,
                                                LevelChunk chunk,
                                                CheckpointData data,
                                                long chunkKey,
                                                Set<BlockPos> forceUpdatePos) {
        if (!data.chunkBlockEntities.containsKey(chunkKey)) {
            return false;
        }
        List<CheckpointData.BlockEntityEntry> beEntries = data.chunkBlockEntities.get(chunkKey);
        for (CheckpointData.BlockEntityEntry entry : beEntries) {
            BlockPos pos = entry.pos();
            BlockEntity be = BlockEntity.loadStatic(
                    pos, chunk.getBlockState(pos), entry.nbt(), level.registryAccess());
            if (be != null) {
                chunk.addAndRegisterBlockEntity(be);
                forceUpdatePos.add(pos);
            }
        }
        return !beEntries.isEmpty();
    }

    private static void pushUpdatesToClients(ServerLevel level, Set<BlockPos> forceUpdatePos) {
        for (BlockPos pos : forceUpdatePos) {
            level.getChunkSource().blockChanged(pos);
            level.getLightEngine().checkBlock(pos);
            BlockState state = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, state, UPDATE_NEIGHBORS_AND_CLIENTS);
        }
    }

    @SuppressWarnings("unchecked")
    private static void restoreScheduledTicks(ServerLevel level,
                                              CheckpointData data,
                                              long chunkKey,
                                              boolean restoreBlockTicks,
                                              boolean restoreFluidTicks) {
        long gameTime = level.getGameTime();

        if (restoreBlockTicks && data.chunkBlockTicks.containsKey(chunkKey)) {
            ScheduledTickCodec.load(
                    data.chunkBlockTicks.get(chunkKey),
                    BuiltInRegistries.BLOCK,
                    gameTime,
                    (MixinLevelTicksSchedule<Block>) level.getBlockTicks());
        }
        if (restoreFluidTicks && data.chunkFluidTicks.containsKey(chunkKey)) {
            ScheduledTickCodec.load(
                    data.chunkFluidTicks.get(chunkKey),
                    BuiltInRegistries.FLUID,
                    gameTime,
                    (MixinLevelTicksSchedule<Fluid>) level.getFluidTicks());
        }
    }

    private static void replayBlockEvents(ServerLevel level, CheckpointData data, ChunkPos cPos) {
        for (int i = 0; i < data.blockEvents.size(); i++) {
            CompoundTag t = data.blockEvents.getCompound(i);
            BlockPos pos = BlockPos.of(t.getLong("pos"));
            if ((pos.getX() >> 4) != cPos.x || (pos.getZ() >> 4) != cPos.z) {
                continue;
            }
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(t.getString("block")));
            level.blockEvent(pos, block, t.getInt("p1"), t.getInt("p2"));
        }
    }

    public static long maxSubTickOrder(CheckpointData data) {
        long maxSub = -1L;
        maxSub = Math.max(maxSub, maxSubIn(data.chunkBlockTicks.values()));
        maxSub = Math.max(maxSub, maxSubIn(data.chunkFluidTicks.values()));
        return maxSub;
    }

    private static long maxSubIn(Iterable<ListTag> lists) {
        long maxSub = -1L;
        for (ListTag list : lists) {
            for (int i = 0; i < list.size(); i++) {
                long s = list.getCompound(i).getLong("sub");
                if (s > maxSub) {
                    maxSub = s;
                }
            }
        }
        return maxSub;
    }
}
