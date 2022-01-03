package me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.glyph;

import me.jellysquid.mods.sodium.render.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.glyph.writer.GlyphVertexBufferWriterNio;
import me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.glyph.writer.GlyphVertexBufferWriterUnsafe;
import me.jellysquid.mods.sodium.interop.vanilla.vertex.formats.glyph.writer.GlyphVertexWriterFallback;
import me.jellysquid.mods.sodium.render.vertex.type.BlittableVertexType;
import me.jellysquid.mods.sodium.interop.vanilla.vertex.VanillaVertexType;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;

public class GlyphVertexType implements VanillaVertexType<GlyphVertexSink>, BlittableVertexType<GlyphVertexSink> {
    @Override
    public GlyphVertexSink createBufferWriter(VertexBufferView buffer, boolean direct) {
        return direct ? new GlyphVertexBufferWriterUnsafe(buffer) : new GlyphVertexBufferWriterNio(buffer);
    }

    @Override
    public GlyphVertexSink createFallbackWriter(VertexConsumer consumer) {
        return new GlyphVertexWriterFallback(consumer);
    }

    @Override
    public VertexFormat getVertexFormat() {
        return GlyphVertexSink.VERTEX_FORMAT;
    }

    @Override
    public BlittableVertexType<GlyphVertexSink> asBlittable() {
        return this;
    }
}
