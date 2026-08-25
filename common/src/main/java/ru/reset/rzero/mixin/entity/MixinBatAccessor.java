package ru.reset.rzero.mixin.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ambient.Bat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.jetbrains.annotations.Nullable;

@Mixin(Bat.class)
public interface MixinBatAccessor {
    @Accessor("targetPosition")
    @Nullable
    BlockPos rzero$getTargetPosition();

    @Accessor("targetPosition")
    void rzero$setTargetPosition(@Nullable BlockPos pos);
}
