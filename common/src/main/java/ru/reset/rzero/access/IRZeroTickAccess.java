package ru.reset.rzero.access;

import net.minecraft.world.ticks.ScheduledTick;
import java.util.List;

public interface IRZeroTickAccess<T> {
    List<ScheduledTick<T>> rzero$getTicksInChunk(long chunkPos);
}