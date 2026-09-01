package ru.reset.rzero.mixin.world;

import ru.reset.rzero.runtime.RZeroRuntime;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.reset.rzero.RZero;

@Mixin(Explosion.class)
public abstract class MixinExplosion {
    @Shadow @Final private ObjectArrayList<BlockPos> toBlow;
    @Shadow @Final private Level level;
    @Shadow @Final private double x;
    @Shadow @Final private double y;
    @Shadow @Final private double z;
    @Shadow @Final private RandomSource random;

    @Inject(method = "explode", at = @At("HEAD"))
    private void onExplode(CallbackInfo ci) {
        long worldSeed = (this.level instanceof ServerLevel sl) ? sl.getSeed() : 0L;
        long seed = ((long) Math.floor(this.x) * 3129871) ^ ((long) Math.floor(this.y) * 116129781L) ^ ((long) Math.floor(this.z)) ^ worldSeed;
        this.random.setSeed(seed);
    }

    @Redirect(method = "explode", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/Level;random:Lnet/minecraft/util/RandomSource;"))
    private RandomSource redirectLevelRandomAccess(Level instance) {
        return this.random;
    }

    @Inject(method = "finalizeExplosion", at = @At("HEAD"), cancellable = true)
    private void onFinalizeExplosion(boolean spawnParticles, CallbackInfo ci) {
        if (RZeroRuntime.wasRestoredThisTick) {
            this.toBlow.clear();
            ci.cancel();
        }
    }
}