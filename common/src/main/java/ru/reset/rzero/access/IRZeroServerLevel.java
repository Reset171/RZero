package ru.reset.rzero.access;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockEventData;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;

public interface IRZeroServerLevel {
    ObjectLinkedOpenHashSet<BlockEventData> rzero$getBlockEvents();
    void rzero$deepRemoveEntity(Entity entity);

    boolean rzero$surgicalSpawn(Entity entity);

    void rzero$eradicateGhostEntity(java.util.UUID uuid);
    void rzero$eradicateAllGhostsExcept(java.util.Set<java.util.UUID> keep);

    void rzero$pushDeterministicRandom(net.minecraft.util.RandomSource random);

    void rzero$popRandom();
}