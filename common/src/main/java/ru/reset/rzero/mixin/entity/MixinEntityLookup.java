package ru.reset.rzero.mixin.entity;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityLookup;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import ru.reset.rzero.access.IRZeroEntityLookup;

import java.util.Map;
import java.util.UUID;

@Mixin(EntityLookup.class)
public abstract class MixinEntityLookup implements IRZeroEntityLookup {

    @Shadow @Final private Map<UUID, EntityAccess> byUuid;
    @Shadow @Final private Int2ObjectMap<EntityAccess> byId;

    @Override
    public boolean rzero$removeByUuid(UUID uuid) {
        EntityAccess removed = byUuid.remove(uuid);
        if (removed == null) return false;
        byId.remove(removed.getId());
        return true;
    }
}
