package ru.reset.rzero.util;

import java.util.UUID;

public final class LootSeed {
    private LootSeed() {}

    public static final long EQUIPMENT_SALT = 0x9E3779B97F4A7C15L;
    public static final long CHEST_SALT = 0x5F3759DF7F4A7C15L;

    public static long mix(UUID uuid, long levelSeed) {
        long h = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits() ^ levelSeed;
        h ^= (h >>> 30);
        h *= 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 27);
        h *= 0x94D049BB133111EBL;
        h ^= (h >>> 31);
        return h == 0L ? 0xA5A5A5A5A5A5A5A5L : h;
    }

    public static long mixPos(long posLong, long levelSeed) {
        long h = posLong ^ levelSeed ^ CHEST_SALT;
        h ^= (h >>> 30);
        h *= 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 27);
        h *= 0x94D049BB133111EBL;
        h ^= (h >>> 31);
        return h == 0L ? 0xA5A5A5A5A5A5A5A5L : h;
    }
}

