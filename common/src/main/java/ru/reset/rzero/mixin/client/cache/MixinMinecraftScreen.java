package ru.reset.rzero.mixin.client.cache;

import ru.reset.rzero.RZero;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ru.reset.rzero.RZero;
import ru.reset.rzero.client.cache.RZeroClientCache;
import ru.reset.rzero.runtime.RZeroRuntime;

@Mixin(Minecraft.class)
public abstract class MixinMinecraftScreen {

    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen rzero$suppressReceivingLevelScreen(Screen screen) {
        if (screen instanceof ReceivingLevelScreen
                && RZeroRuntime.clientRestore().suppressTerrainLoadingScreen()
                && RZeroClientCache.get().isInterDimensionalRollback()) {
            RZero.logInfo("[RZero][cache] Suppressed ReceivingLevelScreen during rollback");
            return null;
        }
        return screen;
    }
}
