package ru.reset.rzero.engine;

import ru.reset.rzero.RZero;

import net.minecraft.world.entity.ai.Brain;
import sun.misc.Unsafe;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class BrainCloner {
    private static Unsafe unsafe;
    private static final java.util.concurrent.ConcurrentHashMap<Class<?>, CloneAction> ACTION_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.ConcurrentHashMap<Class<?>, FastField[]> FIELD_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    enum CloneAction {
        RETURN_AS_IS,
        CLONE_ENTITY,
        CLONE_ARRAY,
        CLONE_MAP,
        CLONE_SET,
        CLONE_LIST,
        CLONE_OPTIONAL,
        CLONE_UNSAFE,
        CLONE_ATOMIC_LONG,
        CLONE_ATOMIC_INT,
        CLONE_BITSET
    }

    static abstract class FastField {
        protected long offset;
        FastField(long offset) { this.offset = offset; }
        abstract void copy(Object from, Object to, Map<Object, Object> visited, java.util.function.Function<net.minecraft.world.entity.Entity, net.minecraft.world.entity.Entity> entityRemapper);
    }
    static class IntField extends FastField {
        IntField(long offset) { super(offset); }
        void copy(Object from, Object to, Map<Object, Object> visited, java.util.function.Function<net.minecraft.world.entity.Entity, net.minecraft.world.entity.Entity> entityRemapper) {
            unsafe.putInt(to, offset, unsafe.getInt(from, offset));
        }
    }
    static class LongField extends FastField {
        LongField(long offset) { super(offset); }
        void copy(Object from, Object to, Map<Object, Object> visited, java.util.function.Function<net.minecraft.world.entity.Entity, net.minecraft.world.entity.Entity> entityRemapper) {
            unsafe.putLong(to, offset, unsafe.getLong(from, offset));
        }
    }
    static class BooleanField extends FastField {
        BooleanField(long offset) { super(offset); }
        void copy(Object from, Object to, Map<Object, Object> visited, java.util.function.Function<net.minecraft.world.entity.Entity, net.minecraft.world.entity.Entity> entityRemapper) {
            unsafe.putBoolean(to, offset, unsafe.getBoolean(from, offset));
        }
    }
    static class DoubleField extends FastField {
        DoubleField(long offset) { super(offset); }
        void copy(Object from, Object to, Map<Object, Object> visited, java.util.function.Function<net.minecraft.world.entity.Entity, net.minecraft.world.entity.Entity> entityRemapper) {
            unsafe.putDouble(to, offset, unsafe.getDouble(from, offset));
        }
    }
    static class FloatField extends FastField {
        FloatField(long offset) { super(offset); }
        void copy(Object from, Object to, Map<Object, Object> visited, java.util.function.Function<net.minecraft.world.entity.Entity, net.minecraft.world.entity.Entity> entityRemapper) {
            unsafe.putFloat(to, offset, unsafe.getFloat(from, offset));
        }
    }
    static class ByteField extends FastField {
        ByteField(long offset) { super(offset); }
        void copy(Object from, Object to, Map<Object, Object> visited, java.util.function.Function<net.minecraft.world.entity.Entity, net.minecraft.world.entity.Entity> entityRemapper) {
            unsafe.putByte(to, offset, unsafe.getByte(from, offset));
        }
    }
    static class ShortField extends FastField {
        ShortField(long offset) { super(offset); }
        void copy(Object from, Object to, Map<Object, Object> visited, java.util.function.Function<net.minecraft.world.entity.Entity, net.minecraft.world.entity.Entity> entityRemapper) {
            unsafe.putShort(to, offset, unsafe.getShort(from, offset));
        }
    }
    static class CharField extends FastField {
        CharField(long offset) { super(offset); }
        void copy(Object from, Object to, Map<Object, Object> visited, java.util.function.Function<net.minecraft.world.entity.Entity, net.minecraft.world.entity.Entity> entityRemapper) {
            unsafe.putChar(to, offset, unsafe.getChar(from, offset));
        }
    }
    static class ObjectField extends FastField {
        ObjectField(long offset) { super(offset); }
        void copy(Object from, Object to, Map<Object, Object> visited, java.util.function.Function<net.minecraft.world.entity.Entity, net.minecraft.world.entity.Entity> entityRemapper) {
            Object fieldVal = unsafe.getObject(from, offset);
            unsafe.putObject(to, offset, deepClone(fieldVal, visited, entityRemapper));
        }
    }

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
        } catch (Exception e) {
            RZero.LOGGER.error("[RZero] Failed to initialize Unsafe for BrainCloner", e);
        }
    }

    private static CloneAction getAction(Class<?> clazz, Object obj) {
        return ACTION_CACHE.computeIfAbsent(clazz, c -> determineAction(c, obj));
    }

    private static CloneAction determineAction(Class<?> clazz, Object obj) {
        if (obj instanceof java.util.concurrent.atomic.AtomicLong) return CloneAction.CLONE_ATOMIC_LONG;
        if (obj instanceof java.util.concurrent.atomic.AtomicInteger) return CloneAction.CLONE_ATOMIC_INT;

        if (clazz.isPrimitive() || clazz == String.class || clazz == Boolean.class ||
                Number.class.isAssignableFrom(clazz) || clazz.isEnum() || obj instanceof Enum<?> || clazz == Class.class) {
            return CloneAction.RETURN_AS_IS;
        }

        if (net.minecraft.world.entity.ai.memory.MemoryModuleType.class.isAssignableFrom(clazz) ||
            net.minecraft.world.entity.ai.sensing.SensorType.class.isAssignableFrom(clazz) ||
            net.minecraft.world.entity.schedule.Activity.class.isAssignableFrom(clazz) ||
            net.minecraft.resources.ResourceKey.class.isAssignableFrom(clazz) ||
            net.minecraft.resources.ResourceLocation.class.isAssignableFrom(clazz) ||
            net.minecraft.core.Holder.class.isAssignableFrom(clazz) ||
            net.minecraft.world.entity.EntityType.class.isAssignableFrom(clazz)) {
            return CloneAction.RETURN_AS_IS;
        }

        if (clazz.getName().contains("$$Lambda")) {
            return CloneAction.RETURN_AS_IS;
        }
        
        String pkg = clazz.getName();
        if (pkg.startsWith("java.") || pkg.startsWith("javax.") || pkg.startsWith("sun.") || pkg.startsWith("jdk.") || pkg.startsWith("org.apache.") || pkg.startsWith("com.mojang.datafixers.")) {
            if (!pkg.startsWith("java.util.") && !pkg.startsWith("java.lang.Enum") && clazz != java.util.UUID.class && clazz != java.util.BitSet.class && clazz != java.util.Optional.class) {
                return CloneAction.RETURN_AS_IS;
            }
        }

        if (clazz.isRecord()) {
            return CloneAction.RETURN_AS_IS;
        }

        if (obj instanceof net.minecraft.world.level.LevelAccessor || obj instanceof net.minecraft.world.level.Level) {
            return CloneAction.RETURN_AS_IS;
        }

        if (obj instanceof net.minecraft.world.entity.Entity) {
            return CloneAction.CLONE_ENTITY;
        }

        if (obj instanceof net.minecraft.world.level.block.state.BlockState) {
            return CloneAction.RETURN_AS_IS;
        }

        if (obj instanceof net.minecraft.core.BlockPos) {
            return CloneAction.RETURN_AS_IS;
        }

        if (obj instanceof com.google.common.collect.ImmutableCollection || 
            obj instanceof com.google.common.collect.ImmutableMap) {
            return CloneAction.RETURN_AS_IS;
        }

        if (clazz.isArray()) {
            return CloneAction.CLONE_ARRAY;
        }

        if (clazz.getName().startsWith("java.util.")) {
            if (obj instanceof Map) return CloneAction.CLONE_MAP;
            if (obj instanceof Set) return CloneAction.CLONE_SET;
            if (obj instanceof List) return CloneAction.CLONE_LIST;
            if (obj instanceof java.util.UUID) return CloneAction.RETURN_AS_IS;
            if (obj instanceof java.util.BitSet) return CloneAction.CLONE_BITSET;
            if (obj instanceof Optional) return CloneAction.CLONE_OPTIONAL;
        }

        if (obj instanceof Optional) {
            return CloneAction.CLONE_OPTIONAL;
        }

        return CloneAction.CLONE_UNSAFE;
    }

    private static FastField[] getCachedFields(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, k -> {
            List<FastField> validFields = new ArrayList<>();
            Class<?> current = k;
            while (current != null && current != Object.class) {
                for (Field field : current.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) continue;
                    try {
                        long offset = unsafe.objectFieldOffset(field);
                        Class<?> type = field.getType();
                        if (type == int.class) validFields.add(new IntField(offset));
                        else if (type == long.class) validFields.add(new LongField(offset));
                        else if (type == boolean.class) validFields.add(new BooleanField(offset));
                        else if (type == double.class) validFields.add(new DoubleField(offset));
                        else if (type == float.class) validFields.add(new FloatField(offset));
                        else if (type == byte.class) validFields.add(new ByteField(offset));
                        else if (type == short.class) validFields.add(new ShortField(offset));
                        else if (type == char.class) validFields.add(new CharField(offset));
                        else validFields.add(new ObjectField(offset));
                    } catch (Exception ignored) {
                    }
                }
                current = current.getSuperclass();
            }
            return validFields.toArray(new FastField[0]);
        });
    }

    public static <T> T deepClone(T obj) {
        return deepClone(obj, null);
    }

    public static <T> T deepClone(T obj, java.util.function.Function<net.minecraft.world.entity.Entity, net.minecraft.world.entity.Entity> entityRemapper) {
        if (unsafe == null) return obj;
        return deepClone(obj, new IdentityHashMap<>(), entityRemapper);
    }

    @SuppressWarnings("unchecked")
    private static <T> T deepClone(T obj, Map<Object, Object> visited, java.util.function.Function<net.minecraft.world.entity.Entity, net.minecraft.world.entity.Entity> entityRemapper) {
        if (obj == null) return null;
        Class<?> clazz = obj.getClass();

        CloneAction action = getAction(clazz, obj);

        switch (action) {
            case RETURN_AS_IS:
                return obj;
            case CLONE_ENTITY:
                if (entityRemapper != null) return (T) entityRemapper.apply((net.minecraft.world.entity.Entity) obj);
                return obj;
        }

        if (visited.containsKey(obj)) {
            return (T) visited.get(obj);
        }

        switch (action) {
            case CLONE_ATOMIC_LONG: {
                java.util.concurrent.atomic.AtomicLong clone = new java.util.concurrent.atomic.AtomicLong(((java.util.concurrent.atomic.AtomicLong) obj).get());
                visited.put(obj, clone);
                return (T) clone;
            }
            case CLONE_ATOMIC_INT: {
                java.util.concurrent.atomic.AtomicInteger clone = new java.util.concurrent.atomic.AtomicInteger(((java.util.concurrent.atomic.AtomicInteger) obj).get());
                visited.put(obj, clone);
                return (T) clone;
            }
            case CLONE_MAP: {
                if (obj instanceof EnumMap) {
                    EnumMap clonedMap = new EnumMap((EnumMap) obj);
                    visited.put(obj, clonedMap);
                    for (Map.Entry<?, ?> entry : ((Map<?, ?>) obj).entrySet()) {
                        clonedMap.put((Enum) entry.getKey(), deepClone(entry.getValue(), visited, entityRemapper));
                    }
                    return (T) clonedMap;
                }
                Map<Object, Object> clonedMap;
                if (obj instanceof TreeMap) {
                    clonedMap = new TreeMap<Object, Object>(((TreeMap<Object, Object>) obj).comparator());
                } else {
                    clonedMap = new LinkedHashMap<>();
                }
                visited.put(obj, clonedMap);
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) obj).entrySet()) {
                    clonedMap.put(deepClone(entry.getKey(), visited, entityRemapper), deepClone(entry.getValue(), visited, entityRemapper));
                }
                return (T) clonedMap;
            }
            case CLONE_SET: {
                if (obj instanceof EnumSet) {
                    EnumSet clonedSet = EnumSet.copyOf((EnumSet) obj);
                    visited.put(obj, clonedSet);
                    return (T) clonedSet;
                }
                Set<Object> clonedSet;
                if (obj instanceof TreeSet) {
                    clonedSet = new TreeSet<Object>(((TreeSet<Object>) obj).comparator());
                } else {
                    clonedSet = new LinkedHashSet<>();
                }
                visited.put(obj, clonedSet);
                for (Object item : (Set<?>) obj) {
                    clonedSet.add(deepClone(item, visited, entityRemapper));
                }
                return (T) clonedSet;
            }
            case CLONE_LIST: {
                List<Object> clonedList = new ArrayList<>();
                if (obj instanceof LinkedList) {
                    clonedList = new LinkedList<>();
                }
                visited.put(obj, clonedList);
                for (Object item : (List<?>) obj) {
                    clonedList.add(deepClone(item, visited, entityRemapper));
                }
                return (T) clonedList;
            }
            case CLONE_BITSET: {
                java.util.BitSet clonedSet = (java.util.BitSet) ((java.util.BitSet) obj).clone();
                visited.put(obj, clonedSet);
                return (T) clonedSet;
            }
            case CLONE_OPTIONAL: {
                Optional<?> opt = (Optional<?>) obj;
                Optional<?> clone = opt.isPresent() ? Optional.of(deepClone(opt.get(), visited, entityRemapper)) : Optional.empty();
                visited.put(obj, clone);
                return (T) clone;
            }
            case CLONE_ARRAY: {
                int length = java.lang.reflect.Array.getLength(obj);
                Object clonedArray = java.lang.reflect.Array.newInstance(clazz.getComponentType(), length);
                visited.put(obj, clonedArray);
                for (int i = 0; i < length; i++) {
                    java.lang.reflect.Array.set(clonedArray, i, deepClone(java.lang.reflect.Array.get(obj, i), visited, entityRemapper));
                }
                return (T) clonedArray;
            }
            case CLONE_UNSAFE: {
                try {
                    T clone = (T) unsafe.allocateInstance(clazz);
                    visited.put(obj, clone);

                    FastField[] fields = getCachedFields(clazz);
                    for (FastField field : fields) {
                        try {
                            field.copy(obj, clone, visited, entityRemapper);
                        } catch (Exception ignored) {
                        }
                    }
                    return clone;
                } catch (Exception e) {
                    RZero.LOGGER.error("[RZero] Error cloning " + clazz.getName(), e);
                    return obj;
                }
            }
        }
        return obj;
    }
}
