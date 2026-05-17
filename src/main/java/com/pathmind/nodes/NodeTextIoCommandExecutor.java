package com.pathmind.nodes;

import com.pathmind.util.PlayerInventoryBridge;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WritableBookContentComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.util.Hand;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

final class NodeTextIoCommandExecutor {
    private final Node owner;

    NodeTextIoCommandExecutor(Node owner) {
        this.owner = owner;
    }

    void executeWriteBookCommand(CompletableFuture<Void> future) {
        if (preprocessAttachedParameter(EnumSet.noneOf(Node.ParameterUsage.class), future) == Node.ParameterHandlingResult.COMPLETE) {
            return;
        }

        int pageNumber = getIntParameter("Page", 1);
        String text = getBookTextForPage(pageNumber);
        // Convert to 0-indexed page
        int pageIndex = Math.max(0, pageNumber - 1);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            sendNodeErrorMessage(client, "Client or player not available");
            future.completeExceptionally(new RuntimeException("Client or player not available"));
            return;
        }

        // Check if a book edit screen is open
        if (!(client.currentScreen instanceof BookEditScreen)) {
            sendNodeErrorMessage(client, "No book and quill screen is open");
            future.completeExceptionally(new RuntimeException("No book and quill screen is open"));
            return;
        }

        BookEditScreen bookScreen = (BookEditScreen) client.currentScreen;

