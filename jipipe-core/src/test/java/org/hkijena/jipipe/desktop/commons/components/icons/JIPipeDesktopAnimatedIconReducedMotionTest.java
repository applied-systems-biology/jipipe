package org.hkijena.jipipe.desktop.commons.components.icons;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

// Headless. Two 1x1 ImageIcons stand in for the real orange/blue badges. We assert the
// static-state contract (opacity pinned to full, timer never running) — the actual
// orange glyph is verified by the user by eye.
class JIPipeDesktopAnimatedIconReducedMotionTest {

    private static ImageIcon pixel() {
        return new ImageIcon(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));
    }

    @Test
    void reducedMotionShowsStaticTopIconAndNoTimer() {
        JIPipeDesktopAnimatedIcon icon = new JIPipeDesktopAnimatedIcon(new JPanel(), pixel(), pixel(), 100, 0.05);
        icon.setReducedMotion(true);
        icon.start();
        assertFalse(icon.getTimer().isRunning(), "badge must not animate under reduced motion");
        assertEquals(1.0, icon.getOpacity(), 1e-9, "top (orange) icon must be fully opaque when static");
        assertTrue(icon.isReducedMotion());
    }

    @Test
    void animationRunsWhenNotReduced() {
        JIPipeDesktopAnimatedIcon icon = new JIPipeDesktopAnimatedIcon(new JPanel(), pixel(), pixel(), 100, 0.05);
        icon.setReducedMotion(false);
        icon.start();
        assertTrue(icon.getTimer().isRunning());
        icon.stop();
        assertFalse(icon.getTimer().isRunning());
    }

    @Test
    void turningReducedMotionOffWhilePresentResumesCrossfade() {
        // A notification is present (start() called) while reduced motion is on: static orange.
        // When the user disables reduced motion, the cross-fade resumes immediately.
        JIPipeDesktopAnimatedIcon icon = new JIPipeDesktopAnimatedIcon(new JPanel(), pixel(), pixel(), 100, 0.05);
        icon.setReducedMotion(true);
        icon.start();
        assertFalse(icon.getTimer().isRunning());
        assertEquals(1.0, icon.getOpacity(), 1e-9);
        icon.setReducedMotion(false);
        assertTrue(icon.getTimer().isRunning(), "cross-fade must resume when reduced motion is off");
        icon.stop();
        assertFalse(icon.getTimer().isRunning());
    }
}
