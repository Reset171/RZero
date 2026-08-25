package ru.reset.rzero.engine;

import net.minecraft.world.entity.Entity;

import java.lang.reflect.Method;

public final class EntityIdCounter {
    private static final Method GET;
    private static final Method SET;

    static {
        try {
            GET = Entity.class.getDeclaredMethod("rzero$getEntityCounter");
            SET = Entity.class.getDeclaredMethod("rzero$setEntityCounter", int.class);
            GET.setAccessible(true);
            SET.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(
                    "MixinEntity failed to weave rzero$ counter accessors — check mixin config: " + e);
        }
    }

    private EntityIdCounter() {}

    public static int get() {
        try {
            return (int) GET.invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void set(int val) {
        try {
            SET.invoke(null, val);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
