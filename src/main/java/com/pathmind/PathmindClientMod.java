package com.pathmind;

import com.pathmind.data.PresetManager;
import com.pathmind.data.SettingsManager;
import com.pathmind.execution.ExecutionManager;
import com.pathmind.execution.PathmindNavigator;
import com.pathmind.marketplace.MarketplaceAuthManager;
import com.pathmind.nodes.Node;
import com.pathmind.screen.PathmindMainMenuIntegration;
import com.pathmind.screen.PathmindScreens;
import com.pathmind.ui.overlay.ActiveNodeOverlay;
import com.pathmind.ui.overlay.NavigatorChatSuggestions;
import com.pathmind.ui.overlay.NavigatorDebugOverlay;
import com.pathmind.ui.overlay.NodeErrorNotificationOverlay;
import com.pathmind.ui.overlay.VariablesOverlay;
import com.pathmind.util.BlockSelection;
import com.pathmind.util.ChatMessageTracker;
import com.pathmind.util.DrawContextBridge;
import com.pathmind.util.FabricEventTracker;
import com.pathmind.util.PoseStackBridge;
import com.pathmind.util.ServerJoinTracker;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionResult;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * The client-side mod class for Pathmind.
 * This class initializes client-specific features and event handlers.
 */
@SuppressWarnings({"deprecation", "removal"})
@Mod(value = PathmindMod.MOD_ID, dist = Dist.CLIENT)
public class PathmindClientMod {
    private static final Logger LOGGER = LoggerFactory.getLogger("Pathmind/Client");
    private static PathmindClientMod instance;
    private static final String RECIPE_CACHE_NOTIFICATION_KEY = "recipe_cache_warmup";
    private static final int NAVIGATOR_NOTIFICATION_COLOR = 0xFF66D8FF;
    private static final double NAVIGATOR_PARAMETER_SEARCH_RADIUS = 64.0D;
    private static ActiveNodeOverlay activeNodeOverlay;
    private static NavigatorDebugOverlay navigatorDebugOverlay;
    private static NodeErrorNotificationOverlay nodeErrorNotificationOverlay;
    private static VariablesOverlay variablesOverlay;
    private volatile boolean worldShutdownHandled;
    private boolean recipeCacheWarmed;
    private int recipeCacheWarmupCooldownTicks;
    private boolean playGraphsKeyDown;
    private boolean stopGraphsKeyDown;

    private static final String EVT_CLIENT_BLOCK_ENTITY_LOAD = "fabric.client.lifecycle.block_entity_load";
    private static final String EVT_CLIENT_BLOCK_ENTITY_UNLOAD = "fabric.client.lifecycle.block_entity_unload";
    private static final String EVT_CLIENT_CHUNK_LOAD = "fabric.client.lifecycle.chunk_load";
    private static final String EVT_CLIENT_CHUNK_UNLOAD = "fabric.client.lifecycle.chunk_unload";
    private static final String EVT_CLIENT_ENTITY_LOAD = "fabric.client.lifecycle.entity_load";
    private static final String EVT_CLIENT_ENTITY_UNLOAD = "fabric.client.lifecycle.entity_unload";
    private static final String EVT_CLIENT_LIFECYCLE_STARTED = "fabric.client.lifecycle.client_started";
    private static final String EVT_CLIENT_LIFECYCLE_STOPPING = "fabric.client.lifecycle.client_stopping";
    private static final String EVT_CLIENT_TICK_END = "fabric.client.lifecycle.end_client_tick";
    private static final String EVT_CLIENT_TICK_START = "fabric.client.lifecycle.start_client_tick";
    private static final String EVT_CLIENT_WORLD_TICK_END = "fabric.client.lifecycle.end_world_tick";
    private static final String EVT_CLIENT_WORLD_TICK_START = "fabric.client.lifecycle.start_world_tick";

