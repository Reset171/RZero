package ru.reset.rzero.mixin.dirty;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.reset.rzero.checkpoint.data.ServerGlobalsSnapshot;

@Mixin(GameRules.Value.class)
public abstract class MixinGameRulesValueDirty {
    @Inject(method = "onChanged", at = @At("HEAD"))
    private void rzero$onChangedHead(MinecraftServer server, CallbackInfo ci) {
        ServerGlobalsSnapshot.markGameRulesDirty();
    }
}
