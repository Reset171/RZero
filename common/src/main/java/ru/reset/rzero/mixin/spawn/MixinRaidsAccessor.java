package ru.reset.rzero.mixin.spawn;

import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(Raids.class)
public interface MixinRaidsAccessor {
    @Accessor("raidMap")            Map<Integer, Raid> rzero$getRaidMap();
    @Accessor("nextAvailableID")    int rzero$getNextAvailableID();
    @Accessor("nextAvailableID")    void rzero$setNextAvailableID(int v);
    @Accessor("tick")               int rzero$getTick();
    @Accessor("tick")               void rzero$setTick(int v);
}
