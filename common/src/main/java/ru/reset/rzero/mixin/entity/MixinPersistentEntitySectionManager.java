package ru.reset.rzero.mixin.entity;

import ru.reset.rzero.runtime.SnapshotRegistry;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityLookup;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.reset.rzero.RZero;
import ru.reset.rzero.checkpoint.data.CheckpointData;
import ru.reset.rzero.access.IRZeroEntityLookup;
import ru.reset.rzero.access.IRZeroEntitySectionManager;

import java.util.Set;
import java.util.UUID;

@Mixin(PersistentEntitySectionManager.class)
public abstract class MixinPersistentEntitySectionManager implements IRZeroEntitySectionManager {

    @Shadow @Final Set<UUID> knownUuids;
    @Shadow @Final private EntityLookup<EntityAccess> visibleEntityStorage;

    @Override
    public boolean rzero$eradicate(UUID uuid) {
        boolean removedKnown = knownUuids.remove(uuid);
        boolean removedVisible = ((IRZeroEntityLookup) visibleEntityStorage).rzero$removeByUuid(uuid);
        return removedKnown || removedVisible;
    }

    @Override
    public void rzero$eradicateAllExcept(Set<UUID> keep) {
        java.util.List<UUID> toRemove = new java.util.ArrayList<>();
        for (UUID u : knownUuids) {
            if (!keep.contains(u)) {
                toRemove.add(u);
            }
        }
        for (UUID u : toRemove) {
            rzero$eradicate(u);
        }
    }

    @Inject(
            method = "addEntity(Lnet/minecraft/world/level/entity/EntityAccess;Z)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void rzero$filterDiskGhosts(EntityAccess entityAccess, boolean existing,
                                        CallbackInfoReturnable<Boolean> cir) {
        if (!(entityAccess instanceof Entity e)) return;
        UUID uuid = e.getUUID();

        if (SnapshotRegistry.allowedSnapshotEntities.contains(uuid) && knownUuids.contains(uuid)) {
            cir.setReturnValue(false);
            return;
        }

        if (!existing) return;
        long chunkKey = e.chunkPosition().toLong();
        ResourceKey<Level> dim = e.level().dimension();

        CheckpointData data = SnapshotRegistry.activeSnapshots.get(dim);
        boolean inRollback = data != null && data.sectionSnapshots.containsKey(chunkKey);

        if (inRollback && !SnapshotRegistry.allowedSnapshotEntities.contains(uuid)) {
            cir.setReturnValue(false);
        }
    }
}
