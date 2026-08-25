package ru.reset.rzero.mixin.spawn;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.WanderingTraderSpawner;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.reset.rzero.runtime.RZeroRuntime;

@Mixin(WanderingTraderSpawner.class)
public class MixinWanderingTraderSpawner {

    @Mutable
    @Shadow @Final private RandomSource random;

    @Inject(method = "tick", at = @At("HEAD"))
    private void rzero$useLevelRandom(ServerLevel level, boolean spawnEnemies, boolean spawnFriendlies, CallbackInfoReturnable<Integer> cir) {
        if (!RZeroRuntime.checkpointPolicy().determinism().spawns().wanderingTrader()) {
            return;
        }
        this.random = level.getRandom();
    }
}
