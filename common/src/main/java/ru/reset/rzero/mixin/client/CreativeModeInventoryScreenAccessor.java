package ru.reset.rzero.mixin.client;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CreativeModeInventoryScreen.class)
public interface CreativeModeInventoryScreenAccessor {
    @Accessor("selectedTab")
    static CreativeModeTab rzero$getSelectedTab() { throw new AssertionError(); }

    @Accessor("searchBox")
    net.minecraft.client.gui.components.EditBox rzero$getSearchBox();

    @Invoker("refreshSearchResults")
    void rzero$refreshSearchResults();

    @Invoker("selectTab")
    void rzero$selectTab(CreativeModeTab tab);
}
