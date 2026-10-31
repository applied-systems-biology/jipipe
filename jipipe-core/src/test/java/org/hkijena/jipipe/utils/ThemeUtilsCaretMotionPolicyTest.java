package org.hkijena.jipipe.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

// Headless test of the global reduced-motion caret policy: a single "focusOwner"
// listener on the keyboard focus manager re-applies the caret policy to whatever
// text component gains focus. A test focus manager exposes the protected
// setGlobalFocusOwner so the REAL property-change path can be driven headlessly
// (verified against the Azul Zulu 21 sources that ship in the JIPipe distribution).
class ThemeUtilsCaretMotionPolicyTest {

    private TestFocusManager focusManager;

    @BeforeEach
    void installTestFocusManager() {
        focusManager = new TestFocusManager();
        KeyboardFocusManager.setCurrentKeyboardFocusManager(focusManager);
    }

    @AfterEach
    void restoreDefaultFocusManager() {
        KeyboardFocusManager.setCurrentKeyboardFocusManager(null);
    }

    @Test
    void focusOwnerChangeReappliesCaretPolicyToFocusedField() {
        ThemeUtils.installCaretMotionPolicy();
        JTextField field = new JTextField();
        DefaultCaret caret = (DefaultCaret) field.getCaret();
        caret.setBlinkRate(0); // distinguishable starting state: suppressed

        focusManager.forceFocusOwner(field); // real "focusOwner" property change

        assertEquals(500, caret.getBlinkRate(),
                "the global focus-owner hook must re-apply the caret policy when a field gains focus");
    }

    @Test
    void focusOwnerChangeKeepsCaretSuppressedUnderReducedMotion() {
        ThemeUtils.installCaretMotionPolicy();
        JTextField field = new JTextField();
        DefaultCaret caret = (DefaultCaret) field.getCaret();
        assertEquals(500, caret.getBlinkRate());

        ThemeUtils.applyCaretMotion(field, true); // reduced motion on (policy path under test)
        focusManager.forceFocusOwner(field);

        assertEquals(0, caret.getBlinkRate(),
                "a suppressed caret must stay solid when its field gains focus");
    }

    @Test
    void nonTextFocusOwnerIsIgnored() {
        ThemeUtils.installCaretMotionPolicy();
        assertDoesNotThrow(() -> focusManager.forceFocusOwner(new JButton()));
        assertDoesNotThrow(() -> focusManager.forceFocusOwner(null));
    }

    @Test
    void installIsIdempotent() {
        ThemeUtils.installCaretMotionPolicy();
        int listenersAfterFirstInstall = focusManager.getPropertyChangeListeners("focusOwner").length;
        ThemeUtils.installCaretMotionPolicy();
        assertEquals(listenersAfterFirstInstall, focusManager.getPropertyChangeListeners("focusOwner").length,
                "installing the policy twice must not register a second listener");
    }

    /**
     * Exposes the protected setGlobalFocusOwner so tests can drive a real focus-owner change.
     */
    static class TestFocusManager extends DefaultKeyboardFocusManager {
        void forceFocusOwner(Component owner) {
            setGlobalFocusOwner(owner);
        }
    }
}
