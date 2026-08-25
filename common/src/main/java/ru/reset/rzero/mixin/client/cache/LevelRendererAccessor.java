package ru.reset.rzero.mixin.client.cache;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {

    @Accessor("viewArea")
    ViewArea rzero$getViewArea();

    @Accessor("sectionOcclusionGraph")
    SectionOcclusionGraph rzero$getSectionOcclusionGraph();
}
