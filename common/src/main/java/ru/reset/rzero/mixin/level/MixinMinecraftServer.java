package ru.reset.rzero.mixin.level;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftServer.class)
public interface MixinMinecraftServer {
    @Accessor("tickCount")
    int rzero$getTickCount();

    @Accessor("tickCount")
    void rzero$setTickCount(int value);
}
