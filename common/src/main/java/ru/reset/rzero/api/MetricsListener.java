package ru.reset.rzero.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.PrimedTnt;

import java.util.List;

@FunctionalInterface
public interface MetricsListener {
    void onMetrics(ServerPlayer player,
                   double threat,
                   int mobsTrack,
                   int mobsAttack,
                   int freeSpace,
                   float corneredPct,
                   int cliffsNear,
                   String event,
                   List<Mob> mobs,
                   List<PrimedTnt> tnts,
                   int projAir);
}