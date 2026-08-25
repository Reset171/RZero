package ru.reset.rzero.mixin.entity;

import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MixinMob {
    @Inject(method = "serverAiStep", at = @At("HEAD"))
    private void rzero$pushRandomMask(CallbackInfo ci) {
        net.minecraft.world.entity.Mob mob = (Mob)(Object)this;
        ru.reset.rzero.engine.RZeroRandomMask.push(mob.level(), mob.getRandom(), mob.getRandom());
    }

    @Inject(method = "serverAiStep", at = @At("RETURN"))
    private void rzero$popRandomMask(CallbackInfo ci) {
        net.minecraft.world.entity.Mob mob = (Mob)(Object)this;
        ru.reset.rzero.engine.RZeroRandomMask.pop(mob.level());
    }
}
