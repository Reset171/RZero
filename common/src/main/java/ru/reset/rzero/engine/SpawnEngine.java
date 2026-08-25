package ru.reset.rzero.engine;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.RandomSupport;

public final class SpawnEngine {

    public static final long CREATURE_EPOCH_TICKS = 400L;
    public static final long MONSTER_EPOCH_TICKS = 24000L;
    public static final int CAP_TILE_CHUNKS = 17;

    private static final int CATEGORY_COUNT = MobCategory.values().length;

    private static final long DIM_MIX = 0x9E3779B97F4A7C15L;
    private static final long CHUNK_MIX = 0xC2B2AE3D27D4EB4FL;
    private static final long EPOCH_MIX = 0x165667B19E3779F9L;

    public static final long SALT_NATURAL_SPAWN = 0x5350_41574E_455255L;
    public static final long SALT_CATCHUP = 0x4341_544348_5550L;
    public static final long SALT_CATCHUP_MONSTER = 0x4D4F_4E43_4154_4348L;

    public record Context(long epoch, long gameTime, long chunkKey, long seed) {}

    private static final ThreadLocal<Context> CONTEXT = new ThreadLocal<>();

    private static final Long2ObjectMap<int[]> TILE_COUNTS = new Long2ObjectOpenHashMap<>();
    private static final Long2ObjectMap<int[]> CHUNK_COUNTS = new Long2ObjectOpenHashMap<>();

    private static long countsTick = Long.MIN_VALUE;
    private static Object countsLevel = null;

    private SpawnEngine() {}

    public static long epochOf(long gameTime) {
        return Math.floorDiv(gameTime, CREATURE_EPOCH_TICKS);
    }

    public static long monsterEpochOf(long gameTime) {
        return Math.floorDiv(gameTime, MONSTER_EPOCH_TICKS);
    }

    public static long derive(ServerLevel level, long chunkKey, long tick, long salt) {
        long dimension = level.dimension().location().toString().hashCode();
        long raw = level.getSeed()
                ^ (dimension * DIM_MIX)
                ^ (chunkKey * CHUNK_MIX)
                ^ (tick * EPOCH_MIX)
                ^ salt;
        return RandomSupport.mixStafford13(raw);
    }

    public static void open(Context context) {
        CONTEXT.set(context);
    }

    public static void close() {
        CONTEXT.remove();
    }

    public static Context current() {
        return CONTEXT.get();
    }

    public static int getTileCategoryCap(MobCategory category) {
        return switch (category) {
            case MONSTER -> 50;
            case CREATURE -> 4;
            case AMBIENT -> 4;
            case UNDERGROUND_WATER_CREATURE, WATER_CREATURE -> 3;
            case WATER_AMBIENT -> 6;
            case AXOLOTLS -> 3;
            case MISC -> 0;
        };
    }

    public static int getPerChunkCap(MobCategory category) {
        return switch (category) {
            case MONSTER -> 5;
            case CREATURE -> 3;
            case AMBIENT -> 2;
            case UNDERGROUND_WATER_CREATURE, WATER_CREATURE -> 2;
            case WATER_AMBIENT -> 3;
            case AXOLOTLS -> 2;
            case MISC -> 0;
        };
    }

    public static boolean canSpawnLocally(ServerLevel level, ChunkPos pos, MobCategory category) {
        if (category == MobCategory.MISC) return true;
        
        int[] tile = tileCounts(level, pos.x, pos.z);
        if (tile[category.ordinal()] >= getTileCategoryCap(category)) {
            return false;
        }

        int[] chunk = chunkCounts(level, pos.x, pos.z);
        return chunk[category.ordinal()] < getPerChunkCap(category);
    }

    public static void afterSpawn(Mob mob) {
        if (countsLevel != mob.level() || countsTick != mob.level().getGameTime()) {
            return;
        }
        MobCategory category = mob.getType().getCategory();
        if (category == MobCategory.MISC) {
            return;
        }
        int cx = mob.getBlockX() >> 4;
        int cz = mob.getBlockZ() >> 4;
        tileFor(cx, cz)[category.ordinal()]++;
        chunkFor(cx, cz)[category.ordinal()]++;
    }

    private static int[] tileCounts(ServerLevel level, int chunkX, int chunkZ) {
        long now = level.getGameTime();
        if (now != countsTick || countsLevel != level) {
            rebuild(level, now);
        }
        return tileFor(chunkX, chunkZ);
    }

    private static int[] chunkCounts(ServerLevel level, int chunkX, int chunkZ) {
        long now = level.getGameTime();
        if (now != countsTick || countsLevel != level) {
            rebuild(level, now);
        }
        return chunkFor(chunkX, chunkZ);
    }

    private static void rebuild(ServerLevel level, long now) {
        countsTick = now;
        countsLevel = level;
        TILE_COUNTS.clear();
        CHUNK_COUNTS.clear();

        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof Mob mob)) continue;
            if (mob.isRemoved()) continue;
            
            MobCategory category = entity.getType().getCategory();
            if (category == MobCategory.MISC) {
                continue;
            }
            int cx = entity.getBlockX() >> 4;
            int cz = entity.getBlockZ() >> 4;
            tileFor(cx, cz)[category.ordinal()]++;
            chunkFor(cx, cz)[category.ordinal()]++;
        }
    }

    private static int[] tileFor(int chunkX, int chunkZ) {
        long key = tileKey(chunkX, chunkZ);
        int[] counts = TILE_COUNTS.get(key);
        if (counts == null) {
            counts = new int[CATEGORY_COUNT];
            TILE_COUNTS.put(key, counts);
        }
        return counts;
    }

    private static int[] chunkFor(int chunkX, int chunkZ) {
        long key = ChunkPos.asLong(chunkX, chunkZ);
        int[] counts = CHUNK_COUNTS.get(key);
        if (counts == null) {
            counts = new int[CATEGORY_COUNT];
            CHUNK_COUNTS.put(key, counts);
        }
        return counts;
    }

    private static long tileKey(int chunkX, int chunkZ) {
        return ChunkPos.asLong(Math.floorDiv(chunkX, CAP_TILE_CHUNKS),
                               Math.floorDiv(chunkZ, CAP_TILE_CHUNKS));
    }
}