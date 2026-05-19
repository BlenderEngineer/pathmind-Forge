package com.pathmind.util;

import net.minecraft.client.gui.Font;

/**
 * Utility helpers for rendering text within constrained widths.
 */
public final class TextRenderUtil {
    private static final String ELLIPSIS = "...";

    private TextRenderUtil() {
    }

    public static String trimWithEllipsis(Font renderer, String text, int availableWidth) {
        if (renderer == null || text == null) {
            return "";
        }
        if (availableWidth <= 0) {
            return ELLIPSIS;
        }
        if (renderer.width(text) <= availableWidth) {
            return text;
        }

        int ellipsisWidth = renderer.width(ELLIPSIS);
        if (ellipsisWidth >= availableWidth) {
            return ELLIPSIS;
        }

        int trimmedWidth = Math.max(0, availableWidth - ellipsisWidth);
        return renderer.plainSubstrByWidth(text, trimmedWidth) + ELLIPSIS;
    }
}
