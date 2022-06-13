package net.caffeinemc.sodium.render.sequence;

import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.buffer.ImmutableBufferFlags;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.api.types.ElementFormat;

import java.util.EnumSet;

import org.lwjgl.system.MemoryUtil;

public class SequenceIndexBuffer {
    private final RenderDevice device;
    private final SequenceBuilder builder;

    private Buffer buffer;
    private int maxVertices;

    public SequenceIndexBuffer(RenderDevice device, SequenceBuilder builder) {
        this.device = device;
        this.builder = builder;
    }

    public void ensureCapacity(int vertexCount) {
        if (vertexCount > this.maxVertices) {
            this.grow(this.getNextSize(vertexCount));
        }
    }

    private int getNextSize(int vertexCount) {
        return Math.max(this.maxVertices * 2, vertexCount + 2048);
    }

    private void grow(int vertexCount) {
        this.delete();

        var verticesPerPrimitive = this.builder.getVerticesPerPrimitive();
        var indicesPerPrimitive = this.builder.getIndicesPerPrimitive();
        var bytesPerIndex = this.builder.getElementFormat().getSize();

        var primitiveCount = vertexCount / verticesPerPrimitive;
        var bufferSize = (long) indicesPerPrimitive * primitiveCount * bytesPerIndex;

        this.buffer = this.device.createBuffer(bufferSize, (buffer) -> {
            var pointer = MemoryUtil.memAddress(buffer);

            for (int primitiveIndex = 0; primitiveIndex < primitiveCount; primitiveIndex++) {
                this.builder.write(pointer + ((long) primitiveIndex * indicesPerPrimitive * bytesPerIndex), primitiveIndex * verticesPerPrimitive);
            }
        }, EnumSet.noneOf(ImmutableBufferFlags.class));

        this.maxVertices = vertexCount;
    }

    public Buffer getBuffer() {
        return this.buffer;
    }

    public void delete() {
        if (this.buffer != null) {
            this.device.deleteBuffer(this.buffer);
            this.buffer = null;
        }
    }

    public ElementFormat getElementFormat() {
        return ElementFormat.UNSIGNED_INT;
    }
}
