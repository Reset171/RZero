package ru.reset.rzero.mixin.level;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.reset.rzero.block.SectionSnapshot;
import ru.reset.rzero.checkpoint.data.CheckpointData;
import ru.reset.rzero.runtime.RZeroRuntime;
import ru.reset.rzero.runtime.SnapshotRegistry;

@Mixin(ChunkAccess.class)
public abstract class MixinChunkAccess {
    @Inject(method = "fillBiomesFromNoise", at = @At("HEAD"))
    private void rzero$onFillBiomesFromNoise(BiomeResolver biomeResolver, Climate.Sampler sampler, CallbackInfo ci) {
        if (RZeroRuntime.isRestoring) return;
        if (!((Object) this instanceof LevelChunk chunk)) return;
        if (!(chunk.getLevel() instanceof ServerLevel level)) return;
        if (level.isClientSide()) return;

        CheckpointData data = SnapshotRegistry.activeSnapshots.get(level.dimension());
        if (data == null || data.anchorId == null) return;

        long chunkKey = chunk.getPos().toLong();
        SectionSnapshot[] arr = data.sectionSnapshots.get(chunkKey);
        if (arr == null) return;

        boolean capturedAny = false;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == null) {
                LevelChunkSection sec = chunk.getSection(i);
                arr[i] = SectionSnapshot.capture(sec);
                capturedAny = true;
            }
        }
        if (capturedAny) {
            data.setDirty();
        }
    }
}