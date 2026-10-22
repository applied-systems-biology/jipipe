package org.hkijena.jipipe.desktop.commons.components.icons;

import org.junit.jupiter.api.Test;

import javax.swing.JPanel;

import static org.junit.jupiter.api.Assertions.*;

// Headless: Timer start/stop and isRunning() are screen-independent. We force the flag
// via setReducedMotion and assert the animation decision, not the drawn pixels
// (the static hourglass is verified by the user by eye).
class SpinnerIconReducedMotionTest {

    @Test
    void reducedMotionSuppressesAnimation() {
        SpinnerIcon icon = new SpinnerIcon(new JPanel(), 16);
        icon.setReducedMotion(true);
        icon.start();
        assertFalse(icon.getTimer().isRunning(), "spinner must not animate under reduced motion");
        assertTrue(icon.isReducedMotion());
    }

    @Test
    void animationRunsWhenNotReduced() {
        SpinnerIcon icon = new SpinnerIcon(new JPanel(), 16);
        icon.setReducedMotion(false);
        icon.start();
        assertTrue(icon.getTimer().isRunning(), "spinner animates when reduced motion is off");
        icon.stop();
        assertFalse(icon.getTimer().isRunning());
    }

    @Test
    void stoppingWhileReducedKeepsItStatic() {
        SpinnerIcon icon = new SpinnerIcon(new JPanel(), 16);
        icon.setReducedMotion(true);
        icon.start();
        icon.stop();
        assertFalse(icon.getTimer().isRunning());
    }

    @Test
    void turningReducedMotionOffMidRunResumesAnimation() {
        // A run is in progress (start() already called) while reduced motion is on:
        // static. When the user disables reduced motion, the in-flight run must resume animating
        // immediately (no waiting for the run to finish).
        SpinnerIcon icon = new SpinnerIcon(new JPanel(), 16);
        icon.setReducedMotion(true);
        icon.start();               // run started
        assertFalse(icon.getTimer().isRunning());
        icon.setReducedMotion(false); // user turns the setting off
        assertTrue(icon.getTimer().isRunning(), "in-flight run must resume animating");
        icon.stop();                // run finished
        assertFalse(icon.getTimer().isRunning());
    }
}
