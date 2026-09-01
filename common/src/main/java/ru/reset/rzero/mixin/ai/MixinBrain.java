package ru.reset.rzero.mixin.ai;

import ru.reset.rzero.RZero;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.reset.rzero.engine.BrainRngContext;
import ru.reset.rzero.engine.DeterministicRandomSource;
import ru.reset.rzero.engine.RZeroRandomMask;
import ru.reset.rzero.runtime.RZeroRuntime;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Optional;

@Mixin(Brain.class)
public abstract class MixinBrain {

    @Unique
    private static final ThreadLocal<DeterministicRandomSource> rzero$THREAD_BRAIN_RANDOM =
            ThreadLocal.withInitial(() -> new DeterministicRandomSource(0L));

    @Redirect(
            method = "<init>*",
            at = @At(value = "INVOKE",
                    target = "Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;",
                    ordinal = 0,
                    remap = false),
            require = 0, allow = 1
    )
    private HashMap<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> rzero$replaceMemoriesMap() {
        if (!RZeroRuntime.checkpointPolicy().determinism().mobAi().memoryIterationOrder()) {
            return new HashMap<>();
        }
        return new LinkedHashMap<>();
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "tick", at = @At("HEAD"))
    private void rzero$setBrainRngContext(ServerLevel level, LivingEntity entity, CallbackInfo ci) {
        if (!RZeroRuntime.mobAiPolicy().brainRng()) {
            return;
        }

        if (BrainRngContext.get() != null) {
            RandomSource restored = RZeroRandomMask.resetLevel(level);
            if (restored != null) {
                level.random = restored;
            }
            BrainRngContext.clear();
            RZero.LOGGER.warn("[RZero] Recovered leaked Brain RNG mask before applying a new one.");
        }

        long seed = entity.getUUID().getLeastSignificantBits() ^ level.getGameTime();
        RandomSource detRandom = rzero$THREAD_BRAIN_RANDOM.get();
        detRandom.setSeed(seed);
        BrainRngContext.set(detRandom);

        RandomSource saved = level.random;
        RZeroRandomMask.push(level, saved, detRandom);
        level.random = detRandom;
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "tick", at = @At("RETURN"))
    private void rzero$clearBrainRngContext(ServerLevel level, LivingEntity entity, CallbackInfo ci) {
        if (BrainRngContext.get() == null) {
            return;
        }
        RandomSource saved = RZeroRandomMask.pop(level);
        if (saved != null) {
            level.random = saved;
        }
        BrainRngContext.clear();
    }
}
