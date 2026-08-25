package ru.reset.rzero.mixin.client;

import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.reset.rzero.client.input.KeyBindings;
import ru.reset.rzero.network.LoadRequestPacket;
import ru.reset.rzero.network.SaveRequestPacket;
import ru.reset.rzero.platform.Services;

@Mixin(Screen.class)
public class MixinScreen {
    @Inject(method = "keyPressed", at = @At("RETURN"), cancellable = true)
    private void rzero$onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            return;
        }

        if (KeyBindings.SAVE_KEY.matches(keyCode, scanCode)) {
            Services.PLATFORM.sendToServer(new SaveRequestPacket());
            cir.setReturnValue(true);
        } else if (KeyBindings.LOAD_KEY.matches(keyCode, scanCode)) {
            Services.PLATFORM.sendToServer(new LoadRequestPacket());
            cir.setReturnValue(true);
        }
    }
}
