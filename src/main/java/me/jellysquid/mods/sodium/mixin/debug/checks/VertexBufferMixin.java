package me.jellysquid.mods.sodium.mixin.debug.checks;

import me.jellysquid.mods.sodium.client.render.util.RenderAsserts;
import net.minecraft.client.gl.VertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(VertexBuffer.class)
public class VertexBufferMixin {
    @Redirect(method = {
            "draw(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/client/gl/ShaderProgram;)V",
    }, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;isOnRenderThread()Z"))
    private boolean validateCurrentThread$draw() {
        return RenderAsserts.validateCurrentThread();
    }
}
