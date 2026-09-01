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
import ru.reset.rzero.runtime.RestoreQueues;
import ru.reset.rzero.runtime.RZeroRuntime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ChunkRestorer {

    private static final int UPDATE_NEIGHBORS_AND_CLIENTS = 3;
    private static final int RESEND_CHUNK_THRESHOLD = 64;

    public static volatile boolean isRestoringChunk = false;

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

        long t0 = System.nanoTime();
        if (restoreBlockTicksEnabled || restoreFluidTicksEnabled) {
            clearScheduledTicks(level, cPos);
        }
        Set<BlockPos> forceUpdatePos = restoreBlockEntitiesEnabled ? clearBlockEntities(chunk) : new HashSet<>();
        ru.reset.rzero.util.RZBenchmark.accum(ru.reset.rzero.util.RZBenchmark.Phase.CHUNK_BE_CLEAR, t0);

        it.unimi.dsi.fastutil.longs.LongList changedPositions =
                isDuringLoad || !restoreBlocksEnabled ? null : new it.unimi.dsi.fastutil.longs.LongArrayList();
        int changedBlocks = 0;
        boolean needsUpdate = false;
        if (restoreBlocksEnabled) {
            long t1 = System.nanoTime();
            changedBlocks = applySections(level, chunk, data, chunkKey, isDuringLoad, changedPositions);
            ru.reset.rzero.util.RZBenchmark.accum(ru.reset.rzero.util.RZBenchmark.Phase.CHUNK_BLOCKS, t1);
            ru.reset.rzero.util.RZBenchmark.addBlocksChanged(changedBlocks);
            needsUpdate = changedBlocks > 0;
        }
        if (restoreBlockEntitiesEnabled) {
            long t2 = System.nanoTime();
            needsUpdate |= restoreBlockEntities(level, chunk, data, chunkKey, forceUpdatePos);
            ru.reset.rzero.util.RZBenchmark.accum(ru.reset.rzero.util.RZBenchmark.Phase.CHUNK_BE_LOAD, t2);
        }
        ru.reset.rzero.util.RZBenchmark.addChunks(1);

        if (needsUpdate) {
            chunk.setUnsaved(true);
        }
        if (!isDuringLoad) {
            if (changedPositions != null && !changedPositions.isEmpty()) {
                if (changedBlocks <= RESEND_CHUNK_THRESHOLD) {
                    long tPush = System.nanoTime();
                    List<net.minecraft.server.level.ServerPlayer> trackingPlayers =
                            level.getChunkSource().chunkMap.getPlayers(cPos, false);
                    if (!trackingPlayers.isEmpty()) {
                        for (int i = 0; i < changedPositions.size(); i++) {
                            BlockPos p = BlockPos.of(changedPositions.getLong(i));
                            net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket packet =
                                    new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(
                                            p, level.getBlockState(p));
                            for (net.minecraft.server.level.ServerPlayer sp : trackingPlayers) {
                                sp.connection.send(packet);
                            }
                        }
                    }
                    ru.reset.rzero.util.RZBenchmark.accum(ru.reset.rzero.util.RZBenchmark.Phase.CHUNK_PUSH, tPush);
                } else if (level.getServer() != null) {
                    RestoreQueues.enqueueChunkResend(level, cPos.x, cPos.z,
                            level.getServer().getTickCount() + 1);
                }
            }
            if (!forceUpdatePos.isEmpty()) {
                long t3 = System.nanoTime();
                pushUpdatesToClients(level, forceUpdatePos);
                ru.reset.rzero.util.RZBenchmark.accum(ru.reset.rzero.util.RZBenchmark.Phase.CHUNK_PUSH, t3);
            }
        }

        if (restoreBlockTicksEnabled || restoreFluidTicksEnabled) {
            long t4 = System.nanoTime();
            restoreScheduledTicks(level, data, chunkKey, restoreBlockTicksEnabled, restoreFluidTicksEnabled);
            ru.reset.rzero.util.RZBenchmark.accum(ru.reset.rzero.util.RZBenchmark.Phase.CHUNK_TICKS, t4);
        }

        if (!isDuringLoad && restorePoisEnabled && data.chunkPois.containsKey(chunkKey)) {
            long t5 = System.nanoTime();
            ru.reset.rzero.checkpoint.capture.PoiCapture.restore(level, cPos, data.chunkPois.get(chunkKey));
            ru.reset.rzero.util.RZBenchmark.accum(ru.reset.rzero.util.RZBenchmark.Phase.CHUNK_POIS, t5);
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

    private static int applySections(ServerLevel level,
                                     LevelChunk chunk,
                                     CheckpointData data,
                                     long chunkKey,
                                     boolean isDuringLoad,
                                     it.unimi.dsi.fastutil.longs.LongList changedPositions) {
        SectionSnapshot[] sections = data.sectionSnapshots.get(chunkKey);
        if (sections == null) {
            return 0;
        }
        ChunkPos cPos = chunk.getPos();
        int totalChanged = 0;

        isRestoringChunk = true;
        try {
            for (int idx = 0; idx < sections.length; idx++) {
                SectionSnapshot snap = sections[idx];
                if (snap == null) {
                    continue;
                }
                LevelChunkSection live = chunk.getSection(idx);
                int yBase = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(idx));
                totalChanged += snap.applyDiffTo(live, chunk, level,
                        cPos.getMinBlockX(), yBase, cPos.getMinBlockZ(), isDuringLoad, changedPositions);
            }
        } finally {
            isRestoringChunk = false;
        }
        return totalChanged;
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
            BlockState state = chunk.getBlockState(pos);
            if (!state.hasBlockEntity()) {
                continue;
            }
            BlockEntity be = BlockEntity.loadStatic(
                    pos, state, entry.nbt(), level.registryAccess());
            if (be != null && be.getType().isValid(state)) {
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
