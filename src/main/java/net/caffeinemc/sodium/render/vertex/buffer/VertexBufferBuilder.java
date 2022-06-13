package net.caffeinemc.sodium.render.vertex.buffer;

import net.caffeinemc.gfx.api.buffer.BufferVertexFormat;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class VertexBufferBuilder implements VertexBufferView {
    private final BufferVertexFormat vertexFormat;
    private final int initialCapacity;

    private ByteBuffer buffer;
    private int writerOffset;
    private int count;
    private int capacity;

    public VertexBufferBuilder(BufferVertexFormat vertexFormat, int initialCapacity) {
        this.vertexFormat = vertexFormat;
        this.buffer = MemoryUtil.memAlloc(initialCapacity);
        this.capacity = initialCapacity;
        this.writerOffset = 0;
        this.initialCapacity = initialCapacity;
    }

    private void grow(int len) {
        // The new capacity will at least as large as the write it needs to service
        int cap = Math.max(this.capacity * 2, this.capacity + len);

        // Update the buffer and capacity now
        this.setBufferSize(cap);
    }

    private void setBufferSize(int cap) {
        this.buffer = MemoryUtil.memRealloc(this.buffer, cap);
        this.capacity = cap;
    }

    @Override
    public boolean ensureBufferCapacity(int bytes) {
        if (this.writerOffset + bytes <= this.capacity) {
            return false;
        }

        this.grow(bytes);

        return true;
    }

    @Override
    public ByteBuffer getDirectBuffer() {
        return this.buffer;
    }

    @Override
    public int getWriterPosition() {
        return this.writerOffset;
    }

    @Override
    public void flush(int vertexCount, BufferVertexFormat format) {
        if (this.vertexFormat != format) {
            throw new IllegalStateException("Mis-matched vertex format (expected: [" + format + "], currently using: [" + this.vertexFormat + "])");
        }

        this.count += vertexCount;
        this.writerOffset = this.count * format.stride();
    }

    @Override
    public BufferVertexFormat getVertexFormat() {
        return this.vertexFormat;
    }

    public boolean isEmpty() {
        return this.count <= 0;
    }

    public int getCount() {
        return this.count;
    }

    public void reset() {
        this.writerOffset = 0;
        this.count = 0;

        this.setBufferSize(this.initialCapacity);
    }

    public void destroy() {
        // this does nothing if the buffer reference is null
        MemoryUtil.memFree(this.buffer);

        this.buffer = null;
    }

    public ByteBuffer slice() {
        return MemoryUtil.memByteBuffer(MemoryUtil.memAddress(this.buffer), this.writerOffset);
    }
}
