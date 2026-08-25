package ru.reset.rzero.access;

public interface IRZeroSpawnStamp {

    void rzero$stampSpawn(long gameTime, long epoch, long seed, long chunkKey);

    boolean rzero$hasSpawnStamp();

    long rzero$getSpawnTick();

    long rzero$getSpawnEpoch();

    long rzero$getSpawnSeed();

    long rzero$getSpawnChunk();
}
