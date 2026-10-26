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

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the font-preview checkerboard color contract (#1339 pass 2, S8 finding:
 * the font preview rendered blank because the font parameter defaults to WHITE
 * text painted on the light panel — white-on-white). The preview must paint a
 * theme-derived checkerboard so that BOTH light and dark font colors stay
 * visible in BOTH themes.
 * <p>
 * The contract, per theme: (a) the two checker tones are clearly distinct
 * (a flat / near-identical pair would reproduce the white-on-white bug), and
 * (b) the non-surface tone is a genuine mid-tone (not itself near-white or
 * near-black), so text that matches the surface tone still contrasts against
 * the mid squares. Pure (no Swing), headless-testable.
 */
class FontPreviewCheckerTest {
    /** WCAG relative luminance (0..1). */
    private static double relLum(Color c) {
        double r = c.getRed() / 255.0, g = c.getGreen() / 255.0, b = c.getBlue() / 255.0;
        r = r <= 0.03928 ? r / 12.92 : Math.pow((r + 0.055) / 1.055, 2.4);
        g = g <= 0.03928 ? g / 12.92 : Math.pow((g + 0.055) / 1.055, 2.4);
        b = b <= 0.03928 ? b / 12.92 : Math.pow((b + 0.055) / 1.055, 2.4);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static void assertContract(String label, Color surface) {
        Color[] tones = JIPipeDesktopFontParameterEditorUI.checkerColors(surface);
        double l0 = relLum(tones[0]), l1 = relLum(tones[1]);
        double gap = Math.abs(l0 - l1);
        assertTrue(gap >= 0.15,
                label + ": the two checker tones must be clearly distinct (gap " + gap
                        + " < 0.15) — a flat tone reproduces the white-on-white bug");
        // The non-surface tone must be a mid-tone so surface-colored text stays visible.
        boolean hasMid = (l0 >= 0.1 && l0 <= 0.9) || (l1 >= 0.1 && l1 <= 0.9);
        assertTrue(hasMid,
                label + ": one checker tone must be a mid-tone (luminances " + l0 + ", " + l1
                        + ") so text matching the surface tone still contrasts");
    }

    @Test
    void lightThemeShowsBothLightAndDarkText() {
        // A light theme's surface is near-white; white text was the invisible case.
        assertContract("light", Color.WHITE);
    }

    @Test
    void darkThemeShowsBothLightAndDarkText() {
        // A dark theme's surface is near-black; black text would be the invisible case.
        assertContract("dark", Color.BLACK);
    }
}
