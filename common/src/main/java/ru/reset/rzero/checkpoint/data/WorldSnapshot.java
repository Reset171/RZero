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
        RNG_STATE_CODEC.optionalFieldOf("rngState").forGetter(w -> Optional.ofNullable(w.rngState))
    ).apply(instance, (d, g, r, t, rt, tt, cwt, rng) -> {
        WorldSnapshot ws = new WorldSnapshot();
        ws.dayTime = d; ws.gameTime = g; ws.isRaining = r; ws.isThundering = t;
        ws.rainTime = rt; ws.thunderTime = tt; ws.clearWeatherTime = cwt;
        ws.rngState = rng.orElse(null);
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
        return ws;
    }
}
