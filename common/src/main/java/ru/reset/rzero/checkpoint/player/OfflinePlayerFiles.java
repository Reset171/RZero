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
        File playerDir = playerDataDir(server);
        if (playerDir == null) {
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

    private static File playerDataDir(MinecraftServer server) {
        File dir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile();
        return dir.isDirectory() ? dir : null;
    }
}
