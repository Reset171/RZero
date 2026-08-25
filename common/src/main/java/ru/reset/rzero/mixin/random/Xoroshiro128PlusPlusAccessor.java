package ru.reset.rzero.mixin.random;

import net.minecraft.world.level.levelgen.Xoroshiro128PlusPlus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Xoroshiro128PlusPlus.class)
public interface Xoroshiro128PlusPlusAccessor {
    @Accessor("seedLo")
    long rzero$getSeedLo();

    @Accessor("seedHi")
    long rzero$getSeedHi();

    @Accessor("seedLo")
    void rzero$setSeedLo(long seedLo);

    @Accessor("seedHi")
    void rzero$setSeedHi(long seedHi);
}