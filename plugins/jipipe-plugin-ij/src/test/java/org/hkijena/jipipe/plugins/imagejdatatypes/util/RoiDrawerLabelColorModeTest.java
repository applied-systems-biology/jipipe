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

package org.hkijena.jipipe.plugins.imagejdatatypes.util;

import ij.gui.Roi;
import ij.gui.ShapeRoi;
import org.hkijena.jipipe.plugins.parameters.library.colors.OptionalColorParameter;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test for the per-ROI label foreground resolved by {@link RoiDrawer}:
 * either the fixed {@code labelForeground} or the effective ROI line color
 * (task 7 of the font parameter family refactor, #1339).
 * <p>
 * The ROI line color change of {@link RoiDrawer.RoiLabelColorMode#FROM_ROI_LINE}
 * is what makes {@code RoiToRGB} label each ROI in its own color; a single
 * fixed color would fail the two-ROI case below.
 * <p>
 * ImageJ1 classes ({@code ij.gui.*}) are loaded here, which is safe because the
 * module registers {@code ImageJLegacyPatcherLauncherSessionListener} to run the
 * ImageJ2 legacy patcher before any test class is loaded.
 */
class RoiDrawerLabelColorModeTest {

    private static Roi rectRoi(double x, Color strokeColor) {
        ShapeRoi roi = new ShapeRoi(new Rectangle2D.Double(x, 0, 10, 10));
        roi.setStrokeColor(strokeColor);
        return roi;
    }

    @Test
    void fromRoiLineUsesTheRoisOwnLineColor() {
        RoiDrawer drawer = new RoiDrawer();
        drawer.setLabelColorMode(RoiDrawer.RoiLabelColorMode.FROM_ROI_LINE);

        Roi roiRed = rectRoi(0, Color.RED);
        Roi roiGreen = rectRoi(20, Color.GREEN);

        assertEquals(Color.RED, drawer.effectiveLabelForeground(roiRed));
        assertEquals(Color.GREEN, drawer.effectiveLabelForeground(roiGreen));
    }

    @Test
    void overrideLineWins() {
        RoiDrawer drawer = new RoiDrawer();
        drawer.setLabelColorMode(RoiDrawer.RoiLabelColorMode.FROM_ROI_LINE);
        drawer.setOverrideLineColor(new OptionalColorParameter(Color.BLUE, true));

        Roi roiRed = rectRoi(0, Color.RED);
        Roi roiGreen = rectRoi(20, Color.GREEN);

        assertEquals(Color.BLUE, drawer.effectiveLabelForeground(roiRed));
        assertEquals(Color.BLUE, drawer.effectiveLabelForeground(roiGreen));
    }

    @Test
    void fixedModeIgnoresRoi() {
        RoiDrawer drawer = new RoiDrawer();
        drawer.setLabelColorMode(RoiDrawer.RoiLabelColorMode.FIXED);
        drawer.setLabelForeground(Color.MAGENTA);

        Roi roiRed = rectRoi(0, Color.RED);
        Roi roiGreen = rectRoi(20, Color.GREEN);

        assertEquals(Color.MAGENTA, drawer.effectiveLabelForeground(roiRed));
        assertEquals(Color.MAGENTA, drawer.effectiveLabelForeground(roiGreen));
    }

    @Test
    void defaultModeIsFixed() {
        assertEquals(RoiDrawer.RoiLabelColorMode.FIXED, new RoiDrawer().getLabelColorMode());
    }
}
