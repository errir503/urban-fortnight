package me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.quad;

import me.jellysquid.mods.sodium.render.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.quad.writer.QuadVertexBufferWriterNio;
import me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.quad.writer.QuadVertexBufferWriterUnsafe;
import me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.quad.writer.QuadVertexWriterFallback;
import me.jellysquid.mods.sodium.render.vertex.type.BlittableVertexType;
import me.jellysquid.mods.sodium.interop.vanilla.vertex.VanillaVertexType;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;

public class QuadVertexType implements VanillaVertexType<QuadVertexSink>, BlittableVertexType<QuadVertexSink> {
    @Override
    public QuadVertexSink createFallbackWriter(VertexConsumer consumer) {
        return new QuadVertexWriterFallback(consumer);
    }

    @Override
    public QuadVertexSink createBufferWriter(VertexBufferView buffer, boolean direct) {
        return direct ? new QuadVertexBufferWriterUnsafe(buffer) : new QuadVertexBufferWriterNio(buffer);
    }

    @Override
    public VertexFormat getVertexFormat() {
        return QuadVertexSink.VERTEX_FORMAT;
    }

    @Override
    public BlittableVertexType<QuadVertexSink> asBlittable() {
        return this;
    }
}