    private static final String EVT_CLIENT_CONFIG_CHANNEL_REGISTER = "fabric.client.networking.c2s_configuration_channel_register";
    private static final String EVT_CLIENT_CONFIG_CHANNEL_UNREGISTER = "fabric.client.networking.c2s_configuration_channel_unregister";
    private static final String EVT_CLIENT_PLAY_CHANNEL_REGISTER = "fabric.client.networking.c2s_play_channel_register";
    private static final String EVT_CLIENT_PLAY_CHANNEL_UNREGISTER = "fabric.client.networking.c2s_play_channel_unregister";
    private static final String EVT_CLIENT_CONFIGURATION_COMPLETE = "fabric.client.networking.configuration_connection_complete";
    private static final String EVT_CLIENT_CONFIGURATION_DISCONNECT = "fabric.client.networking.configuration_connection_disconnect";
    private static final String EVT_CLIENT_CONFIGURATION_INIT = "fabric.client.networking.configuration_connection_init";
    private static final String EVT_CLIENT_CONFIGURATION_START = "fabric.client.networking.configuration_connection_start";
    private static final String EVT_CLIENT_LOGIN_DISCONNECT = "fabric.client.networking.login_connection_disconnect";
    private static final String EVT_CLIENT_LOGIN_INIT = "fabric.client.networking.login_connection_init";
    private static final String EVT_CLIENT_LOGIN_QUERY_START = "fabric.client.networking.login_connection_query_start";
    private static final String EVT_CLIENT_PLAY_DISCONNECT = "fabric.client.networking.play_connection_disconnect";
    private static final String EVT_CLIENT_PLAY_INIT = "fabric.client.networking.play_connection_init";
    private static final String EVT_CLIENT_PLAY_JOIN = "fabric.client.networking.play_connection_join";

    private static final String EVT_MESSAGE_RECEIVE_ALLOW_CHAT = "fabric.client.message.receive_allow_chat";
    private static final String EVT_MESSAGE_RECEIVE_ALLOW_GAME = "fabric.client.message.receive_allow_game";
    public static final String EVT_MESSAGE_RECEIVE_CHAT = "fabric.client.message.receive_chat";
    private static final String EVT_MESSAGE_RECEIVE_CHAT_CANCELED = "fabric.client.message.receive_chat_canceled";
    private static final String EVT_MESSAGE_RECEIVE_GAME = "fabric.client.message.receive_game";
    private static final String EVT_MESSAGE_RECEIVE_GAME_CANCELED = "fabric.client.message.receive_game_canceled";
    private static final String EVT_MESSAGE_RECEIVE_MODIFY_GAME = "fabric.client.message.receive_modify_game";
    public static final String EVT_MESSAGE_SEND_ALLOW_CHAT = "fabric.client.message.send_allow_chat";
    private static final String EVT_MESSAGE_SEND_ALLOW_COMMAND = "fabric.client.message.send_allow_command";
    private static final String EVT_MESSAGE_SEND_CHAT = "fabric.client.message.send_chat";
    private static final String EVT_MESSAGE_SEND_CHAT_CANCELED = "fabric.client.message.send_chat_canceled";
    private static final String EVT_MESSAGE_SEND_COMMAND = "fabric.client.message.send_command";
    private static final String EVT_MESSAGE_SEND_COMMAND_CANCELED = "fabric.client.message.send_command_canceled";
    private static final String EVT_MESSAGE_SEND_MODIFY_CHAT = "fabric.client.message.send_modify_chat";
    private static final String EVT_MESSAGE_SEND_MODIFY_COMMAND = "fabric.client.message.send_modify_command";
    private static final String EVT_SERVER_MESSAGE_CHAT = "fabric.server.message.chat_message";

    public static final String EVT_RENDER_HUD = "fabric.client.render.hud";
    public static final String EVT_PLAYER_ATTACK_BLOCK = "fabric.player.attack_block";
    public static final String EVT_PLAYER_ATTACK_ENTITY = "fabric.player.attack_entity";
    public static final String EVT_PLAYER_USE_BLOCK = "fabric.player.use_block";
    public static final String EVT_PLAYER_USE_ENTITY = "fabric.player.use_entity";
    public static final String EVT_PLAYER_USE_ITEM = "fabric.player.use_item";

    public PathmindClientMod(IEventBus modBus) {
        instance = this;
        LOGGER.info("Initializing Pathmind client mod");

        PresetManager.initialize();
        MarketplaceAuthManager.initialize();
        activeNodeOverlay = new ActiveNodeOverlay();
        navigatorDebugOverlay = new NavigatorDebugOverlay();
        nodeErrorNotificationOverlay = NodeErrorNotificationOverlay.getInstance();
        variablesOverlay = new VariablesOverlay();

        LOGGER.info("Pathmind client mod initialized successfully");
    }

