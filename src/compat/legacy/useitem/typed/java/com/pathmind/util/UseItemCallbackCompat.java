package com.pathmind.util;

import java.util.function.Consumer;

/**
 * Fabric UseItemCallback compatibility shim. On NeoForge, item-use events are handled on the game bus.
 */
public final class UseItemCallbackCompat {
    private UseItemCallbackCompat() {
    }

    public static void register(Consumer<String> eventSink, String eventName) {
        // No-op: NeoForge registers PlayerInteractEvent.RightClickItem in PathmindNeoForgeEvents.
    }
}
