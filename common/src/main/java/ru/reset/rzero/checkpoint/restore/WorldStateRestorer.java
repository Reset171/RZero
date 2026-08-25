package ru.reset.rzero.checkpoint.restore;

import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import ru.reset.rzero.RZero;
import ru.reset.rzero.access.IRZeroRandomState;
import ru.reset.rzero.checkpoint.data.CheckpointData;
import ru.reset.rzero.config.RZeroCheckpointPolicy;
import ru.reset.rzero.mixin.level.MixinEndDragonFightAccessor;
import ru.reset.rzero.mixin.level.MixinLevelSubTick;
import ru.reset.rzero.mixin.spawn.MixinRaidsAccessor;
import ru.reset.rzero.runtime.RZeroRuntime;

public final class WorldStateRestorer {

    private WorldStateRestorer() {
    }

    public static void restore(ServerLevel level, CheckpointData data) {
        RZeroCheckpointPolicy policy = RZeroRuntime.effectivePolicy(data);
        if (data.worldState != null) {
            restoreTimeAndWeather(level, data, policy);
            if (policy.rollback().world().levelRng()) {
                restoreLevelRng(level, data);
            }
            if (policy.rollback().blockTicks() || policy.rollback().fluidTicks()) {
                restoreSubTickCounter(level, data);
            }
            if (policy.rollback().world().weather()) {
                broadcastWeather(level, data);
            }
        }
        if (policy.rollback().world().dragonFight()) {
            restoreDragonFight(level, data);
        }
        if (policy.rollback().world().raids()) {
            restoreRaids(level, data);
        }
    }

    private static void restoreTimeAndWeather(ServerLevel level, CheckpointData data, RZeroCheckpointPolicy policy) {
        if (!(level.getLevelData() instanceof ServerLevelData sld)) {
            return;
        }
        if (policy.rollback().world().time()) {
            level.setDayTime(data.worldState.dayTime);
            sld.setGameTime(data.worldState.gameTime);
            boolean doDaylight = level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DAYLIGHT);
            net.minecraft.network.protocol.game.ClientboundSetTimePacket timePacket =
                    new net.minecraft.network.protocol.game.ClientboundSetTimePacket(
                            level.getGameTime(), level.getDayTime(), doDaylight);
            for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
                if (p.level() == level) {
                    p.connection.send(timePacket);
                }
            }
        }
        if (!policy.rollback().world().weather()) {
            return;
        }
        sld.setClearWeatherTime(data.worldState.clearWeatherTime);
        if (sld instanceof PrimaryLevelData pld) {
            pld.setRaining(data.worldState.isRaining);
            pld.setThundering(data.worldState.isThundering);
        }
        sld.setRainTime(data.worldState.rainTime);
        sld.setThunderTime(data.worldState.thunderTime);
        level.setRainLevel(data.worldState.isRaining ? 1.0f : 0.0f);
        level.setThunderLevel(data.worldState.isThundering ? 1.0f : 0.0f);
    }

    private static void restoreLevelRng(ServerLevel level, CheckpointData data) {
        if (data.worldState.rngState != null
                && level.getRandom() instanceof IRZeroRandomState rState) {
            rState.rzero$setState(data.worldState.rngState);
        }
    }

    private static void restoreSubTickCounter(ServerLevel level, CheckpointData data) {
        long maxSub = ChunkRestorer.maxSubTickOrder(data);
        if (maxSub < 0) {
            return;
        }
        MixinLevelSubTick acc = (MixinLevelSubTick) (Object) level;
        if (acc.rzero$getSubTickCount() <= maxSub) {
            acc.rzero$setSubTickCount(maxSub + 1L);
        }
    }

    private static void broadcastWeather(ServerLevel level, CheckpointData data) {
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            if (p.level() != level) {
                continue;
            }
            p.connection.send(new ClientboundGameEventPacket(
                    data.worldState.isRaining
                            ? ClientboundGameEventPacket.START_RAINING
                            : ClientboundGameEventPacket.STOP_RAINING, 0f));
            p.connection.send(new ClientboundGameEventPacket(
                    ClientboundGameEventPacket.RAIN_LEVEL_CHANGE,
                    data.worldState.isRaining ? 1f : 0f));
            p.connection.send(new ClientboundGameEventPacket(
                    ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE,
                    data.worldState.isThundering ? 1f : 0f));
        }
    }

    private static void restoreDragonFight(ServerLevel level, CheckpointData data) {
        if (data.dragonFightTag == null || !data.dragonFightTag.contains("data")) {
            return;
        }
        try {
            EndDragonFight.Data.CODEC
                    .parse(NbtOps.INSTANCE, data.dragonFightTag.get("data"))
                    .result()
                    .ifPresent(parsedData -> {
                        EndDragonFight oldFight = level.getDragonFight();
                        if (oldFight != null) {
                            net.minecraft.server.level.ServerBossEvent dragonEvent =
                                    ((MixinEndDragonFightAccessor) oldFight).rzero$getDragonEvent();
                            if (dragonEvent != null) {
                                dragonEvent.removeAllPlayers();
                            }
                        }
                        level.setDragonFight(new EndDragonFight(level, level.getSeed(), parsedData));
                    });
        } catch (Exception e) {
            RZero.LOGGER.warn("[RZero] DragonFight restore failed: {}", e.getMessage());
        }
    }

    private static void restoreRaids(ServerLevel level, CheckpointData data) {
        if (data.raidsTag == null) {
            return;
        }
        try {
            Raids fresh = Raids.load(level, data.raidsTag);
            MixinRaidsAccessor liveAcc = (MixinRaidsAccessor) (Object) level.getRaids();
            MixinRaidsAccessor freshAcc = (MixinRaidsAccessor) (Object) fresh;

            for (Raid oldRaid : liveAcc.rzero$getRaidMap().values()) {
                oldRaid.stop();
            }
            liveAcc.rzero$getRaidMap().clear();
            liveAcc.rzero$getRaidMap().putAll(freshAcc.rzero$getRaidMap());
            liveAcc.rzero$setNextAvailableID(freshAcc.rzero$getNextAvailableID());
            liveAcc.rzero$setTick(freshAcc.rzero$getTick());
            level.getRaids().setDirty();
        } catch (Exception e) {
            RZero.LOGGER.warn("[RZero] Raids restore for {} failed: {}",
                    level.dimension().location(), e.getMessage());
        }
    }
}
