package ru.reset.rzero.adaptive;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import ru.reset.rzero.metrics.PlayerMetrics;
import ru.reset.rzero.metrics.MetricsWriter;

import java.util.Collections;
import java.util.List;

public final class ThreatAssessor {

    private static final double MOB_SCAN_RADIUS = 24.0;
    private static final double PROJECTILE_SCAN_RADIUS = 10.0;

    private ThreatAssessor() {
    }

    public static double calculateInstantThreat(ServerPlayer player, String eventLabel) {
        if (!PlayerSafetyCheck.isPlayerSafe(player)) {
            MetricsWriter.write(player, 100.0, 0, 0, 0, 0f, 0, "FATAL_VETO",
                    Collections.emptyList(), Collections.emptyList(), 0);
            return -1.0;
        }

        long tick = player.server.overworld().getGameTime();
        double envThreat = environmentalThreat(player);

        AABB box = player.getBoundingBox().inflate(MOB_SCAN_RADIUS);
        List<Mob> mobs = player.level().getEntitiesOfClass(Mob.class, box, m -> m.getTarget() == player);

        accumulateCameraJitter(player);

        int mobsTracking = 0;
        int mobsAttacking = 0;
        for (Mob mob : mobs) {
            boolean canSee = mob.getSensing().hasLineOfSight(player);
            Path path = mob.getNavigation().getPath();
            boolean canReach = path != null && path.canReach();
            if (!canSee && !canReach && !(mob.distanceToSqr(player) < 16.0)) {
                continue;
            }
            mobsTracking++;
            if (isActivelyAttacking(mob, player)) {
                mobsAttacking++;
            }
        }

        double mobThreat = Math.min((mobsTracking * 5.0) + (mobsAttacking * 25.0), 60.0);
        if (mobThreat == 0) {
            envThreat *= 0.5;
        }

        double totalThreat = Math.min(envThreat + mobThreat, 100.0);
        trackCombatWindow(player, tick, totalThreat);

        if (tick % 20 == 0) {
            writeDetailedMetrics(player, box, mobs, totalThreat, mobsTracking, mobsAttacking, eventLabel);
        }
        return totalThreat;
    }

    private static double environmentalThreat(ServerPlayer player) {
        double threat = 0;

        float hpPct = player.getHealth() / player.getMaxHealth();
        threat += (1.0 - hpPct) * 60.0;

        if (player.getFoodData().getFoodLevel() < 6) {
            threat += 15.0;
        }
        if (player.getAirSupply() < player.getMaxAirSupply() * 0.5f) {
            float airPct = (float) player.getAirSupply() / player.getMaxAirSupply();
            threat += (0.5f - airPct) * 80.0;
        }
        if (player.hasEffect(MobEffects.POISON) || player.hasEffect(MobEffects.WITHER)) {
            threat += 40.0;
        }
        return threat;
    }

    private static boolean isActivelyAttacking(Mob mob, ServerPlayer player) {
        if (mob instanceof Creeper c && c.getSwellDir() > 0) {
            return true;
        }
        if (mob instanceof AbstractSkeleton s && s.isAggressive()) {
            return true;
        }
        return mob.distanceToSqr(player) < 9.0;
    }

    private static void accumulateCameraJitter(ServerPlayer player) {
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        float lastY = PlayerMetrics.lastYaw.getOrDefault(player.getUUID(), yaw);
        float lastP = PlayerMetrics.lastPitch.getOrDefault(player.getUUID(), pitch);
        float jitter = Math.abs(yaw - lastY) + Math.abs(pitch - lastP);

        PlayerMetrics.cameraJitterAccumulator.merge(player.getUUID(), jitter, Float::sum);
        PlayerMetrics.lastYaw.put(player.getUUID(), yaw);
        PlayerMetrics.lastPitch.put(player.getUUID(), pitch);
    }

    private static void trackCombatWindow(ServerPlayer player, long tick, double totalThreat) {
        if (totalThreat > 20) {
            if (PlayerMetrics.combatStartTick.getOrDefault(player.getUUID(), 0L) == 0L) {
                PlayerMetrics.combatStartTick.put(player.getUUID(), tick);
            }
        } else {
            PlayerMetrics.combatStartTick.put(player.getUUID(), 0L);
        }
    }

    private static void writeDetailedMetrics(ServerPlayer player,
                                             AABB box,
                                             List<Mob> mobs,
                                             double totalThreat,
                                             int mobsTracking,
                                             int mobsAttacking,
                                             String eventLabel) {
        LongOpenHashSet mobBlocks = new LongOpenHashSet();
        for (Mob m : mobs) {
            if (m.getTarget() != player) {
                continue;
            }
            int mx = m.getBlockX();
            int my = m.getBlockY();
            int mz = m.getBlockZ();
            mobBlocks.add(BlockPos.asLong(mx, my, mz));
            mobBlocks.add(BlockPos.asLong(mx, my, mz - 1));
            mobBlocks.add(BlockPos.asLong(mx, my, mz + 1));
            mobBlocks.add(BlockPos.asLong(mx + 1, my, mz));
            mobBlocks.add(BlockPos.asLong(mx - 1, my, mz));
        }

        BlockPos start = player.blockPosition();
        EscapeSpaceAnalyzer.Result total =
                EscapeSpaceAnalyzer.flood(player.level(), start, null, true);
        EscapeSpaceAnalyzer.Result safe =
                EscapeSpaceAnalyzer.flood(player.level(), start, mobBlocks, false);

        int freeSpace = total.reachable();
        float corneredPct = freeSpace > 0 ? 1.0f - ((float) safe.reachable() / freeSpace) : 0f;

        List<PrimedTnt> tnts = player.level().getEntitiesOfClass(PrimedTnt.class, box);
        int projAir = player.level().getEntitiesOfClass(Projectile.class,
                player.getBoundingBox().inflate(PROJECTILE_SCAN_RADIUS)).size();

        MetricsWriter.write(player, totalThreat, mobsTracking, mobsAttacking, freeSpace,
                corneredPct, total.cliffs(), eventLabel, mobs, tnts, projAir);
    }
}