        client.execute(() -> {
            try {
                // Use reflection to access the book screen's internal state
                // Get the pages list
                java.util.List<Object> pages = null;
                int currentPage = 0;

                // Try to find the pages field
                java.util.List<Object> emptyCandidate = null;
                java.util.List<Field> stringListFields = new java.util.ArrayList<>();
                Field pagesField = null;
                try {
                    pagesField = bookScreen.getClass().getDeclaredField("pages");
                    pagesField.setAccessible(true);
                    Object value = pagesField.get(bookScreen);
                    if (value instanceof java.util.List) {
                    @SuppressWarnings("unchecked")
                    java.util.List<Object> list = (java.util.List<Object>) value;
                    pages = list;
                    }
                } catch (NoSuchFieldException ignored) {
                    // Fallback to heuristic search below
                }
                for (Field field : bookScreen.getClass().getDeclaredFields()) {
                    if (pages != null) {
                        break;
                    }
                    if (field.getType() != java.util.List.class) {
                        continue;
                    }
                    field.setAccessible(true);
                    Object value = field.get(bookScreen);
                    if (!(value instanceof java.util.List)) {
                        continue;
                    }
                    @SuppressWarnings("unchecked")
                    java.util.List<Object> list = (java.util.List<Object>) value;
                    stringListFields.add(field);
                    if (!list.isEmpty()) {
                        pages = list;
                        break;
                    }
                    String fieldName = field.getName().toLowerCase();
                    if (fieldName.contains("page")) {
                        pages = list;
                        break;
                    }
                    if (emptyCandidate == null) {
                        emptyCandidate = list;
                    }
                }
                if (pages == null && emptyCandidate != null) {
                    pages = emptyCandidate;
                }

                if (pages == null) {
                    for (Field field : bookScreen.getClass().getDeclaredFields()) {
                    }
                    sendNodeErrorMessage(client, "Could not access book pages");
                    future.completeExceptionally(new RuntimeException("Could not access book pages"));
                    return;
                }

                // Ensure we have enough pages
                Method appendNewPageMethod = null;
                Method countPagesMethod = null;
                Method setPageTextMethod = null;
                Method updatePageMethod = null;
                Method writeNbtDataMethod = null;
                for (Method method : bookScreen.getClass().getDeclaredMethods()) {
                    String methodName = method.getName().toLowerCase();
                    if (method.getParameterCount() == 0 && method.getReturnType() == void.class) {
                        if (appendNewPageMethod == null
                            && (methodName.contains("appendnewpage") || methodName.contains("method_2436"))) {
                            method.setAccessible(true);
                            appendNewPageMethod = method;
                        }
                    }
                    if (method.getParameterCount() == 0 && method.getReturnType() == int.class) {
                        if (countPagesMethod == null
                            && (methodName.contains("countpages") || methodName.contains("method_17046"))) {
                            method.setAccessible(true);
                            countPagesMethod = method;
                        }
                    }
                    if (method.getParameterCount() == 0 && method.getReturnType() == void.class) {
                        if (updatePageMethod == null
                            && (methodName.contains("updatepage") || methodName.contains("method_71537"))) {
                            method.setAccessible(true);
                            updatePageMethod = method;
                        }
                        if (writeNbtDataMethod == null
                            && (methodName.contains("writenbtdata") || methodName.contains("method_37433"))) {
                            method.setAccessible(true);
                            writeNbtDataMethod = method;
                        }
                    }
                    if (method.getParameterCount() == 1
                        && method.getParameterTypes()[0] == String.class
                        && method.getReturnType() == void.class) {
                        if (setPageTextMethod == null
                            && (methodName.contains("setpage") || methodName.contains("pagetext") || methodName.contains("method_71539"))) {
                            method.setAccessible(true);
                            setPageTextMethod = method;
                        }
                    }
                }

                if (pagesField != null) {
                    Object value = pagesField.get(bookScreen);
                    if (value instanceof java.util.List) {
                        @SuppressWarnings("unchecked")
                        java.util.List<Object> list = (java.util.List<Object>) value;
                        if (list.isEmpty()) {
                            try {
                                list.add(setPageTextMethod != null ? RawFilteredPair.of("") : "");
                            } catch (UnsupportedOperationException ignored) {
                                // replace with mutable list if backing list is immutable
                                list = null;
                            }
                        }
                        if (list == null) {
                            java.util.List<Object> seeded = new java.util.ArrayList<>();
                            seeded.add(setPageTextMethod != null ? RawFilteredPair.of("") : "");
                            pagesField.set(bookScreen, seeded);
                            pages = seeded;
                        } else {
                            pages = list;
                        }
                    }
                } else if (pages.isEmpty()) {
                    pages.add(setPageTextMethod != null ? RawFilteredPair.of("") : "");
                }

                boolean useRawFilteredPairs = false;
                if (!pages.isEmpty()) {
                    useRawFilteredPairs = !(pages.get(0) instanceof String);
                } else if (setPageTextMethod != null) {
                    useRawFilteredPairs = true;
                }
                if (!pages.isEmpty()) {
                    Object first = pages.get(0);
                }
                int pageCount = pages.size();
                if (countPagesMethod != null) {
                    try {
                        pageCount = (int) countPagesMethod.invoke(bookScreen);
                    } catch (Exception ignored) {
                        pageCount = pages.size();
                    }
                }

                while (pageCount <= pageIndex) {
                    if (appendNewPageMethod != null) {
                        int beforeSize = pages.size();
                        appendNewPageMethod.invoke(bookScreen);
                        if (pagesField != null) {
                            Object value = pagesField.get(bookScreen);
                            if (value instanceof java.util.List) {
                                @SuppressWarnings("unchecked")
                                java.util.List<Object> list = (java.util.List<Object>) value;
                                pages = list;
                            }
                        }
                        if (countPagesMethod != null) {
                            pageCount = (int) countPagesMethod.invoke(bookScreen);
                        } else {
                            pageCount = pages.size();
                        }
                        if (pages.size() == beforeSize && pageCount == beforeSize) {
                            pages.add(useRawFilteredPairs ? RawFilteredPair.of("") : "");
                            pageCount = pages.size();
                        }
                    } else {
                        pages.add(useRawFilteredPairs ? RawFilteredPair.of("") : "");
                        pageCount = pages.size();
                    }
                }

                // Set the current page before applying text
                for (Field field : bookScreen.getClass().getDeclaredFields()) {
                    if (field.getType() == int.class) {
                        String fieldName = field.getName().toLowerCase();
                        if (fieldName.contains("page") || fieldName.contains("current")) {
                            field.setAccessible(true);
                            field.setInt(bookScreen, pageIndex);
                            break;
                        }
                    }
                }

                // Set the text on the specified page
                String truncatedText = text;
                if (truncatedText.length() > Node.BOOK_PAGE_MAX_CHARS) {
                    truncatedText = truncatedText.substring(0, Node.BOOK_PAGE_MAX_CHARS);
                }
                boolean setViaMethod = false;
                if (setPageTextMethod != null && pageIndex >= 0 && pageIndex < pages.size()) {
                    try {
                        setPageTextMethod.invoke(bookScreen, truncatedText);
                        setViaMethod = true;
                    } catch (Exception ignored) {
                        // Ignore UI refresh errors
                    }
                }
                if (!setViaMethod && pageIndex >= 0 && pageIndex < pages.size()) {
                    pages.set(pageIndex, useRawFilteredPairs ? RawFilteredPair.of(truncatedText) : truncatedText);
                    if (pagesField != null) {
                        java.util.List<Object> copy = new java.util.ArrayList<>(pages);
                        pagesField.set(bookScreen, copy);
                        pages = copy;
                    }
                } else if (setViaMethod && pagesField != null) {
                    Object value = pagesField.get(bookScreen);
                    if (value instanceof java.util.List) {
                        @SuppressWarnings("unchecked")
                        java.util.List<Object> list = (java.util.List<Object>) value;
                        pages = list;
                    }
                }

                java.util.List<String> pageStrings = new java.util.ArrayList<>();
                for (Object page : pages) {
                    if (page instanceof String) {
                        pageStrings.add((String) page);
                    } else if (page instanceof RawFilteredPair) {
                        @SuppressWarnings("unchecked")
                        RawFilteredPair<String> pair = (RawFilteredPair<String>) page;
                        pageStrings.add(pair.get(false));
                    } else {
                        pageStrings.add("");
                    }
                }

                // Update any page-related fields to ensure the UI refreshes
                TextFieldWidget editBox = null;
                for (Field field : bookScreen.getClass().getDeclaredFields()) {
                    String fieldName = field.getName().toLowerCase();
                    if (field.getType() == java.util.List.class) {
                        if (!fieldName.contains("page")) {
                            continue;
                        }
                        field.setAccessible(true);
                        Object value = field.get(bookScreen);
                        if (value instanceof java.util.List) {
                            @SuppressWarnings("unchecked")
                            java.util.List<Object> list = (java.util.List<Object>) value;
                            try {
                                list.clear();
                                list.addAll(pages);
                            } catch (UnsupportedOperationException ignored) {
                                // Skip immutable lists
                            }
                        }
                        continue;
                    }
                    if (field.getType() == String[].class && fieldName.contains("page")) {
                        field.setAccessible(true);
                        field.set(bookScreen, pageStrings.toArray(new String[0]));
                        continue;
                    }
                    if (field.getType() == String.class
                        && fieldName.contains("page")
                        && (fieldName.contains("text") || fieldName.contains("content"))) {
                        field.setAccessible(true);
                        field.set(bookScreen, truncatedText);
                        continue;
                    }
                    if (field.getType() == TextFieldWidget.class
                        && (fieldName.contains("page") || fieldName.contains("text"))) {
                        field.setAccessible(true);
                        Object value = field.get(bookScreen);
                        if (value instanceof TextFieldWidget) {
                            editBox = (TextFieldWidget) value;
                            editBox.setText(truncatedText);
                        }
                    }
                }
                if (editBox == null) {
                    for (Field field : bookScreen.getClass().getDeclaredFields()) {
                        if (field.getType() == TextFieldWidget.class) {
                            field.setAccessible(true);
                            Object value = field.get(bookScreen);
                            if (value instanceof TextFieldWidget) {
                                editBox = (TextFieldWidget) value;
                                editBox.setText(truncatedText);
                                break;
                            }
                        }
                    }
                }

                // Keep the screen's backing ItemStack in sync so UI updates immediately
                ItemStack screenStack = null;
                for (Field field : bookScreen.getClass().getDeclaredFields()) {
                    if (field.getType() == ItemStack.class) {
                        field.setAccessible(true);
                        Object value = field.get(bookScreen);
                        if (value instanceof ItemStack) {
                            screenStack = (ItemStack) value;
                            break;
                        }
                    }
                }
                if (screenStack != null && screenStack.isOf(Items.WRITABLE_BOOK)) {
                    try {
                        java.util.List<RawFilteredPair<String>> componentPages = new java.util.ArrayList<>();
                        for (String page : pageStrings) {
                            componentPages.add(RawFilteredPair.of(page));
                        }
                        screenStack.set(DataComponentTypes.WRITABLE_BOOK_CONTENT,
                            new WritableBookContentComponent(componentPages));
                    } catch (Exception ignored) {
                        // Ignore component sync errors
                    }
                }

                // Write updated pages into the book stack if possible
                if (writeNbtDataMethod != null) {
                    try {
                        writeNbtDataMethod.invoke(bookScreen);
                    } catch (Exception ignored) {
                        // Ignore persistence errors to avoid stopping execution
                    }
                }

                // Send book update to server and client so text becomes visible immediately.
                Hand hand = Hand.MAIN_HAND;
                for (Field field : bookScreen.getClass().getDeclaredFields()) {
                    if (field.getType() == Hand.class) {
                        field.setAccessible(true);
                        Object value = field.get(bookScreen);
                        if (value instanceof Hand) {
                            hand = (Hand) value;
                            break;
                        }
                    }
                }
                ItemStack main = client.player.getMainHandStack();
                ItemStack offhand = client.player.getOffHandStack();
                if (!main.isOf(Items.WRITABLE_BOOK) && offhand.isOf(Items.WRITABLE_BOOK)) {
                    hand = Hand.OFF_HAND;
                }
                ItemStack heldBook = client.player.getStackInHand(hand);
                if (heldBook != null && heldBook.isOf(Items.WRITABLE_BOOK)) {
                    // Keep stack component in sync so the UI reflects the change immediately
                    try {
                        java.util.List<RawFilteredPair<String>> componentPages = new java.util.ArrayList<>();
                        for (String page : pageStrings) {
                            componentPages.add(RawFilteredPair.of(page));
                        }
                        heldBook.set(DataComponentTypes.WRITABLE_BOOK_CONTENT,
                            new WritableBookContentComponent(componentPages));
                    } catch (Exception ignored) {
                        // Fallback to packet-only update
                    }

                    // Tell server (and client) via standard packet (works in dev env)
                    int slot = hand == Hand.MAIN_HAND
                        ? PlayerInventoryBridge.getSelectedSlot(client.player.getInventory())
                        : PlayerInventory.MAIN_SIZE + Node.PLAYER_ARMOR_SLOT_COUNT;
                    client.getNetworkHandler().sendPacket(
                        new BookUpdateC2SPacket(slot, pageStrings, java.util.Optional.empty())
                    );

                    // Reopen the book screen to force a full UI refresh of the edited text
                    ItemStack reopenStack = screenStack != null ? screenStack : heldBook;
                    if (reopenStack != null && reopenStack.isOf(Items.WRITABLE_BOOK)) {
                        WritableBookContentComponent content = reopenStack.get(DataComponentTypes.WRITABLE_BOOK_CONTENT);
                        if (content == null) {
                            content = WritableBookContentComponent.DEFAULT;
                        }
                        final ItemStack reopenStackFinal = reopenStack;
                        final WritableBookContentComponent contentFinal = content;
                        final Hand reopenHand = hand;
                        final PlayerEntity playerFinal = client.player;
                        client.execute(() -> {
                            if (playerFinal != null) {
                                net.minecraft.client.gui.screen.Screen bookEditScreen = createBookEditScreen(playerFinal, reopenStackFinal, reopenHand, contentFinal);
                                if (bookEditScreen != null) {
                                    client.setScreen(bookEditScreen);
                                }
                            }
                        });
                    }
                }

                // Flag book screen as dirty if such a field exists
                for (Field field : bookScreen.getClass().getDeclaredFields()) {
                    if (field.getType() == boolean.class) {
                        String fieldName = field.getName().toLowerCase();
                        if (fieldName.contains("dirty") || fieldName.contains("modified")) {
                            field.setAccessible(true);
                            field.setBoolean(bookScreen, true);
                            break;
                        }
                    }
                }

                // Safely invoke updatePage if it exists (only after pages are populated)
                if (updatePageMethod != null) {
                    try {
                        if (!pages.isEmpty()) {
                            updatePageMethod.invoke(bookScreen);
                        }
                    } catch (Exception ignored) {
                        // Ignore UI refresh errors to avoid stopping execution
                    }
                }

                // One more refresh on the next tick in case the edit box wasn't ready yet
                final Method setPageTextMethodFinal = setPageTextMethod;
                final Method updatePageMethodFinal = updatePageMethod;
                final java.util.List<Object> pagesFinal = pages;
                final String truncatedTextFinal = truncatedText;
                final int pageIndexFinal = pageIndex;
                final BookEditScreen bookScreenFinal = bookScreen;
                client.execute(() -> {
                    try {
                        if (setPageTextMethodFinal != null && pageIndexFinal >= 0 && pageIndexFinal < pagesFinal.size()) {
                            setPageTextMethodFinal.invoke(bookScreenFinal, truncatedTextFinal);
                        }
                        if (updatePageMethodFinal != null && !pagesFinal.isEmpty()) {
                            updatePageMethodFinal.invoke(bookScreenFinal);
                        }
                        // Force edit box text refresh on the next tick
                        TextFieldWidget delayedEditBox = null;
                        try {
                            Field editBoxField = bookScreenFinal.getClass().getDeclaredField("editBox");
                            editBoxField.setAccessible(true);
                            Object value = editBoxField.get(bookScreenFinal);
                            if (value instanceof TextFieldWidget) {
                                delayedEditBox = (TextFieldWidget) value;
                            }
                        } catch (NoSuchFieldException ignored) {
                            // fall back to scanning fields below
                        }
                        if (delayedEditBox == null) {
                            for (Field field : bookScreenFinal.getClass().getDeclaredFields()) {
                                if (field.getType() == TextFieldWidget.class) {
                                    field.setAccessible(true);
                                    Object value = field.get(bookScreenFinal);
                                    if (value instanceof TextFieldWidget) {
                                        delayedEditBox = (TextFieldWidget) value;
                                        break;
                                    }
                                }
                            }
                        }
                        if (delayedEditBox != null) {
                            delayedEditBox.setText(truncatedTextFinal);
                            bookScreenFinal.setFocused(delayedEditBox);
                        }
                    } catch (Exception ignored) {
                        // Ignore delayed UI refresh errors
                    }
                });

                future.complete(null);
            } catch (Exception e) {
                String message = e.getMessage();
                if (message == null || message.isBlank()) {
                    message = e.getClass().getSimpleName();
                }
                sendNodeErrorMessage(client, "Error writing to book: " + message);
                future.completeExceptionally(e);
            }
        });
    }

    private static net.minecraft.client.gui.screen.Screen createBookEditScreen(
            PlayerEntity player, ItemStack stack, Hand hand, WritableBookContentComponent content) {
        try {
            // Try 4-arg constructor (newer MC versions)
            java.lang.reflect.Constructor<?> ctor = BookEditScreen.class.getConstructor(
                PlayerEntity.class, ItemStack.class, Hand.class, WritableBookContentComponent.class);
            return (net.minecraft.client.gui.screen.Screen) ctor.newInstance(player, stack, hand, content);
        } catch (NoSuchMethodException ignored) {
            // Fall through to 3-arg constructor
        } catch (ReflectiveOperationException e) {
            return null;
        }
        try {
            // Try 3-arg constructor (MC 1.21)
            java.lang.reflect.Constructor<?> ctor = BookEditScreen.class.getConstructor(
                PlayerEntity.class, ItemStack.class, Hand.class);
            return (net.minecraft.client.gui.screen.Screen) ctor.newInstance(player, stack, hand);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    void executeWriteSignCommand(CompletableFuture<Void> future) {
        if (preprocessAttachedParameter(EnumSet.noneOf(Node.ParameterUsage.class), future) == Node.ParameterHandlingResult.COMPLETE) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            sendNodeErrorMessage(client, "Client or player not available");
            future.completeExceptionally(new RuntimeException("Client or player not available"));
            return;
        }

        if (!(client.currentScreen instanceof AbstractSignEditScreen)) {
            sendNodeErrorMessage(client, "No sign edit screen is open");
            future.completeExceptionally(new RuntimeException("No sign edit screen is open"));
            return;
        }

        String[] lines = new String[Node.SIGN_MAX_LINES];
        Arrays.fill(lines, "");
        String[] split = (getBookText() == null ? "" : getBookText()).split("\\n", -1);
        int copyCount = Math.min(Node.SIGN_MAX_LINES, split.length);
        for (int i = 0; i < copyCount; i++) {
            String line = split[i] == null ? "" : split[i];
            lines[i] = line.length() > Node.SIGN_LINE_MAX_CHARS ? line.substring(0, Node.SIGN_LINE_MAX_CHARS) : line;
        }

        final String[] signLines = lines;
        final AbstractSignEditScreen signScreen = (AbstractSignEditScreen) client.currentScreen;
        client.execute(() -> {
            try {
                Field currentRowField = null;
                Method setCurrentRowMessageMethod = null;
                for (Class<?> cls = signScreen.getClass(); cls != null; cls = cls.getSuperclass()) {
                    for (Field field : cls.getDeclaredFields()) {
                        String fieldName = field.getName().toLowerCase(Locale.ROOT);
                        if (field.getType() == int.class && (fieldName.contains("currentrow") || fieldName.equals("field_40428"))) {
                            field.setAccessible(true);
                            currentRowField = field;
                            break;
                        }
                    }
                    if (currentRowField != null) {
                        break;
                    }
                }
                for (Class<?> cls = signScreen.getClass(); cls != null; cls = cls.getSuperclass()) {
                    for (Method method : cls.getDeclaredMethods()) {
                        String methodName = method.getName().toLowerCase(Locale.ROOT);
                        if (method.getParameterCount() == 1
                            && method.getParameterTypes()[0] == String.class
                            && method.getReturnType() == void.class
                            && (methodName.contains("setcurrentrowmessage") || methodName.equals("method_49913"))) {
                            method.setAccessible(true);
                            setCurrentRowMessageMethod = method;
                            break;
                        }
                    }
                    if (setCurrentRowMessageMethod != null) {
                        break;
                    }
                }

                if (currentRowField != null && setCurrentRowMessageMethod != null) {
                    for (int i = 0; i < signLines.length; i++) {
                        currentRowField.setInt(signScreen, i);
                        setCurrentRowMessageMethod.invoke(signScreen, signLines[i]);
                    }
                }

                for (Class<?> cls = signScreen.getClass(); cls != null; cls = cls.getSuperclass()) {
                    for (Field field : cls.getDeclaredFields()) {
                        field.setAccessible(true);
                        if (field.getType() == String[].class) {
                            Object raw = field.get(signScreen);
                            if (raw instanceof String[] target && target.length >= Node.SIGN_MAX_LINES) {
                                for (int i = 0; i < Node.SIGN_MAX_LINES; i++) {
                                    target[i] = signLines[i];
                                }
                                field.set(signScreen, target);
                            }
                        }
                    }
                }

                future.complete(null);
            } catch (Exception e) {
                String message = e.getMessage();
                if (message == null || message.isBlank()) {
                    message = e.getClass().getSimpleName();
                }
                sendNodeErrorMessage(client, "Error writing to sign: " + message);
                future.completeExceptionally(e);
            }
        });
    }

    private Node.ParameterHandlingResult preprocessAttachedParameter(EnumSet<Node.ParameterUsage> usages, CompletableFuture<Void> future) {
        return owner.preprocessAttachedParameter(usages, future);
    }

    private int getIntParameter(String name, int defaultValue) {
        return owner.getIntParameter(name, defaultValue);
    }

    private String getBookTextForPage(int pageNumber) {
        return owner.getBookTextForPage(pageNumber);
    }

    private String getBookText() {
        return owner.getBookText();
    }

    private void sendNodeErrorMessage(MinecraftClient client, String message) {
        owner.sendNodeErrorMessage(client, message);
    }
}
