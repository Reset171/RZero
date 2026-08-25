package ru.reset.rzero.engine;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.Deque;

public final class RZeroRandomMask {
    private record Frame(Level level, RandomSource saved, RandomSource pushed) {}

    private static final ThreadLocal<Deque<Frame>> STACK =
            ThreadLocal.withInitial(ArrayDeque::new);

    private RZeroRandomMask() {}

    public static void push(Level level, RandomSource saved, RandomSource pushed) {
        STACK.get().push(new Frame(level, saved, pushed));
    }

    public static RandomSource pop(Level level) {
        Deque<Frame> s = STACK.get();
        if (s.isEmpty()) return null;
        Frame frame = s.peek();
        if (frame.level != level) return null;
        return s.pop().saved;
    }

    public static RandomSource resetLevel(Level level) {
        Deque<Frame> s = STACK.get();
        RandomSource restored = null;
        while (!s.isEmpty()) {
            Frame frame = s.peek();
            if (frame.level != level) break;
            restored = s.pop().saved;
        }
        return restored;
    }

    public static RandomSource peek() {
        Deque<Frame> s = STACK.get();
        if (s.isEmpty()) return null;
        return s.peek().pushed;
    }
}
