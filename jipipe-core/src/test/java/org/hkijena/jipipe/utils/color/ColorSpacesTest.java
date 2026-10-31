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

package org.hkijena.jipipe.utils.color;

import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The color-space math behind the modern color dialog (#1339): RGB/HSB and
 * RGB/LAB round-trips, the LAB→sRGB gamut clamp, and hex parsing/formatting.
 */
class ColorSpacesTest {

    private static boolean near(int got, int want) {
        return near(got, want, 0.5);
    }

    private static boolean near(int got, int want, double tol) {
        return Math.abs(got - want) <= tol;
    }

    @Test
    void hsbRoundTrip() {
        Color c = new Color(59, 157, 255);
        double[] hsb = ColorSpaces.toHsb(c);
        Color back = ColorSpaces.fromHsb(hsb[0], hsb[1], hsb[2]);
        assertTrue(near(back.getRed(), c.getRed()));
        assertTrue(near(back.getGreen(), c.getGreen()));
        assertTrue(near(back.getBlue(), c.getBlue()));
    }

    @Test
    void hsbKnownValue() {
        // red: h=0, s=100, b=100
        assertArrayEquals(new double[]{0, 100, 100}, ColorSpaces.toHsb(Color.RED), 0.01);
    }

    @Test
    void labRoundTripInGamut() {
        Color c = new Color(59, 157, 255);
        double[] lab = ColorSpaces.toLab(c);
        Color back = ColorSpaces.fromLab(lab[0], lab[1], lab[2]);
        assertTrue(near(back.getRed(), c.getRed(), 1.0), "R " + back.getRed() + " vs " + c.getRed());
        assertTrue(near(back.getGreen(), c.getGreen(), 1.0), "G " + back.getGreen() + " vs " + c.getGreen());
        assertTrue(near(back.getBlue(), c.getBlue(), 1.0), "B " + back.getBlue() + " vs " + c.getBlue());
    }

    @Test
    void labOutOfGamutClamps() {
        Color c = ColorSpaces.fromLab(100, 100, 100);
        // must be a valid sRGB (in-range), and a saturated red-ish color
        assertTrue(c.getRed() > 150 && c.getGreen() < 150 && c.getBlue() < 150,
                "expected a clamped red-ish color, got " + c.getRed() + "," + c.getGreen() + "," + c.getBlue());
    }

    @Test
    void hexRoundTripWithAlpha() {
        assertEquals("#3B9DFF", ColorSpaces.toHex(new Color(0x3b, 0x9d, 0xff)));
        assertEquals("#3B9DFF80", ColorSpaces.toHex(new Color(0x3b, 0x9d, 0xff, 0x80)));
        Color c = ColorSpaces.fromHex("#3b9dffb3");
        assertEquals(0x3b, c.getRed());
        assertEquals(0xb3, c.getAlpha());
    }
}
