package ru.reset.rzero.mixin.client;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.reset.rzero.runtime.RZeroRuntime;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer {
    @Inject(method = "tick", at = @At("RETURN"))
    private void rzero$onTickReturn(CallbackInfo ci) {
        if (!RZeroRuntime.clientRestore().snapPlayerRotation()) {
            return;
        }
        if (ru.reset.rzero.RZeroClient.snapPlayerRotationTicks > 0) {
            ru.reset.rzero.RZeroClient.snapPlayerRotationTicks--;
            LocalPlayer player = (LocalPlayer) (Object) this;
            player.yHeadRotO = player.yHeadRot;
            player.yBodyRotO = player.yBodyRot;
            player.xRotO = player.getXRot();
            player.yRotO = player.getYRot();
        }
    }
}
