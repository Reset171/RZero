package ru.reset.rzero.checkpoint.data;

import net.minecraft.nbt.CompoundTag;

public final class ServerGlobalsCache {
    private ServerGlobalsCache() {}

    private static volatile boolean gameRulesDirty = true;
    private static volatile CompoundTag cachedGameRulesTag;

    public static void markGameRulesDirty() {
        gameRulesDirty = true;
        cachedGameRulesTag = null;
    }

    public static boolean isGameRulesDirty() { return gameRulesDirty; }

    public static CompoundTag getCachedGameRulesTag() { return cachedGameRulesTag; }

    public static void setCachedGameRulesTag(CompoundTag tag) {
        cachedGameRulesTag = tag;
        gameRulesDirty = false;
    }
}
