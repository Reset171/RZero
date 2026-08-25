package ru.reset.rzero.checkpoint.data;

import ru.reset.rzero.RZero;
import ru.reset.rzero.checkpoint.player.PlayerData;
import ru.reset.rzero.config.RZeroCheckpointPolicy;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import ru.reset.rzero.block.SectionSnapshot;
import ru.reset.rzero.serial.RZBlob;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CheckpointData extends SavedData {
    public static record BlockEntityEntry(BlockPos pos, RZBlob blob) {
        public CompoundTag nbt() { return blob.toCompound(); }
    }

    public UUID anchorId;

    public final Set<UUID> anchorIds = Collections.synchronizedSet(new LinkedHashSet<>());

    public RZeroCheckpointPolicy policy;
    public final java.util.Map<java.util.UUID, PlayerData> playersData = new java.util.HashMap<>();
    public final java.util.Map<java.util.UUID, CompoundTag> rawPlayersNbt = new java.util.HashMap<>();
    public WorldSnapshot worldState;
    public AdaptiveState adaptiveState;
    public CompoundTag raidsTag;
    public CompoundTag scoreboardTag;
    public CompoundTag dragonFightTag;
    public ServerGlobalsSnapshot serverGlobals;
    public transient ChunkScope scope = null;
    public final it.unimi.dsi.fastutil.longs.Long2ObjectMap<ListTag> chunkPois =
            it.unimi.dsi.fastutil.longs.Long2ObjectMaps.synchronize(new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>());
    public final List<EntitySnapshot> entities = new ArrayList<>();
    public final java.util.Map<java.util.UUID, EntityRAMSnapshot> entityRamSnapshots = new java.util.concurrent.ConcurrentHashMap<>();
    public final it.unimi.dsi.fastutil.longs.Long2ObjectMap<SectionSnapshot[]> sectionSnapshots =
            it.unimi.dsi.fastutil.longs.Long2ObjectMaps.synchronize(new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>());
    public final it.unimi.dsi.fastutil.longs.Long2ObjectMap<List<BlockEntityEntry>> chunkBlockEntities = it.unimi.dsi.fastutil.longs.Long2ObjectMaps.synchronize(new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>());
    public final it.unimi.dsi.fastutil.longs.Long2ObjectMap<ListTag> chunkBlockTicks = it.unimi.dsi.fastutil.longs.Long2ObjectMaps.synchronize(new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>());
    public final it.unimi.dsi.fastutil.longs.Long2ObjectMap<ListTag> chunkFluidTicks = it.unimi.dsi.fastutil.longs.Long2ObjectMaps.synchronize(new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>());
    public final ListTag blockEvents = new ListTag();
    public final it.unimi.dsi.fastutil.longs.LongSet pendingBlockRollbacks = it.unimi.dsi.fastutil.longs.LongSets.synchronize(new it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet());
    public final it.unimi.dsi.fastutil.longs.Long2LongMap chunkInhabitedTime = it.unimi.dsi.fastutil.longs.Long2LongMaps.synchronize(new it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap());

    public transient volatile ru.reset.rzero.serial.RZBlobEncoder.Session asyncSession;

    public boolean isAnchor(UUID candidate) {
        if (candidate == null) {
            return false;
        }
        if (anchorIds.isEmpty()) {
            return candidate.equals(anchorId);
        }
        return anchorIds.contains(candidate);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider p) {
        if (asyncSession != null) {
            try {
                asyncSession.awaitAll();
            } finally {
                asyncSession = null;
            }
        }

        if (anchorId != null) {
            tag.putUUID("anchor", anchorId);
        }
        List<UUID> sortedAnchors;
        synchronized (anchorIds) {
            sortedAnchors = new ArrayList<>(anchorIds);
        }
        if (!sortedAnchors.isEmpty()) {
            sortedAnchors.sort(Comparator.comparing(UUID::toString));
            ListTag anchorsTag = new ListTag();
            for (UUID anchor : sortedAnchors) {
                CompoundTag entry = new CompoundTag();
                entry.putUUID("id", anchor);
                anchorsTag.add(entry);
            }
            tag.put("anchors", anchorsTag);
        }
        if (policy != null) {
            tag.put("policy", policy.toNbt());
        }
        if (!playersData.isEmpty()) {
            CompoundTag playersTag = new CompoundTag();
            for (java.util.Map.Entry<java.util.UUID, PlayerData> entry : playersData.entrySet()) {
                playersTag.put(entry.getKey().toString(), entry.getValue().toNBT(p));
            }
            tag.put("players", playersTag);
        }
        if (!rawPlayersNbt.isEmpty()) {
            CompoundTag rawTag = new CompoundTag();
            for (java.util.Map.Entry<java.util.UUID, CompoundTag> entry : rawPlayersNbt.entrySet()) {
                rawTag.put(entry.getKey().toString(), entry.getValue());
            }
            tag.put("offlinePlayers", rawTag);
        }
        if (worldState != null) {
            net.minecraft.nbt.NbtOps.INSTANCE.withEncoder(WorldSnapshot.CODEC).apply(worldState).result().ifPresent(t -> tag.put("world", t));
        }
        if (adaptiveState != null) {
            tag.put("adaptive", adaptiveState.toNBT());
        }
        if (raidsTag != null) {
            tag.put("raids", raidsTag);
        }
        if (scoreboardTag != null) {
            tag.put("scoreboard", scoreboardTag);
        }
        if (dragonFightTag != null) {
            tag.put("dragonFight", dragonFightTag);
        }
        if (serverGlobals != null) {
            tag.put("serverGlobals", serverGlobals.toNBT(p));
        }

        CompoundTag poisTag = new CompoundTag();
        synchronized (chunkPois) {
            for (it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<ListTag> e : chunkPois.long2ObjectEntrySet()) {
                poisTag.put(String.valueOf(e.getLongKey()), e.getValue());
            }
        }
        if (!poisTag.isEmpty()) tag.put("pois", poisTag);

        ListTag entitiesTag = new ListTag();
        for (EntitySnapshot es : entities) {
            entitiesTag.add(es.toNBT());
        }
        tag.put("entities", entitiesTag);

        CompoundTag ramTag = new CompoundTag();
        for (java.util.Map.Entry<UUID, EntityRAMSnapshot> e : entityRamSnapshots.entrySet()) {
            ramTag.put(e.getKey().toString(), e.getValue().toNBT());
        }
        tag.put("ramSnapshots", ramTag);

        CompoundTag sectionsTag = new CompoundTag();
        synchronized (sectionSnapshots) {
            for (it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<SectionSnapshot[]> e : sectionSnapshots.long2ObjectEntrySet()) {
                SectionSnapshot[] arr = e.getValue();
                CompoundTag chunkTag = new CompoundTag();
                for (int i = 0; i < arr.length; i++) {
                    if (arr[i] != null) chunkTag.put(Integer.toString(i), arr[i].toNBT());
                }
                chunkTag.putInt("len", arr.length);
                sectionsTag.put(String.valueOf(e.getLongKey()), chunkTag);
            }
        }
        tag.put("sections", sectionsTag);

        CompoundTag beTag = new CompoundTag();
        synchronized (chunkBlockEntities) {
            for (it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<List<BlockEntityEntry>> e : chunkBlockEntities.long2ObjectEntrySet()) {
                ListTag list = new ListTag();
                for (BlockEntityEntry entry : e.getValue()) {
                    CompoundTag entryTag = new CompoundTag();
                    entryTag.putLong("pos", entry.pos().asLong());
                    entryTag.put("nbt", entry.nbt());
                    list.add(entryTag);
                }
                beTag.put(String.valueOf(e.getLongKey()), list);
            }
        }
        tag.put("blockEntities", beTag);

        CompoundTag blockTicksTag = new CompoundTag();
        synchronized (chunkBlockTicks) {
            for (it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<ListTag> e : chunkBlockTicks.long2ObjectEntrySet()) {
                blockTicksTag.put(String.valueOf(e.getLongKey()), e.getValue());
            }
        }
        tag.put("blockTicks", blockTicksTag);

        CompoundTag fluidTicksTag = new CompoundTag();
        synchronized (chunkFluidTicks) {
            for (it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<ListTag> e : chunkFluidTicks.long2ObjectEntrySet()) {
                fluidTicksTag.put(String.valueOf(e.getLongKey()), e.getValue());
            }
        }
        tag.put("fluidTicks", fluidTicksTag);

        tag.put("blockEvents", blockEvents);
        
        CompoundTag inhabitedTag = new CompoundTag();
        synchronized (chunkInhabitedTime) {
            for (it.unimi.dsi.fastutil.longs.Long2LongMap.Entry e : chunkInhabitedTime.long2LongEntrySet()) {
                inhabitedTag.putLong(String.valueOf(e.getLongKey()), e.getLongValue());
            }
        }
        tag.put("inhabitedTime", inhabitedTag);

        synchronized (pendingBlockRollbacks) {
            if (!pendingBlockRollbacks.isEmpty()) {
                tag.putLongArray("pendingRollbacks", pendingBlockRollbacks.toLongArray());
            }
        }

        return tag;
    }

    public static CheckpointData load(CompoundTag tag, HolderLookup.Provider p) {
        CheckpointData d = new CheckpointData();
        if (tag.hasUUID("anchor")) {
            d.anchorId = tag.getUUID("anchor");
        }
        if (tag.contains("anchors", Tag.TAG_LIST)) {
            ListTag anchorsTag = tag.getList("anchors", Tag.TAG_COMPOUND);
            for (int i = 0; i < anchorsTag.size(); i++) {
                CompoundTag entry = anchorsTag.getCompound(i);
                if (entry.hasUUID("id")) {
                    d.anchorIds.add(entry.getUUID("id"));
                }
            }
        }
        if (d.anchorIds.isEmpty() && d.anchorId != null) {
            d.anchorIds.add(d.anchorId);
        }
        if (tag.contains("policy", Tag.TAG_COMPOUND)) {
            d.policy = RZeroCheckpointPolicy.fromNbt(tag.getCompound("policy"));
        }
        if (tag.contains("players", Tag.TAG_COMPOUND)) {
            CompoundTag playersTag = tag.getCompound("players");
            for (String key : playersTag.getAllKeys()) {
                try {
                    java.util.UUID uuid = java.util.UUID.fromString(key);
                    d.playersData.put(uuid, PlayerData.fromNBT(playersTag.getCompound(key)));
                } catch (IllegalArgumentException ignored) {}
            }
        } else if (tag.contains("player")) {
            if (d.anchorId != null) {
                d.playersData.put(d.anchorId, PlayerData.fromNBT(tag.getCompound("player")));
            }
        }
        if (tag.contains("offlinePlayers", Tag.TAG_COMPOUND)) {
            CompoundTag rawTag = tag.getCompound("offlinePlayers");
            for (String key : rawTag.getAllKeys()) {
                try {
                    UUID uuid = UUID.fromString(key);
                    d.rawPlayersNbt.put(uuid, rawTag.getCompound(key));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        if (tag.contains("world")) {
            WorldSnapshot.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.get("world")).result().ifPresent(w -> d.worldState = w);
        }
        if (tag.contains("adaptive", Tag.TAG_COMPOUND)) {
            d.adaptiveState = AdaptiveState.fromNBT(tag.getCompound("adaptive"));
        } else if (tag.contains("director", Tag.TAG_COMPOUND)) {
            d.adaptiveState = AdaptiveState.fromNBT(tag.getCompound("director"));
        }
        if (tag.contains("raids", Tag.TAG_COMPOUND)) {
            d.raidsTag = tag.getCompound("raids");
        }
        if (tag.contains("scoreboard", Tag.TAG_COMPOUND)) {
            d.scoreboardTag = tag.getCompound("scoreboard");
        }
        if (tag.contains("dragonFight", Tag.TAG_COMPOUND)) {
            d.dragonFightTag = tag.getCompound("dragonFight");
        }
        if (tag.contains("serverGlobals", Tag.TAG_COMPOUND)) {
            d.serverGlobals = ServerGlobalsSnapshot.fromNBT(tag.getCompound("serverGlobals"));
        }
        if (tag.contains("pois", Tag.TAG_COMPOUND)) {
            CompoundTag poisTag = tag.getCompound("pois");
            for (String k : poisTag.getAllKeys()) {
                d.chunkPois.put(Long.parseLong(k), poisTag.getList(k, Tag.TAG_COMPOUND));
            }
        }

        if (tag.contains("entities", Tag.TAG_LIST)) {
            ListTag entitiesTag = tag.getList("entities", Tag.TAG_COMPOUND);
            for (int i = 0; i < entitiesTag.size(); i++) {
                d.entities.add(EntitySnapshot.fromNBT(entitiesTag.getCompound(i)));
            }
        }

        if (tag.contains("ramSnapshots", Tag.TAG_COMPOUND)) {
            CompoundTag ramTag = tag.getCompound("ramSnapshots");
            for (String key : ramTag.getAllKeys()) {
                try {
                    UUID uuid = UUID.fromString(key);
                    d.entityRamSnapshots.put(uuid, EntityRAMSnapshot.fromNBT(ramTag.getCompound(key)));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        if (tag.contains("sections", Tag.TAG_COMPOUND)) {
            CompoundTag sectionsTag = tag.getCompound("sections");
            net.minecraft.core.HolderGetter<net.minecraft.world.level.block.Block> blockGetter =
                    p.lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK);
            for (String k : sectionsTag.getAllKeys()) {
                long chunkKey = Long.parseLong(k);
                CompoundTag chunkTag = sectionsTag.getCompound(k);
                int len = chunkTag.getInt("len");
                if (len <= 0) continue;
                SectionSnapshot[] arr = new SectionSnapshot[len];
                for (String sk : chunkTag.getAllKeys()) {
                    if ("len".equals(sk)) continue;
                    int idx;
                    try { idx = Integer.parseInt(sk); } catch (NumberFormatException ex) { continue; }
                    if (idx < 0 || idx >= len) continue;
                    arr[idx] = SectionSnapshot.fromNBT(chunkTag.getCompound(sk), blockGetter);
                }
                d.sectionSnapshots.put(chunkKey, arr);
            }
        } else if (tag.contains("deltas", Tag.TAG_COMPOUND)) {
            RZero.LOGGER.warn(
                    "[RZero] Legacy 'deltas' checkpoint format detected on disk; discarding. Run /rzero set to start a fresh timeline.");
        }

        CompoundTag beTag = tag.getCompound("blockEntities");
        for (String k : beTag.getAllKeys()) {
            ListTag list = beTag.getList(k, Tag.TAG_COMPOUND);
            List<BlockEntityEntry> entries = new ArrayList<>(list.size());
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entryTag = list.getCompound(i);
                entries.add(new BlockEntityEntry(BlockPos.of(entryTag.getLong("pos")), RZBlob.of(entryTag.getCompound("nbt"))));
            }
            d.chunkBlockEntities.put(Long.parseLong(k), entries);
        }

        if (tag.contains("blockTicks")) {
            CompoundTag blockTicksTag = tag.getCompound("blockTicks");
            for (String k : blockTicksTag.getAllKeys()) {
                d.chunkBlockTicks.put(Long.parseLong(k), blockTicksTag.getList(k, Tag.TAG_COMPOUND));
            }
        }

        if (tag.contains("fluidTicks")) {
            CompoundTag fluidTicksTag = tag.getCompound("fluidTicks");
            for (String k : fluidTicksTag.getAllKeys()) {
                d.chunkFluidTicks.put(Long.parseLong(k), fluidTicksTag.getList(k, Tag.TAG_COMPOUND));
            }
        }

        if (tag.contains("blockEvents", Tag.TAG_LIST)) {
            d.blockEvents.addAll(tag.getList("blockEvents", Tag.TAG_COMPOUND));
        }
        
        if (tag.contains("inhabitedTime", Tag.TAG_COMPOUND)) {
            CompoundTag inhabitedTag = tag.getCompound("inhabitedTime");
            for (String k : inhabitedTag.getAllKeys()) {
                d.chunkInhabitedTime.put(Long.parseLong(k), inhabitedTag.getLong(k));
            }
        }

        if (tag.contains("pendingRollbacks")) {
            long[] prArr = tag.getLongArray("pendingRollbacks");
            for (long l : prArr) {
                d.pendingBlockRollbacks.add(l);
            }
        }

        return d;
    }
}
