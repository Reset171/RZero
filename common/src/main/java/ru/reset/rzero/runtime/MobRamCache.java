package ru.reset.rzero.runtime;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import ru.reset.rzero.RZero;
import ru.reset.rzero.checkpoint.data.CheckpointData;
import ru.reset.rzero.checkpoint.data.EntityRAMSnapshot;
import ru.reset.rzero.checkpoint.data.EntitySnapshot;
import ru.reset.rzero.checkpoint.data.MobRamLive;
import ru.reset.rzero.util.DetOrder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MobRamCache {

    private static final long TTL_TICKS = 24000L;

    public static final ConcurrentHashMap<ResourceKey<Level>, ConcurrentHashMap<UUID, MobRamLive>>
            mobRamCache = new ConcurrentHashMap<>();

    private MobRamCache() {
    }

    public static void clear() {
        mobRamCache.clear();
    }

    public static void evictExpired(long currentTick) {
        if ((currentTick & 0xFF) != 0 || mobRamCache.isEmpty()) {
            return;
        }
        long cutoff = currentTick - TTL_TICKS;
        for (Map<UUID, MobRamLive> dimCache : DetOrder.commutativeValues(mobRamCache)) {
            dimCache.values().removeIf(live -> live.capturedAtTick < cutoff);
        }
    }

    public static int seedFrom(ServerLevel level, CheckpointData data, long bootTick) {
        Map<UUID, EntitySnapshot> byUuid = new HashMap<>();
        for (EntitySnapshot es : data.entities) {
            byUuid.put(es.uuid, es);
        }

        ConcurrentHashMap<UUID, MobRamLive> dimCache = new ConcurrentHashMap<>();
        for (Map.Entry<UUID, EntityRAMSnapshot> e : data.entityRamSnapshots.entrySet()) {
            MobRamLive live = MobRamLive.fromSnapshots(e.getValue(), byUuid.get(e.getKey()));
            live.capturedAtTick = bootTick;
            dimCache.put(e.getKey(), live);
        }
        for (EntitySnapshot es : data.entities) {
            if (dimCache.containsKey(es.uuid)) {
                continue;
            }
            MobRamLive live = MobRamLive.fromSnapshots(null, es);
            live.capturedAtTick = bootTick;
            dimCache.put(es.uuid, live);
        }

        if (!dimCache.isEmpty()) {
            mobRamCache.put(level.dimension(), dimCache);
        }
        return dimCache.size();
    }

    public static void applyOnJoin(Mob mob, ServerLevel level) {
        Map<UUID, MobRamLive> dimCache = mobRamCache.get(level.dimension());
        if (dimCache == null) {
            return;
        }
        MobRamLive live = dimCache.remove(mob.getUUID());
        if (live == null) {
            return;
        }
        try {
            live.applyTo(mob, level);
        } catch (Throwable t) {
            RZero.LOGGER.warn("[RZero] MobRamLive.applyTo failed for {}: {}", mob.getUUID(), t.getMessage());
        }
    }
}
