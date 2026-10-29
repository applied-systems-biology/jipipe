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

package org.hkijena.jipipe.desktop.commons.components.color;

import org.hkijena.jipipe.JIPipe;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for the LAB well's click geometry (#1339).
 * <p>
 * The a/b plane spans a, b ∈ [-128, +128] (the well's background image and the
 * marker's draw math both use that range). A click at pixel (x, y) must therefore
 * map to a = (x/w*2 - 1) * 128 and b = (1 - y/h*2) * 128 - i.e. the well CENTER
 * is the neutral axis (a=b=0), TOP-RIGHT is (+128, +128), BOTTOM-LEFT is
 * (-128, -128).
 * <p>
 * The previous version of this test asserted the marker against the click
 * handler's OWN (buggy) formula, so it passed while the well misbehaved: a
 * stray term shifted every clicked b by +128, so the selected color got the
 * wrong b and the marker was drawn in the top half (or above the component,
 * hence "sometimes appears").
 * <p>
 * AWT-backed (drives synthetic mouse presses with real geometry); skips when
 * headless, mirroring {@code ColorChooserDialogRefreshTest}.
 */
class ColorChooserDialogLabMarkerTest {

    private static final double TOL = 1.5;

    @BeforeAll
    static void boot() {
        try {
            if (GraphicsEnvironment.isHeadless()) {
                Assumptions.assumeTrue(false, "headless environment");
            }
        } catch (Throwable t) {
            Assumptions.assumeTrue(false, "AWT not available: " + t);
        }
        JIPipe.ensureInstance();
    }

    private static JIPipeDesktopColorChooserDialog dialogWithLabTab() {
        JIPipeDesktopColorChooserDialog d =
                new JIPipeDesktopColorChooserDialog(null, "t", new Color(128, 128, 128), false);
        d.showLabTab();
        JComponent lab = d.getLabWell();
        assertNotNull(lab, "LAB well must exist after the LAB tab is built");
        return d;
    }

    private static void press(JComponent c, int x, int y) {
        c.dispatchEvent(new MouseEvent(c, MouseEvent.MOUSE_PRESSED, 0L, 0, x, y, 1, false));
    }

    @Test
    void clickAtWellCenterSelectsNeutralAndMarkerSitsAtCenter() {
        JIPipeDesktopColorChooserDialog d = dialogWithLabTab();
        JComponent lab = d.getLabWell();
        int w = Math.max(2, lab.getWidth());
        int h = Math.max(2, lab.getHeight());

        press(lab, w / 2, h / 2);

        // The marker must sit at the neutral axis (0, 0) - a center click.
        double[] marker = d.getLabMarkerAb();
        assertNotNull(marker);
        assertEquals(0.0, marker[0], TOL, "center click: marker a must be ~0");
        assertEquals(0.0, marker[1], TOL, "center click: marker b must be ~0");

        // And the SELECTED color must be neutral too (a=b=0 at the current L) -
        // the center of the plane is the gray axis, not some shifted hue.
        double[] picked = org.hkijena.jipipe.utils.color.ColorSpaces.toLab(
                org.hkijena.jipipe.utils.color.ColorSpaces.fromHex(d.getHexText()));
        assertEquals(0.0, picked[1], 1.0, "center click: selected color a must be ~0");
        assertEquals(0.0, picked[2], 1.0, "center click: selected color b must be ~0");
    }

    @Test
    void clickAtTopRightCornerMapsToPositiveAb() {
        JIPipeDesktopColorChooserDialog d = dialogWithLabTab();
        JComponent lab = d.getLabWell();
        int w = Math.max(2, lab.getWidth());
        int h = Math.max(2, lab.getHeight());

        press(lab, w - 1, 0);

        double[] marker = d.getLabMarkerAb();
        assertNotNull(marker);
        assertEquals(128.0, marker[0], 2.0, "top-right click: marker a must be ~+128");
        assertEquals(128.0, marker[1], 2.0, "top-right click: marker b must be ~+128");
    }

    @Test
    void clickOutOfGamutMarkerFollowsPointerNotClampedColor() {
        JIPipeDesktopColorChooserDialog d = dialogWithLabTab();
        JComponent lab = d.getLabWell();
        int w = Math.max(2, lab.getWidth());
        int h = Math.max(2, lab.getHeight());

        // Bottom-left: a ~ -128, b ~ -128 - far outside the sRGB gamut at this
        // L, so fromLab clamps the color while the marker must stay where the
        // user clicked.
        press(lab, 0, h - 1);

        double[] marker = d.getLabMarkerAb();
        assertNotNull(marker);
        assertEquals(-128.0, marker[0], 2.0, "bottom-left click: marker a must be ~-128");
        assertEquals(-128.0, marker[1], 2.0, "bottom-left click: marker b must be ~-128");

        // The picked color is gamut-clamped, so its a/b differ from the raw
        // pointer by more than the tolerance in at least one channel.
        double[] picked = org.hkijena.jipipe.utils.color.ColorSpaces.toLab(
                org.hkijena.jipipe.utils.color.ColorSpaces.fromHex(d.getHexText()));
        boolean differs = Math.abs(picked[1] - marker[0]) > 1.0
                || Math.abs(picked[2] - marker[1]) > 1.0;
        assertTrue(differs,
                "the marker must differ from the picked color's clamped a/b: marker=("
                        + marker[0] + ", " + marker[1] + ") picked=(" + picked[1] + ", " + picked[2] + ")");
    }
}
