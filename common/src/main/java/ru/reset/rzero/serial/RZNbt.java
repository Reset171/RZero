package ru.reset.rzero.serial;

import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public final class RZNbt {
    private RZNbt() {}

    public static final int FORMAT_VERSION = 1;


    public static byte[] encode(CompoundTag root) {
        Writer w = new Writer(256);
        w.writeCompoundBody(root);
        return w.finish();
    }

    public static final class Writer {
        private byte[] buf;
        private int pos;

        public Writer(int initialCapacity) {
            this.buf = new byte[initialCapacity];
            this.pos = 0;
        }

        public void reset() { pos = 0; }

        public byte[] finish() {
            byte[] out = new byte[pos];
            System.arraycopy(buf, 0, out, 0, pos);
            return out;
        }

        public int size() { return pos; }

        private void ensure(int need) {
            if (pos + need > buf.length) {
                int cap = buf.length;
                while (cap < pos + need) cap <<= 1;
                byte[] grown = new byte[cap];
                System.arraycopy(buf, 0, grown, 0, pos);
                buf = grown;
            }
        }

        public void writeCompoundBody(CompoundTag tag) {
            for (String key : tag.getAllKeys()) {
                Tag t = tag.get(key);
                if (t == null) continue;
                writeByte(t.getId());
                writeString(key);
                writeTag(t);
            }
            writeByte(0);
        }

        public void writeTag(Tag t) {
            switch (t.getId()) {
                case Tag.TAG_BYTE   -> writeByte(((ByteTag) t).getAsByte());
                case Tag.TAG_SHORT  -> writeShortLE(((ShortTag) t).getAsShort());
                case Tag.TAG_INT    -> writeVarInt(((IntTag) t).getAsInt());
                case Tag.TAG_LONG   -> writeVarLong(((LongTag) t).getAsLong());
                case Tag.TAG_FLOAT  -> writeIntLE(Float.floatToRawIntBits(((FloatTag) t).getAsFloat()));
                case Tag.TAG_DOUBLE -> writeLongLE(Double.doubleToRawLongBits(((DoubleTag) t).getAsDouble()));
                case Tag.TAG_BYTE_ARRAY -> {
                    byte[] arr = ((ByteArrayTag) t).getAsByteArray();
                    writeVarUInt(arr.length);
                    writeBytes(arr);
                }
                case Tag.TAG_STRING -> writeString(((StringTag) t).getAsString());
                case Tag.TAG_LIST -> {
                    ListTag list = (ListTag) t;
                    int n = list.size();
                    int elemId = list.getElementType();
                    writeByte(elemId);
                    writeVarUInt(n);
                    for (int i = 0; i < n; i++) writeTag(list.get(i));
                }
                case Tag.TAG_COMPOUND -> writeCompoundBody((CompoundTag) t);
                case Tag.TAG_INT_ARRAY -> {
                    int[] arr = ((IntArrayTag) t).getAsIntArray();
                    writeVarUInt(arr.length);
                    for (int v : arr) writeIntLE(v);
                }
                case Tag.TAG_LONG_ARRAY -> {
                    long[] arr = ((LongArrayTag) t).getAsLongArray();
                    writeVarUInt(arr.length);
                    for (long v : arr) writeLongLE(v);
                }
                case Tag.TAG_END -> {  }
                default -> throw new IllegalStateException("Unknown NBT tag id " + t.getId());
            }
        }

        private void writeByte(int v) {
            ensure(1);
            buf[pos++] = (byte) v;
        }

        private void writeShortLE(int v) {
            ensure(2);
            buf[pos] = (byte) v;
            buf[pos + 1] = (byte) (v >>> 8);
            pos += 2;
        }

        private void writeIntLE(int v) {
            ensure(4);
            buf[pos]     = (byte) v;
            buf[pos + 1] = (byte) (v >>> 8);
            buf[pos + 2] = (byte) (v >>> 16);
            buf[pos + 3] = (byte) (v >>> 24);
            pos += 4;
        }

        private void writeLongLE(long v) {
            ensure(8);
            buf[pos]     = (byte) v;
            buf[pos + 1] = (byte) (v >>> 8);
            buf[pos + 2] = (byte) (v >>> 16);
            buf[pos + 3] = (byte) (v >>> 24);
            buf[pos + 4] = (byte) (v >>> 32);
            buf[pos + 5] = (byte) (v >>> 40);
            buf[pos + 6] = (byte) (v >>> 48);
            buf[pos + 7] = (byte) (v >>> 56);
            pos += 8;
        }

        private void writeBytes(byte[] arr) {
            ensure(arr.length);
            System.arraycopy(arr, 0, buf, pos, arr.length);
            pos += arr.length;
        }

        private void writeVarInt(int v) {
            int z = (v << 1) ^ (v >> 31);
            writeVarUInt(z);
        }

        private void writeVarUInt(int v) {
            while ((v & ~0x7F) != 0) {
                ensure(1);
                buf[pos++] = (byte) ((v & 0x7F) | 0x80);
                v >>>= 7;
            }
            ensure(1);
            buf[pos++] = (byte) v;
        }

        private void writeVarLong(long v) {
            long z = (v << 1) ^ (v >> 63);
            while ((z & ~0x7FL) != 0L) {
                ensure(1);
                buf[pos++] = (byte) (((int) z & 0x7F) | 0x80);
                z >>>= 7;
            }
            ensure(1);
            buf[pos++] = (byte) z;
        }

        private void writeString(String s) {
            int len = s.length();
            ensure(len + 5);
            int posBefore = pos;
            pos += 5;
            int byteStart = pos;
            boolean ascii = true;
            for (int i = 0; i < len; i++) {
                char c = s.charAt(i);
                if (c >= 0x80) { ascii = false; break; }
                buf[pos++] = (byte) c;
            }
            if (!ascii) {
                pos = byteStart;
                byte[] enc = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                ensure(enc.length);
                System.arraycopy(enc, 0, buf, pos, enc.length);
                pos += enc.length;
            }
            int byteLen = pos - byteStart;
            int prefixLen = varIntSize(byteLen);
            int shift = 5 - prefixLen;
            if (shift > 0) {
                System.arraycopy(buf, byteStart, buf, byteStart - shift, byteLen);
                pos -= shift;
            }
            int writeAt = posBefore;
            int v = byteLen;
            while ((v & ~0x7F) != 0) {
                buf[writeAt++] = (byte) ((v & 0x7F) | 0x80);
                v >>>= 7;
            }
            buf[writeAt] = (byte) v;
        }

        private static int varIntSize(int v) {
            if ((v & 0xFFFFFF80) == 0) return 1;
            if ((v & 0xFFFFC000) == 0) return 2;
            if ((v & 0xFFE00000) == 0) return 3;
            if ((v & 0xF0000000) == 0) return 4;
            return 5;
        }
    }


    public static CompoundTag decode(byte[] data) {
        Reader r = new Reader(data);
        return r.readCompoundBody();
    }

    public static final class Reader {
        private final byte[] buf;
        private int pos;

        public Reader(byte[] buf) {
            this.buf = buf;
            this.pos = 0;
        }

        public CompoundTag readCompoundBody() {
            CompoundTag out = new CompoundTag();
            for (;;) {
                int id = readByte() & 0xFF;
                if (id == 0) return out;
                String key = readString();
                Tag t = readTag(id);
                out.put(key, t);
            }
        }

        public Tag readTag(int id) {
            return switch (id) {
                case Tag.TAG_BYTE   -> ByteTag.valueOf(readByte());
                case Tag.TAG_SHORT  -> ShortTag.valueOf((short) readShortLE());
                case Tag.TAG_INT    -> IntTag.valueOf(readVarInt());
                case Tag.TAG_LONG   -> LongTag.valueOf(readVarLong());
                case Tag.TAG_FLOAT  -> FloatTag.valueOf(Float.intBitsToFloat(readIntLE()));
                case Tag.TAG_DOUBLE -> DoubleTag.valueOf(Double.longBitsToDouble(readLongLE()));
                case Tag.TAG_BYTE_ARRAY -> {
                    int n = readVarUInt();
                    byte[] arr = new byte[n];
                    System.arraycopy(buf, pos, arr, 0, n);
                    pos += n;
                    yield new ByteArrayTag(arr);
                }
                case Tag.TAG_STRING -> StringTag.valueOf(readString());
                case Tag.TAG_LIST -> {
                    int elemId = readByte() & 0xFF;
                    int n = readVarUInt();
                    ListTag list = new ListTag();
                    if (elemId == 0 && n > 0) throw new IllegalStateException("Empty type list with elements");
                    for (int i = 0; i < n; i++) list.add(readTag(elemId));
                    yield list;
                }
                case Tag.TAG_COMPOUND -> readCompoundBody();
                case Tag.TAG_INT_ARRAY -> {
                    int n = readVarUInt();
                    int[] arr = new int[n];
                    for (int i = 0; i < n; i++) arr[i] = readIntLE();
                    yield new IntArrayTag(arr);
                }
                case Tag.TAG_LONG_ARRAY -> {
                    int n = readVarUInt();
                    long[] arr = new long[n];
                    for (int i = 0; i < n; i++) arr[i] = readLongLE();
                    yield new LongArrayTag(arr);
                }
                case Tag.TAG_END -> EndTag.INSTANCE;
                default -> throw new IllegalStateException("Unknown NBT tag id " + id);
            };
        }

        private byte readByte() { return buf[pos++]; }

        private int readShortLE() {
            int v = (buf[pos] & 0xFF) | ((buf[pos + 1] & 0xFF) << 8);
            pos += 2;
            return (short) v;
        }

        private int readIntLE() {
            int v = (buf[pos]     & 0xFF)
                  | ((buf[pos + 1] & 0xFF) << 8)
                  | ((buf[pos + 2] & 0xFF) << 16)
                  | ((buf[pos + 3] & 0xFF) << 24);
            pos += 4;
            return v;
        }

        private long readLongLE() {
            long v = (buf[pos]     & 0xFFL)
                   | ((buf[pos + 1] & 0xFFL) << 8)
                   | ((buf[pos + 2] & 0xFFL) << 16)
                   | ((buf[pos + 3] & 0xFFL) << 24)
                   | ((buf[pos + 4] & 0xFFL) << 32)
                   | ((buf[pos + 5] & 0xFFL) << 40)
                   | ((buf[pos + 6] & 0xFFL) << 48)
                   | ((buf[pos + 7] & 0xFFL) << 56);
            pos += 8;
            return v;
        }

        private int readVarUInt() {
            int v = 0, shift = 0;
            for (;;) {
                byte b = buf[pos++];
                v |= (b & 0x7F) << shift;
                if ((b & 0x80) == 0) return v;
                shift += 7;
                if (shift > 28) throw new IllegalStateException("varint overflow");
            }
        }

        private int readVarInt() {
            int v = readVarUInt();
            return (v >>> 1) ^ -(v & 1);
        }

        private long readVarLong() {
            long v = 0;
            int shift = 0;
            for (;;) {
                byte b = buf[pos++];
                v |= ((long) (b & 0x7F)) << shift;
                if ((b & 0x80) == 0) return (v >>> 1) ^ -(v & 1);
                shift += 7;
                if (shift > 63) throw new IllegalStateException("varlong overflow");
            }
        }

        private String readString() {
            int n = readVarUInt();
            boolean ascii = true;
            for (int i = 0; i < n; i++) {
                if ((buf[pos + i] & 0x80) != 0) { ascii = false; break; }
            }
            String s;
            if (ascii) {
                char[] chars = new char[n];
                for (int i = 0; i < n; i++) chars[i] = (char) (buf[pos + i] & 0xFF);
                s = new String(chars);
            } else {
                s = new String(buf, pos, n, java.nio.charset.StandardCharsets.UTF_8);
            }
            pos += n;
            return s;
        }
    }
}
