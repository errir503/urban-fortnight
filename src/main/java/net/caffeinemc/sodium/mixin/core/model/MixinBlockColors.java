package net.caffeinemc.sodium.mixin.core.model;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.caffeinemc.sodium.render.terrain.color.ColorSampler;
import net.caffeinemc.sodium.interop.vanilla.mixin.BlockColorProviderRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockColors.class)
public class MixinBlockColors implements BlockColorProviderRegistry {
    private Reference2ReferenceMap<Block, ColorSampler<BlockState>> blocksToColor;

    private static final ColorSampler<?> DEFAULT_PROVIDER = (state, view, pos, tint) -> -1;

    @SuppressWarnings("unchecked")
    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        this.blocksToColor = new Reference2ReferenceOpenHashMap<>();
        this.blocksToColor.defaultReturnValue((ColorSampler<BlockState>) DEFAULT_PROVIDER);
    }

    @Inject(method = "registerColorProvider", at = @At("HEAD"))
    private void preRegisterColor(net.minecraft.client.color.block.BlockColorProvider provider, Block[] blocks, CallbackInfo ci) {
        for (Block block : blocks) {
            this.blocksToColor.put(block, provider::getColor);
        }
    }

    @Override
    public ColorSampler<BlockState> getColorProvider(BlockState state) {
        return this.blocksToColor.get(state.getBlock());
    }
}
