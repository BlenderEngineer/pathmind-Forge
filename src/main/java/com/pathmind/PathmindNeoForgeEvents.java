package com.pathmind;

import com.pathmind.data.PresetManager;
import com.pathmind.execution.ExecutionManager;
import com.pathmind.execution.PathmindNavigator;
import com.pathmind.marketplace.MarketplaceAuthManager;
import com.pathmind.nodes.Node;
import com.pathmind.screen.PathmindMainMenuIntegration;
import com.pathmind.ui.overlay.NodeErrorNotificationOverlay;
import com.pathmind.util.ChatMessageTracker;
import com.pathmind.util.FabricEventTracker;
import com.pathmind.util.ServerJoinTracker;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * NeoForge event bus subscribers replacing Fabric API callbacks.
 */
@EventBusSubscriber(modid = PathmindMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class PathmindNeoForgeEvents {
    private PathmindNeoForgeEvents() {
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        PathmindKeybinds.registerKeybinds();
        event.register(PathmindKeybinds.OPEN_VISUAL_EDITOR);
        event.register(PathmindKeybinds.PLAY_GRAPHS);
        event.register(PathmindKeybinds.STOP_GRAPHS);
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        PathmindMainMenuIntegration.onScreenInit(event);
    }

    @SubscribeEvent
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Post event) {
        PathmindMainMenuIntegration.onScreenKeyPressed(event);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        PathmindClientMod.onEndClientTick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        PathmindClientMod.onPlayJoin();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        PathmindClientMod.onPlayDisconnect();
    }

    @SubscribeEvent
    public static void onRenderHud(RenderGuiEvent.Post event) {
        PathmindClientMod.fireLoaderEvent(PathmindClientMod.EVT_RENDER_HUD);
    }

    @SubscribeEvent
    public static void onChatReceived(net.neoforged.neoforge.client.event.ClientChatReceivedEvent event) {
        if (event.getMessage() != null) {
            PathmindClientMod.onChatReceived(event.getMessage().getString());
        }
        PathmindClientMod.fireLoaderEvent(PathmindClientMod.EVT_MESSAGE_RECEIVE_CHAT);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onChatSend(ClientChatEvent event) {
        String message = event.getMessage();
        if (message.startsWith("/")) {
            PathmindClientMod.fireLoaderEvent("fabric.client.message.send_allow_command");
            if (PathmindClientMod.handlePathmindNavigatorCommand(message.substring(1))) {
                event.setCanceled(true);
            }
            return;
        }
        PathmindClientMod.fireLoaderEvent(PathmindClientMod.EVT_MESSAGE_SEND_ALLOW_CHAT);
        if (PathmindClientMod.handlePathmindNavigatorChat(message)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        PathmindClientMod.fireLoaderEvent(PathmindClientMod.EVT_PLAYER_ATTACK_BLOCK);
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        PathmindClientMod.fireLoaderEvent(PathmindClientMod.EVT_PLAYER_ATTACK_ENTITY);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        PathmindClientMod.fireLoaderEvent(PathmindClientMod.EVT_PLAYER_USE_BLOCK);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        PathmindClientMod.fireLoaderEvent(PathmindClientMod.EVT_PLAYER_USE_ENTITY);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        PathmindClientMod.fireLoaderEvent(PathmindClientMod.EVT_PLAYER_USE_ITEM);
    }

}
