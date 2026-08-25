package ru.reset.rzero.client.cache.ext;

import net.minecraft.world.level.lighting.LevelLightEngine;
import ru.reset.rzero.client.cache.SpatialGrid;

public interface IRZeroLevelLightEngine {
    void rzero$setActiveColumns(SpatialGrid<Boolean> grid);

    static IRZeroLevelLightEngine get(LevelLightEngine provider) {
        return (provider instanceof IRZeroLevelLightEngine) ? (IRZeroLevelLightEngine) provider : null;
    }
}
