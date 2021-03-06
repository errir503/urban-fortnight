package net.caffeinemc.sodium.mixin.features.entity.fast_render;

import net.caffeinemc.sodium.interop.vanilla.mixin.ModelCuboidAccessor;
import net.minecraft.client.model.ModelPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ModelPart.Cuboid.class)
public class MixinCuboid implements ModelCuboidAccessor {
    @Shadow
    @Final
    private ModelPart.Quad[] sides;

    @Override
    public ModelPart.Quad[] getQuads() {
        return this.sides;
    }
}
