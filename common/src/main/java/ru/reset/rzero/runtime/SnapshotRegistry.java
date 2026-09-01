package ru.reset.rzero.runtime;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import ru.reset.rzero.checkpoint.data.CheckpointData;
import ru.reset.rzero.util.DetOrder;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SnapshotRegistry {

    public static final ConcurrentHashMap<ResourceKey<Level>, CheckpointData> activeSnapshots =
            new ConcurrentHashMap<>();

    public static final Set<UUID> allowedSnapshotEntities = ConcurrentHashMap.newKeySet();

    public static final ConcurrentHashMap<ResourceKey<Level>, LongSet> loadedChunks =
            new ConcurrentHashMap<>();

    private SnapshotRegistry() {
    }

    public static LongSet loadedChunksFor(ResourceKey<Level> dim) {
        return loadedChunks.computeIfAbsent(
                dim, k -> LongSets.synchronize(new LongOpenHashSet()));
    }

    public static boolean hasCheckpoint() {
        return DetOrder.anyValueMatches(activeSnapshots, d -> d.anchorId != null);
    }

    public static boolean allPendingBlockRollbacksEmpty() {
        for (CheckpointData data : activeSnapshots.values()) {
            if (!data.pendingBlockRollbacks.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static java.util.List<ServerPlayer> findAnchorPlayers(MinecraftServer server) {
        java.util.List<ServerPlayer> players = new java.util.ArrayList<>();
        for (var entry : DetOrder.sortedEntries(activeSnapshots, k -> k.location().toString())) {
            CheckpointData data = entry.getValue();
            if (!data.anchorIds.isEmpty()) {
                for (UUID id : data.anchorIds) {
                    ServerPlayer p = server.getPlayerList().getPlayer(id);
                    if (p != null) {
                        players.add(p);
                    }
                }
                return players;
            }
            if (data.anchorId != null) {
                ServerPlayer p = server.getPlayerList().getPlayer(data.anchorId);
                if (p != null) players.add(p);
                return players;
            }
        }
        if (!server.getPlayerList().getPlayers().isEmpty()) {
            players.add(server.getPlayerList().getPlayers().get(0));
        }
        return players;
    }

    public static void clear() {
        activeSnapshots.clear();
        allowedSnapshotEntities.clear();
    }
}
