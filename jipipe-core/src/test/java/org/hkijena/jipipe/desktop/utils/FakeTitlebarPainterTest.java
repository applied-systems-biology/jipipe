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

import org.junit.jupiter.api.Test;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the fake titlebar layout: metrics-derived text placement, ellipsization against the
 * reserved text rect, and the right-aligned window control buttons. Headless-safe: all
 * measurements come from a {@link BufferedImage} graphics, no components are realized.
 */
public class FakeTitlebarPainterTest {

    private static FontMetrics metrics() {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            return graphics.getFontMetrics();
        } finally {
            graphics.dispose();
        }
    }

    @Test
    void longTitleIsEllipsizedToReservedRect() {
        FontMetrics metrics = metrics();
        String title = "A very long documentation title that certainly does not fit into a narrow titlebar";
        FakeTitlebarPainter.FakeTitlebarLayout layout = FakeTitlebarPainter.layout(title, metrics, 300);
        String display = FakeTitlebarPainter.ellipsize(title, metrics, layout.maxTitleWidth());
        assertTrue(display.endsWith("..."), "an ellipsized title must paint an ellipsis");
        assertNotEquals(title, display, "a title wider than the reserved rect must be shortened");
        assertTrue(metrics.stringWidth(display) <= layout.maxTitleWidth(),
                "painted text width must never exceed the reserved text rect");
    }

    @Test
    void fittingTitleIsNotEllipsized() {
        FontMetrics metrics = metrics();
        String title = "Pipeline";
        FakeTitlebarPainter.FakeTitlebarLayout layout = FakeTitlebarPainter.layout(title, metrics, 600);
        assertEquals(title, FakeTitlebarPainter.ellipsize(title, metrics, layout.maxTitleWidth()));
    }

    @Test
    void buttonRectsAreRightAlignedWithEqualGapsInsideBar() {
        FontMetrics metrics = metrics();
        FakeTitlebarPainter.FakeTitlebarLayout layout = FakeTitlebarPainter.layout("Title", metrics, 400);
        List<Rectangle> buttons = layout.buttons();
        assertEquals(3, buttons.size(), "minimize, maximize, close");
        for (Rectangle button : buttons) {
            assertEquals(buttons.get(0).width, button.width, "buttons are fixed-size and square-ish");
            assertEquals(buttons.get(0).height, button.height, "buttons are fixed-size and square-ish");
            assertTrue(button.x >= 0 && button.y >= 0, "buttons are inside the bar");
            assertTrue(button.x + button.width <= layout.width(), "buttons are inside the bar");
            assertTrue(button.y + button.height <= layout.barHeight(), "buttons are inside the bar");
        }
        for (int i = 0; i < buttons.size() - 1; i++) {
            int gapAfterThis = buttons.get(i + 1).x - (buttons.get(i).x + buttons.get(i).width);
            int gapAfterLast = layout.width() - (buttons.get(buttons.size() - 1).x + buttons.get(2).width);
            assertTrue(gapAfterThis > 0, "buttons do not overlap");
            assertEquals(gapAfterLast, gapAfterThis, "right margin equals the inter-button gap");
            if (i > 0) {
                int gapBefore = buttons.get(i).x - (buttons.get(i - 1).x + buttons.get(i - 1).width);
                assertEquals(gapBefore, gapAfterThis, "gaps between buttons are equal");
            }
        }
        int rightmost = buttons.stream().mapToInt(button -> button.x + button.width).max().orElseThrow();
        int leftmost = buttons.stream().mapToInt(button -> button.x).min().orElseThrow();
        assertTrue(layout.width() - rightmost < leftmost, "the button group hugs the right edge");
    }

    @Test
    void layoutHasNoNegativeCoordinatesAtWidth100() {
        FontMetrics metrics = metrics();
        FakeTitlebarPainter.FakeTitlebarLayout layout = FakeTitlebarPainter.layout("T", metrics, 100);
        assertTrue(layout.width() == 100);
        assertTrue(layout.barHeight() > 0);
        assertTrue(layout.titleX() >= 0);
        assertTrue(layout.titleBaselineY() >= 0);
        assertTrue(layout.maxTitleWidth() >= 0);
        for (Rectangle button : layout.buttons()) {
            assertTrue(button.x >= 0 && button.y >= 0);
            assertTrue(button.width > 0 && button.height > 0);
        }
    }

    @Test
    void layoutHasNoNegativeCoordinatesAtNarrowWidth() {
        // The three buttons plus margins need roughly 84 px: below that the x clamp must
        // keep every rectangle inside the bar (the javadoc promises non-negative
        // coordinates unqualified). The rects may overlap at such widths.
        FontMetrics metrics = metrics();
        for (int width : List.of(28, 27, 10, 1)) {
            FakeTitlebarPainter.FakeTitlebarLayout layout = FakeTitlebarPainter.layout("T", metrics, width);
            assertTrue(layout.titleX() >= 0, "titleX at width " + width);
            assertTrue(layout.titleBaselineY() >= 0, "titleBaselineY at width " + width);
            assertTrue(layout.maxTitleWidth() >= 0, "maxTitleWidth at width " + width);
            assertEquals(3, layout.buttons().size(), "three buttons at width " + width);
            for (Rectangle button : layout.buttons()) {
                assertTrue(button.x >= 0, "button x at width " + width + ": " + button);
                assertTrue(button.y >= 0, "button y at width " + width + ": " + button);
                assertTrue(button.width > 0 && button.height > 0,
                        "button size at width " + width + ": " + button);
            }
        }
    }

    @Test
    void paintIsDeterministic() {
        FontMetrics metrics = metrics();
        String title = "Deterministic documentation titlebar";
        FakeTitlebarPainter.FakeTitlebarLayout layout = FakeTitlebarPainter.layout(title, metrics, 320);
        BufferedImage first = paint(layout, title, 320);
        BufferedImage second = paint(layout, title, 320);
        assertArrayEquals(pixels(first), pixels(second),
                "two paints of the same layout and title must be identical");
    }

    private static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }

    private static BufferedImage paint(FakeTitlebarPainter.FakeTitlebarLayout layout, String title, int width) {
        BufferedImage image = new BufferedImage(width, layout.barHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            FakeTitlebarPainter.paint(graphics, layout, title);
        } finally {
            graphics.dispose();
        }
        return image;
    }
}
