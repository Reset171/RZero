package ru.reset.rzero.engine;

import ru.reset.rzero.RZero;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

public class ContextAwareRandomSource implements RandomSource {
    private final RandomSource fallback;

    public ContextAwareRandomSource(RandomSource fallback) {
        this.fallback = fallback;
    }

    @Override
    public RandomSource fork() {
        return fallback.fork();
    }

    @Override
    public PositionalRandomFactory forkPositional() {
        return fallback.forkPositional();
    }

    @Override
    public void setSeed(long seed) {
        fallback.setSeed(seed);
    }

    @Override
    public int nextInt() {
        RandomSource ctx = BrainRngContext.get();
        return ctx != null ? ctx.nextInt() : fallback.nextInt();
    }

    @Override
    public int nextInt(int bound) {
        RandomSource ctx = BrainRngContext.get();
        return ctx != null ? ctx.nextInt(bound) : fallback.nextInt(bound);
    }

    @Override
    public long nextLong() {
        RandomSource ctx = BrainRngContext.get();
        return ctx != null ? ctx.nextLong() : fallback.nextLong();
    }

    @Override
    public boolean nextBoolean() {
        RandomSource ctx = BrainRngContext.get();
        return ctx != null ? ctx.nextBoolean() : fallback.nextBoolean();
    }

    @Override
    public float nextFloat() {
        RandomSource ctx = BrainRngContext.get();
        if (ctx == null) {
            RZero.LOGGER.warn("[RZero] ContextAwareRandomSource used outside BrainRngContext! Non-deterministic RNG!");
        }
        return ctx != null ? ctx.nextFloat() : fallback.nextFloat();
    }

    @Override
    public double nextDouble() {
        RandomSource ctx = BrainRngContext.get();
        return ctx != null ? ctx.nextDouble() : fallback.nextDouble();
    }

    @Override
    public double nextGaussian() {
        RandomSource ctx = BrainRngContext.get();
        return ctx != null ? ctx.nextGaussian() : fallback.nextGaussian();
    }
}
