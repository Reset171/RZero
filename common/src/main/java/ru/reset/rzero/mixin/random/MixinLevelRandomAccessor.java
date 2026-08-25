package ru.reset.rzero.mixin.random;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.Mutable;

@Mixin(Level.class)
public interface MixinLevelRandomAccessor {
    @Mutable
    @Accessor("random")
    void rzero$setRandom(RandomSource random);
}
