package ru.reset.rzero.mixin.spawn;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.reset.rzero.engine.SpawnEngine;
import ru.reset.rzero.runtime.RZeroRuntime;

@Mixin(NaturalSpawner.SpawnState.class)
public class MixinSpawnState {
    @Inject(
        method = "afterSpawn(Lnet/minecraft/world/entity/Mob;Lnet/minecraft/world/level/chunk/ChunkAccess;)V",
        at = @At("HEAD")
    )
    private void rzero$trackSpawnedMob(Mob mob, ChunkAccess chunk, CallbackInfo ci) {
        var policy = RZeroRuntime.checkpointPolicy().determinism().naturalSpawn();
        if (policy.enabled() && policy.localCap()) {
            SpawnEngine.afterSpawn(mob);
        }
    }
}
