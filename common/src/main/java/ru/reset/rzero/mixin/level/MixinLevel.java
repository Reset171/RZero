package ru.reset.rzero.mixin.level;

import ru.reset.rzero.RZero;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.reset.rzero.access.IRZeroRandomState;
import ru.reset.rzero.engine.RZeroRandomMask;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomSupport;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Level.class)
public abstract class MixinLevel {
    @Shadow public RandomSource random;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void rzero$initLevel(net.minecraft.world.level.storage.WritableLevelData var1, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> var2, net.minecraft.core.RegistryAccess var3, net.minecraft.core.Holder<net.minecraft.world.level.dimension.DimensionType> var4, java.util.function.Supplier<net.minecraft.util.profiling.ProfilerFiller> var5, boolean var6, boolean var7, long var8, int var10, CallbackInfo ci) {
        if (this.random instanceof IRZeroRandomState rState) {
            rState.rzero$setIsLevelRandom(true);
            RZero.LOGGER.info("[RZero-LevelTick] Successfully set isLevelRandom=true on {}", this.random.getClass().getName());
        } else {
            RZero.LOGGER.warn("[RZero-LevelTick] Failed to set isLevelRandom. {} is not IRZeroRandomState", this.random != null ? this.random.getClass().getName() : "null");
        }
    }

    @Inject(method = "getRandom", at = @At("HEAD"), cancellable = true)
    private void rzero$maskRandom(CallbackInfoReturnable<RandomSource> cir) {
        RandomSource top = RZeroRandomMask.peek();
        if (top != null) {
            cir.setReturnValue(top);
        }
    }

    @Redirect(
            method = "tickBlockEntities",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/TickingBlockEntity;tick()V")
    )
    private void rzero$redirectTickingBlockEntityTick(TickingBlockEntity instance) {
        Level level = (Level) (Object) this;
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            long gameTime = serverLevel.getGameTime();
            long posHash = instance.getPos().asLong();
            long raw = serverLevel.getSeed() ^ gameTime ^ posHash ^ 0xBEEFCAFE1234L;
            long seed = RandomSupport.mixStafford13(raw);
            
            ru.reset.rzero.access.IRZeroServerLevel sl = (ru.reset.rzero.access.IRZeroServerLevel) serverLevel;
            sl.rzero$pushDeterministicRandom(new LegacyRandomSource(seed));
            
            try {
                instance.tick();
            } finally {
                sl.rzero$popRandom();
            }
        } else {
            instance.tick();
        }
    }
}
