package ru.reset.rzero.mixin.ai;

import ru.reset.rzero.runtime.RZeroRuntime;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.behavior.ShufflingList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ShufflingList.class)
public class MixinShufflingList {

    @org.spongepowered.asm.mixin.Mutable
    @Shadow
    @Final
    private RandomSource random;

    @org.spongepowered.asm.mixin.injection.Inject(method = "<init>*", at = @At("RETURN"))
    private void rzero$replaceRandom(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (!RZeroRuntime.checkpointPolicy().determinism().mobAi().shufflingListRng()) {
            return;
        }
        this.random.setSeed(RZeroRuntime.shufflingCounter++);
    }
}
