package ru.reset.rzero.engine;

import net.minecraft.util.RandomSource;

public class BrainRngContext {
    private static final ThreadLocal<RandomSource> CONTEXT = new ThreadLocal<>();

    public static void set(RandomSource random) {
        CONTEXT.set(random);
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static RandomSource get() {
        return CONTEXT.get();
    }
}