    public static void onEndClientTick(Minecraft client) {
        if (instance == null) {
            return;
        }
        instance.handleKeybinds(client);
        instance.handleRecipeCacheWarmup(client);
        NavigatorChatSuggestions.getInstance().tick(client);
        PathmindNavigator.getInstance().tick(client);
        ServerJoinTracker.tick(client);
        fireLoaderEvent("fabric.client.lifecycle.end_client_tick");
    }

    public static void onPlayJoin() {
        if (instance == null) {
            return;
        }
        instance.worldShutdownHandled = false;
        ChatMessageTracker.clear();
        FabricEventTracker.clear();
        ServerJoinTracker.recordClientJoin(Minecraft.getInstance());
        if (nodeErrorNotificationOverlay != null) {
            nodeErrorNotificationOverlay.clear();
        }
        Node.resetRecipeCacheWarmup();
        instance.recipeCacheWarmed = false;
        instance.recipeCacheWarmupCooldownTicks = 0;
        fireLoaderEvent("fabric.client.networking.play_connection_join");
    }

    public static void onPlayDisconnect() {
        if (instance == null) {
            return;
        }
        instance.handleClientShutdown("play disconnect", false);
        PathmindNavigator.getInstance().reset();
        ChatMessageTracker.clear();
        FabricEventTracker.clear();
        ServerJoinTracker.clear();
        if (nodeErrorNotificationOverlay != null) {
            nodeErrorNotificationOverlay.clear();
        }
        Node.resetRecipeCacheWarmup();
        fireLoaderEvent("fabric.client.networking.play_connection_disconnect");
    }

    public static void onChatReceived(String message) {
        fireLoaderEvent(EVT_MESSAGE_RECEIVE_CHAT);
    }

    public static boolean handlePathmindNavigatorChat(String message) {
        if (instance == null) {
            return false;
        }
        return instance.handlePathmindNavigatorChatInternal(message);
    }

    public static void renderHudOverlays(GuiGraphics drawContext, Minecraft client) {
        if (client == null || client.player == null || client.font == null) {
            return;
        }

        boolean showHudOverlays = SettingsManager.getCurrent().showHudOverlays == null
            || SettingsManager.getCurrent().showHudOverlays;
        if (!showHudOverlays) {
            return;
        }

        int scaledWidth = client.getWindow().getGuiScaledWidth();
        int scaledHeight = client.getWindow().getGuiScaledHeight();
        DrawContextBridge.startNewRootLayer(drawContext);
        Object matrices = drawContext.pose();
        PoseStackBridge.push(matrices);
        PoseStackBridge.translateZ(matrices, 500.0f);

        try {
            if (activeNodeOverlay != null) {
                activeNodeOverlay.render(drawContext, client.font, scaledWidth, scaledHeight);
            }
            if (variablesOverlay != null) {
                variablesOverlay.render(drawContext, client.font, scaledWidth, scaledHeight);
            }
            if (navigatorDebugOverlay != null) {
                navigatorDebugOverlay.render(drawContext, client.font, scaledWidth, scaledHeight);
            }
        } finally {
            PoseStackBridge.pop(matrices);
        }
    }

    public static void renderHudNotifications(GuiGraphics drawContext, Minecraft client) {
        if (client == null || client.player == null || client.font == null || nodeErrorNotificationOverlay == null) {
            return;
        }

        boolean showHudOverlays = SettingsManager.getCurrent().showHudOverlays == null
            || SettingsManager.getCurrent().showHudOverlays;
        if (!showHudOverlays) {
            return;
        }

        int scaledWidth = client.getWindow().getGuiScaledWidth();
        int scaledHeight = client.getWindow().getGuiScaledHeight();
        DrawContextBridge.startNewRootLayer(drawContext);
        Object matrices = drawContext.pose();
        PoseStackBridge.push(matrices);
        PoseStackBridge.translateZ(matrices, 500.0f);
        try {
            nodeErrorNotificationOverlay.render(drawContext, client.font, scaledWidth, scaledHeight);
        } finally {
            PoseStackBridge.pop(matrices);
        }
    }

    public static void fireLoaderEvent(String eventName) {
        if (eventName == null || eventName.isEmpty()) {
            return;
        }
        FabricEventTracker.record(eventName);
    }

    public static boolean handlePathmindNavigatorCommand(String command) {
        if (instance == null) {
            return false;
        }
        return instance.handlePathmindNavigatorCommandInternal(command);
    }

