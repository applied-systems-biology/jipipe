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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for the LAB well's marker-follow behavior (#1339, item 5).
 * <p>
 * The marker was previously derived from {@code toLab(currentColor)}, but the
 * picked color passes through {@code ColorSpaces.fromLab} which clamps
 * out-of-gamut a/b values to the sRGB gamut — so the circle "stuck" at the
 * clamped position instead of following the cursor. The fix records the raw
 * pointer a/b. This test presses an out-of-gamut corner of the a/b plane and
 * asserts the marker lands on the pressed (unclamped) coordinates, not on the
 * clamped color's coordinates.
 * <p>
 * AWT-backed (drives a synthetic mouse press with real geometry); skips when
 * headless, mirroring {@code ColorChooserDialogRefreshTest}.
 */
class ColorChooserDialogLabMarkerTest {

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

    @Test
    void labMarkerFollowsRawPointerEvenWhenColorIsGamutClamped() {
        JIPipeDesktopColorChooserDialog d =
                new JIPipeDesktopColorChooserDialog(null, "t", new Color(128, 128, 128), false);
        d.showLabTab();
        JComponent lab = d.getLabWell();
        assertNotNull(lab, "LAB well must exist after the LAB tab is built");

        int w = Math.max(2, lab.getWidth());
        int h = Math.max(2, lab.getHeight());
        // Press the bottom-left corner: a ~ -128, b ~ -128 — far outside the
        // sRGB gamut for any L, so fromLab clamps and the old marker would have
        // moved back toward the gamut center.
        int x = 0;
        int y = h - 1;
        lab.dispatchEvent(new MouseEvent(lab, MouseEvent.MOUSE_PRESSED, 0L, 0, x, y, 1, false));

        double[] marker = d.getLabMarkerAb();
        assertNotNull(marker, "marker coordinates must be available");
        double expectedA = (x / (double) w * 2 - 1) * 128;
        double expectedB = (1 - y / (double) h * 2 + 1) * 128;
        assertTrue(Math.abs(marker[0] - expectedA) < 0.5 && Math.abs(marker[1] - expectedB) < 0.5,
                "marker must sit at the raw pointer a/b (" + expectedA + ", " + expectedB
                        + ") but was (" + marker[0] + ", " + marker[1] + ")");

        // And it must NOT equal the (gamut-clamped) color's own a/b — that was
        // exactly the bug (marker pinned to the clamped position).
        double[] clamped = org.hkijena.jipipe.utils.color.ColorSpaces.toLab(new Color(128, 128, 128));
        Color picked = dialogColor(d);
        double[] pickedLab = org.hkijena.jipipe.utils.color.ColorSpaces.toLab(picked);
        boolean differsFromPicked = Math.abs(pickedLab[1] - marker[0]) > 1.0
                || Math.abs(pickedLab[2] - marker[1]) > 1.0;
        assertTrue(differsFromPicked,
                "the marker must differ from the picked color's clamped a/b (the old bug): marker=("
                        + marker[0] + ", " + marker[1] + ") pickedLab=(" + pickedLab[1] + ", " + pickedLab[2] + ")");
    }

    /** The dialog's current color via its hex field (package-private seam). */
    private static Color dialogColor(JIPipeDesktopColorChooserDialog d) {
        return org.hkijena.jipipe.utils.color.ColorSpaces.fromHex(d.getHexText());
    }
}