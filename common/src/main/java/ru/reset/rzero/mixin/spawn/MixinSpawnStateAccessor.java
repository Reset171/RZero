package ru.reset.rzero.mixin.spawn;

import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NaturalSpawner.SpawnState.class)
public interface MixinSpawnStateAccessor {
    @Invoker("canSpawnForCategory")
    boolean rzero$invokeCanSpawnForCategory(MobCategory category, ChunkPos pos);
}
