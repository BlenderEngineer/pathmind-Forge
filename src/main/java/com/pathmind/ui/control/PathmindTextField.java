package com.pathmind.ui.control;

import com.pathmind.mixin.TextFieldWidgetAccessor;
import com.pathmind.ui.theme.UIStyleHelper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.Util;
import net.minecraft.util.Mth;

import java.lang.reflect.Field;

public class PathmindTextField extends EditBox {
    private static final int SELECTION_COLOR = 0x664F86C6;
    private static final Field TEXT_SHADOW_FIELD = findTextShadowField();

    private final Font pathmindFont;

    public PathmindTextField(Font textRenderer, int x, int y, int width, int height, Component text) {
        super(textRenderer, x, y, width, height, text);
        this.pathmindFont = textRenderer;
    }

    @Override
    public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (!this.isVisible()) {
            return;
        }

        TextFieldWidgetAccessor accessor = (TextFieldWidgetAccessor) this;
        int innerX = this.getX() + (this.isBordered() ? 4 : 0);
        int innerWidth = Math.max(0, this.getInnerWidth());
        int textY = this.getY() + Math.max(0, (this.getHeight() - this.pathmindFont.lineHeight) / 2);
        int textColor = accessor.pathmind$isEditable() ? accessor.pathmind$getEditableColor() : accessor.pathmind$getUneditableColor();
        String text = this.getValue();
        int firstCharacterIndex = Mth.clamp(accessor.pathmind$getFirstCharacterIndex(), 0, text.length());
        String visibleText = this.pathmindFont.plainSubstrByWidth(text.substring(firstCharacterIndex), innerWidth);

        renderSelection(context, accessor, textY);
        renderText(context, accessor, innerX, textY, innerWidth, textColor, visibleText);
        renderSuggestion(context, accessor, textY, textColor, text, visibleText);
        renderCaret(context, textY);
    }

    private void renderSelection(GuiGraphics context, TextFieldWidgetAccessor accessor, int textY) {
        int selectionStart = accessor.pathmind$getSelectionStart();
        int selectionEnd = accessor.pathmind$getSelectionEnd();
        if (selectionStart == selectionEnd) {
            return;
        }
        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);
        int left = this.getScreenX(start);
        int right = this.getScreenX(end);
        if (right < left) {
            int swap = left;
            left = right;
            right = swap;
        }
        int fieldLeft = this.getX() + (this.isBordered() ? 4 : 0);
        int fieldRight = fieldLeft + Math.max(0, this.getInnerWidth());
        left = Mth.clamp(left, fieldLeft, fieldRight);
        right = Mth.clamp(right, fieldLeft, fieldRight);
        if (right > left) {
            context.fill(left, textY - 1, right, textY + this.pathmindFont.lineHeight + 1, SELECTION_COLOR);
        }
    }

    private void renderText(GuiGraphics context, TextFieldWidgetAccessor accessor, int innerX, int textY, int innerWidth,
                            int textColor, String visibleText) {
        if (!visibleText.isEmpty()) {
            if (usesTextShadow()) {
                context.drawString(this.pathmindFont, visibleText, innerX, textY, textColor);
            } else {
                context.drawString(this.pathmindFont, visibleText, innerX, textY, textColor, false);
            }
            return;
        }
        Component placeholder = accessor.pathmind$getPlaceholder();
        if (!this.isFocused() && placeholder != null) {
            String placeholderText = this.pathmindFont.plainSubstrByWidth(placeholder.getString(), innerWidth);
            context.drawString(this.pathmindFont, placeholderText, innerX, textY, textColor, false);
        }
    }

    private void renderSuggestion(GuiGraphics context, TextFieldWidgetAccessor accessor, int textY, int textColor,
                                  String text, String visibleText) {
        String suggestion = accessor.pathmind$getSuggestion();
        if (suggestion == null || suggestion.isEmpty() || this.getCursorPosition() != text.length()) {
            return;
        }
        int suggestionX = this.getScreenX(this.getCursorPosition());
        int fieldRight = this.getX() + (this.isBordered() ? 4 : 0) + Math.max(0, this.getInnerWidth());
        if (suggestionX >= fieldRight) {
            return;
        }
        int remainingWidth = fieldRight - suggestionX;
        if (remainingWidth <= 0) {
            return;
        }
        String visibleSuggestion = this.pathmindFont.plainSubstrByWidth(suggestion, remainingWidth);
        if (visibleSuggestion.isEmpty()) {
            return;
        }
        context.drawString(this.pathmindFont, visibleSuggestion, suggestionX, textY, (textColor & 0x00FFFFFF) | 0x77000000, false);
    }

    private void renderCaret(GuiGraphics context, int textY) {
        if (!this.isFocused() || ((Util.getMillis() / 300L) & 1L) != 0L) {
            return;
        }
        int caretX = this.getScreenX(this.getCursorPosition());
        int fieldLeft = this.getX() + (this.isBordered() ? 4 : 0);
        int fieldRight = fieldLeft + Math.max(0, this.getInnerWidth());
        if (caretX < fieldLeft || caretX > fieldRight) {
            return;
        }
        UIStyleHelper.drawTextCaret(context, caretX, textY, textY + this.pathmindFont.lineHeight, fieldRight, 0xFFFFFFFF);
    }

    private boolean usesTextShadow() {
        if (TEXT_SHADOW_FIELD == null) {
            return false;
        }
        try {
            return TEXT_SHADOW_FIELD.getBoolean(this);
        } catch (IllegalAccessException ignored) {
            return false;
        }
    }

    private static Field findTextShadowField() {
        try {
            Field field = EditBox.class.getDeclaredField("textShadow");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException ignored) {
            return null;
        }
    }
}
