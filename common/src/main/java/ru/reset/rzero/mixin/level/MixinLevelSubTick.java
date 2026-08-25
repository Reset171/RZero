package ru.reset.rzero.mixin.level;

import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Level.class)
public interface MixinLevelSubTick {
    @Accessor("subTickCount") long rzero$getSubTickCount();

    @Accessor("subTickCount") void rzero$setSubTickCount(long v);
}
