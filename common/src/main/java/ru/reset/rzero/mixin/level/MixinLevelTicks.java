package ru.reset.rzero.mixin.level;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import ru.reset.rzero.access.IRZeroTickAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Mixin(LevelTicks.class)
public abstract class MixinLevelTicks<T> implements IRZeroTickAccess<T> {
    @Shadow @Final private Long2ObjectMap<LevelChunkTicks<T>> allContainers;

    @Override
    public List<ScheduledTick<T>> rzero$getTicksInChunk(long chunkPos) {
        LevelChunkTicks<T> chunkTicks = this.allContainers.get(chunkPos);
        if (chunkTicks != null) {
            return chunkTicks.getAll().collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}