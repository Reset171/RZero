package ru.reset.rzero.serial;

import net.minecraft.nbt.CompoundTag;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class RZBlob {
    public static final RZBlob EMPTY = new RZBlob(new byte[]{0}, 0L);

    private static final ConcurrentHashMap<HashKey, RZBlob> INTERN = new ConcurrentHashMap<>();

    private final byte[] data;
    private final long hash;

    private RZBlob(byte[] data, long hash) {
        this.data = data;
        this.hash = hash;
    }

    public byte[] raw() { return data; }
    public int size() { return data.length; }
    public long hash() { return hash; }

    public static RZBlob of(CompoundTag tag) {
        long t0 = ru.reset.rzero.api.DevHooks.BLOB_PROFILER.startTimer();
        byte[] enc = RZNbt.encode(tag);
        ru.reset.rzero.api.DevHooks.BLOB_PROFILER.onEncode(t0, enc.length);
        if (enc.length == 1 && enc[0] == 0) return EMPTY;
        long h = xx64(enc);
        HashKey key = new HashKey(h, enc);
        RZBlob existing = INTERN.get(key);
        if (existing != null && byteArrayEquals(existing.data, enc)) {
            ru.reset.rzero.api.DevHooks.BLOB_PROFILER.onDedup(true);
            return existing;
        }
        ru.reset.rzero.api.DevHooks.BLOB_PROFILER.onDedup(false);
        RZBlob blob = new RZBlob(enc, h);
        INTERN.putIfAbsent(key, blob);
        return blob;
    }

    public static RZBlob wrap(byte[] enc) {
        if (enc.length == 1 && enc[0] == 0) return EMPTY;
        return new RZBlob(enc, xx64(enc));
    }

    public CompoundTag toCompound() {
        long t0 = ru.reset.rzero.api.DevHooks.BLOB_PROFILER.startTimer();
        try {
            return RZNbt.decode(data);
        } finally {
            ru.reset.rzero.api.DevHooks.BLOB_PROFILER.onDecode(t0);
        }
    }

    public static void clearInternPool() {
        INTERN.clear();
    }

    public static int internSize() { return INTERN.size(); }

    private static long xx64(byte[] data) {
        long h = 0x9E3779B97F4A7C15L ^ data.length;
        int i = 0;
        int len = data.length;
        for (; i + 8 <= len; i += 8) {
            long x = ((long) (data[i]     & 0xFF))
                   | ((long) (data[i + 1] & 0xFF) << 8)
                   | ((long) (data[i + 2] & 0xFF) << 16)
                   | ((long) (data[i + 3] & 0xFF) << 24)
                   | ((long) (data[i + 4] & 0xFF) << 32)
                   | ((long) (data[i + 5] & 0xFF) << 40)
                   | ((long) (data[i + 6] & 0xFF) << 48)
                   | ((long) (data[i + 7] & 0xFF) << 56);
            h ^= mix(x);
            h = Long.rotateLeft(h, 27) * 0x100000001B3L;
        }
        long tail = 0;
        int shift = 0;
        for (; i < len; i++) {
            tail |= ((long) (data[i] & 0xFF)) << shift;
            shift += 8;
        }
        h ^= mix(tail);
        return mix(h);
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static boolean byteArrayEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) if (a[i] != b[i]) return false;
        return true;
    }

    private static final class HashKey {
        final long h;
        final byte[] data;

        HashKey(long h, byte[] data) { this.h = h; this.data = data; }

        @Override public int hashCode() { return (int) (h ^ (h >>> 32)); }

        @Override public boolean equals(Object o) {
            if (!(o instanceof HashKey k)) return false;
            return k.h == h && byteArrayEquals(k.data, data);
        }
    }
}
