package me.jellysquid.mods.sodium.mixin.features.item;

import me.jellysquid.mods.sodium.client.model.quad.BakedQuadView;
import me.jellysquid.mods.sodium.client.render.immediate.model.BakedModelEncoder;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import me.jellysquid.mods.sodium.client.model.color.interop.ItemColorsExtended;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.client.color.item.ItemColorProvider;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.LocalRandom;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.*;

import java.util.List;

@Mixin(ItemRenderer.class)
public class MixinItemRenderer {
    private final Random random = new LocalRandom(42L);

    @Shadow
    @Final
    private ItemColors colors;

    /**
     * @reason Avoid allocations
     * @author JellySquid
     */
    @Overwrite
    private void renderBakedItemModel(BakedModel model, ItemStack itemStack, int light, int overlay, MatrixStack matrixStack, VertexConsumer vertexConsumer) {
        var writer = VertexBufferWriter.of(vertexConsumer);

        Random random = this.random;
        MatrixStack.Entry matrices = matrixStack.peek();

        ItemColorProvider colorProvider = null;

        if (!itemStack.isEmpty()) {
            colorProvider = ((ItemColorsExtended) this.colors).getColorProvider(itemStack);
        }

        for (Direction direction : DirectionUtil.ALL_DIRECTIONS) {
            random.setSeed(42L);
            List<BakedQuad> quads = model.getQuads(null, direction, random);

            if (!quads.isEmpty()) {
                this.renderBakedItemQuads(matrices, writer, quads, itemStack, colorProvider, light, overlay);
            }
        }

        random.setSeed(42L);
        List<BakedQuad> quads = model.getQuads(null, null, random);

        if (!quads.isEmpty()) {
            this.renderBakedItemQuads(matrices, writer, quads, itemStack, colorProvider, light, overlay);
        }
    }

    @Unique
    @SuppressWarnings("ForLoopReplaceableByForEach")
    private void renderBakedItemQuads(MatrixStack.Entry matrices, VertexBufferWriter writer, List<BakedQuad> quads, ItemStack itemStack, ItemColorProvider colorProvider, int light, int overlay) {
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad bakedQuad = quads.get(i);
            BakedQuadView quad = (BakedQuadView) bakedQuad;

            int color = 0xFFFFFFFF;

            if (colorProvider != null && quad.hasColor()) {
                color = ColorARGB.toABGR((colorProvider.getColor(itemStack, quad.getColorIndex())), 255);
            }

            BakedModelEncoder.writeQuadVertices(writer, matrices, quad, color, light, overlay);

            SpriteUtil.markSpriteActive(quad.getSprite());
        }
    }
}
