package ru.reset.rzero.access;

import net.minecraft.world.level.saveddata.SavedData;
import java.util.Map;

public interface IRZeroDimensionDataStorage {
    Map<String, SavedData> rzero$getCache();
    Map<String, SavedData.Factory<?>> rzero$getFactories();
}
