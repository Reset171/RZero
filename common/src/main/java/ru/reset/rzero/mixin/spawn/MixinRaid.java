package ru.reset.rzero.mixin.spawn;

import ru.reset.rzero.RZero;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.RandomSupport;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.reset.rzero.access.IRZeroRandomState;
import ru.reset.rzero.runtime.RZeroRuntime;

@Mixin(Raid.class)
public abstract class MixinRaid {

    @Shadow
    @Final
    private RandomSource random;


    @Inject(method = "save", at = @At("RETURN"))
    private void rzero$saveRaidRandom(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        if (this.random instanceof IRZeroRandomState rState) {
            long[] state = rState.rzero$getState();
            tag.putLongArray("rzero$randomState", state);
        }
    }

    @Inject(method = "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    private void rzero$loadRaidRandom(net.minecraft.server.level.ServerLevel level, CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("rzero$randomState") && this.random instanceof IRZeroRandomState rState) {
            long[] state = tag.getLongArray("rzero$randomState");
            rState.rzero$setState(state);
        }
    }


    @Unique
    private static final ThreadLocal<long[]> rzero$pendingSeed = new ThreadLocal<>();

    @Inject(method = "<init>(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)V", at = @At("HEAD"))
    private static void rzero$captureNewRaidArgs(int id, ServerLevel level, BlockPos center, CallbackInfo ci) {
        if (!RZeroRuntime.checkpointPolicy().determinism().spawns().raidSeed()) {
            rzero$pendingSeed.remove();
            return;
        }
        long seed = level.getSeed()
                ^ ((long) id * 6364136223846793005L)
                ^ center.asLong()
                ^ (level.getGameTime() * 2862933555777941757L);
        seed = RandomSupport.mixStafford13(seed);
        rzero$pendingSeed.set(new long[]{seed});
    }

    @Redirect(
            method = "<init>(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;create()Lnet/minecraft/util/RandomSource;")
    )
    private RandomSource rzero$deterministicCreate() {
        if (!RZeroRuntime.checkpointPolicy().determinism().spawns().raidSeed()) {
            return RandomSource.create();
        }
        long[] pending = rzero$pendingSeed.get();
        if (pending != null) {
            rzero$pendingSeed.remove();
            long seed = pending[0];
            RZero.LOGGER.info("[RZero-Dev] Created Raid with deterministic seed: {}", seed);
            return RandomSource.create(seed);
        }
        return RandomSource.create();
    }

    @Inject(method = "spawnGroup", at = @At("HEAD"))
    private void rzero$logSpawnGroup(net.minecraft.core.BlockPos pos, CallbackInfo ci) {
        RZero.LOGGER.info("[RZero-Dev] Raid spawning group at: {}", pos);
    }


    @Redirect(
            method = "findRandomSpawnPos",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextFloat()F")
    )
    private float rzero$redirectSpawnPosNextFloat(RandomSource instance) {
        if (!RZeroRuntime.checkpointPolicy().determinism().spawns().raidSpawnPosition()) {
            return instance.nextFloat();
        }
        return this.random.nextFloat();
    }

    @Redirect(
            method = "findRandomSpawnPos",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I")
    )
    private int rzero$redirectSpawnPosNextInt(RandomSource instance, int bound) {
        if (!RZeroRuntime.checkpointPolicy().determinism().spawns().raidSpawnPosition()) {
            return instance.nextInt(bound);
        }
        return this.random.nextInt(bound);
    }
}
