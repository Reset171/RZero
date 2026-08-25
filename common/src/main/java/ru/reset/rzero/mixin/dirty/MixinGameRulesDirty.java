package ru.reset.rzero.mixin.dirty;

import com.mojang.serialization.DynamicLike;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.reset.rzero.checkpoint.data.ServerGlobalsSnapshot;

@Mixin(GameRules.class)
public abstract class MixinGameRulesDirty {
    @Inject(method = "loadFromTag", at = @At("HEAD"))
    private void rzero$loadFromTagHead(DynamicLike<?> dyn, CallbackInfo ci) {
        ServerGlobalsSnapshot.markGameRulesDirty();
    }
}
