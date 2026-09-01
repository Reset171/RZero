package ru.reset.rzero.client.cache.mesh;

import ru.reset.rzero.RZero;

import static org.lwjgl.opengl.GL11C.GL_NO_ERROR;
import static org.lwjgl.opengl.GL11C.GL_OUT_OF_MEMORY;
import static org.lwjgl.opengl.GL11C.glGetError;
import static org.lwjgl.opengl.GL15C.GL_BUFFER_SIZE;
import static org.lwjgl.opengl.GL15C.GL_STATIC_COPY;
import static org.lwjgl.opengl.GL15C.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15C.glBindBuffer;
import static org.lwjgl.opengl.GL15C.glBufferData;
import static org.lwjgl.opengl.GL15C.glDeleteBuffers;
import static org.lwjgl.opengl.GL15C.glGenBuffers;
import static org.lwjgl.opengl.GL15C.glGetBufferParameteri;
import static org.lwjgl.opengl.GL31C.GL_COPY_READ_BUFFER;
import static org.lwjgl.opengl.GL31C.GL_COPY_WRITE_BUFFER;
import static org.lwjgl.opengl.GL31C.glCopyBufferSubData;

public final class GeometryArena {

    private int bufferId;
    private long capacity;
    private long used;

    public static long queryBufferSize(int glBufferId) {
        if (glBufferId == 0) {
            return 0L;
        }
        glBindBuffer(GL_COPY_READ_BUFFER, glBufferId);
        int size = glGetBufferParameteri(GL_COPY_READ_BUFFER, GL_BUFFER_SIZE);
        glBindBuffer(GL_COPY_READ_BUFFER, 0);
        return Integer.toUnsignedLong(size);
    }

    public boolean isAllocated() {
        return this.bufferId != 0;
    }

    public long capacity() {
        return this.capacity;
    }

    public long used() {
        return this.used;
    }

    public long remaining() {
        return this.capacity - this.used;
    }

    public boolean allocate(long bytes) {
        if (bytes <= 0L) {
            return false;
        }
        if (this.bufferId != 0 && bytes <= this.capacity) {
            this.used = 0L;
            return true;
        }
        if (this.bufferId != 0) {
            this.free();
        }

        glGetError();
        int id = glGenBuffers();
        if (id == 0) {
            RZero.LOGGER.warn("[RZero][mesh] glGenBuffers returned 0 — mesh cache unavailable");
            return false;
        }
        glBindBuffer(GL_COPY_WRITE_BUFFER, id);
        glBufferData(GL_COPY_WRITE_BUFFER, bytes, GL_STATIC_COPY);
        int error = glGetError();
        glBindBuffer(GL_COPY_WRITE_BUFFER, 0);

        if (error != GL_NO_ERROR) {
            glDeleteBuffers(id);
            RZero.LOGGER.warn("[RZero][mesh] failed to allocate {} MB geometry arena (gl error 0x{}{}) — "
                            + "falling back to block-data cache",
                    bytes / (1024 * 1024), Integer.toHexString(error),
                    error == GL_OUT_OF_MEMORY ? ", out of video memory" : "");
            return false;
        }

        this.bufferId = id;
        this.capacity = bytes;
        this.used = 0L;
        RZero.logInfo("[RZero][mesh] geometry arena allocated: {} MB", bytes / (1024 * 1024));
        return true;
    }

    public long push(int srcBufferId, long size) {
        if (this.bufferId == 0 || srcBufferId == 0 || size <= 0L) {
            return -1L;
        }
        if (size > this.remaining()) {
            return -1L;
        }
        long offset = this.used;
        glBindBuffer(GL_COPY_READ_BUFFER, srcBufferId);
        glBindBuffer(GL_COPY_WRITE_BUFFER, this.bufferId);
        glCopyBufferSubData(GL_COPY_READ_BUFFER, GL_COPY_WRITE_BUFFER, 0L, offset, size);
        glBindBuffer(GL_COPY_READ_BUFFER, 0);
        glBindBuffer(GL_COPY_WRITE_BUFFER, 0);
        this.used += size;
        return offset;
    }

    public boolean pull(long offset, long size, int dstBufferId) {
        if (this.bufferId == 0 || dstBufferId == 0 || size <= 0L) {
            return false;
        }
        if (offset < 0L || offset + size > this.capacity) {
            return false;
        }
        glBindBuffer(GL_COPY_WRITE_BUFFER, dstBufferId);
        glBufferData(GL_COPY_WRITE_BUFFER, size, GL_STATIC_DRAW);
        glBindBuffer(GL_COPY_READ_BUFFER, this.bufferId);
        glCopyBufferSubData(GL_COPY_READ_BUFFER, GL_COPY_WRITE_BUFFER, offset, 0L, size);
        glBindBuffer(GL_COPY_READ_BUFFER, 0);
        glBindBuffer(GL_COPY_WRITE_BUFFER, 0);
        return true;
    }

    public static boolean resizeBuffer(int glBufferId, long size) {
        if (glBufferId == 0 || size <= 0L) {
            return false;
        }
        glGetError();
        glBindBuffer(GL_COPY_WRITE_BUFFER, glBufferId);
        glBufferData(GL_COPY_WRITE_BUFFER, size, GL_STATIC_COPY);
        int error = glGetError();
        glBindBuffer(GL_COPY_WRITE_BUFFER, 0);
        return error == GL_NO_ERROR;
    }

    public void free() {
        if (this.bufferId != 0) {
            glDeleteBuffers(this.bufferId);
            this.bufferId = 0;
        }
        this.capacity = 0L;
        this.used = 0L;
    }
}
