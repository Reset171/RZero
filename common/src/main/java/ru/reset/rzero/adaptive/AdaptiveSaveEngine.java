package ru.reset.rzero.adaptive;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import ru.reset.rzero.ModGameRules;
import ru.reset.rzero.RZero;
import ru.reset.rzero.checkpoint.CheckpointManager;
import ru.reset.rzero.checkpoint.data.AdaptiveState;
import ru.reset.rzero.checkpoint.data.RZeroAdaptiveData;
import ru.reset.rzero.runtime.RestoreQueues;
import ru.reset.rzero.runtime.SnapshotRegistry;
import ru.reset.rzero.metrics.PlayerMetrics;
import ru.reset.rzero.metrics.MetricsWriter;

import java.util.Collections;

public final class AdaptiveSaveEngine {

    private static final double DRAMATIC_PEAK = 35.0;

    public static long lastSaveTick = 0;
    public static long currentInterval = 0;
    public static double currentIntensity = 0.0;
    public static double peakIntensity = 0.0;
    public static int relaxTicks = 0;
    public static long lastBossDeathTick = -9999L;

    private AdaptiveSaveEngine() {
    }

    public static AdaptiveState captureAdaptiveState() {
        AdaptiveState as = new AdaptiveState();
        as.lastSaveTick = lastSaveTick;
        as.currentInterval = currentInterval;
        as.currentIntensity = currentIntensity;
        as.peakIntensity = peakIntensity;
        as.relaxTicks = relaxTicks;
        as.lastBossDeathTick = lastBossDeathTick;
        return as;
    }

    public static void restoreAdaptiveState(AdaptiveState as) {
        if (as == null) {
            return;
        }
        lastSaveTick = as.lastSaveTick;
        currentInterval = as.currentInterval;
        currentIntensity = as.currentIntensity;
        peakIntensity = as.peakIntensity;
        relaxTicks = as.relaxTicks;
        lastBossDeathTick = as.lastBossDeathTick;
        RestoreQueues.pendingDeathRollback = null;
    }

    public static void resetAutoSaveTimer(MinecraftServer server) {
        lastSaveTick = server.overworld().getGameTime();
        RZeroAdaptiveData.markDirty(server);
        calculateNextInterval(server.overworld());
        RZero.logInfo("[RZero] Auto-save timer reset after rollback.");
    }

    public static void calculateNextInterval(ServerLevel level) {
        if (level.getGameRules().getBoolean(ModGameRules.RULE_USE_RANDOM_INTERVAL)) {
            int min = level.getGameRules().getInt(ModGameRules.RULE_RANDOM_MIN);
            int max = level.getGameRules().getInt(ModGameRules.RULE_RANDOM_MAX);
            if (min < 5) {
                min = 5;
            }
            if (max <= min) {
                max = min + 1;
            }
            int extra = level.getRandom().nextInt((max - min) + 1);
            currentInterval = (min + extra) * 20L;
        } else {
            int base = level.getGameRules().getInt(ModGameRules.RULE_FIXED_INTERVAL);
            if (base < 5) {
                base = 5;
            }
            currentInterval = base * 20L;
        }
    }

    public static void onCheckpointCreated(MinecraftServer server, java.util.List<ServerPlayer> anchors, long currentTick) {
        lastSaveTick = currentTick;
        currentIntensity = 0.0;
        peakIntensity = 0.0;
        relaxTicks = 0;
        RZeroAdaptiveData.markDirty(server);
        if (anchors != null) {
            for (ServerPlayer anchor : anchors) {
                if (anchor != null) {
                    PlayerMetrics.lastRespawnOrCheckpointTick.put(anchor.getUUID(), currentTick);
                }
            }
        }
    }

    public static void tick(MinecraftServer server, ServerLevel overworld, long currentTick) {
        java.util.List<ServerPlayer> anchors = SnapshotRegistry.findAnchorPlayers(server);
        if (anchors.isEmpty()) {
            return;
        }

        if (overworld.getGameRules().getBoolean(ModGameRules.RULE_USE_ADAPTIVE_MODE)) {
            tickAdaptiveMode(server, overworld, anchors, currentTick);
            return;
        }
        tickIntervalMode(server, overworld, anchors, currentTick);
    }

