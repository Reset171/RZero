package ru.reset.rzero.client.cache;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.ticks.LevelChunkTicks;
import org.jetbrains.annotations.Nullable;

public class RZeroFakeChunk extends LevelChunk {

    public final DataLayer[] blockLight;
    public final DataLayer[] skyLight;

    public RZeroFakeChunk(Level level, ChunkPos pos, LevelChunkSection[] sections,
                          DataLayer[] blockLight, DataLayer[] skyLight) {
        super(level, pos, UpgradeData.EMPTY, new LevelChunkTicks<>(), new LevelChunkTicks<>(),
                0L, sections, null, null);
        this.blockLight = blockLight;
        this.skyLight = skyLight;
    }

    public void rzero$setHeightmap(Heightmap.Types type, Heightmap heightmap) {
        this.heightmaps.put(type, heightmap);
    }

    @Override
    public @Nullable BlockState setBlockState(BlockPos pos, BlockState state, boolean isMoving) {
        return null;
    }

    @Override
    public LevelChunkSection getSection(int index) {
        LevelChunkSection[] sections = this.getSections();
        if (sections.length == 0) {
            return super.getSection(index);
        }
        if (index < 0) {
            return sections[0];
        }
        if (index >= sections.length) {
            return sections[sections.length - 1];
        }
        return super.getSection(index);
    }
}