    private void handleClientShutdown(String reason) {
        handleClientShutdown(reason, false);
    }

    private void handleClientShutdown(String reason, boolean force) {
        if (!force && worldShutdownHandled) {
            return;
        }
        worldShutdownHandled = true;
        LOGGER.debug("Pathmind: handling client shutdown due to {}", reason);
        ExecutionManager.getInstance().requestStopAll();
    }

    private void handleKeybinds(Minecraft client) {
        if (client == null) {
            return;
        }

        // Check if visual editor keybind was pressed (Title screen only)
        while (PathmindKeybinds.OPEN_VISUAL_EDITOR.consumeClick()) {
            if (client.screen != null && !(client.screen instanceof TitleScreen)) {
                continue;
            }
            PathmindScreens.openVisualEditorOrWarn(client, client.screen);
        }

        ExecutionManager manager = ExecutionManager.getInstance();
        boolean editorOpen = PathmindScreens.isVisualEditorScreen(client.screen);
        // Allow execution to continue while GUIs are open so key-pressed sensors can fire.
        manager.setSingleplayerPaused(client.isLocalServer() && editorOpen);

        if (client.level == null) {
            if (!PathmindScreens.isVisualEditorScreen(client.screen)) {
                handleClientShutdown("world unavailable", false);
            }
            return;
        }

        // Don't handle k/j keybinds when chat or Pathmind GUI is open
        boolean chatOrGuiOpen = shouldIgnoreKeybinds(client);

        boolean stopDown = PathmindKeybinds.STOP_GRAPHS.isDown();
        if (!chatOrGuiOpen && stopDown && !stopGraphsKeyDown) {
            ExecutionManager.getInstance().requestStopAll();
        }
        stopGraphsKeyDown = stopDown;

        if (client.player == null) {
            return;
        }

        boolean playDown = PathmindKeybinds.PLAY_GRAPHS.isDown();
        if (!chatOrGuiOpen && playDown && !playGraphsKeyDown) {
            ExecutionManager.getInstance().playAllGraphs();
        }
        playGraphsKeyDown = playDown;
    }

    private boolean shouldIgnoreKeybinds(Minecraft client) {
        if (client == null || client.screen == null) {
            return false;
        }
        // Check if chat screen is open
        if (client.screen instanceof net.minecraft.client.gui.screens.ChatScreen) {
            return true;
        }
        // Check if Pathmind visual editor is open
        if (PathmindScreens.isVisualEditorScreen(client.screen)) {
            return true;
        }
        return false;
    }

