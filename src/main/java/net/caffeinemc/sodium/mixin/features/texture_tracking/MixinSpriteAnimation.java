package net.caffeinemc.sodium.mixin.features.texture_tracking;

import java.util.List;
import net.caffeinemc.sodium.SodiumClientMod;
import net.caffeinemc.sodium.interop.vanilla.mixin.SpriteVisibilityStorage;
import net.minecraft.client.texture.Sprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Sprite.Animation.class)
public class MixinSpriteAnimation {
    @Unique
    private Sprite parent;

    /**
     * @author IMS
     * @reason Replace fragile Shadow
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    public void assignParent(Sprite parent, List<Sprite.AnimationFrame> frames, int frameCount, Sprite.Interpolation interpolation, CallbackInfo ci) {
        this.parent = parent;
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void preTick(CallbackInfo ci) {
        SpriteVisibilityStorage parent = (SpriteVisibilityStorage) this.parent;

        boolean onDemand = SodiumClientMod.options().performance.animateOnlyVisibleTextures;

        if (onDemand && !parent.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void postTick(CallbackInfo ci) {
        SpriteVisibilityStorage parent = (SpriteVisibilityStorage) this.parent;
        parent.setActive(false);
    }
}
