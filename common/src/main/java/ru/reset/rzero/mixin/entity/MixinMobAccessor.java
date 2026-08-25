package ru.reset.rzero.mixin.entity;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Mob.class)
public interface MixinMobAccessor {
    @org.spongepowered.asm.mixin.Mutable
    @Accessor("navigation")
    void rzero$setNavigation(PathNavigation navigation);

    @Accessor("goalSelector")
    GoalSelector rzero$getGoalSelector();

    @org.spongepowered.asm.mixin.Mutable
    @Accessor("goalSelector")
    void rzero$setGoalSelector(GoalSelector selector);

    @Accessor("targetSelector")
    GoalSelector rzero$getTargetSelector();

    @org.spongepowered.asm.mixin.Mutable
    @Accessor("targetSelector")
    void rzero$setTargetSelector(GoalSelector selector);
}
