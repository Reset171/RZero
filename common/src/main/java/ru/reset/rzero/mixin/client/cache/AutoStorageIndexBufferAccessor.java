package ru.reset.rzero.mixin.client.cache;

import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderSystem.AutoStorageIndexBuffer.class)
public interface AutoStorageIndexBufferAccessor {
    @Accessor("name")
    int rzero$getName();
}