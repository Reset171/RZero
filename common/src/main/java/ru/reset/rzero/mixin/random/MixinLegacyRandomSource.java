package ru.reset.rzero.mixin.random;

import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.MarsagliaPolarGaussian;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import ru.reset.rzero.access.IRZeroRandomState;

import java.util.concurrent.atomic.AtomicLong;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LegacyRandomSource.class)
public abstract class MixinLegacyRandomSource implements IRZeroRandomState {
    @Shadow @Final private AtomicLong seed;

    @Shadow @Final private MarsagliaPolarGaussian gaussianSource;

    @Override
    public long[] rzero$getState() {
        MarsagliaPolarGaussianAccessor accessor = (MarsagliaPolarGaussianAccessor) (Object) this.gaussianSource;
        boolean have = accessor.rzero$getHaveNextNextGaussian();
        double next = accessor.rzero$getNextNextGaussian();
        return new long[]{this.seed.get(), have ? 1L : 0L, Double.doubleToRawLongBits(next)};
    }

    @Override
    public void rzero$setState(long[] state) {
        if (state != null && state.length > 0) {
            this.seed.set(state[0]);
            if (state.length >= 3) {
                MarsagliaPolarGaussianAccessor accessor = (MarsagliaPolarGaussianAccessor) (Object) this.gaussianSource;
                accessor.rzero$setHaveNextNextGaussian(state[1] == 1L);
                accessor.rzero$setNextNextGaussian(Double.longBitsToDouble(state[2]));
            }
        }
    }

    private boolean isLevelRandom = false;

    @Override
    public void rzero$setIsLevelRandom(boolean isLevelRandom) {
        this.isLevelRandom = isLevelRandom;
    }

    @Override
    public boolean rzero$isLevelRandom() {
        return this.isLevelRandom;
    }

}