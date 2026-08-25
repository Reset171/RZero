package ru.reset.rzero.mixin.level;

import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.server.level.ServerChunkCache$ChunkAndHolder")
public interface MixinChunkAndHolderAccessor {

    @Accessor("chunk")
    LevelChunk rzero$getChunk();
}
