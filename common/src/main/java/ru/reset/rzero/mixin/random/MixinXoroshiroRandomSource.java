package ru.reset.rzero.mixin.random;

import net.minecraft.world.level.levelgen.Xoroshiro128PlusPlus;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import ru.reset.rzero.access.IRZeroRandomState;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(XoroshiroRandomSource.class)
public abstract class MixinXoroshiroRandomSource implements IRZeroRandomState {
    @Shadow private Xoroshiro128PlusPlus randomNumberGenerator;

    @Override
    public long[] rzero$getState() {
        Xoroshiro128PlusPlusAccessor accessor = (Xoroshiro128PlusPlusAccessor) (Object) this.randomNumberGenerator;
        return new long[]{accessor.rzero$getSeedLo(), accessor.rzero$getSeedHi()};
    }

    @Override
    public void rzero$setState(long[] state) {
        if (state != null && state.length >= 2) {
            Xoroshiro128PlusPlusAccessor accessor = (Xoroshiro128PlusPlusAccessor) (Object) this.randomNumberGenerator;
            accessor.rzero$setSeedLo(state[0]);
            accessor.rzero$setSeedHi(state[1]);
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