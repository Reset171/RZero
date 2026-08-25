package ru.reset.rzero.mixin.client;

import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemInHandRenderer.class)
public interface ItemInHandRendererAccessor {
    @Accessor("mainHandItem")
    void rzero$setMainHandItem(ItemStack stack);

    @Accessor("offHandItem")
    void rzero$setOffHandItem(ItemStack stack);

    @Accessor("mainHandHeight")
    void rzero$setMainHandHeight(float height);

    @Accessor("offHandHeight")
    void rzero$setOffHandHeight(float height);

    @Accessor("oMainHandHeight")
    void rzero$setOMainHandHeight(float height);

    @Accessor("oOffHandHeight")
    void rzero$setOOffHandHeight(float height);
}