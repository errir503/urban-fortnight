package me.jellysquid.mods.sodium.mixin.core.pipeline.vertex;

import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.vertex.attributes.CommonVertexAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.ColorAttribute;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.client.render.FixedColorVertexConsumer;
import net.minecraft.client.render.VertexConsumer;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net/minecraft/client/render/OutlineVertexConsumerProvider$OutlineVertexConsumer")
public abstract class MixinOutlineVertexConsumer extends FixedColorVertexConsumer implements VertexBufferWriter {
    @Shadow
    @Final
    private VertexConsumer delegate;

    @Override
    public void push(MemoryStack stack, long ptr, int count, VertexFormatDescription format) {
        transform(ptr, count, format,
                ColorABGR.pack(this.fixedRed, this.fixedGreen, this.fixedBlue, this.fixedAlpha));

        VertexBufferWriter.of(this.delegate)
                .push(stack, ptr, count, format);
    }

    /**
     * Transforms the color element of each vertex to use the specified value.
     *
     * @param ptr    The buffer of vertices to transform
     * @param count  The number of vertices to transform
     * @param format The format of the vertices
     * @param color  The packed color to use for transforming the vertices
     */
    private static void transform(long ptr, int count, VertexFormatDescription format,
                                  int color) {
        long stride = format.stride();
        long offsetColor = format.getElementOffset(CommonVertexAttribute.COLOR);

        for (int vertexIndex = 0; vertexIndex < count; vertexIndex++) {
            ColorAttribute.set(ptr + offsetColor, color);
            ptr += stride;
        }
    }

}