    private static void tickAdaptiveMode(MinecraftServer server,
                                         ServerLevel overworld,
                                         java.util.List<ServerPlayer> anchors,
                                         long currentTick) {
        double maxInstantThreat = -1.0;
        ServerPlayer mostThreatenedAnchor = null;

        for (ServerPlayer anchorPlayer : anchors) {
            double instantThreat = ThreatAssessor.calculateInstantThreat(anchorPlayer, "NONE");
            if (instantThreat > maxInstantThreat) {
                maxInstantThreat = instantThreat;
                mostThreatenedAnchor = anchorPlayer;
            }
        }

        if (maxInstantThreat < 0) {
            relaxTicks = 0;
            return;
        }

        long timeSinceLastSave = currentTick - lastSaveTick;

        if (maxInstantThreat > currentIntensity) {
            currentIntensity += (maxInstantThreat - currentIntensity) * 0.2;
            relaxTicks = 0;
        } else {
            currentIntensity -= (currentIntensity - maxInstantThreat) * 0.02;
        }
        if (currentIntensity > peakIntensity) {
            peakIntensity = currentIntensity;
        }

        int relaxTicksRequired = Math.max(20, ru.reset.rzero.runtime.RZeroRuntime.adaptiveSettings().relaxTimeSeconds() * 20);
        long minSaveGapTicks = Math.max(100L, ru.reset.rzero.runtime.RZeroRuntime.adaptiveSettings().minSaveGapSeconds() * 20L);
        float saveChance = ru.reset.rzero.runtime.RZeroRuntime.adaptiveSettings().postCombatSavePercent() / 100.0f;

        boolean shouldSave = false;
        if (peakIntensity > DRAMATIC_PEAK) {
            if (currentIntensity < peakIntensity * 0.5 && maxInstantThreat <= 20.0) {
                relaxTicks++;
                if (relaxTicks > relaxTicksRequired) {
                    if (mostThreatenedAnchor != null && mostThreatenedAnchor.getRandom().nextFloat() < saveChance) {
                        shouldSave = true;
                    } else {
                        peakIntensity = 0.0;
                        currentIntensity = 0.0;
                        relaxTicks = 0;
                    }
                }
            } else {
                relaxTicks = 0;
            }
        } else {
            long tranquilIntervalTicks = Math.max(60L,
                    overworld.getGameRules().getInt(ModGameRules.RULE_ADAPTIVE_INTERVAL)) * 20L;
            if (timeSinceLastSave >= tranquilIntervalTicks && maxInstantThreat <= 10.0) {
                shouldSave = true;
            }
        }

        if (shouldSave && timeSinceLastSave > minSaveGapTicks && mostThreatenedAnchor != null) {
            CheckpointManager.setCheckpoint(mostThreatenedAnchor);
            onCheckpointCreated(server, anchors, currentTick);
            MetricsWriter.write(mostThreatenedAnchor, maxInstantThreat, 0, 0, 0, 0f, 0,
                    "CHECKPOINT_CREATED", Collections.emptyList(), Collections.emptyList(), 0);
        }
    }

    private static void tickIntervalMode(MinecraftServer server,
                                        ServerLevel overworld,
                                        java.util.List<ServerPlayer> anchors,
                                        long currentTick) {
        if (currentInterval == 0) {
            calculateNextInterval(overworld);
            lastSaveTick = currentTick;
            RZeroAdaptiveData.markDirty(server);
        }
        if (currentTick - lastSaveTick < currentInterval) {
            return;
        }
        boolean allSafe = true;
        for (ServerPlayer anchorPlayer : anchors) {
            if (!PlayerSafetyCheck.isPlayerSafe(anchorPlayer)) {
                allSafe = false;
                break;
            }
        }
        if (!allSafe) {
            return;
        }
        CheckpointManager.setCheckpoint(anchors.get(0));
        lastSaveTick = currentTick;
        calculateNextInterval(overworld);
        RZeroAdaptiveData.markDirty(server);
    }
}