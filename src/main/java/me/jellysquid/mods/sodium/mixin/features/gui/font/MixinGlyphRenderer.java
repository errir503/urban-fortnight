package me.jellysquid.mods.sodium.mixin.features.gui.font;

import me.jellysquid.mods.sodium.client.render.vertex.formats.GlyphVertex;
import me.jellysquid.mods.sodium.client.render.vertex.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import net.minecraft.client.font.GlyphRenderer;
import net.minecraft.client.render.VertexConsumer;
import org.joml.Math;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GlyphRenderer.class)
public class MixinGlyphRenderer {
    @Shadow
    @Final
    private float minX;

    @Shadow
    @Final
    private float maxX;

    @Shadow
    @Final
    private float minY;

    @Shadow
    @Final
    private float maxY;

    @Shadow
    @Final
    private float minU;

    @Shadow
    @Final
    private float minV;

    @Shadow
    @Final
    private float maxV;

    @Shadow
    @Final
    private float maxU;

    /**
     * @reason Use intrinsics
     * @author JellySquid
     */
    @Overwrite
    public void draw(boolean italic, float x, float y, Matrix4f matrix, VertexConsumer vertexConsumer, float red, float green, float blue, float alpha, int light) {
        float x1 = x + this.minX;
        float x2 = x + this.maxX;
        float y1 = this.minY - 3.0F;
        float y2 = this.maxY - 3.0F;
        float h1 = y + y1;
        float h2 = y + y2;
        float w1 = italic ? 1.0F - 0.25F * y1 : 0.0F;
        float w2 = italic ? 1.0F - 0.25F * y2 : 0.0F;

        int color = ColorABGR.pack(red, green, blue, alpha);

        var writer = VertexBufferWriter.of(vertexConsumer);

        try (MemoryStack stack = VertexBufferWriter.STACK.push()) {
            long buffer = writer.buffer(stack, 4, GlyphVertex.STRIDE, GlyphVertex.FORMAT);
            long ptr = buffer;

            write(ptr, matrix, x1 + w1, h1, 0.0F, color, this.minU, this.minV, light);
            ptr += GlyphVertex.STRIDE;

            write(ptr, matrix, x1 + w2, h2, 0.0F, color, this.minU, this.maxV, light);
            ptr += GlyphVertex.STRIDE;

            write(ptr, matrix, x2 + w2, h2, 0.0F, color, this.maxU, this.maxV, light);
            ptr += GlyphVertex.STRIDE;

            write(ptr, matrix, x2 + w1, h1, 0.0F, color, this.maxU, this.minV, light);
            ptr += GlyphVertex.STRIDE;

            writer.push(buffer, 4, GlyphVertex.STRIDE, GlyphVertex.FORMAT);
        }
    }

    private static void write(long buffer,
                              Matrix4f matrix, float x, float y, float z, int color, float u, float v, int light) {
        float x2 = Math.fma(matrix.m00(), x, Math.fma(matrix.m10(), y, Math.fma(matrix.m20(), z, matrix.m30())));
        float y2 = Math.fma(matrix.m01(), x, Math.fma(matrix.m11(), y, Math.fma(matrix.m21(), z, matrix.m31())));
        float z2 = Math.fma(matrix.m02(), x, Math.fma(matrix.m12(), y, Math.fma(matrix.m22(), z, matrix.m32())));

        GlyphVertex.write(buffer, x2, y2, z2, color, u, v, light);
    }

}
