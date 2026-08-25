package ru.reset.rzero.mixin.world;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(AbstractContainerMenu.class)
public interface MixinAbstractContainerMenu {
    @Accessor("dataSlots")
    List<DataSlot> rzero$getDataSlots();
}
