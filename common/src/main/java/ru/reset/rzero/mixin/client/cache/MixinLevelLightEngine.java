package ru.reset.rzero.mixin.client.cache;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import ru.reset.rzero.client.cache.SpatialGrid;
import ru.reset.rzero.client.cache.ext.IRZeroLevelLightEngine;

@Mixin(LevelLightEngine.class)
public abstract class MixinLevelLightEngine implements IRZeroLevelLightEngine {

    @Unique
    private SpatialGrid<Boolean> rzero$activeColumns;

    @Override
    public void rzero$setActiveColumns(SpatialGrid<Boolean> grid) {
        this.rzero$activeColumns = grid;
    }

    @ModifyReturnValue(method = "lightOnInSection", at = @At("RETURN"))
    private boolean rzero$forceLightOnInSection(boolean original, SectionPos pos) {
        if (original) return true;
        SpatialGrid<Boolean> grid = rzero$activeColumns;
        if (grid == null) return false;
        Boolean active = grid.get(pos.x(), pos.z());
        return active != null && active;
    }
}
