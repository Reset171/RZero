package ru.reset.rzero.mixin.level;

import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LevelTicks.class)
public interface MixinLevelTicksSchedule<T> {
    @Invoker("schedule")
    void rzero$schedule(ScheduledTick<T> tick);
}
