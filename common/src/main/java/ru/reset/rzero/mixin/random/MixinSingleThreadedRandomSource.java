package ru.reset.rzero.mixin.random;

import net.minecraft.world.level.levelgen.MarsagliaPolarGaussian;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import ru.reset.rzero.access.IRZeroRandomState;

@Mixin(SingleThreadedRandomSource.class)
public abstract class MixinSingleThreadedRandomSource implements IRZeroRandomState {
    @Shadow private long seed;
    @Shadow @Final private MarsagliaPolarGaussian gaussianSource;
    private boolean isLevelRandom = false;

    @Override
    public long[] rzero$getState() {
        MarsagliaPolarGaussianAccessor accessor = (MarsagliaPolarGaussianAccessor) (Object) this.gaussianSource;
        boolean have = accessor.rzero$getHaveNextNextGaussian();
        double next = accessor.rzero$getNextNextGaussian();
        return new long[]{this.seed, have ? 1L : 0L, Double.doubleToRawLongBits(next)};
    }

    @Override
    public void rzero$setState(long[] state) {
        if (state != null && state.length > 0) {
            this.seed = state[0];
            if (state.length >= 3) {
                MarsagliaPolarGaussianAccessor accessor = (MarsagliaPolarGaussianAccessor) (Object) this.gaussianSource;
                accessor.rzero$setHaveNextNextGaussian(state[1] == 1L);
                accessor.rzero$setNextNextGaussian(Double.longBitsToDouble(state[2]));
            }
        }
    }

    @Override
    public void rzero$setIsLevelRandom(boolean isLevelRandom) {
        this.isLevelRandom = isLevelRandom;
    }

    @Override
    public boolean rzero$isLevelRandom() {
        return this.isLevelRandom;
    }
}
