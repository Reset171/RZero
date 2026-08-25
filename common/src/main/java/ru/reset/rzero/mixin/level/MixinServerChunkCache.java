package ru.reset.rzero.mixin.level;

import net.minecraft.Util;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mixin(ServerChunkCache.class)
public abstract class MixinServerChunkCache {
    @Shadow public abstract Level getLevel();

    @Redirect(
            method = "tickChunks",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;shuffle(Ljava/util/List;Lnet/minecraft/util/RandomSource;)V")
    )
    private void rzero$deterministicChunkOrder(List<?> chunks, RandomSource ignored) {
        @SuppressWarnings("unchecked")
        List<Object> mutable = (List<Object>) chunks;
        mutable.sort(Comparator.comparingLong(entry ->
                ((MixinChunkAndHolderAccessor) entry).rzero$getChunk().getPos().toLong()));
        Util.shuffle(mutable, RandomSource.create(this.getLevel().getGameTime()));
    }

    @ModifyArg(
            method = "tickChunks",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/NaturalSpawner;createState(ILjava/lang/Iterable;Lnet/minecraft/world/level/NaturalSpawner$ChunkGetter;Lnet/minecraft/world/level/LocalMobCapCalculator;)Lnet/minecraft/world/level/NaturalSpawner$SpawnState;"),
            index = 1
    )
    private Iterable<Entity> rzero$canonicalEntityOrder(Iterable<Entity> entities) {
        List<Entity> sorted = new ArrayList<>();
        entities.forEach(sorted::add);
        sorted.sort(Comparator.comparing(Entity::getUUID));
        return sorted;
    }
}
