package ru.reset.rzero.mixin.level;

import com.google.common.collect.Table;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.timers.TimerQueue;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import ru.reset.rzero.access.IRZeroTimerQueue;

import java.util.Queue;

@Mixin(TimerQueue.class)
public abstract class MixinTimerQueue<T> implements IRZeroTimerQueue {
    @Shadow @Final private Queue<?> queue;
    @Shadow @Final private Table<?, ?, ?> events;

    @Invoker("loadEvent")
    protected abstract void rzero$loadEvent(CompoundTag tag);

    @Override
    public void rzero$restore(ListTag list) {
        this.queue.clear();
        this.events.clear();
        for (int i = 0; i < list.size(); i++) {
            this.rzero$loadEvent(list.getCompound(i));
        }
    }
}