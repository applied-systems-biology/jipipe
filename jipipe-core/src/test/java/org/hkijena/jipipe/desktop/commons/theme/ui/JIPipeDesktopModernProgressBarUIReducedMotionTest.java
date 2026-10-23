package org.hkijena.jipipe.desktop.commons.theme.ui;

import org.junit.jupiter.api.Test;

import javax.swing.JProgressBar;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

// Headless regression guard: the app's custom ProgressBarUI (installed for all JProgressBars in
// the modern theme) must install and paint an indeterminate bar without throwing. We do NOT
// assert the hourglass pixels (Swing-drawing tests are out of scope per test policy) and we
// cannot force the reduced-motion branch (it needs a live JIPipe instance); the static-hourglass
// result is verified by the user by eye.
class JIPipeDesktopModernProgressBarUIReducedMotionTest {

    @Test
    void customUiInstallsAndPaintsIndeterminateBarHeadlessly() {
        JProgressBar bar = new JProgressBar();
        try {
            bar.setUI(new JIPipeDesktopModernProgressBarUI());
            assertInstanceOf(JIPipeDesktopModernProgressBarUI.class, bar.getUI());
            bar.setIndeterminate(true);
            bar.setSize(100, 8);
            BufferedImage img = new BufferedImage(100, 8, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            try {
                bar.paint(g); // routes through JIPipeDesktopModernProgressBarUI.paintIndeterminate
            } finally {
                g.dispose();
            }
            assertTrue(bar.isIndeterminate());
        } finally {
            bar.updateUI(); // restore the default UI
        }
    }
}
