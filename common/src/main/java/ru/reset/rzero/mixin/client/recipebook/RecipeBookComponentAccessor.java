package ru.reset.rzero.mixin.client.recipebook;

import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(RecipeBookComponent.class)
public interface RecipeBookComponentAccessor {
    @Accessor("selectedTab")
    RecipeBookTabButton rzero$getSelectedTab();

    @Accessor("selectedTab")
    void rzero$setSelectedTab(RecipeBookTabButton tab);

    @Accessor("tabButtons")
    List<RecipeBookTabButton> rzero$getTabButtons();

    @Accessor("searchBox")
    net.minecraft.client.gui.components.EditBox rzero$getSearchBox();

    @Invoker("updateCollections")
    void rzero$updateCollections(boolean resetPage);
}
