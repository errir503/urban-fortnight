package net.caffeinemc.sodium.mixin.features.chunk_rendering;

import net.caffeinemc.sodium.interop.vanilla.mixin.PalettedContainerAccessor;
import net.minecraft.world.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PalettedContainer.class)
public class MixinPalettedContainer<T> implements PalettedContainerAccessor<T> {
    @Shadow
    private PalettedContainer.Data<T> data;

    @Override
    public PalettedContainer.Data<T> getData() {
        return this.data;
    }
}
