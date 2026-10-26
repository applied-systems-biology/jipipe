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
 * Regression test for the color dialog's input plumbing (#1339 pass 2, S5
 * item 4). The dialog's {@code setColor()} previously set its re-entrancy
 * guard before calling {@code refresh()}, whose first line returned early when
 * the guard was already set — so rail/well input moved the handle but never
 * updated the hex field, the preview swatch, or the other wells. These tests
 * drive a synthetic press on a rail and on a well and assert the hex field
 * changes (i.e. {@code refresh()} ran to completion). They are AWT-backed, so
 * they skip when headless (mirroring {@code TemplateManagementDialogTest}).
 */
class ColorChooserDialogRefreshTest {

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

    private static void press(JComponent c, int x, int y) {
        c.dispatchEvent(new MouseEvent(c, MouseEvent.MOUSE_PRESSED, 0L, 0, x, y, 1, false));
    }

    @Test
    void railPressUpdatesHexField() {
        JIPipeDesktopColorChooserDialog d = new JIPipeDesktopColorChooserDialog(null, "t", new Color(0, 0, 0), false);
        String before = d.getHexText();
        JComponent rail = d.getRedRail();
        press(rail, Math.max(1, rail.getWidth() / 2), 5);   // press mid-rail -> Red ~ 127
        assertTrue(!before.equals(d.getHexText()),
                "hex field must change after a rail press (refresh() must run): before=" + before + " after=" + d.getHexText());
    }

    @Test
    void wellPressUpdatesHexField() {
        JIPipeDesktopColorChooserDialog d = new JIPipeDesktopColorChooserDialog(null, "t", new Color(0, 0, 0), false);
        d.showLabTab();  // build the HSB/LAB wells
        String before = d.getHexText();
        JComponent well = d.getHsbWell();
        assertNotNull(well, "HSB well must exist after the HSB tab is built");
        press(well, Math.max(1, well.getWidth() / 2), Math.max(1, well.getHeight() / 2));
        assertTrue(!before.equals(d.getHexText()),
                "hex field must change after a well press (refresh() must run): before=" + before + " after=" + d.getHexText());
    }
}
