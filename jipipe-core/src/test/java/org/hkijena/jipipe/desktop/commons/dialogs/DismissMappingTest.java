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

package org.hkijena.jipipe.desktop.commons.dialogs;

import org.hkijena.jipipe.JIPipe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JTextArea;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The dismiss-mapping contract of the {@link JIPipeDesktopDialogs} facade
 * (modern dialog API, #1341): dismissing a dialog (the single code path that
 * X, Escape, and the Cancel button all funnel into) must map to
 * {@code Optional.empty()} / {@code false} - never to a real choice.
 * <p>
 * Headless-safe: a capturing {@link DialogPresenter} records the constructed
 * dialog instead of showing it, and the user's decision is simulated inside
 * the presenter (via {@code dismiss()} or {@link JButton#doClick()}) so that
 * the facade's return value is observed in the real order: decide, then
 * return. AWT is required to construct the dialog, so the suite skips in
 * headless environments (mirroring {@code ColorChooserDialogRefreshTest}).
 */
class DismissMappingTest {

    /**
     * The dialog presented during the current test call, for assertions.
     */
    private JIPipeDesktopDialog captured;

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

    @BeforeEach
    void installCapturingPresenter() {
        // The default presenter: record the dialog and hand it back unchanged,
        // so the facade's return value reflects "no decision yet" unless a
        // test installs a deciding presenter.
        JIPipeDesktopDialogs.setPresenter(dialog -> {
            captured = dialog;
            return dialog;
        });
    }

    @AfterEach
    void restoreDefaultPresenter() {
        // Don't leak the test presenter into other test classes in the same JVM.
        JIPipeDesktopDialogs.setPresenter(new ModalPresenter());
        captured = null;
    }

    @Test
    void confirmDismissIsFalse() {
        // Dismissal (X / Escape / Cancel all funnel into dismiss()) → false.
        JIPipeDesktopDialogs.setPresenter(dialog -> {
            captured = dialog;
            dialog.dismiss();
            return dialog;
        });
        assertFalse(JIPipeDesktopDialogs.confirm(null, "t", "m"),
                "a dismissed confirm dialog must return false");
        assertTrue(captured.getResult().isEmpty(),
                "dismiss() must leave the dialog result empty");

        // Yes → true.
        JIPipeDesktopDialogs.setPresenter(dialog -> {
            captured = dialog;
            dialog.getActionButtons().get(0).doClick();
            return dialog;
        });
        assertTrue(JIPipeDesktopDialogs.confirm(null, "t", "m"),
                "clicking Yes must return true");
        assertTrue(captured.getResult().isPresent(), "the Yes click must set the result");
        assertEquals("Yes", captured.getResult().get().getLabel(),
                "the Yes click must record the Yes descriptor");
        assertTrue(captured.getResult().get().isDefault(),
                "confirm must flag YES as the default button");

        // No → false.
        JIPipeDesktopDialogs.setPresenter(dialog -> {
            captured = dialog;
            dialog.getActionButtons().get(1).doClick();
            return dialog;
        });
        assertFalse(JIPipeDesktopDialogs.confirm(null, "t", "m"),
                "clicking No must return false");
        assertTrue(captured.getResult().isPresent(), "the No click must set the result");
        assertEquals("No", captured.getResult().get().getLabel(),
                "the No click must record the No descriptor");
    }

    @Test
    void chooseDismissIsEmpty() {
        Button custom = Button.custom("Custom");

        // Dismissal → Optional.empty().
        JIPipeDesktopDialogs.setPresenter(dialog -> {
            captured = dialog;
            dialog.dismiss();
            return dialog;
        });
        Optional<Button> dismissed = JIPipeDesktopDialogs.choose(null, "t", "m", custom);
        assertTrue(dismissed.isEmpty(),
                "a dismissed choose dialog must return Optional.empty()");

        // Clicking an option → that exact instance.
        JIPipeDesktopDialogs.setPresenter(dialog -> {
            captured = dialog;
            dialog.getActionButtons().get(0).doClick();
            return dialog;
        });
        Optional<Button> chosen = JIPipeDesktopDialogs.choose(null, "t", "m", custom);
        assertTrue(chosen.isPresent(), "clicking an option must set the result");
        assertSame(custom, chosen.get(),
                "the result must be the exact Button instance the caller passed");
    }

    @Test
    void messageDoesNotBlockOrReturn() {
        // Smoke: with the capturing presenter (no decision, no show) the call
        // must simply complete and have presented an OK-only dialog.
        JIPipeDesktopDialogs.message(null, "t", "m");
        assertEquals(1, captured.getActionButtons().size(),
                "message must be an OK-only dialog");
    }

    @Test
    void messageAndErrorRegisterDefaultOkButton() {
        // Enter must acknowledge notify dialogs: the OK button handed to the
        // chrome is default-flagged, so the root pane registers it as the
        // default button (the old JOptionPane did this everywhere).
        JIPipeDesktopDialogs.message(null, "t", "m");
        JButton messageDefault = captured.getRootPane().getDefaultButton();
        assertNotNull(messageDefault, "message() must register a default button (Enter)");
        assertEquals("OK", messageDefault.getText(), "the default button must be OK");

        JIPipeDesktopDialogs.error(null, "t", "m", null);
        JButton errorDefault = captured.getRootPane().getDefaultButton();
        assertNotNull(errorDefault, "error() must register a default button (Enter)");
        assertEquals("OK", errorDefault.getText(), "the default button must be OK");
    }

    @Test
    void inputDialogRegistersDefaultOkButtonAndMatchesResultByIdentity() {
        // The input dialogs hand a default-flagged OK copy to the chrome; the
        // facade must compare the result against that same copy - asDefault()
        // returns a copy, so comparing against the Button.OK constant would
        // never match (and Enter would silently do nothing on ~170 dialogs).
        //
        // Undecided first (a decision disposes the dialog, which resets the
        // root pane's default button - the assertion below must observe the
        // presented dialog, not a disposed one).
        JIPipeDesktopDialogs.inputText(null, "t", "m", "abc");
        JButton inputDefault = captured.getRootPane().getDefaultButton();
        assertNotNull(inputDefault, "inputText() must register a default button (Enter)");
        assertEquals("OK", inputDefault.getText(), "the default button must be OK");

        // OK click: the submitted value is returned and the recorded result
        // is the default-flagged copy (never the Button.OK constant).
        JIPipeDesktopDialogs.setPresenter(dialog -> {
            captured = dialog;
            dialog.getActionButtons().get(0).doClick();
            return dialog;
        });
        Optional<String> value = JIPipeDesktopDialogs.inputText(null, "t", "m", "abc");
        assertEquals(Optional.of("abc"), value, "clicking OK must submit the field value");
        assertTrue(captured.getResult().orElseThrow().isDefault(),
                "the recorded result must be the default-flagged OK copy");

        // Cancel (the implicit last button) is not an OK choice - and, being
        // a different instance, can never be confused with the flagged copy.
        JIPipeDesktopDialogs.setPresenter(dialog -> {
            captured = dialog;
            dialog.getActionButtons().get(1).doClick();
            return dialog;
        });
        Optional<String> cancelled = JIPipeDesktopDialogs.inputText(null, "t", "m", "abc");
        assertTrue(cancelled.isEmpty(), "clicking Cancel must not submit");
    }

    @Test
    void errorWithCauseShowsTrace() {
        RuntimeException cause = new RuntimeException("boom");

        JIPipeDesktopDialogs.error(null, "t", "m", cause);
        JTextArea trace = findTextAreas(captured, cause.getClass().getName());
        assertNotNull(trace,
                "error with a cause must include a text area with the stack trace");
        assertTrue(trace.getText().contains(cause.getClass().getName()),
                "the trace area must contain the exception's class name");

        JIPipeDesktopDialogs.error(null, "t", "m", null);
        assertNull(findTextAreas(captured, "java."),
                "error without a cause must not include a stack-trace area");
    }

    /**
     * Finds the first {@link JTextArea} in the component tree whose text
     * contains the given marker, or null if there is none. (Component
     * class + content, not layout.)
     */
    private static JTextArea findTextAreas(Component component, String marker) {
        if (component instanceof JTextArea area) {
            if (area.getText().contains(marker)) {
                return area;
            }
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JTextArea found = findTextAreas(child, marker);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
