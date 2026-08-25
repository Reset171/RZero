package ru.reset.rzero.mixin.client.cache;

import net.minecraft.client.multiplayer.LevelLoadStatusManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.reset.rzero.client.cache.RZeroClientCache;
import ru.reset.rzero.runtime.RZeroRuntime;

@Mixin(LevelLoadStatusManager.class)
public abstract class MixinLevelLoadStatusManager {

    @Inject(method = "levelReady", at = @At("HEAD"), cancellable = true)
    private void rzero$forceReadyDuringRollback(CallbackInfoReturnable<Boolean> cir) {
        if (RZeroRuntime.clientRestore().suppressTerrainLoadingScreen()
                && (RZeroClientCache.get().isInterDimensionalRollback() || RZeroClientCache.get().isInRollback())) {
            cir.setReturnValue(true);
        }
    }
}
