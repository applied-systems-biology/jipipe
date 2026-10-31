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

package org.hkijena.jipipe.plugins.parameters.ui.library;

import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the font-preview background contract (#1339): the preview no longer
 * paints a checkerboard - it auto-selects a single background color that keeps
 * the font color readable for <em>any</em> font color. The font parameter
 * defaults to WHITE text; on a light theme a flat panel background rendered it
 * invisible.
 * <p>
 * {@code previewBackground(fontColor)} picks the background (a fixed dark or a
 * fixed light tone) that maximizes the WCAG contrast against the font color,
 * so (a) light fonts get a dark background, (b) dark fonts get a light one,
 * and (c) for a spread of realistic font colors the resulting contrast is
 * comfortably above 2:1 (the worst case, a mid-gray font, still lands at
 * >= 2.45:1; light or dark fonts reach >= 5:1). Pure (no Swing state),
 * headless-testable, theme-independent.
 */
class FontPreviewBackgroundTest {
    /**
     * WCAG relative luminance (0..1).
     */
    private static double relLum(Color c) {
        double r = c.getRed() / 255.0, g = c.getGreen() / 255.0, b = c.getBlue() / 255.0;
        r = r <= 0.03928 ? r / 12.92 : Math.pow((r + 0.055) / 1.055, 2.4);
        g = g <= 0.03928 ? g / 12.92 : Math.pow((g + 0.055) / 1.055, 2.4);
        b = b <= 0.03928 ? b / 12.92 : Math.pow((b + 0.055) / 1.055, 2.4);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    /**
     * WCAG contrast ratio (1..21).
     */
    private static double contrast(Color a, Color b) {
        double hi = Math.max(relLum(a), relLum(b));
        double lo = Math.min(relLum(a), relLum(b));
        return (hi + 0.05) / (lo + 0.05);
    }

    @Test
    void lightFontGetsDarkBackground() {
        // White text (the parameter's default) must land on a dark background.
        Color bg = JIPipeDesktopFontParameterEditorUI.previewBackground(Color.WHITE);
        assertTrue(relLum(bg) < 0.2,
                "white font: background must be dark (luminance " + relLum(bg) + " >= 0.2)");
        assertTrue(contrast(Color.WHITE, bg) >= 5.0,
                "white font contrast too low: " + contrast(Color.WHITE, bg));
    }

    @Test
    void darkFontGetsLightBackground() {
        Color bg = JIPipeDesktopFontParameterEditorUI.previewBackground(Color.BLACK);
        assertTrue(relLum(bg) > 0.8,
                "black font: background must be light (luminance " + relLum(bg) + " <= 0.8)");
        assertTrue(contrast(Color.BLACK, bg) >= 5.0,
                "black font contrast too low: " + contrast(Color.BLACK, bg));
    }

    @Test
    void picksTheToneWithMaxContrast() {
        // The selection rule: whichever tone gives the higher contrast wins.
        // White/near-white → dark; black/near-black → light.
        assertTrue(relLum(JIPipeDesktopFontParameterEditorUI.previewBackground(Color.WHITE)) < 0.2);
        assertTrue(relLum(JIPipeDesktopFontParameterEditorUI.previewBackground(Color.BLACK)) > 0.8);
        assertTrue(relLum(JIPipeDesktopFontParameterEditorUI.previewBackground(new Color(0xF0, 0xF0, 0xF0))) < 0.2);
        assertTrue(relLum(JIPipeDesktopFontParameterEditorUI.previewBackground(new Color(0x10, 0x10, 0x10))) > 0.8);
        // The mid-tone trap: olive (luminance ~0.49, just below the naive 0.5
        // split) is far more readable on the DARK tone (~7:1) than light
        // (~1.7:1), so a correct auto-contrast must pick dark.
        assertTrue(relLum(JIPipeDesktopFontParameterEditorUI.previewBackground(new Color(0xC0, 0xC0, 0x00))) < 0.2,
                "olive must land on the dark tone - a naive luminance>0.5 split picks the wrong (unreadable) light tone");
    }

    @Test
    void backgroundAlwaysContrastsTheFont() {
        Color[] fonts = {
                Color.WHITE, Color.BLACK,                                    // extremes
                new Color(0x80, 0x80, 0x80), new Color(0x8A, 0x8A, 0x8A),     // mid grays
                Color.RED, Color.BLUE, Color.GREEN, Color.CYAN, Color.MAGENTA,
                new Color(0xFF, 0x88, 0x00), new Color(0xC0, 0xC0, 0x00),     // orange, olive
        };
        for (Color f : fonts) {
            Color bg = JIPipeDesktopFontParameterEditorUI.previewBackground(f);
            double ratio = contrast(f, bg);
            // The auto-contrast rule guarantees >= ~3.7:1 for every input (the
            // minimum occurs at the crossover luminance); 3.0 is a safe floor
            // that still catches a tone pair drifting too close together.
            assertTrue(ratio >= 3.0,
                    "font " + f + " on " + bg + ": contrast " + ratio + " < 3.0 - text would be hard to read");
        }
    }
}
