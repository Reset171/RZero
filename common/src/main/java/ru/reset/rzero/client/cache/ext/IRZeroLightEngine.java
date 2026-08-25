package ru.reset.rzero.client.cache.ext;

import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import ru.reset.rzero.client.cache.SpatialSectionGrid;

public interface IRZeroLightEngine {
    void rzero$setSectionGrid(SpatialSectionGrid<DataLayer> grid);

    static IRZeroLightEngine get(LayerLightEventListener view) {
        return (view instanceof IRZeroLightEngine) ? (IRZeroLightEngine) view : null;
    }
}
