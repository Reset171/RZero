package ru.reset.rzero.engine;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;

public final class RZeroRandomMask {
    private static final class Frame {
        Level level;
        RandomSource saved;
        RandomSource pushed;
    }

    private static final class MaskContext {
        final ArrayDeque<Frame> stack = new ArrayDeque<>(64);
        final ArrayDeque<Frame> pool = new ArrayDeque<>(32);
    }

    private static final ThreadLocal<MaskContext> CONTEXT =
            ThreadLocal.withInitial(MaskContext::new);

    private RZeroRandomMask() {}

    public static void push(Level level, RandomSource saved, RandomSource pushed) {
        MaskContext ctx = CONTEXT.get();
        Frame frame = ctx.pool.pollFirst();
        if (frame == null) {
            frame = new Frame();
        }
        frame.level = level;
        frame.saved = saved;
        frame.pushed = pushed;
        ctx.stack.push(frame);
    }

    public static RandomSource pop(Level level) {
        MaskContext ctx = CONTEXT.get();
        if (ctx.stack.isEmpty()) return null;
        Frame frame = ctx.stack.peek();
        if (frame.level != level) return null;
        ctx.stack.pop();
        RandomSource saved = frame.saved;
        recycle(ctx, frame);
        return saved;
    }

    public static RandomSource resetLevel(Level level) {
        MaskContext ctx = CONTEXT.get();
        RandomSource restored = null;
        while (!ctx.stack.isEmpty()) {
            Frame frame = ctx.stack.peek();
            if (frame.level != level) break;
            ctx.stack.pop();
            restored = frame.saved;
            recycle(ctx, frame);
        }
        return restored;
    }

    public static RandomSource peek() {
        MaskContext ctx = CONTEXT.get();
        if (ctx.stack.isEmpty()) return null;
        return ctx.stack.peek().pushed;
    }

    private static void recycle(MaskContext ctx, Frame frame) {
        frame.level = null;
        frame.saved = null;
        frame.pushed = null;
        ctx.pool.addLast(frame);
    }
}
