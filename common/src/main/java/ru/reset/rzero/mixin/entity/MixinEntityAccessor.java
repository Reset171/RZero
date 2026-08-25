package ru.reset.rzero.mixin.entity;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface MixinEntityAccessor {
    @Accessor("portalCooldown")
    int rzero$getPortalCooldown();

    @Accessor("portalCooldown")
    void rzero$setPortalCooldown(int value);

    @Accessor("firstTick")
    boolean rzero$getFirstTick();

    @Accessor("firstTick")
    void rzero$setFirstTick(boolean value);

    @org.spongepowered.asm.mixin.gen.Invoker("unsetRemoved")
    void rzero$unsetRemoved();
}