    private void handleRecipeCacheWarmup(Minecraft client) {
        // Integrated-server availability is the real prerequisite for recipe-cache warmup.
        // Using it directly is more robust across version-specific singleplayer state handling.
        if (client == null || client.getSingleplayerServer() == null) {
            if (nodeErrorNotificationOverlay != null) {
                nodeErrorNotificationOverlay.dismiss(RECIPE_CACHE_NOTIFICATION_KEY);
            }
            recipeCacheWarmed = false;
            return;
        }
        boolean cacheReady = Node.hasUsableRecipeCache(client);
        boolean warmupInProgress = Node.isRecipeCacheWarmupInProgress(client);
        if (cacheReady && !warmupInProgress) {
            if (!recipeCacheWarmed && nodeErrorNotificationOverlay != null) {
                nodeErrorNotificationOverlay.dismiss(RECIPE_CACHE_NOTIFICATION_KEY);
                nodeErrorNotificationOverlay.show("Recipe cache ready.", com.pathmind.ui.theme.UITheme.ACCENT_SKY);
            } else if (nodeErrorNotificationOverlay != null) {
                nodeErrorNotificationOverlay.dismiss(RECIPE_CACHE_NOTIFICATION_KEY);
            }
            recipeCacheWarmed = true;
            recipeCacheWarmupCooldownTicks = 0;
            return;
        }

        recipeCacheWarmed = false;
        if (nodeErrorNotificationOverlay != null) {
            String message = "Building recipe cache\nPreparing singleplayer recipes...";
            nodeErrorNotificationOverlay.showProgress(
                RECIPE_CACHE_NOTIFICATION_KEY,
                message,
                com.pathmind.ui.theme.UITheme.ACCENT_SKY,
                0.0f
            );
        }
        if (client.getSingleplayerServer() == null) {
            if (nodeErrorNotificationOverlay != null) {
                nodeErrorNotificationOverlay.showProgress(
                    RECIPE_CACHE_NOTIFICATION_KEY,
                    "Building recipe cache\nPreparing singleplayer recipes...",
                    com.pathmind.ui.theme.UITheme.ACCENT_SKY,
                    0.0f
                );
            }
            recipeCacheWarmupCooldownTicks = 20;
            return;
        }
        if (recipeCacheWarmupCooldownTicks > 0) {
            recipeCacheWarmupCooldownTicks--;
            return;
        }

        boolean cached = Node.warmRecipeCache(client);
        Node.RecipeCacheWarmupProgress progress = Node.getRecipeCacheWarmupProgress(client);
        if (nodeErrorNotificationOverlay != null) {
            String message = progress != null
                ? "Building recipe cache\n" + progress.completed() + " / " + progress.total()
                : "Building recipe cache\nPreparing singleplayer recipes...";
            float fraction = progress != null ? progress.fraction() : 0.0f;
            nodeErrorNotificationOverlay.showProgress(
                RECIPE_CACHE_NOTIFICATION_KEY,
                message,
                com.pathmind.ui.theme.UITheme.ACCENT_SKY,
                fraction
            );
        }
        if (cached) {
            recipeCacheWarmed = true;
            if (nodeErrorNotificationOverlay != null) {
                nodeErrorNotificationOverlay.dismiss(RECIPE_CACHE_NOTIFICATION_KEY);
                nodeErrorNotificationOverlay.show("Recipe cache ready.", com.pathmind.ui.theme.UITheme.ACCENT_SKY);
            }
            LOGGER.debug("Pathmind recipe cache populated from singleplayer recipes.");
        } else if (!Node.isRecipeCacheWarmupInProgress(client) && !Node.hasUsableRecipeCache(client)) {
            recipeCacheWarmupCooldownTicks = 100;
            LOGGER.debug("Pathmind recipe cache warmup attempted but no recipes found.");
        }
    }

    private boolean handlePathmindNavigatorChatInternal(String message) {
        if (message == null) {
            return false;
        }
        String trimmed = message.trim();
        if (!trimmed.startsWith("!")) {
            return false;
        }
        return runNavigatorCommand(trimmed.substring(1).trim());
    }

    private boolean handlePathmindNavigatorCommandInternal(String command) {
        if (command == null) {
            return false;
        }
        String trimmed = command.trim();
        if (!trimmed.toLowerCase(Locale.ROOT).startsWith("pathmindnav")) {
            return false;
        }
        return runNavigatorCommand(trimmed.substring("pathmindnav".length()).trim());
    }

