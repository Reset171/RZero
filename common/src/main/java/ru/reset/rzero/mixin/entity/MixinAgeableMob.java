package ru.reset.rzero.mixin.entity;

import net.minecraft.world.entity.AgeableMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AgeableMob.class)
public interface MixinAgeableMob {
    @Accessor("forcedAgeTimer")
    int rzero$getForcedAgeTimer();

    @Accessor("forcedAgeTimer")
    void rzero$setForcedAgeTimer(int value);
}
