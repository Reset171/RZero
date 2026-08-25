package ru.reset.rzero.access;

public interface IRZeroLevelChunkSection {
    void rzero$copyCountsFrom(net.minecraft.world.level.chunk.LevelChunkSection src);
    short rzero$getNonEmptyBlockCount();
    short rzero$getTickingBlockCount();
    short rzero$getTickingFluidCount();
}
