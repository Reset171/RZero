package ru.reset.rzero.anchor;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import ru.reset.rzero.runtime.RZeroRuntime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class AnchorSelector {

    private AnchorSelector() {
    }

    private static List<ServerPlayer> sortedOnline(MinecraftServer server) {
        List<ServerPlayer> players = new ArrayList<>(server.getPlayerList().getPlayers());
        players.sort(Comparator.comparing(p -> p.getUUID().toString()));
        return players;
    }

    public static Set<UUID> resolveAnchors(MinecraftServer server, long gameTime, UUID requester) {
        RZeroAnchorSettings settings = RZeroRuntime.anchorSettings();
        List<ServerPlayer> online = sortedOnline(server);
        Set<UUID> anchors = new LinkedHashSet<>();

        switch (settings.mode()) {
            case EVERYONE -> {
                for (ServerPlayer player : online) {
                    anchors.add(player.getUUID());
                }
            }
            case MULTI -> {
                for (UUID pinned : settings.pinned()) {
                    if (server.getPlayerList().getPlayer(pinned) != null) {
                        anchors.add(pinned);
                    }
                }
            }
            case ROTATING -> {
                if (!online.isEmpty()) {
                    long rotationTicks = Math.max(20L, settings.rotationSeconds() * 20L);
                    int index = (int) Math.floorMod(gameTime / rotationTicks, (long) online.size());
                    anchors.add(online.get(index).getUUID());
                }
            }
            case FIXED -> {
                for (UUID pinned : settings.pinned()) {
                    if (server.getPlayerList().getPlayer(pinned) != null) {
                        anchors.add(pinned);
                        break;
                    }
                }
            }
        }

        if (anchors.isEmpty() && requester != null) {
            anchors.add(requester);
        }
        if (anchors.isEmpty() && !online.isEmpty()) {
            anchors.add(online.get(0).getUUID());
        }
        return anchors;
    }

    public static UUID resolvePrimary(MinecraftServer server, long gameTime, UUID requester) {
        for (UUID anchor : resolveAnchors(server, gameTime, requester)) {
            return anchor;
        }
        return requester;
    }
}
