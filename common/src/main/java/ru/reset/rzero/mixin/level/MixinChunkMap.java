package ru.reset.rzero.mixin.level;

import ru.reset.rzero.RZero;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

@Mixin(ChunkMap.class)
public abstract class MixinChunkMap {

    @Shadow @Final private Int2ObjectMap<?> entityMap;

    @Inject(method = "addEntity", at = @At("HEAD"))
    private void rzero$fixEntityIdCollision(Entity entity, CallbackInfo ci) {
        if (this.entityMap.containsKey(entity.getId())) {
            int oldId = entity.getId();
            int newId = ru.reset.rzero.engine.EntityIdCounter.get() + 1;
            while (this.entityMap.containsKey(newId)) {
                newId++;
            }
            entity.setId(newId);
            ru.reset.rzero.engine.EntityIdCounter.set(newId);
            RZero.LOGGER.debug("[RZero] Entity ID collision: {} (type={}) reassigned {} -> {}",
                    entity.getUUID(), entity.getType().toShortString(), oldId, newId);
        }
    }
}
