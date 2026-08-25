package ru.reset.rzero.mixin.random;

import net.minecraft.world.level.levelgen.MarsagliaPolarGaussian;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MarsagliaPolarGaussian.class)
public interface MarsagliaPolarGaussianAccessor {
    @Accessor("haveNextNextGaussian")
    boolean rzero$getHaveNextNextGaussian();

    @Accessor("haveNextNextGaussian")
    void rzero$setHaveNextNextGaussian(boolean haveNextNextGaussian);

    @Accessor("nextNextGaussian")
    double rzero$getNextNextGaussian();

    @Accessor("nextNextGaussian")
    void rzero$setNextNextGaussian(double nextNextGaussian);
}
