package ru.reset.rzero.checkpoint.restore;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import ru.reset.rzero.RZero;
import ru.reset.rzero.checkpoint.data.CheckpointData;
import ru.reset.rzero.checkpoint.player.AdvancementSnapshot;
import ru.reset.rzero.checkpoint.player.OfflinePlayerFiles;
import ru.reset.rzero.checkpoint.player.PlayerData;
import ru.reset.rzero.config.RZeroCheckpointPolicy;
import ru.reset.rzero.runtime.RZeroRuntime;
import ru.reset.rzero.runtime.RestoreQueues;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PlayerRestorer {

    private PlayerRestorer() {
    }

    public static void restoreAll(MinecraftServer server, CheckpointData data, ServerLevel targetLevel) {
        RestoreQueues.pendingOfflineRollbacks.clear();
        RZeroCheckpointPolicy policy = RZeroRuntime.effectivePolicy(data);

        List<ServerPlayer> currentPlayers = new ArrayList<>(server.getPlayerList().getPlayers());
        for (Map.Entry<UUID, PlayerData> entry : data.playersData.entrySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(entry.getKey());
            if (p == null) {
                RestoreQueues.pendingOfflineRollbacks.put(entry.getKey(), entry.getValue());
            } else {
                ru.reset.rzero.util.RZBenchmark.addPlayers(1);
                long t1 = System.nanoTime();
                restoreOne(server, p, entry.getValue(), targetLevel, policy);
                ru.reset.rzero.util.RZBenchmark.accum(ru.reset.rzero.util.RZBenchmark.Phase.PLAYERS_ONLINE, t1);
            }
        }
        resetPlayersOutsideTimeline(currentPlayers, data, server, targetLevel.registryAccess(), policy);
    }


    public static void restoreOfflineFiles(MinecraftServer server, CheckpointData data, RZeroCheckpointPolicy policy) {
        File playerDir = OfflinePlayerFiles.playerDataDir(server);
        File statsDir = OfflinePlayerFiles.playerStatsDir(server);

        Map<UUID, CompoundTag> datCopy = Map.copyOf(data.rawPlayersNbt);
        boolean restoreStats = policy.rollback().players().stats() && !data.rawPlayerStats.isEmpty();
        Map<UUID, String> statsCopy = restoreStats ? Map.copyOf(data.rawPlayerStats) : Map.of();

        if (!datCopy.isEmpty() || !statsCopy.isEmpty()) {
            CompletableFuture.runAsync(() -> {
                OfflinePlayerFiles.writeDatFiles(playerDir, datCopy);
                if (!statsCopy.isEmpty()) {
                    OfflinePlayerFiles.writeStatsFiles(statsDir, statsCopy);
                }
            }, Util.ioPool());
        }

        if (restoreStats) {
            try {
                var statsMap = ((ru.reset.rzero.mixin.player.MixinPlayerList) server.getPlayerList()).rzero$getStats();
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    statsMap.remove(p.getUUID());
                    p.getStats().markAllDirty();
                }
            } catch (Throwable t) {
                RZero.LOGGER.warn("[RZero] Failed to reload in-memory player stats: {}", t.getMessage());
            }
        }
    }

    private static void restoreOne(MinecraftServer server,
                                   ServerPlayer p,
                                   PlayerData pd,
                                   ServerLevel targetLevel,
                                   RZeroCheckpointPolicy policy) {

        boolean wasRemoved = p.isRemoved();
        boolean isDead = p.getHealth() <= 0.0F || p.isDeadOrDying();
        boolean sameDimension = p.serverLevel() == targetLevel;

        if (wasRemoved) {
            ((ru.reset.rzero.mixin.entity.MixinEntityAccessor) p).rzero$unsetRemoved();
        }

        if (isDead && sameDimension) {
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundRespawnPacket(p.createCommonSpawnInfo(targetLevel), (byte) 3));
        }

        pd.applyTo(p, server, targetLevel.registryAccess(), policy);

        if (wasRemoved) {
            targetLevel.addRespawnedPlayer(p);
        }

        AdvancementSnapshot.apply(p, server, pd.advancements);

        RestoreQueues.pendingRides.remove(p.getUUID());
        if (pd.vehicleUUID != null) {
            RestoreQueues.pendingRides.put(p.getUUID(), pd.vehicleUUID);
        }
    }

    private static void resetPlayersOutsideTimeline(List<ServerPlayer> currentPlayers,
                                                    CheckpointData data,
                                                    MinecraftServer server,
                                                    net.minecraft.core.RegistryAccess registryAccess,
                                                    RZeroCheckpointPolicy policy) {
        for (ServerPlayer p : currentPlayers) {
            if (data.playersData.containsKey(p.getUUID())) {
                continue;
            }

            boolean wasRemoved = p.isRemoved();
            boolean isDead = p.getHealth() <= 0.0F || p.isDeadOrDying();

            if (wasRemoved) {
                ((ru.reset.rzero.mixin.entity.MixinEntityAccessor) p).rzero$unsetRemoved();
            }

            if (isDead) {
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundRespawnPacket(p.createCommonSpawnInfo(p.serverLevel()), (byte) 3));
            }

            if (data.rawPlayersNbt.containsKey(p.getUUID())) {
                CompoundTag tag = data.rawPlayersNbt.get(p.getUUID());
                try {
                    p.load(tag);
                    PlayerData pd = PlayerData.captureFrom(p, registryAccess);
                    pd.applyTo(p, server, registryAccess, policy);
                } catch (Exception e) {
                    ru.reset.rzero.RZero.LOGGER.error("Failed to live-restore offline player " + p.getName().getString(), e);
                }
            } else {
                if (!policy.rollback().players().preserveNewPlayerInventory()) {
                    p.getInventory().clearContent();
                    p.setExperienceLevels(0);
                    p.setExperiencePoints(0);
                    p.setHealth(p.getMaxHealth());
                    p.getFoodData().setFoodLevel(20);
                    p.removeAllEffects();

                    BlockPos spawnPos = p.serverLevel().getSharedSpawnPos();
                    float spawnAngle = p.serverLevel().getSharedSpawnAngle();
                    p.teleportTo(p.serverLevel(),
                            spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, spawnAngle, 0);
                }
            }

            if (wasRemoved) {
                p.serverLevel().addRespawnedPlayer(p);
            }
        }
    }
}
