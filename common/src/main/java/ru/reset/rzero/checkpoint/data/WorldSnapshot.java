package ru.reset.rzero.checkpoint.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;

import java.util.Arrays;
import java.util.Optional;

public class WorldSnapshot {
    private static final Codec<long[]> RNG_STATE_CODEC = Codec.LONG.listOf().xmap(
            list -> list.stream().mapToLong(Long::longValue).toArray(),
            arr -> Arrays.stream(arr).boxed().toList());

    public static final Codec<WorldSnapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.fieldOf("dayTime").forGetter(w -> w.dayTime),
        Codec.LONG.fieldOf("gameTime").forGetter(w -> w.gameTime),
        Codec.BOOL.fieldOf("isRaining").forGetter(w -> w.isRaining),
        Codec.BOOL.fieldOf("isThundering").forGetter(w -> w.isThundering),
        Codec.INT.fieldOf("rainTime").forGetter(w -> w.rainTime),
        Codec.INT.fieldOf("thunderTime").forGetter(w -> w.thunderTime),
        Codec.INT.fieldOf("clearWeatherTime").forGetter(w -> w.clearWeatherTime),
        RNG_STATE_CODEC.optionalFieldOf("rngState").forGetter(w -> Optional.ofNullable(w.rngState)),
        Codec.STRING.optionalFieldOf("difficulty", "normal").forGetter(w -> w.difficulty != null ? w.difficulty : "normal"),
        Codec.BOOL.optionalFieldOf("difficultyLocked", false).forGetter(w -> w.difficultyLocked),
        Codec.INT.optionalFieldOf("spawnX", 0).forGetter(w -> w.spawnX),
        Codec.INT.optionalFieldOf("spawnY", 0).forGetter(w -> w.spawnY),
        Codec.INT.optionalFieldOf("spawnZ", 0).forGetter(w -> w.spawnZ),
        Codec.FLOAT.optionalFieldOf("spawnAngle", 0f).forGetter(w -> w.spawnAngle),
        Codec.BOOL.optionalFieldOf("hasWorldSpawn", false).forGetter(w -> w.hasWorldSpawn)
    ).apply(instance, (d, g, r, t, rt, tt, cwt, rng, diff, locked, sx, sy, sz, sa, hasWs) -> {
        WorldSnapshot ws = new WorldSnapshot();
        ws.dayTime = d; ws.gameTime = g; ws.isRaining = r; ws.isThundering = t;
        ws.rainTime = rt; ws.thunderTime = tt; ws.clearWeatherTime = cwt;
        ws.rngState = rng.orElse(null);
        ws.difficulty = diff;
        ws.difficultyLocked = locked;
        ws.spawnX = sx;
        ws.spawnY = sy;
        ws.spawnZ = sz;
        ws.spawnAngle = sa;
        ws.hasWorldSpawn = hasWs;
        return ws;
    }));
    public long dayTime;
    public long gameTime;
    public boolean isRaining;
    public boolean isThundering;
    public int rainTime;
    public int thunderTime;
    public int clearWeatherTime;
    public long[] rngState;
    public String difficulty = "normal";
    public boolean difficultyLocked = false;
    public int spawnX;
    public int spawnY;
    public int spawnZ;
    public float spawnAngle;
    public boolean hasWorldSpawn;

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("dayTime", dayTime);
        tag.putLong("gameTime", gameTime);
        tag.putBoolean("isRaining", isRaining);
        tag.putBoolean("isThundering", isThundering);
        tag.putInt("rainTime", rainTime);
        tag.putInt("thunderTime", thunderTime);
        tag.putInt("clearWeatherTime", clearWeatherTime);
        if (rngState != null) {
            tag.putLongArray("rngState", rngState);
        }
        if (difficulty != null) {
            tag.putString("difficulty", difficulty);
        }
        tag.putBoolean("difficultyLocked", difficultyLocked);
        if (hasWorldSpawn) {
            tag.putInt("spawnX", spawnX);
            tag.putInt("spawnY", spawnY);
            tag.putInt("spawnZ", spawnZ);
            tag.putFloat("spawnAngle", spawnAngle);
            tag.putBoolean("hasWorldSpawn", true);
        }
        return tag;
    }

    public static WorldSnapshot fromNBT(CompoundTag tag) {
        WorldSnapshot ws = new WorldSnapshot();
        ws.dayTime = tag.getLong("dayTime");
        ws.gameTime = tag.getLong("gameTime");
        ws.isRaining = tag.getBoolean("isRaining");
        ws.isThundering = tag.getBoolean("isThundering");
        ws.rainTime = tag.getInt("rainTime");
        ws.thunderTime = tag.getInt("thunderTime");
        ws.clearWeatherTime = tag.getInt("clearWeatherTime");
        if (tag.contains("rngState")) {
            ws.rngState = tag.getLongArray("rngState");
        }
        if (tag.contains("difficulty")) {
            ws.difficulty = tag.getString("difficulty");
        }
        ws.difficultyLocked = tag.getBoolean("difficultyLocked");
        if (tag.getBoolean("hasWorldSpawn")) {
            ws.spawnX = tag.getInt("spawnX");
            ws.spawnY = tag.getInt("spawnY");
            ws.spawnZ = tag.getInt("spawnZ");
            ws.spawnAngle = tag.getFloat("spawnAngle");
            ws.hasWorldSpawn = true;
        }
        return ws;
    }
}
