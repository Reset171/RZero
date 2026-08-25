package ru.reset.rzero.checkpoint.codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickPriority;
import ru.reset.rzero.mixin.level.MixinLevelTicksSchedule;

import java.util.List;

public final class ScheduledTickCodec {

    private static final String KEY_POS = "pos";
    private static final String KEY_ID = "id";
    private static final String KEY_DELAY = "delay";
    private static final String KEY_SUB = "sub";
    private static final String KEY_PRIORITY = "priority";
    private static final String KEY_PRIORITY_LEGACY = "prio";

    private ScheduledTickCodec() {
    }

    public static <T> ListTag save(List<ScheduledTick<T>> ticks, Registry<T> registry, long gameTime) {
        ListTag list = new ListTag();
        for (ScheduledTick<T> tick : ticks) {
            CompoundTag tag = new CompoundTag();
            tag.putLong(KEY_POS, tick.pos().asLong());
            tag.putString(KEY_ID, registry.getKey(tick.type()).toString());
            tag.putLong(KEY_DELAY, tick.triggerTick() - gameTime);
            tag.putString(KEY_PRIORITY, tick.priority().name());
            tag.putLong(KEY_SUB, tick.subTickOrder());
            list.add(tag);
        }
        return list;
    }

    public static <T> void load(ListTag list,
                                Registry<T> registry,
                                long gameTime,
                                MixinLevelTicksSchedule<T> sink) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            BlockPos pos = BlockPos.of(tag.getLong(KEY_POS));
            T type = registry.get(ResourceLocation.parse(tag.getString(KEY_ID)));
            int delay = (int) tag.getLong(KEY_DELAY);
            long sub = tag.getLong(KEY_SUB);
            sink.rzero$schedule(
                    new ScheduledTick<>(type, pos, gameTime + delay, readPriority(tag), sub));
        }
    }

    private static TickPriority readPriority(CompoundTag tag) {
        if (tag.contains(KEY_PRIORITY_LEGACY, 99)) {
            int value = tag.getInt(KEY_PRIORITY_LEGACY);
            for (TickPriority p : TickPriority.values()) {
                if (p.getValue() == value) {
                    return p;
                }
            }
            return TickPriority.NORMAL;
        }
        String name = tag.getString(KEY_PRIORITY);
        if (name.isEmpty()) {
            return TickPriority.NORMAL;
        }
        try {
            return TickPriority.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return TickPriority.NORMAL;
        }
    }
}
