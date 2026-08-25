package ru.reset.rzero.mixin.entity;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ItemCooldowns.class)
public interface MixinItemCooldowns {
    @Accessor("cooldowns")
    Map<Item, ?> rzero$getCooldowns();

    @Accessor("tickCount")
    int rzero$getTickCount();

    @Accessor("tickCount")
    void rzero$setTickCount(int value);
}
