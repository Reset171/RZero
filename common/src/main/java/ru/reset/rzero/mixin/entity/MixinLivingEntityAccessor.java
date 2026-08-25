package ru.reset.rzero.mixin.entity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface MixinLivingEntityAccessor {
    @Accessor("hurtTime")
    int rzero$getHurtTime();
    @Accessor("hurtTime")
    void rzero$setHurtTime(int value);

    @Accessor("lastHurt")
    float rzero$getLastHurt();
    @Accessor("lastHurt")
    void rzero$setLastHurt(float value);

    @Accessor("attackStrengthTicker")
    int rzero$getAttackStrengthTicker();
    @Accessor("attackStrengthTicker")
    void rzero$setAttackStrengthTicker(int value);

    @Accessor("noActionTime")
    int rzero$getNoActionTime();
    @Accessor("noActionTime")
    void rzero$setNoActionTime(int value);

    @Accessor("deathTime")
    int rzero$getDeathTime();
    @Accessor("deathTime")
    void rzero$setDeathTime(int value);

    @Accessor("swingTime")
    int rzero$getSwingTime();
    @Accessor("swingTime")
    void rzero$setSwingTime(int value);

    @Accessor("swinging")
    boolean rzero$getSwinging();
    @Accessor("swinging")
    void rzero$setSwinging(boolean value);

    @Accessor("swingingArm")
    InteractionHand rzero$getSwingingArm();
    @Accessor("swingingArm")
    void rzero$setSwingingArm(InteractionHand value);

    @Accessor("noJumpDelay")
    int rzero$getNoJumpDelay();
    @Accessor("noJumpDelay")
    void rzero$setNoJumpDelay(int value);

    @Accessor("useItem")
    ItemStack rzero$getUseItem();
    @Accessor("useItem")
    void rzero$setUseItem(ItemStack value);

    @Accessor("useItemRemaining")
    int rzero$getUseItemRemaining();
    @Accessor("useItemRemaining")
    void rzero$setUseItemRemaining(int value);

    @Accessor("lastHurtByMobTimestamp")
    int rzero$getLastHurtByMobTimestamp();
    @Accessor("lastHurtByMobTimestamp")
    void rzero$setLastHurtByMobTimestamp(int value);

    @org.spongepowered.asm.mixin.Mutable
    @Accessor("brain")
    void rzero$setBrain(net.minecraft.world.entity.ai.Brain<?> brain);

    @Accessor("dead")
    boolean rzero$getDead();
    @Accessor("dead")
    void rzero$setDead(boolean value);
}
