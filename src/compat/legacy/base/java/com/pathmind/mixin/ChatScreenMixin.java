package com.pathmind.mixin;

import com.pathmind.ui.overlay.NavigatorChatSuggestions;
import com.pathmind.util.DrawContextBridge;
import com.pathmind.util.PoseStackBridge;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void pathmind$renderNavigatorSuggestions(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        DrawContextBridge.startNewRootLayer(context);
        Object matrices = context.pose();
        PoseStackBridge.push(matrices);
        PoseStackBridge.translateZ(matrices, 500.0f);
        try {
            NavigatorChatSuggestions.getInstance().render((ChatScreen) (Object) this, context, mouseX, mouseY);
        } finally {
            PoseStackBridge.pop(matrices);
        }
    }

    @Inject(method = "keyPressed(III)Z", at = @At("HEAD"), cancellable = true)
    private void pathmind$consumeNavigatorKeys(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (NavigatorChatSuggestions.getInstance().handleKeyPressed((ChatScreen) (Object) this, keyCode)) {
            cir.setReturnValue(true);
        }
    }
}
