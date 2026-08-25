package ru.reset.rzero.anchor;

public final class RollbackCooldown {

    private static volatile long lastRollbackTick = Long.MIN_VALUE;

    private RollbackCooldown() {
    }

    public static boolean tryConsume(int cooldownSeconds, long serverTickCount) {
        if (cooldownSeconds <= 0) {
            lastRollbackTick = serverTickCount;
            return true;
        }
        if (lastRollbackTick != Long.MIN_VALUE && serverTickCount >= lastRollbackTick) {
            long cooldownTicks = cooldownSeconds * 20L;
            if (serverTickCount - lastRollbackTick < cooldownTicks) {
                return false;
            }
        }
        lastRollbackTick = serverTickCount;
        return true;
    }

    public static long remainingTicks(int cooldownSeconds, long serverTickCount) {
        if (cooldownSeconds <= 0 || lastRollbackTick == Long.MIN_VALUE
                || serverTickCount < lastRollbackTick) {
            return 0L;
        }
        long remaining = cooldownSeconds * 20L - (serverTickCount - lastRollbackTick);
        return Math.max(0L, remaining);
    }

    public static void reset() {
        lastRollbackTick = Long.MIN_VALUE;
    }
}
