package ru.reset.rzero.metrics;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.PrimedTnt;
import ru.reset.rzero.api.DevHooks;

import java.util.List;

public final class MetricsWriter {

    private MetricsWriter() {
    }

    public static void write(ServerPlayer player,
                             double threat,
                             int mobsTrack,
                             int mobsAttack,
                             int freeSpace,
                             float corneredPct,
                             int cliffsNear,
                             String event,
                             List<Mob> mobs,
                             List<PrimedTnt> tnts,
                             int projAir) {
        DevHooks.fireMetrics(player, threat, mobsTrack, mobsAttack, freeSpace, corneredPct,
                cliffsNear, event, mobs, tnts, projAir);
    }
}