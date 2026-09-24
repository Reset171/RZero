package ru.reset.rzero.checkpoint.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import ru.reset.rzero.RZero;

import java.io.File;
import java.util.Map;
import java.util.UUID;

public final class OfflinePlayerFiles {

    private static final String DAT_SUFFIX = ".dat";

    private OfflinePlayerFiles() {
    }

    public static void backupInto(MinecraftServer server, Map<UUID, CompoundTag> target) {
        File playerDir = playerDataDir(server);
        if (playerDir == null) {
            return;
        }
        File[] files = playerDir.listFiles((dir, name) -> name.endsWith(DAT_SUFFIX));
        if (files == null) {
            return;
        }
        for (File f : files) {
            try {
                String name = f.getName();
                UUID uuid = UUID.fromString(name.substring(0, name.length() - DAT_SUFFIX.length()));
                target.put(uuid, NbtIo.readCompressed(f.toPath(), NbtAccounter.unlimitedHeap()));
            } catch (Exception e) {
                RZero.LOGGER.error("Failed to backup offline player data: " + f, e);
            }
        }
    }

    public static void restoreFrom(MinecraftServer server, Map<UUID, CompoundTag> saved) {
        writeDatFiles(playerDataDir(server), saved);
    }

    public static void writeDatFiles(File playerDir, Map<UUID, CompoundTag> saved) {
        if (playerDir == null || saved.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, CompoundTag> entry : saved.entrySet()) {
            File f = new File(playerDir, entry.getKey() + DAT_SUFFIX);
            try {
                NbtIo.writeCompressed(entry.getValue(), f.toPath());
            } catch (Exception e) {
                RZero.LOGGER.error("Failed to restore offline player data: " + f, e);
            }
        }
    }

    public static void backupStatsInto(MinecraftServer server, Map<UUID, String> target) {
        File statsDir = playerStatsDir(server);
        if (statsDir == null) {
            return;
        }
        File[] files = statsDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            return;
        }
        for (File f : files) {
            try {
                String name = f.getName();
                UUID uuid = UUID.fromString(name.substring(0, name.length() - ".json".length()));
                target.put(uuid, java.nio.file.Files.readString(f.toPath()));
            } catch (Exception e) {
                RZero.LOGGER.error("Failed to backup player stats: " + f, e);
            }
        }
    }

    public static void restoreStatsFrom(MinecraftServer server, Map<UUID, String> saved) {
        writeStatsFiles(playerStatsDir(server), saved);
    }

    public static void writeStatsFiles(File statsDir, Map<UUID, String> saved) {
        if (statsDir == null || saved.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, String> entry : saved.entrySet()) {
            File f = new File(statsDir, entry.getKey() + ".json");
            try {
                java.nio.file.Files.writeString(f.toPath(), entry.getValue());
            } catch (Exception e) {
                RZero.LOGGER.error("Failed to restore player stats: " + f, e);
            }
        }
    }

    public static File playerDataDir(MinecraftServer server) {
        File dir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile();
        return dir.isDirectory() ? dir : null;
    }

    public static File playerStatsDir(MinecraftServer server) {
        File dir = server.getWorldPath(LevelResource.PLAYER_STATS_DIR).toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir.isDirectory() ? dir : null;
    }
}