    private boolean runNavigatorCommand(String rawCommand) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return true;
        }
        String command = rawCommand == null ? "" : rawCommand.trim();
        if (command.isEmpty() || command.equalsIgnoreCase("help")) {
            showNavigatorMessage("Pathmind Nav: !travel, !path, !nav debug, !stop");
            return true;
        }

        String[] parts = command.split("\\s+");
        if (parts.length == 0) {
            return true;
        }

        if (parts[0].equalsIgnoreCase("stop")) {
            PathmindNavigator.getInstance().stop("chat stop");
            showNavigatorMessage("Pathmind Nav stopped.");
            return true;
        }

        if (parts[0].equalsIgnoreCase("travel")) {
            handleNavigatorGoto(client, parts);
            return true;
        }

        if (parts[0].equalsIgnoreCase("path")) {
            handleNavigatorPathPreview(client, parts);
            return true;
        }

        if (parts[0].equalsIgnoreCase("nav") && parts.length >= 2 && parts[1].equalsIgnoreCase("debug")) {
            handleNavigatorDebug();
            return true;
        }

        if (parts[0].equalsIgnoreCase("nav") && parts.length >= 3 && parts[1].equalsIgnoreCase("water")) {
            handleNavigatorWaterMode(parts[2]);
            return true;
        }

        if (parts[0].equalsIgnoreCase("nav") && parts.length >= 3 && parts[1].equalsIgnoreCase("logs")) {
            handleNavigatorLogs(parts[2]);
            return true;
        }

        if (parts[0].equalsIgnoreCase("flag") && parts.length >= 3) {
            handleNavigatorFlag(parts[1], parts[2]);
            return true;
        }

        showNavigatorMessage("Unknown Pathmind Nav command. Use !travel, !path, !nav debug, !nav water, !nav logs, !flag, or !stop.");
        return true;
    }

    private void handleNavigatorGoto(Minecraft client, String[] parts) {
        if (client == null || client.player == null || client.level == null) {
            showNavigatorMessage("Pathmind Nav is unavailable right now.");
            return;
        }

        NavigatorTarget target = parseNavigatorTarget(client, parts, 1, "!travel");
        if (target == null || target.pos() == null) {
            return;
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        boolean started = target.nearBlock()
            ? PathmindNavigator.getInstance().startGotoNearBlock(target.pos(), "Chat Travel", future)
            : PathmindNavigator.getInstance().startGoto(target.pos(), "Chat Travel", future);
        if (!started) {
            showNavigatorMessage("Could not start Pathmind Nav.");
            return;
        }
        showNavigatorMessage("Pathmind Nav: heading to " + target.pos().getX() + " " + target.pos().getY() + " " + target.pos().getZ());
    }

    private void handleNavigatorPathPreview(Minecraft client, String[] parts) {
        if (client == null || client.player == null || client.level == null) {
            showNavigatorMessage("Pathmind Nav is unavailable right now.");
            return;
        }

        NavigatorTarget target = parseNavigatorTarget(client, parts, 1, "!path");
        if (target == null || target.pos() == null) {
            return;
        }

        PathmindNavigator.PreviewResult result = target.nearBlock()
            ? PathmindNavigator.getInstance().previewPathNearBlock(client, target.pos(), "Path Preview")
            : PathmindNavigator.getInstance().previewPath(client, target.pos(), "Path Preview");
        showNavigatorMessage(result.message());
    }

    private void handleNavigatorDebug() {
        boolean enabled = navigatorDebugOverlay != null && navigatorDebugOverlay.toggle();
        showNavigatorMessage(enabled ? "Pathmind Nav debug overlay enabled." : "Pathmind Nav debug overlay disabled.");
    }

    private record NavigatorTarget(BlockPos pos, boolean nearBlock) {
    }

    private NavigatorTarget parseNavigatorTarget(Minecraft client, String[] parts, int coordinateStartIndex, String usageCommand) {
        if (client == null || client.player == null) {
            showNavigatorMessage("Pathmind Nav is unavailable right now.");
            return null;
        }

        int remaining = parts.length - coordinateStartIndex;
        if (remaining >= 2) {
            String modeToken = parts[coordinateStartIndex];
            String rawTarget = parts[coordinateStartIndex + 1];
            if (modeToken.equalsIgnoreCase("block")) {
                return resolveNavigatorBlockTarget(client, rawTarget, usageCommand);
            }
            if (modeToken.equalsIgnoreCase("item")) {
                return resolveNavigatorItemTarget(client, rawTarget, usageCommand);
            }
        }
        try {
            if (remaining == 2) {
                int x = parseNavigatorCoordinate(parts[coordinateStartIndex], client.player.getBlockX(), false);
                int z = parseNavigatorCoordinate(parts[coordinateStartIndex + 1], client.player.getBlockZ(), false);
                return new NavigatorTarget(new BlockPos(x, client.player.getBlockY(), z), false);
            }
            if (remaining == 3) {
                int x = parseNavigatorCoordinate(parts[coordinateStartIndex], client.player.getBlockX(), false);
                int y = parseNavigatorCoordinate(parts[coordinateStartIndex + 1], client.player.getBlockY(), false);
                int z = parseNavigatorCoordinate(parts[coordinateStartIndex + 2], client.player.getBlockZ(), false);
                return new NavigatorTarget(new BlockPos(x, y, z), false);
            }
        } catch (NumberFormatException exception) {
            showNavigatorMessage("Invalid coordinates for " + usageCommand + ".");
            return null;
        }

        showNavigatorMessage("Usage: " + usageCommand + " <x> <y> <z>, " + usageCommand + " <x> <z>, " + usageCommand + " block <block_id>, or " + usageCommand + " item <item_id>");
        return null;
    }

    private NavigatorTarget resolveNavigatorBlockTarget(Minecraft client, String rawBlockId, String usageCommand) {
        if (client == null || client.player == null || client.level == null) {
            showNavigatorMessage("Pathmind Nav is unavailable right now.");
            return null;
        }
        String normalized = normalizeNavigatorResourceId(rawBlockId, true);
        if (normalized == null) {
            showNavigatorMessage("Usage: " + usageCommand + " block <block_id>");
            return null;
        }
        ResourceLocation identifier = ResourceLocation.tryParse(normalized);
        if (identifier == null || !BuiltInRegistries.BLOCK.containsKey(identifier)) {
            showNavigatorMessage("Unknown block identifier: " + rawBlockId);
            return null;
        }

        List<BlockSelection> selections = new ArrayList<>();
        BlockSelection.parse(normalized).ifPresent(selections::add);
        if (selections.isEmpty()) {
            showNavigatorMessage("Unknown block identifier: " + rawBlockId);
            return null;
        }

        Optional<BlockPos> nearest = findNearestBlock(client, selections, NAVIGATOR_PARAMETER_SEARCH_RADIUS);
        if (nearest.isEmpty()) {
            showNavigatorMessage("No nearby block found for " + normalized + ".");
            return null;
        }
        return new NavigatorTarget(nearest.get(), true);
    }

    private NavigatorTarget resolveNavigatorItemTarget(Minecraft client, String rawItemId, String usageCommand) {
        if (client == null || client.player == null || client.level == null) {
            showNavigatorMessage("Pathmind Nav is unavailable right now.");
            return null;
        }
        String normalized = normalizeNavigatorResourceId(rawItemId, false);
        if (normalized == null) {
            showNavigatorMessage("Usage: " + usageCommand + " item <item_id>");
            return null;
        }
        ResourceLocation identifier = ResourceLocation.tryParse(normalized);
        if (identifier == null || !BuiltInRegistries.ITEM.containsKey(identifier)) {
            showNavigatorMessage("Unknown item identifier: " + rawItemId);
            return null;
        }

        Item item = BuiltInRegistries.ITEM.get(identifier);
        Optional<ItemEntity> nearest = findNearestDroppedItemEntity(client, item, NAVIGATOR_PARAMETER_SEARCH_RADIUS);
        if (nearest.isEmpty()) {
            showNavigatorMessage("No nearby dropped item found for " + normalized + ".");
            return null;
        }
        return new NavigatorTarget(nearest.get().blockPosition(), false);
    }

    private String normalizeNavigatorResourceId(String rawId, boolean block) {
        if (rawId == null) {
            return null;
        }
        String trimmed = rawId.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (block) {
            ResourceLocation identifier = BlockSelection.extractBlockResourceLocation(trimmed);
            return identifier != null ? identifier.toString() : null;
        }
        ResourceLocation identifier = ResourceLocation.tryParse(trimmed);
        if (identifier != null) {
            return identifier.toString();
        }
        ResourceLocation namespaced = ResourceLocation.tryParse("minecraft:" + trimmed.toLowerCase(Locale.ROOT));
        return namespaced != null ? namespaced.toString() : null;
    }

    private Optional<BlockPos> findNearestBlock(Minecraft client, List<BlockSelection> selections, double range) {
        if (client == null || client.player == null || client.level == null || selections == null || selections.isEmpty()) {
            return Optional.empty();
        }
        int radius = Math.max(1, Math.min((int) Math.ceil(range), 64));
        BlockPos playerPos = client.player.blockPosition();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        BlockPos bestPos = null;
        double bestDistance = Double.MAX_VALUE;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    mutable.set(playerPos.getX() + dx, playerPos.getY() + dy, playerPos.getZ() + dz);
                    BlockState state = client.level.getBlockState(mutable);
                    if (state == null || state.isAir()) {
                        continue;
                    }
                    boolean matches = false;
                    for (BlockSelection selection : selections) {
                        if (selection.matches(state)) {
                            matches = true;
                            break;
                        }
                    }
                    if (!matches) {
                        continue;
                    }
                    double distance = mutable.distSqr(playerPos);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestPos = mutable.immutable();
                    }
                }
            }
        }

        return Optional.ofNullable(bestPos);
    }

    private Optional<ItemEntity> findNearestDroppedItemEntity(Minecraft client, Item item, double range) {
        if (client == null || client.player == null || client.level == null || item == null) {
            return Optional.empty();
        }
        double searchRadius = Math.max(1.0D, range);
        AABB searchBox = client.player.getBoundingBox().inflate(searchRadius);
        List<ItemEntity> entities = client.level.getEntitiesOfClass(
            ItemEntity.class,
            searchBox,
            entity -> entity != null && !entity.isRemoved() && !entity.getItem().isEmpty() && entity.getItem().is(item)
        );
        if (entities.isEmpty()) {
            return Optional.empty();
        }
        ItemEntity nearest = entities.stream()
            .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(client.player)))
            .orElse(null);
        return Optional.ofNullable(nearest);
    }

    private int parseNavigatorCoordinate(String token, int base, boolean normalizeAbsoluteHorizontal) {
        if (token == null) {
            throw new NumberFormatException("null coordinate");
        }
        if (!token.startsWith("~")) {
            int absolute = Integer.parseInt(token);
            return absolute;
        }
        if (token.length() == 1) {
            return base;
        }
        return base + Integer.parseInt(token.substring(1));
    }

    private void handleNavigatorWaterMode(String modeToken) {
        if (modeToken == null) {
            showNavigatorMessage("Usage: !nav water <normal|avoid>");
            return;
        }
        if (modeToken.equalsIgnoreCase("avoid")) {
            PathmindNavigator.getInstance().setWaterMode(PathmindNavigator.WaterMode.AVOID);
            showNavigatorMessage("Pathmind Nav water mode: avoid");
            return;
        }
        if (modeToken.equalsIgnoreCase("normal") || modeToken.equalsIgnoreCase("allow")) {
            PathmindNavigator.getInstance().setWaterMode(PathmindNavigator.WaterMode.NORMAL);
            showNavigatorMessage("Pathmind Nav water mode: normal");
            return;
        }
        showNavigatorMessage("Usage: !nav water <normal|avoid>");
    }

    private void handleNavigatorLogs(String modeToken) {
        if (modeToken == null) {
            showNavigatorMessage("Usage: !nav logs <enable|disable>");
            return;
        }
        if (modeToken.equalsIgnoreCase("enable") || modeToken.equalsIgnoreCase("on")) {
            PathmindNavigator.getInstance().setEventLoggingEnabled(true);
            showNavigatorMessage("Pathmind Nav logs enabled: .pathmind/logs/navigator-debug.log");
            return;
        }
        if (modeToken.equalsIgnoreCase("disable") || modeToken.equalsIgnoreCase("off")) {
            PathmindNavigator.getInstance().setEventLoggingEnabled(false);
            showNavigatorMessage("Pathmind Nav logs disabled.");
            return;
        }
        showNavigatorMessage("Usage: !nav logs <enable|disable>");
    }

    private void handleNavigatorFlag(String flagName, String action) {
        if (flagName == null || action == null) {
            showNavigatorMessage("Usage: !flag <break|place> <enable|disable>");
            return;
        }
        boolean enable;
        if (action.equalsIgnoreCase("enable") || action.equalsIgnoreCase("on")) {
            enable = true;
        } else if (action.equalsIgnoreCase("disable") || action.equalsIgnoreCase("off")) {
            enable = false;
        } else {
            showNavigatorMessage("Usage: !flag <break|place> <enable|disable>");
            return;
        }

        PathmindNavigator navigator = PathmindNavigator.getInstance();
        if (flagName.equalsIgnoreCase("break") || flagName.equalsIgnoreCase("breaking")) {
            navigator.setBlockBreakingAllowed(enable);
            showNavigatorMessage("Pathmind Nav flag break: " + (enable ? "enabled" : "disabled"));
            return;
        }
        if (flagName.equalsIgnoreCase("place") || flagName.equalsIgnoreCase("placing")) {
            navigator.setBlockPlacingAllowed(enable);
            showNavigatorMessage("Pathmind Nav flag place: " + (enable ? "enabled" : "disabled"));
            return;
        }
        showNavigatorMessage("Usage: !flag <break|place> <enable|disable>");
    }

    private String formatDebugPos(BlockPos pos) {
        if (pos == null) {
            return "--";
        }
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private String sanitizeDebugText(String value) {
        if (value == null || value.isBlank()) {
            return "none";
        }
        return value.replace(' ', '_');
    }

    private void showNavigatorMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        NodeErrorNotificationOverlay overlay = nodeErrorNotificationOverlay != null
            ? nodeErrorNotificationOverlay
            : NodeErrorNotificationOverlay.getInstance();
        overlay.show(message, NAVIGATOR_NOTIFICATION_COLOR);
    }

}
