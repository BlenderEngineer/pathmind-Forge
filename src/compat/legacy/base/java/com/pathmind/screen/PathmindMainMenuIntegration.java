package com.pathmind.screen;

import com.pathmind.mixin.ScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Hooks into the main menu to add the Pathmind visual editor button and key handling.
 */
public final class PathmindMainMenuIntegration {
    private static final int BUTTON_SIZE = 20;
    private static final int BUTTON_MARGIN = 8;

    private PathmindMainMenuIntegration() {
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof TitleScreen screen) {
            addButton(screen);
        }
    }

    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Post event) {
        if (!(event.getScreen() instanceof TitleScreen)) {
            return;
        }
        if (event.getKeyCode() == GLFW.GLFW_KEY_RIGHT_ALT) {
            PathmindScreens.openVisualEditorOrWarn(Minecraft.getInstance(), event.getScreen());
        }
    }

    private static void addButton(Screen screen) {
        int x = BUTTON_MARGIN;
        int y = BUTTON_MARGIN;

        ((ScreenAccessor) screen).pathmind$addRenderableWidget(new PathmindMainMenuButton(x, y, BUTTON_SIZE, button -> {
            Minecraft client = Minecraft.getInstance();
            PathmindScreens.openVisualEditorOrWarn(client, screen);
        }));
    }
}
