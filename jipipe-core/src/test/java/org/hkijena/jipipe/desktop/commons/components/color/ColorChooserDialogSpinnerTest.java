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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for the color dialog's numeric channel editing (#1339):
 * each channel rail carries a Krita-style {@code JSpinner} next to it, and
 * typing a value there drives the color (and, via {@code refresh()}, every
 * other control). AWT-backed; skips when headless, mirroring
 * {@code ColorChooserDialogRefreshTest}.
 */
class ColorChooserDialogSpinnerTest {

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
    void redSpinnerDrivesColor() {
        JIPipeDesktopColorChooserDialog d = new JIPipeDesktopColorChooserDialog(null, "t", new Color(0, 0, 0), false);
        JSpinner red = (JSpinner) d.getRedSpinner();
        red.setValue(200);
        assertEquals(new Color(200, 0, 0), d.getCurrentColor(),
                "setting the Red spinner must set the color's red channel");
    }

    @Test
    void labBSpinnerDrivesColorAndHex() {
        JIPipeDesktopColorChooserDialog d = new JIPipeDesktopColorChooserDialog(null, "t", new Color(0, 0, 0), false);
        d.showLabTab();
        JSpinner b = (JSpinner) d.getLabBSpinner();
        b.setValue(50);
        // refresh() must have run to completion (the hex field follows).
        assertTrue(d.getHexText().startsWith("#"),
                "hex field must be updated after a spinner commit, was: " + d.getHexText());
        double[] lab = org.hkijena.jipipe.utils.color.ColorSpaces.toLab(d.getCurrentColor());
        // b=50 is far from the black start (b~0); the commit must have taken effect.
        assertTrue(Math.abs(lab[2]) > 5,
                "the b* channel must reflect the spinner value, was: " + lab[2]);
    }
}
