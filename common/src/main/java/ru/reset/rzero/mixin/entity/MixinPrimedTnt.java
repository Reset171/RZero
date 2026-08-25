package ru.reset.rzero.mixin.entity;

import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PrimedTnt.class)
public abstract class MixinPrimedTnt {

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/entity/LivingEntity;)V", at = @At("RETURN"))
    private void onInit(Level level, double x, double y, double z, LivingEntity owner, CallbackInfo ci) {
        long worldSeed = (level instanceof ServerLevel sl) ? sl.getSeed() : 0L;
        long seed = ((long) Math.floor(x) * 3129871) ^ ((long) Math.floor(y) * 116129781L) ^ ((long) Math.floor(z)) ^ worldSeed;
        RandomSource deterministicRandom = new LegacyRandomSource(seed);
        double d = deterministicRandom.nextDouble() * (Math.PI * 2.0);
        ((PrimedTnt) (Object) this).setDeltaMovement(-Math.sin(d) * 0.02D, 0.2D, -Math.cos(d) * 0.02D);
    }
}