package ru.reset.rzero.checkpoint.restore;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.npc.Villager;
import ru.reset.rzero.RZero;
import ru.reset.rzero.access.IRZeroVillagerBrainMarker;
import ru.reset.rzero.checkpoint.data.EntityRAMSnapshot;
import ru.reset.rzero.checkpoint.data.EntitySnapshot;
import ru.reset.rzero.engine.BrainCloner;
import ru.reset.rzero.mixin.entity.MixinLivingEntityAccessor;
import ru.reset.rzero.mixin.entity.MixinMobAccessor;

import java.util.Map;
import java.util.UUID;

import ru.reset.rzero.runtime.RestoreQueues;

public final class BrainRestorer {

    private BrainRestorer() {
    }

    public static boolean isComplexBrainMob(Mob mob) {
        return mob instanceof Villager || mob instanceof AbstractPiglin;
    }

    public static boolean restore(Mob mob, EntityRAMSnapshot ram, EntitySnapshot snap) {
        long t0 = System.nanoTime();
        boolean result;
        if (ram.clonedBrainAndNav != null && isComplexBrainMob(mob)) {
            if (restoreDeepClone(mob, ram)) {
                snap.hasPath = false;
                clearStripMarker(mob);
                result = true;
            } else {
                rebuildStrippedBrain(mob);
                restoreMemories(mob, ram);
                result = false;
            }
        } else {
            rebuildStrippedBrain(mob);
            restoreMemories(mob, ram);
            result = false;
        }
        ru.reset.rzero.util.RZBenchmark.accum(ru.reset.rzero.util.RZBenchmark.Phase.BRAIN_RELINK, t0);
        return result;
    }

    private static void rebuildStrippedBrain(Mob mob) {
        if (mob instanceof Villager villager
                && ((IRZeroVillagerBrainMarker) villager).rzero$isBrainStripped()
                && mob.level() instanceof ServerLevel level) {
            ((IRZeroVillagerBrainMarker) villager).rzero$setBrainStripped(false);
            villager.refreshBrain(level);
        }
    }

    private static void clearStripMarker(Mob mob) {
        if (mob instanceof Villager) {
            ((IRZeroVillagerBrainMarker) mob).rzero$setBrainStripped(false);
        }
    }

    private static boolean restoreDeepClone(Mob mob, EntityRAMSnapshot ram) {
        try {
            ServerLevel level = (ServerLevel) mob.level();
            long t0 = System.nanoTime();
            Object[] restoredBundle = BrainCloner.deepClone(
                    ram.clonedBrainAndNav,
                    oldEnt -> resolveLiveEntity(level, oldEnt));
            ru.reset.rzero.util.RZBenchmark.accum(ru.reset.rzero.util.RZBenchmark.Phase.BRAIN_DEEPCLONE, t0);

            Brain<?> restoredBrain = (Brain<?>) restoredBundle[0];
            PathNavigation restoredNav = (PathNavigation) restoredBundle[1];

            ((MixinLivingEntityAccessor) mob).rzero$setBrain(restoredBrain);
            ((MixinMobAccessor) mob).rzero$setNavigation(restoredNav);
            return true;
        } catch (Exception ex) {
            RZero.LOGGER.error(
                    "[RZero] Failed to restore deep cloned BrainAndNav for mob " + mob.getUUID(), ex);
            return false;
        }
    }

    private static Entity resolveLiveEntity(ServerLevel level, Entity oldEnt) {
        Entity byIdentity = RestoreQueues.entityRemapByIdentity.get(oldEnt);
        if (byIdentity != null && !byIdentity.isRemoved()) {
            return byIdentity;
        }
        UUID uuid = oldEnt.getUUID();
        Entity cached = RestoreQueues.entityRemapCache.get(uuid);
        if (cached != null && !cached.isRemoved()) {
            RestoreQueues.entityRemapByIdentity.put(oldEnt, cached);
            return cached;
        }
        Entity resolved = level.getEntity(uuid);
        if (resolved != null && !resolved.isRemoved()) {
            RestoreQueues.entityRemapCache.put(uuid, resolved);
            RestoreQueues.entityRemapByIdentity.put(oldEnt, resolved);
            return resolved;
        }
        return resolved != null ? resolved : oldEnt;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void restoreMemories(Mob mob, EntityRAMSnapshot ram) {
        if (mob.getBrain() == null || mob.getBrain().memories == null || ram.memories.isEmpty()) {
            return;
        }
        for (var entry : ram.memories.entrySet()) {
            if (mob.getBrain().memories.containsKey(entry.getKey())) {
                Map raw = mob.getBrain().memories;
                raw.put(entry.getKey(), entry.getValue());
            }
        }
    }
}
