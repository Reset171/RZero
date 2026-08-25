package ru.reset.rzero.mixin.player;

import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

@Mixin(Player.class)
public interface MixinPlayer {
    @Accessor("sleepCounter")
    int rzero$getSleepCounter();

    @Accessor("sleepCounter")
    void rzero$setSleepCounter(int value);

    @Accessor("enchantmentSeed")
    int rzero$getEnchantmentSeed();

    @Accessor("enchantmentSeed")
    void rzero$setEnchantmentSeed(int value);

    @Accessor("abilities")
    Abilities rzero$getAbilities();

    @Accessor("cooldowns")
    ItemCooldowns rzero$getCooldowns();

    @Accessor("lastDeathLocation")
    Optional<GlobalPos> rzero$getLastDeathLocation();

    @Accessor("lastDeathLocation")
    void rzero$setLastDeathLocation(Optional<GlobalPos> value);

    @Accessor("currentImpulseImpactPos")
    Vec3 rzero$getCurrentImpulseImpactPos();

    @Accessor("currentImpulseImpactPos")
    void rzero$setCurrentImpulseImpactPos(Vec3 value);

    @Accessor("ignoreFallDamageFromCurrentImpulse")
    boolean rzero$getIgnoreFallDamageFromCurrentImpulse();

    @Accessor("ignoreFallDamageFromCurrentImpulse")
    void rzero$setIgnoreFallDamageFromCurrentImpulse(boolean value);

    @Accessor("currentImpulseContextResetGraceTime")
    int rzero$getCurrentImpulseContextResetGraceTime();

    @Accessor("currentImpulseContextResetGraceTime")
    void rzero$setCurrentImpulseContextResetGraceTime(int value);

    @org.spongepowered.asm.mixin.gen.Invoker("setShoulderEntityLeft")
    void rzero$setShoulderEntityLeft(net.minecraft.nbt.CompoundTag tag);

    @org.spongepowered.asm.mixin.gen.Invoker("setShoulderEntityRight")
    void rzero$setShoulderEntityRight(net.minecraft.nbt.CompoundTag tag);
}
