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
        if (!hasCheckpoint()) {
            return java.util.Collections.emptyList();
        }
        java.util.Set<UUID> anchors = ru.reset.rzero.anchor.AnchorSelector.resolveAnchors(
                server, server.overworld().getGameTime(), null);
        java.util.List<ServerPlayer> players = new java.util.ArrayList<>();
        for (UUID id : anchors) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                players.add(p);
            }
        }
        if (players.isEmpty() && !server.getPlayerList().getPlayers().isEmpty()) {
            players.add(server.getPlayerList().getPlayers().get(0));
        }
        return players;
    }

    public static void syncActiveSnapshotAnchors(MinecraftServer server) {
        if (server == null) return;
        java.util.Set<UUID> anchors = ru.reset.rzero.anchor.AnchorSelector.resolveAnchors(
                server, server.overworld().getGameTime(), null);
        UUID primary = anchors.isEmpty() ? null : anchors.iterator().next();
        for (CheckpointData data : activeSnapshots.values()) {
            data.anchorId = primary;
            data.anchorIds.clear();
            data.anchorIds.addAll(anchors);
            data.setDirty();
        }
    }

    public static void clear() {
        activeSnapshots.clear();
        allowedSnapshotEntities.clear();
    }
}
