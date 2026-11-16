/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.utils;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws a fake window titlebar in KDE 6 Breeze style (flat dark bar, left-aligned title,
 * three vector-drawn window control glyphs right-aligned: minimize, maximize, close) for
 * documentation screenshots.
 * <p>
 * Text placement is always derived from the {@link FontMetrics} passed to
 * {@link #layout(String, FontMetrics, int)} - never from hardcoded text sizes. Only pure
 * gap constants (padding between elements) and the fixed button size (the buttons contain
 * no text) are hardcoded. The painter is deterministic and headless-safe: it only draws
 * onto a caller-provided {@link Graphics2D} (a {@code BufferedImage.createGraphics()}
 * works headless), it never instantiates components.
 */
public final class FakeTitlebarPainter {

    /**
     * Pure gap constants. Nothing here encodes a text size: the bar height and the title
     * position are derived from the font metrics, the buttons contain no text.
     */
    private static final int BAR_VERTICAL_PADDING = 5;
    private static final int TITLE_LEFT_PADDING = 8;
    private static final int BUTTON_GAP = 8;
    private static final int BUTTON_RIGHT_MARGIN = 8;
    private static final int BUTTON_SIZE = 20;

    /**
     * Breeze dark theme colors (approximate by design: flat neutral bar, light content).
     */
    private static final Color BAR_BACKGROUND = new Color(0x31, 0x36, 0x3B);
    private static final Color CONTENT = new Color(0xFC, 0xFC, 0xFC);

    private FakeTitlebarPainter() {
    }

    /**
     * Computes the layout of a titlebar of the given width.
     *
     * @param title   the window title that will be painted later (the geometry itself does
     *                not depend on it: the painter ellipsizes against the reserved text rect)
     * @param metrics the font metrics of the font that {@link #paint(Graphics2D, FakeTitlebarLayout, String)}
     *                will draw with
     * @param width   the total titlebar width
     * @return the layout, with non-negative coordinates
     */
    public static FakeTitlebarLayout layout(String title, FontMetrics metrics, int width) {
        int barHeight = metrics.getHeight() + 2 * BAR_VERTICAL_PADDING;
        int baseline = BAR_VERTICAL_PADDING + metrics.getAscent();
        int right = width - BUTTON_RIGHT_MARGIN;
        List<Rectangle> buttons = new ArrayList<>(3);
        for (int i = 0; i < 3; i++) {
            // Build from the right edge so the group is right-aligned; insertion at index 0
            // yields the minimize, maximize, close order left to right.
            int x = right - BUTTON_SIZE;
            int y = Math.max(0, (barHeight - BUTTON_SIZE) / 2);
            buttons.add(0, new Rectangle(x, y, BUTTON_SIZE, BUTTON_SIZE));
            right = x - BUTTON_GAP;
        }
        int titleX = TITLE_LEFT_PADDING;
        int maxTitleWidth = Math.max(0, buttons.get(0).x - BUTTON_GAP - titleX);
        return new FakeTitlebarLayout(width, barHeight, titleX, baseline, maxTitleWidth,
                List.copyOf(buttons));
    }

    /**
     * Paints the titlebar: flat bar, the title (ellipsized when it would collide with the
     * button area), and the three window control glyphs. Uses the font currently set on
     * {@code g}: the layout must have been computed with the same font's metrics. Restores
     * the rendering hints and color after painting.
     *
     * @param g      the target graphics (headless-safe, e.g. from a BufferedImage)
     * @param layout the layout to paint
     * @param title  the window title
     */
    public static void paint(Graphics2D g, FakeTitlebarLayout layout, String title) {
        FontMetrics metrics = g.getFontMetrics();
        Object oldAntialiasing = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Color oldColor = g.getColor();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(BAR_BACKGROUND);
            g.fillRect(0, 0, layout.width(), layout.barHeight());
            g.setColor(CONTENT);
            String display = ellipsize(title == null ? "" : title, metrics, layout.maxTitleWidth());
            g.drawString(display, layout.titleX(), layout.titleBaselineY());
            paintButtons(g, layout.buttons());
        } finally {
            if (oldAntialiasing != null) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialiasing);
            }
            g.setColor(oldColor);
        }
    }

    /**
     * Shortens the title with an ellipsis so that it fits into the given maximum width
     * according to the metrics. Returns the title unchanged when it already fits, and an
     * empty string when not even the ellipsis fits.
     */
    public static String ellipsize(String title, FontMetrics metrics, int maxWidth) {
        if (title == null || title.isEmpty() || metrics.stringWidth(title) <= maxWidth) {
            return title;
        }
        String ellipsis = "...";
        if (metrics.stringWidth(ellipsis) > maxWidth) {
            return "";
        }
        int low = 0;
        int high = title.length();
        while (low < high) {
            int mid = (low + high + 1) / 2;
            if (metrics.stringWidth(title.substring(0, mid) + ellipsis) <= maxWidth) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return title.substring(0, low) + ellipsis;
    }

    private static void paintButtons(Graphics2D g, List<Rectangle> buttons) {
        // Glyph shapes are vector-drawn inside the fixed-size buttons: a horizontal line
        // (minimize), a square outline (maximize), and a diagonal cross (close).
        paintMinimize(g, buttons.get(0));
        paintMaximize(g, buttons.get(1));
        paintClose(g, buttons.get(2));
    }

    private static void paintMinimize(Graphics2D g, Rectangle button) {
        int y = button.y + button.height - 6;
        g.drawLine(button.x + 5, y, button.x + button.width - 6, y);
    }

    private static void paintMaximize(Graphics2D g, Rectangle button) {
        int size = 9;
        int x = button.x + (button.width - size) / 2;
        int y = button.y + (button.height - size) / 2 - 1;
        g.drawRect(x, y, size, size);
    }

    private static void paintClose(Graphics2D g, Rectangle button) {
        int size = 8;
        int x1 = button.x + (button.width - size) / 2;
        int y1 = button.y + (button.height - size) / 2;
        int x2 = x1 + size;
        int y2 = y1 + size;
        g.drawLine(x1, y1, x2, y2);
        g.drawLine(x1, y2, x2, y1);
    }

    /**
     * Layout of a fake titlebar: the reserved title text position, the reserved text width,
     * and the window control button rects (minimize, maximize, close, left to right). All
     * geometry is derived in {@link #layout(String, FontMetrics, int)}.
     */
    public record FakeTitlebarLayout(int width,
                                     int barHeight,
                                     int titleX,
                                     int titleBaselineY,
                                     int maxTitleWidth,
                                     List<Rectangle> buttons) {
    }
}
