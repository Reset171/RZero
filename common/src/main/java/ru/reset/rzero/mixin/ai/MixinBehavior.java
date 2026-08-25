package ru.reset.rzero.mixin.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.reset.rzero.runtime.RZeroRuntime;

@Mixin(Behavior.class)
public abstract class MixinBehavior {

    @Redirect(
        method = "tryStart",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getRandom()Lnet/minecraft/util/RandomSource;")
    )
    private RandomSource rzero$redirectBehaviorRandom(ServerLevel level, ServerLevel var1, LivingEntity var2, long var3) {
        if (!RZeroRuntime.checkpointPolicy().determinism().mobAi().behaviorDurationRng()) {
            return level.getRandom();
        }
        return var2.getRandom();
    }
}
