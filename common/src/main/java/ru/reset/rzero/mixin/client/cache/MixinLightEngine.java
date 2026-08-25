package ru.reset.rzero.mixin.client.cache;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import ru.reset.rzero.client.cache.SpatialSectionGrid;
import ru.reset.rzero.client.cache.ext.IRZeroLightEngine;

@Mixin(LightEngine.class)
public abstract class MixinLightEngine implements IRZeroLightEngine {

    @Unique
    private SpatialSectionGrid<DataLayer> rzero$sectionGrid;

    @Override
    public void rzero$setSectionGrid(SpatialSectionGrid<DataLayer> grid) {
        this.rzero$sectionGrid = grid;
    }

    @ModifyReturnValue(
            method = "getDataLayerData(Lnet/minecraft/core/SectionPos;)Lnet/minecraft/world/level/chunk/DataLayer;",
            at = @At("RETURN")
    )
    private DataLayer rzero$injectDataLayer(DataLayer original, SectionPos pos) {
        SpatialSectionGrid<DataLayer> grid = rzero$sectionGrid;
        if (grid == null) return original;
        DataLayer cached = grid.get(pos.x(), pos.y(), pos.z());
        return cached != null ? cached : original;
    }

    @ModifyReturnValue(
            method = "getLightValue(Lnet/minecraft/core/BlockPos;)I",
            at = @At("RETURN")
    )
    private int rzero$injectLightValue(int original, BlockPos blockPos) {
        SpatialSectionGrid<DataLayer> grid = rzero$sectionGrid;
        if (grid == null) return original;
        int sx = SectionPos.blockToSectionCoord(blockPos.getX());
        int sy = SectionPos.blockToSectionCoord(blockPos.getY());
        int sz = SectionPos.blockToSectionCoord(blockPos.getZ());
        DataLayer cached = grid.get(sx, sy, sz);
        if (cached != null) {
            return cached.get(
                    SectionPos.sectionRelative(blockPos.getX()),
                    SectionPos.sectionRelative(blockPos.getY()),
                    SectionPos.sectionRelative(blockPos.getZ())
            );
        }
        return original;
    }
}
