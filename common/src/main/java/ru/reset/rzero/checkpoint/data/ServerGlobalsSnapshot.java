package ru.reset.rzero.checkpoint.data;

import ru.reset.rzero.RZero;
import ru.reset.rzero.runtime.RZeroRuntime;

import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.bossevents.CustomBossEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.saveddata.maps.MapIndex;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import ru.reset.rzero.config.RZeroCheckpointPolicy;
import ru.reset.rzero.util.DetOrder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ServerGlobalsSnapshot {
    public CompoundTag bossbars;
    public Map<String, long[]> forcedChunks = new LinkedHashMap<>();
    public CompoundTag worldBorder;
    public CompoundTag randomSequences;
    public int serverTickCount;
    public CompoundTag gameRules;
    public long shufflingCounter;
    public Map<String, Map<String, CompoundTag>> savedDataSnapshots = new LinkedHashMap<>();
    public long[] levelRandomState;

    public static void markGameRulesDirty() {
        ServerGlobalsCache.markGameRulesDirty();
    }

    public CompoundTag toNBT(HolderLookup.Provider lookup) {
        CompoundTag tag = new CompoundTag();
        if (bossbars != null) tag.put("bossbars", bossbars);
        if (worldBorder != null) tag.put("border", worldBorder);
        if (randomSequences != null) tag.put("rngSeq", randomSequences);
        if (gameRules != null) tag.put("rules", gameRules);
        tag.putInt("tick", serverTickCount);
        tag.putLong("shuff", shufflingCounter);
        if (levelRandomState != null) tag.putLongArray("lvlRng", levelRandomState);
        if (!forcedChunks.isEmpty()) {
            CompoundTag fc = new CompoundTag();
            for (var e : DetOrder.sortedEntries(forcedChunks, k -> k)) {
                fc.putLongArray(e.getKey(), e.getValue());
            }
            tag.put("forced", fc);
        }
        if (!savedDataSnapshots.isEmpty()) {
            CompoundTag allDims = new CompoundTag();
            for (var dimEntry : DetOrder.sortedEntries(savedDataSnapshots, k -> k)) {
                CompoundTag dimTag = new CompoundTag();
                for (var dataEntry : DetOrder.sortedEntries(dimEntry.getValue(), k -> k)) {
                    dimTag.put(dataEntry.getKey(), dataEntry.getValue());
                }
                allDims.put(dimEntry.getKey(), dimTag);
            }
            tag.put("savedData", allDims);
        }
        return tag;
    }

    public static ServerGlobalsSnapshot fromNBT(CompoundTag tag) {
        ServerGlobalsSnapshot s = new ServerGlobalsSnapshot();
        if (tag.contains("bossbars", Tag.TAG_COMPOUND)) s.bossbars = tag.getCompound("bossbars");
        if (tag.contains("border", Tag.TAG_COMPOUND)) s.worldBorder = tag.getCompound("border");
        if (tag.contains("rngSeq", Tag.TAG_COMPOUND)) s.randomSequences = tag.getCompound("rngSeq");
        if (tag.contains("rules", Tag.TAG_COMPOUND)) s.gameRules = tag.getCompound("rules");
        s.serverTickCount = tag.getInt("tick");
        s.shufflingCounter = tag.getLong("shuff");
        if (tag.contains("lvlRng", Tag.TAG_LONG_ARRAY)) s.levelRandomState = tag.getLongArray("lvlRng");
        if (tag.contains("forced", Tag.TAG_COMPOUND)) {
            CompoundTag fc = tag.getCompound("forced");
            for (String k : sortedKeys(fc)) {
                s.forcedChunks.put(k, fc.getLongArray(k));
            }
        }
        if (tag.contains("savedData", Tag.TAG_COMPOUND)) {
            CompoundTag allDims = tag.getCompound("savedData");
            for (String dimKey : sortedKeys(allDims)) {
                CompoundTag dimTag = allDims.getCompound(dimKey);
                Map<String, CompoundTag> dimMap = new LinkedHashMap<>();
                for (String dataKey : sortedKeys(dimTag)) {
                    dimMap.put(dataKey, dimTag.getCompound(dataKey).copy());
                }
                s.savedDataSnapshots.put(dimKey, dimMap);
            }
        }
        return s;
    }

    public static ServerGlobalsSnapshot capture(MinecraftServer server, HolderLookup.Provider lookup) {
        ServerGlobalsSnapshot s = new ServerGlobalsSnapshot();
        try {
            CustomBossEvents cbe = server.getCustomBossEvents();
            if (cbe != null && !cbe.getEvents().isEmpty()) {
                s.bossbars = cbe.save(lookup);
            }
        } catch (Throwable ignored) {}

        try {
            WorldBorder.Settings settings = server.overworld().getWorldBorder().createSettings();
            CompoundTag t = new CompoundTag();
            settings.write(t);
            s.worldBorder = t;
        } catch (Throwable ignored) {}

        try {
            CompoundTag cached = ServerGlobalsCache.getCachedGameRulesTag();
            if (ServerGlobalsCache.isGameRulesDirty() || cached == null) {
                cached = server.getGameRules().createTag();
                ServerGlobalsCache.setCachedGameRulesTag(cached);
            }
            s.gameRules = cached;
        } catch (Throwable ignored) {}

        for (ServerLevel level : orderedLevels(server)) {
            try {
                ForcedChunksSavedData fc = level.getDataStorage()
                        .get(ForcedChunksSavedData.factory(), ForcedChunksSavedData.FILE_ID);
                if (fc != null && !fc.getChunks().isEmpty()) {
                    long[] forced = fc.getChunks().toLongArray();
                    Arrays.sort(forced);
                    s.forcedChunks.put(level.dimension().location().toString(), forced);
                }
            } catch (Throwable ignored) {}
        }

        for (ServerLevel level : orderedLevels(server)) {
            String dimKey = level.dimension().location().toString();
            try {
                ru.reset.rzero.access.IRZeroDimensionDataStorage ds =
                        (ru.reset.rzero.access.IRZeroDimensionDataStorage) (Object) level.getDataStorage();
                var cache = ds.rzero$getCache();
                if (cache != null && !cache.isEmpty()) {
                    Map<String, CompoundTag> dimSnaps = new LinkedHashMap<>();
                    List<String> ids = new ArrayList<>(cache.keySet());
                    ids.removeIf(id -> id.startsWith("rzero_data_"));
                    ids.sort(String::compareTo);
                    for (String id : ids) {
                        net.minecraft.world.level.saveddata.SavedData data = cache.get(id);
                        if (data != null) {
                            try {
                                CompoundTag nbt = data.save(new CompoundTag(), lookup).copy();
                                dimSnaps.put(id, nbt);
                            } catch (Throwable t) {
                                RZero.LOGGER.warn("[RZero] Failed to capture SavedData '{}' in dimension {}: {}", id, dimKey, t.getMessage());
                            }
                        }
                    }
                    if (!dimSnaps.isEmpty()) {
                        s.savedDataSnapshots.put(dimKey, dimSnaps);
                    }
                }
            } catch (Throwable ignored) {}
        }

        try {
            RandomSequences rs = server.overworld().getRandomSequences();
            boolean hasAny = false;
            if (rs != null) {
                final boolean[] flag = {false};
                rs.forAllSequences((id, seq) -> flag[0] = true);
                hasAny = flag[0];
            }
            if (hasAny) {
                CompoundTag t = new CompoundTag();
                rs.save(t, lookup);
                s.randomSequences = t;
            }
        } catch (Throwable ignored) {}

        try {
            net.minecraft.util.RandomSource rs = server.overworld().getRandom();
            if (rs instanceof ru.reset.rzero.access.IRZeroRandomState irs) {
                s.levelRandomState = irs.rzero$getState();
            }
        } catch (Throwable ignored) {}

        try {
            s.serverTickCount = ((ru.reset.rzero.mixin.level.MixinMinecraftServer)(Object) server).rzero$getTickCount();
        } catch (Throwable ignored) {}
        s.shufflingCounter = RZeroRuntime.shufflingCounter;

        return s;
    }

    public void restore(MinecraftServer server,
                        HolderLookup.Provider lookup,
                        RZeroCheckpointPolicy.ServerGlobals policy) {
        RZeroCheckpointPolicy.ServerGlobals effectivePolicy =
                policy == null ? RZeroCheckpointPolicy.ServerGlobals.defaults() : policy;

        if (effectivePolicy.bossbars() && bossbars != null) {
            try {
                CustomBossEvents cbe = server.getCustomBossEvents();
                try {
                    java.lang.reflect.Field f = CustomBossEvents.class.getDeclaredField("events");
                    f.setAccessible(true);
                    Map<?, ?> events = (Map<?, ?>) f.get(cbe);
                    for (var ev : new java.util.ArrayList<>(events.values())) {
                        try {
                            ev.getClass().getMethod("removeAllPlayers").invoke(ev);
                        } catch (Throwable ignored) {}
                    }
                    events.clear();
                } catch (Throwable ignored) {}
                cbe.load(bossbars, lookup);
            } catch (Throwable t) {
                RZero.LOGGER.warn(
                        "[RZero] Bossbar restore failed: {}", t.getMessage());
            }
        }

        if (effectivePolicy.worldBorder() && worldBorder != null) {
            try {
                WorldBorder border = server.overworld().getWorldBorder();
                Dynamic<Tag> dyn = new Dynamic<>(NbtOps.INSTANCE, worldBorder);
                WorldBorder.Settings settings = WorldBorder.Settings.read(dyn, WorldBorder.DEFAULT_SETTINGS);
                border.applySettings(settings);
            } catch (Throwable ignored) {}
        }

        if (effectivePolicy.gameRules() && gameRules != null) {
            try {
                Dynamic<Tag> dyn = new Dynamic<>(NbtOps.INSTANCE, gameRules);
                java.lang.reflect.Method m = net.minecraft.world.level.GameRules.class
                        .getDeclaredMethod("loadFromTag", com.mojang.serialization.DynamicLike.class);
                m.setAccessible(true);
                m.invoke(server.getGameRules(), dyn);
            } catch (Throwable ignored) {}
        }

        if (effectivePolicy.forcedChunks()) {
            for (ServerLevel level : orderedLevels(server)) {
                try {
                    ForcedChunksSavedData fc = level.getDataStorage()
                            .computeIfAbsent(ForcedChunksSavedData.factory(), ForcedChunksSavedData.FILE_ID);
                    LongSet currentChunks = fc.getChunks();
                    long[] saved = forcedChunks.get(level.dimension().location().toString());
                    Set<Long> savedSet = new HashSet<>();
                    if (saved != null) {
                        long[] sortedSaved = Arrays.copyOf(saved, saved.length);
                        Arrays.sort(sortedSaved);
                        for (long k : sortedSaved) savedSet.add(k);
                    }

                    long[] current = currentChunks.toLongArray();
                    Arrays.sort(current);
                    for (long k : current) {
                        if (!savedSet.contains(k)) {
                            ChunkPos cp = new ChunkPos(k);
                            level.setChunkForced(cp.x, cp.z, false);
                        }
                    }
                    if (saved != null) {
                        long[] sortedSaved = Arrays.copyOf(saved, saved.length);
                        Arrays.sort(sortedSaved);
                        for (long k : sortedSaved) {
                            if (!currentChunks.contains(k)) {
                                ChunkPos cp = new ChunkPos(k);
                                level.setChunkForced(cp.x, cp.z, true);
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }
        }

        if (effectivePolicy.savedData()) {
            for (ServerLevel level : orderedLevels(server)) {
                String dimKey = level.dimension().location().toString();
                try {
                    net.minecraft.world.level.storage.DimensionDataStorage dataStorage = level.getDataStorage();
                    ru.reset.rzero.access.IRZeroDimensionDataStorage ds =
                            (ru.reset.rzero.access.IRZeroDimensionDataStorage) (Object) dataStorage;
                    var cache = ds.rzero$getCache();
                    var factories = ds.rzero$getFactories();
                    Map<String, CompoundTag> dimSnaps = savedDataSnapshots.get(dimKey);

                    if (cache != null) {
                        if (dimSnaps == null || dimSnaps.isEmpty()) {
                            cache.keySet().removeIf(id -> !id.startsWith("rzero_data_"));
                        } else {
                            cache.keySet().removeIf(id -> !id.startsWith("rzero_data_") && !dimSnaps.containsKey(id));
                        }

                        if (dimSnaps != null) {
                            for (var entry : DetOrder.sortedEntries(dimSnaps, k -> k)) {
                                String id = entry.getKey();
                                CompoundTag tag = entry.getValue().copy();
                                @SuppressWarnings("rawtypes")
                                net.minecraft.world.level.saveddata.SavedData.Factory factory = factories != null ? factories.get(id) : null;
                                if (factory == null) {
                                    if (id.startsWith("map_")) {
                                        factory = MapItemSavedData.factory();
                                    } else if (id.equals(MapIndex.FILE_NAME)) {
                                        factory = MapIndex.factory();
                                    }
                                }
                                if (factory != null && factory.deserializer() != null) {
                                    try {
                                        @SuppressWarnings("unchecked")
                                        net.minecraft.world.level.saveddata.SavedData freshData =
                                                (net.minecraft.world.level.saveddata.SavedData) factory.deserializer().apply(tag, lookup);
                                        if (freshData != null) {
                                            dataStorage.set(id, freshData);
                                        }
                                    } catch (Throwable t) {
                                        RZero.LOGGER.warn("[RZero] Failed to restore SavedData '{}' in dimension {}: {}", id, dimKey, t.getMessage());
                                    }
                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    RZero.LOGGER.warn("[RZero] DimensionDataStorage restore failed for dimension {}: {}", dimKey, t.getMessage());
                }
            }
        }

        if (effectivePolicy.randomSequences() && randomSequences != null) {
            try {
                long seed = server.overworld().getSeed();
                RandomSequences fresh = RandomSequences.load(seed, randomSequences);
                RandomSequences live = server.overworld().getRandomSequences();
                java.lang.reflect.Field f = RandomSequences.class.getDeclaredField("sequences");
                f.setAccessible(true);
                Map liveMap = (Map) f.get(live);
                Map freshMap = (Map) f.get(fresh);
                liveMap.clear();
                liveMap.putAll(freshMap);
            } catch (Throwable ignored) {}
        }

        if (effectivePolicy.serverTickCount()) {
            try {
                ((ru.reset.rzero.mixin.level.MixinMinecraftServer)(Object) server).rzero$setTickCount(serverTickCount);
            } catch (Throwable ignored) {}
        }
        if (effectivePolicy.shufflingCounter()) {
            RZeroRuntime.shufflingCounter = shufflingCounter;
        }
    }

    private static List<String> sortedKeys(CompoundTag tag) {
        List<String> keys = new ArrayList<>(tag.getAllKeys());
        keys.sort(String::compareTo);
        return keys;
    }

    private static List<ServerLevel> orderedLevels(MinecraftServer server) {
        List<ServerLevel> levels = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            levels.add(level);
        }
        levels.sort(java.util.Comparator.comparing(level -> level.dimension().location().toString()));
        return levels;
    }
}
