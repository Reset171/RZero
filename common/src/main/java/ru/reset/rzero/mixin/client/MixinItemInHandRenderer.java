package ru.reset.rzero.mixin.client;

import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.reset.rzero.runtime.RZeroRuntime;

@Mixin(ItemInHandRenderer.class)
public class MixinItemInHandRenderer {
    @Inject(method = "tick", at = @At("RETURN"))
    private void rzero$onTickReturn(CallbackInfo ci) {
        if (!RZeroRuntime.clientRestore().skipEquipAnimation()) {
            return;
        }
        if (ru.reset.rzero.RZeroClient.skipEquipAnimationTicks > 0) {
            ru.reset.rzero.RZeroClient.skipEquipAnimationTicks--;
            ItemInHandRenderer renderer = (ItemInHandRenderer) (Object) this;
            ItemInHandRendererAccessor accessor = (ItemInHandRendererAccessor) renderer;
            net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
            if (client.player != null) {
                accessor.rzero$setMainHandItem(client.player.getMainHandItem());
                accessor.rzero$setOffHandItem(client.player.getOffhandItem());
            }
            accessor.rzero$setMainHandHeight(1.0f);
            accessor.rzero$setOMainHandHeight(1.0f);
            accessor.rzero$setOffHandHeight(1.0f);
            accessor.rzero$setOOffHandHeight(1.0f);
        }
    }
}
