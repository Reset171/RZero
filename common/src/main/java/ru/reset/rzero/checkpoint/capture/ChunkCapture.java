package ru.reset.rzero.checkpoint.capture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.ticks.ScheduledTick;
import ru.reset.rzero.access.IRZeroServerLevel;
import ru.reset.rzero.access.IRZeroTickAccess;
import ru.reset.rzero.api.DevHooks;
import ru.reset.rzero.block.SectionSnapshot;
import ru.reset.rzero.checkpoint.codec.ScheduledTickCodec;
import ru.reset.rzero.checkpoint.data.CheckpointData;
import ru.reset.rzero.runtime.SnapshotRegistry;
import ru.reset.rzero.serial.RZBlobEncoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class ChunkCapture {

    private ChunkCapture() {
    }

    public static void capture(ServerLevel level, LevelChunk chunk, CheckpointData data) {
        if (data.asyncSession == null) {
            data.asyncSession = RZBlobEncoder.newSession();
        }
        capture(level, chunk, data, data.asyncSession, null);
    }

    public static void capture(ServerLevel level,
                               LevelChunk chunk,
                               CheckpointData data,
                               RZBlobEncoder.Session session) {
        capture(level, chunk, data, session, null);
    }

    public static void capture(ServerLevel level,
                               LevelChunk chunk,
                               CheckpointData data,
                               RZBlobEncoder.Session session,
                               List<Entity> preloadedEntities) {
        long chunkKey = chunk.getPos().toLong();
        if (data.sectionSnapshots.containsKey(chunkKey)) {
            return;
        }

        DevHooks.SAVE_PROFILER.beginPhase("blocks");
        data.sectionSnapshots.put(chunkKey, new SectionSnapshot[chunk.getSectionsCount()]);
        captureBlockEntities(level, chunk, data, session, chunkKey);
        captureScheduledTicks(level, data, chunkKey);
        DevHooks.SAVE_PROFILER.endPhase("blocks");

        DevHooks.SAVE_PROFILER.beginPhase("pois");
        ListTag pois = PoiCapture.capture(level, chunk.getPos());
        if (pois != null) {
            data.chunkPois.put(chunkKey, pois);
        }
        DevHooks.SAVE_PROFILER.endPhase("pois");

        DevHooks.SAVE_PROFILER.beginPhase("entities");
        captureBlockEvents(level, data, chunkKey);
        captureEntities(level, chunk, data, session, preloadedEntities, chunkKey);
        DevHooks.SAVE_PROFILER.endPhase("entities");
    }

    private static void captureBlockEntities(ServerLevel level,
                                             LevelChunk chunk,
                                             CheckpointData data,
                                             RZBlobEncoder.Session session,
                                             long chunkKey) {
        List<BlockPos> bePositions = new ArrayList<>(chunk.getBlockEntitiesPos());
        bePositions.sort(Comparator.comparingLong(BlockPos::asLong));

        List<BlockPos> liveBe = new ArrayList<>(bePositions.size());
        List<CompoundTag> beTags = new ArrayList<>(bePositions.size());
        for (BlockPos pos : bePositions) {
            BlockEntity be = chunk.getBlockEntity(pos);
            if (be != null) {
                beTags.add(be.saveWithFullMetadata(level.registryAccess()));
                liveBe.add(pos.immutable());
            }
        }
        if (liveBe.isEmpty()) {
            return;
        }

        final CheckpointData.BlockEntityEntry[] beArr =
                new CheckpointData.BlockEntityEntry[liveBe.size()];
        for (int i = 0; i < liveBe.size(); i++) {
            final int slot = i;
            final BlockPos immut = liveBe.get(i);
            session.submitBe(beTags.get(i),
                    blob -> beArr[slot] = new CheckpointData.BlockEntityEntry(immut, blob));
        }
        data.chunkBlockEntities.put(chunkKey, Arrays.asList(beArr));
    }

    @SuppressWarnings("unchecked")
    private static void captureScheduledTicks(ServerLevel level, CheckpointData data, long chunkKey) {
        long gameTime = level.getGameTime();

        List<ScheduledTick<Block>> bTicks =
                ((IRZeroTickAccess<Block>) level.getBlockTicks()).rzero$getTicksInChunk(chunkKey);
        if (bTicks != null && !bTicks.isEmpty()) {
            data.chunkBlockTicks.put(chunkKey,
                    ScheduledTickCodec.save(bTicks, BuiltInRegistries.BLOCK, gameTime));
        }

        List<ScheduledTick<Fluid>> fTicks =
                ((IRZeroTickAccess<Fluid>) level.getFluidTicks()).rzero$getTicksInChunk(chunkKey);
        if (fTicks != null && !fTicks.isEmpty()) {
            data.chunkFluidTicks.put(chunkKey,
                    ScheduledTickCodec.save(fTicks, BuiltInRegistries.FLUID, gameTime));
        }
    }

    private static void captureBlockEvents(ServerLevel level, CheckpointData data, long chunkKey) {
        for (BlockEventData be : ((IRZeroServerLevel) level).rzero$getBlockEvents()) {
            if (new ChunkPos(be.pos()).toLong() != chunkKey) {
                continue;
            }
            CompoundTag t = new CompoundTag();
            t.putLong("pos", be.pos().asLong());
            t.putString("block", BuiltInRegistries.BLOCK.getKey(be.block()).toString());
            t.putInt("p1", be.paramA());
            t.putInt("p2", be.paramB());
            synchronized (data.blockEvents) {
                data.blockEvents.add(t);
            }
        }
    }

    private static void captureEntities(ServerLevel level,
                                        LevelChunk chunk,
                                        CheckpointData data,
                                        RZBlobEncoder.Session session,
                                        List<Entity> preloadedEntities,
                                        long chunkKey) {
        List<Entity> entities;
        if (preloadedEntities != null) {
            entities = new ArrayList<>(preloadedEntities);
        } else {
            ChunkPos cPos = chunk.getPos();
            AABB box = new AABB(cPos.getMinBlockX(), level.getMinBuildHeight(), cPos.getMinBlockZ(),
                    cPos.getMaxBlockX(), level.getMaxBuildHeight(), cPos.getMaxBlockZ());
            entities = new ArrayList<>(
                    level.getEntities((Entity) null, box, e -> !(e instanceof ServerPlayer)));
        }
        entities.sort(Comparator.comparing(Entity::getUUID));

        for (Entity e : entities) {
            if (e == null || SnapshotRegistry.allowedSnapshotEntities.contains(e.getUUID())) {
                continue;
            }
            EntityCapture.Captured captured = EntityCapture.captureWithRam(e, chunkKey, session);
            if (captured == null) {
                continue;
            }
            synchronized (data.entities) {
                data.entities.add(captured.snapshot());
                data.entityRamSnapshots.put(e.getUUID(), captured.ram());
            }
            SnapshotRegistry.allowedSnapshotEntities.add(e.getUUID());
        }
    }
}
