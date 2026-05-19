package com.pathmind.mixin;

import com.pathmind.ui.overlay.NavigatorWorldOverlay;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.debug.DebugRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugRenderer.class)
public class NavigatorWorldRendererMixin {
    @Inject(method = "renderLate", at = @At("TAIL"))
    private void pathmind$renderLegacyNavigatorOverlay(
        PoseStack matrices,
        MultiBufferSource.BufferSource consumers,
        double cameraX,
        double cameraY,
        double cameraZ,
        CallbackInfo ci
    ) {
        NavigatorWorldOverlay.render(matrices, consumers, cameraX, cameraY, cameraZ);
    }
}
