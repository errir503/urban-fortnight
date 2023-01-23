package me.jellysquid.mods.sodium.client.render.vertex;

import net.minecraft.client.render.VertexConsumer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public interface VertexBufferWriter {
    // TODO: Move this elsewhere?
    MemoryStack STACK = MemoryStack.create(1024 * 64);
    long BUFFER = MemoryUtil.nmemAlignedAlloc(32, 16 * 1024);

    static VertexBufferWriter of(VertexConsumer consumer) {
        return (VertexBufferWriter) consumer;
    }

    /**
     * Copy the vertices from the source pointer into this vertex buffer. The buffer referenced by {@code ptr} must be
     * that which was returned by {@link VertexBufferWriter#buffer(MemoryStack, int, int, VertexFormatDescription)}.
     * <p>
     * Depending on the implementation, this may not copy any data at all, such as when the source buffer is actually
     * memory from the vertex buffer itself. In that case, this function will only advance the buffer's pointer, making it
     * "zero-copy" when possible.
     * <p>
     * WARNING: The buffer is "consumed" after calling this method, and you cannot read or write to it again. You must
     * obtain a new buffer and write any additional vertices into that instead.
     *
     * @param ptr    The pointer to read vertices from
     * @param count  The number of vertices to copy
     * @param stride The stride (in bytes) between consecutive vertices
     * @param format The format of the vertices
     */
    void push(long ptr, int count, int stride, VertexFormatDescription format);

    /**
     * Creates a temporary buffer from which vertices of the given format and stride can be written into.
     * <p>
     * Depending on the implementation, this may return memory from the vertex buffer itself, such as when the
     * vertex formats match. If this cannot happen, a temporary buffer is allocated on the stack provided by
     * {@code stack}.
     * <p>
     * Since this can allocate on the stack, you probably don't want to allocate too much memory here. A few vertices
     * should be enough for most use cases. You should also make sure that the stack is popped before calling this
     * function again.
     *
     * @param stack  The memory stack to allocate on, if a zero-copy transfer is not possible
     * @param count  The number of vertices to allocate
     * @param stride The stride (in bytes) between consecutive vertices
     * @param format The format of the vertices
     * @return A pointer to an uninitialized buffer of {@code count} vertices of the specified format
     */
    default long buffer(MemoryStack stack, int count, int stride, VertexFormatDescription format) {
        return stack.nmalloc(count * stride);
    }
}
