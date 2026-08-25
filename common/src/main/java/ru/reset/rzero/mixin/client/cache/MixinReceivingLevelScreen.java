package ru.reset.rzero.mixin.client.cache;

import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.reset.rzero.client.cache.RZeroClientCache;
import ru.reset.rzero.runtime.RZeroRuntime;

@Mixin(ReceivingLevelScreen.class)
public abstract class MixinReceivingLevelScreen extends Screen {

    protected MixinReceivingLevelScreen(Component title) {
        super(title);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void rzero$closeIfSeamlessRollback(CallbackInfo ci) {
        if (RZeroRuntime.clientRestore().suppressTerrainLoadingScreen() && RZeroClientCache.get().isInterDimensionalRollback()) {
            this.onClose();
        }
    }
}
