package ru.reset.rzero.mixin.client.cache;

import net.minecraft.client.renderer.ViewArea;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.reset.rzero.client.cache.RZeroClientCache;
import ru.reset.rzero.client.cache.mesh.RZeroMeshCache;

@Mixin(ViewArea.class)
public abstract class MixinViewAreaReposition {

    @Inject(method = "repositionCamera", at = @At("TAIL"))
    private void rzero$onCameraRepositioned(double x, double z, CallbackInfo ci) {
        RZeroClientCache cache = RZeroClientCache.get();
        if (cache.isInRollback() && cache.isPendingSectionRefresh()) {
            cache.tickPendingRefresh();
        }
    }
}
