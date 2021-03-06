package net.caffeinemc.sodium.render.vertex.buffer;

import net.caffeinemc.gfx.api.buffer.BufferVertexFormat;
import net.caffeinemc.sodium.render.vertex.VertexSink;
import net.caffeinemc.sodium.render.vertex.type.BufferVertexType;

/**
 * Base implementation of a {@link VertexSink} which writes into a {@link VertexBufferView} directly.
 */
public abstract class VertexBufferWriter implements VertexSink {
    protected final VertexBufferView backingBuffer;

    protected final BufferVertexFormat vertexFormat;
    protected final int vertexStride;

    private int vertexCount;
    private int vertexCountFlushed;

    protected VertexBufferWriter(VertexBufferView backingBuffer, BufferVertexType<?> vertexType) {
        this.backingBuffer = backingBuffer;

        this.vertexFormat = vertexType.getBufferVertexFormat();
        this.vertexStride = this.vertexFormat.stride();

        this.onBufferStorageChanged();
    }

    @Override
    public void ensureCapacity(int count) {
        if (this.backingBuffer.ensureBufferCapacity((this.vertexCount + count) * this.vertexStride)) {
            this.onBufferStorageChanged();
        }
    }

    @Override
    public void flush() {
        this.backingBuffer.flush(this.vertexCount, this.vertexFormat);
        this.vertexCountFlushed += this.vertexCount;
        this.vertexCount = 0;
    }

    /**
     * Advances the write pointer forward by the stride of one vertex. This should always be called after a
     * vertex is written. Implementations which override this should always call invoke the super implementation.
     */
    protected void advance() {
        this.vertexCount++;
    }

    /**
     * Called when the underlying memory buffer to the backing storage changes. When this is called, the implementation
     * should update any pointers
     */
    protected abstract void onBufferStorageChanged();
}
