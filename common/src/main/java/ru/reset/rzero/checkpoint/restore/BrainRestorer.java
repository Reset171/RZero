package ru.reset.rzero.checkpoint.restore;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.npc.Villager;
import ru.reset.rzero.RZero;
import ru.reset.rzero.checkpoint.data.EntityRAMSnapshot;
import ru.reset.rzero.checkpoint.data.EntitySnapshot;
import ru.reset.rzero.engine.BrainCloner;
import ru.reset.rzero.mixin.entity.MixinLivingEntityAccessor;
import ru.reset.rzero.mixin.entity.MixinMobAccessor;

import java.util.Map;

public final class BrainRestorer {

    private BrainRestorer() {
    }

    public static boolean isComplexBrainMob(Mob mob) {
        return mob instanceof Villager || mob instanceof AbstractPiglin;
    }

    public static boolean restore(Mob mob, EntityRAMSnapshot ram, EntitySnapshot snap) {
        if (ram.clonedBrainAndNav != null && isComplexBrainMob(mob)) {
            if (restoreDeepClone(mob, ram)) {
                snap.hasPath = false;
                return true;
            }
            return false;
        }
        restoreMemories(mob, ram);
        return false;
    }

    private static boolean restoreDeepClone(Mob mob, EntityRAMSnapshot ram) {
        try {
            Object[] restoredBundle = BrainCloner.deepClone(
                    ram.clonedBrainAndNav,
                    oldEnt -> {
                        Entity newEnt = ((ServerLevel) mob.level()).getEntity(oldEnt.getUUID());
                        return newEnt != null ? newEnt : oldEnt;
                    });

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
