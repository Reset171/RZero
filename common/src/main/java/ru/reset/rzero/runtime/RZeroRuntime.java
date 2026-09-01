package ru.reset.rzero.runtime;

import ru.reset.rzero.anchor.RZeroAnchorSettings;
import ru.reset.rzero.checkpoint.data.CheckpointData;
import ru.reset.rzero.config.RZeroCheckpointPolicy;
import ru.reset.rzero.config.RZeroClientRestoreSettings;
import ru.reset.rzero.config.RZeroSettings;

public final class RZeroRuntime {

    public static boolean isRestoring = false;

    public static boolean isHeadlessTicking = false;

    public static boolean wasRestoredThisTick = false;

    public static boolean isVerifyRun = false;

    public static volatile boolean rzerochashEnabled = true;

    public static long shufflingCounter = 0;

    private static volatile RZeroSettings settings = RZeroSettings.defaults();
    private static volatile RZeroCheckpointPolicy activeCheckpointPolicy = settings.checkpointPolicy();

    private static volatile RZeroCheckpointPolicy.NaturalSpawn naturalSpawnPolicy =
            activeCheckpointPolicy.determinism().naturalSpawn();
    private static volatile RZeroCheckpointPolicy.MobAi mobAiPolicy =
            activeCheckpointPolicy.determinism().mobAi();

    private static volatile RZeroAnchorSettings anchorSettings = settings.anchor();

    private RZeroRuntime() {
    }

    private static void refreshPolicyCache(RZeroCheckpointPolicy policy) {
        RZeroCheckpointPolicy p = policy == null ? RZeroCheckpointPolicy.defaults() : policy;
        naturalSpawnPolicy = p.determinism().naturalSpawn();
        mobAiPolicy = p.determinism().mobAi();
    }

    public static RZeroCheckpointPolicy.NaturalSpawn naturalSpawnPolicy() {
        return naturalSpawnPolicy;
    }

    public static RZeroCheckpointPolicy.MobAi mobAiPolicy() {
        return mobAiPolicy;
    }

    public static RZeroSettings settings() {
        return settings;
    }

    public static void setSettings(RZeroSettings newSettings) {
        settings = newSettings == null ? RZeroSettings.defaults() : newSettings.sanitize();
        activeCheckpointPolicy = settings.checkpointPolicy();
        refreshPolicyCache(activeCheckpointPolicy);
        rzerochashEnabled = settings.rzerochashEnabled();
        anchorSettings = settings.anchor();
    }

    public static void setRzerochashEnabled(boolean enabled) {
        settings = settings.withRzerochashEnabled(enabled);
        rzerochashEnabled = enabled;
    }

    public static RZeroAnchorSettings anchorSettings() {
        return anchorSettings != null ? anchorSettings : settings.anchor();
    }

    public static void setAnchorSettings(RZeroAnchorSettings newAnchorSettings) {
        RZeroAnchorSettings sanitized = newAnchorSettings == null
                ? RZeroAnchorSettings.defaults()
                : newAnchorSettings.sanitize();
        settings = settings.withAnchor(sanitized);
        anchorSettings = settings.anchor();
    }

    public static void setActiveCheckpointPolicy(RZeroCheckpointPolicy policy) {
        activeCheckpointPolicy = policy == null ? settings.checkpointPolicy() : policy.sanitize();
        refreshPolicyCache(activeCheckpointPolicy);
    }

    public static void resetActiveCheckpointPolicyToConfig() {
        activeCheckpointPolicy = settings.checkpointPolicy();
        refreshPolicyCache(activeCheckpointPolicy);
    }

    public static RZeroCheckpointPolicy checkpointPolicy() {
        return activeCheckpointPolicy != null ? activeCheckpointPolicy : settings.checkpointPolicy();
    }

    public static RZeroCheckpointPolicy effectivePolicy(CheckpointData data) {
        return data != null && data.policy != null ? data.policy : checkpointPolicy();
    }

    public static RZeroClientRestoreSettings clientRestore() {
        return settings.clientRestore();
    }

    public static ru.reset.rzero.config.RZeroAdaptiveSettings adaptiveSettings() {
        return settings.adaptive();
    }
}
