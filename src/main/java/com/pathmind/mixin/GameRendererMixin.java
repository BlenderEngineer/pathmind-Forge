package com.pathmind.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to prevent other mods from rendering overlays during or after the Pathmind screen renders.
 * This targets the main game rendering pipeline to catch aggressive overlay mods.
 */
@Mixin(value = GameRenderer.class, priority = 1500)
public class GameRendererMixin {

    private static final String DRAW_SCREEN =
        "Lnet/neoforged/neoforge/client/ClientHooks;drawScreen(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/gui/GuiGraphics;IIF)V";

    private static final ThreadLocal<Boolean> pathmind$finalRenderPass =
        ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<GuiGraphics> pathmind$lastGuiGraphics = new ThreadLocal<>();
    private static final ThreadLocal<Integer> pathmind$lastMouseX = new ThreadLocal<>();
    private static final ThreadLocal<Integer> pathmind$lastMouseY = new ThreadLocal<>();
    private static final ThreadLocal<Float> pathmind$lastDelta = new ThreadLocal<>();

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = DRAW_SCREEN,
            shift = At.Shift.AFTER
        ),
        cancellable = false,
        require = 0
    )
    private void pathmind$afterScreenRender(DeltaTracker tickCounter, boolean tick, CallbackInfo ci) {
        // Tracks when NeoForge finishes the primary screen draw pass.
    }

    @ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = DRAW_SCREEN
        ),
        index = 1,
        require = 0
    )
    private GuiGraphics pathmind$captureGuiGraphics(GuiGraphics context) {
        if (!pathmind$finalRenderPass.get()) {
            pathmind$lastGuiGraphics.set(context);
        }
        return context;
    }

    @ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = DRAW_SCREEN
        ),
        index = 2,
        require = 0
    )
    private int pathmind$captureMouseX(int mouseX) {
        if (!pathmind$finalRenderPass.get()) {
            pathmind$lastMouseX.set(mouseX);
        }
        return mouseX;
    }

    @ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = DRAW_SCREEN
        ),
        index = 3,
        require = 0
    )
    private int pathmind$captureMouseY(int mouseY) {
        if (!pathmind$finalRenderPass.get()) {
            pathmind$lastMouseY.set(mouseY);
        }
        return mouseY;
    }

    @ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = DRAW_SCREEN
        ),
        index = 4,
        require = 0
    )
    private float pathmind$captureDelta(float delta) {
        if (!pathmind$finalRenderPass.get()) {
            pathmind$lastDelta.set(delta);
        }
        return delta;
    }

    /**
     * Final catch-all to draw the Pathmind screen after other mods render.
     */
    @Inject(
        method = "render",
        at = @At("TAIL"),
        cancellable = false,
        require = 0
    )
    private void pathmind$finalScreenRender(DeltaTracker tickCounter, boolean tick, CallbackInfo ci) {
        if (pathmind$finalRenderPass.get()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null) {
            return;
        }

        if (!com.pathmind.screen.PathmindScreens.isVisualEditorScreen(minecraft.screen)) {
            return;
        }
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        GuiGraphics context = pathmind$lastGuiGraphics.get();
        Integer mouseX = pathmind$lastMouseX.get();
        Integer mouseY = pathmind$lastMouseY.get();
        Float delta = pathmind$lastDelta.get();
        if (context == null || mouseX == null || mouseY == null || delta == null) {
            return;
        }

        pathmind$finalRenderPass.set(true);
        com.pathmind.util.OverlayProtection.setPathmindRendering(true);
        try {
            minecraft.screen.renderWithTooltip(context, mouseX, mouseY, delta);
        } catch (Throwable ignored) {
            // Avoid crashing render if another mod misbehaves.
        } finally {
            com.pathmind.util.OverlayProtection.setPathmindRendering(false);
            pathmind$finalRenderPass.set(false);
        }
    }
}
