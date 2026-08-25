package ru.reset.rzero.mixin.ai;

import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PoiRecord.class)
public interface MixinPoiRecordAccessor {
    @Accessor("freeTickets")
    void rzero$setFreeTickets(int freeTickets);
    @Accessor("freeTickets")
    int rzero$getFreeTickets();
}
