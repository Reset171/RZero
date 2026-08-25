package ru.reset.rzero.client.cache.ext;

import ru.reset.rzero.client.cache.RZeroFakeChunk;
import ru.reset.rzero.client.cache.SpatialGrid;

public interface IRZeroClientChunkCache {
    void rzero$setGrid(SpatialGrid<RZeroFakeChunk> grid);
}
