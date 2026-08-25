package ru.reset.rzero.metrics;

import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerMetrics {

    public static final ConcurrentHashMap<UUID, Float> damageTakenAccumulator = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<UUID, Float> damageDealtAccumulator = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<UUID, Float> damageBehindAccumulator = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<UUID, Integer> blocksPlacedAccumulator = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<UUID, Integer> itemsBurnedAccumulator = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<UUID, Float> cameraJitterAccumulator = new ConcurrentHashMap<>();

    public static final ConcurrentHashMap<UUID, Float> lastYaw = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<UUID, Float> lastPitch = new ConcurrentHashMap<>();

    public static final ConcurrentHashMap<UUID, Long> combatStartTick = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<UUID, Long> lastRespawnOrCheckpointTick = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<UUID, LongArrayList> deathTimestamps = new ConcurrentHashMap<>();

    private PlayerMetrics() {
    }
}
