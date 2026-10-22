package org.hkijena.jipipe.utils;

import org.fife.ui.rtextarea.ConfigurableCaret;
import org.junit.jupiter.api.Test;

import javax.swing.JTextPane;
import javax.swing.text.DefaultCaret;

import static org.junit.jupiter.api.Assertions.*;

// Headless: a CustomEditorPane (RSyntaxTextArea subclass) has a ConfigurableCaret, and a plain
// JTextPane has a BasicCaret. We pin the blink mechanism on both, and the no-op guard.
class ThemeUtilsCaretMotionTest {

    @Test
    void reducedMotionDisablesBlinkingOnRsyntaxCaret() {
        CustomEditorPane pane = new CustomEditorPane();
        DefaultCaret caret = (DefaultCaret) pane.getCaret();
        caret.setBlinkRate(500); // known starting state
        ThemeUtils.applyCaretMotion(pane, true);
        assertEquals(0, caret.getBlinkRate(), "blink rate must be 0 under reduced motion");
        assertTrue(((ConfigurableCaret) pane.getCaret()).isAlwaysVisible(),
                "caret must stay visible (no flicker) under reduced motion");
    }

    @Test
    void clearingReducedMotionRestoresBlinking() {
        CustomEditorPane pane = new CustomEditorPane();
        DefaultCaret caret = (DefaultCaret) pane.getCaret();
        ThemeUtils.applyCaretMotion(pane, true);
        ThemeUtils.applyCaretMotion(pane, false);
        assertNotEquals(0, caret.getBlinkRate(), "blink rate restored when reduced motion is off");
        assertFalse(((ConfigurableCaret) pane.getCaret()).isAlwaysVisible());
    }

    @Test
    void reducedMotionDisablesBlinkingOnPlainBasicCaret() {
        // Plain JTextPane uses a BasicCaret (a DefaultCaret subclass) — the HTML editor and
        // read-only panes rely on the same path.
        JTextPane pane = new JTextPane();
        DefaultCaret caret = (DefaultCaret) pane.getCaret();
        ThemeUtils.applyCaretMotion(pane, true);
        assertEquals(0, caret.getBlinkRate(), "plain BasicCaret must not blink under reduced motion");
    }
}
