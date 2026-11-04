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
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The empty-submit contract of {@link JIPipeDesktopDialogs#inputText}
 * (modern dialog API, #1341, spec §4.2): submitting the field with empty or
 * whitespace-only text counts as <em>dismiss</em> - the result is
 * {@code Optional.empty()}, never an empty string.
 * <p>
 * Headless-safe: a capturing {@link DialogPresenter} records the constructed
 * dialog instead of showing it, and the user's decision (OK click, Enter, or
 * dismissal) is simulated inside the presenter so that the facade's return
 * value is observed in the real order: decide, then return. AWT is required
 * to construct the dialog, so the suite skips in headless environments
 * (mirroring {@code DismissMappingTest}).
 */
class InputTextEmptySubmitTest {

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

    /**
     * Installs a deciding presenter that clicks the dialog's OK button
     * (found by label, not position) inside show() - the decision must happen
     * in the real production order: decide, then return.
     */
    private void clickOkOnShow() {
        JIPipeDesktopDialogs.setPresenter(dialog -> {
            captured = dialog;
            findButton(dialog, "OK").doClick();
            return dialog;
        });
    }

    @Test
    void emptySubmitIsDismissed() {
        // Field text "" → OK → empty().
        clickOkOnShow();
        Optional<String> empty = JIPipeDesktopDialogs.inputText(null, "t", "m", "");
        assertTrue(empty.isEmpty(), "submitting empty text must count as dismiss");

        // Field text "   " → OK → empty().
        Optional<String> blank = JIPipeDesktopDialogs.inputText(null, "t", "m", "   ");
        assertTrue(blank.isEmpty(), "submitting whitespace-only text must count as dismiss");

        // The same rule when the user clears a pre-filled field before submitting.
        JIPipeDesktopDialogs.setPresenter(dialog -> {
            captured = dialog;
            findTextField(dialog).setText("");
            findButton(dialog, "OK").doClick();
            return dialog;
        });
        Optional<String> cleared = JIPipeDesktopDialogs.inputText(null, "t", "m", "abc");
        assertTrue(cleared.isEmpty(), "submitting a cleared field must count as dismiss");

        // For contrast: dismissing (X / Escape / Cancel funnel into dismiss())
        // is also empty - the two "no value" cases are collapsed.
        JIPipeDesktopDialogs.setPresenter(dialog -> {
            captured = dialog;
            dialog.dismiss();
            return dialog;
        });
        Optional<String> dismissed = JIPipeDesktopDialogs.inputText(null, "t", "m", "abc");
        assertTrue(dismissed.isEmpty(), "a dismissed input dialog must return Optional.empty()");
    }

    @Test
    void valueSubmitIsPresent() {
        // Field text "abc" → OK → of("abc").
        clickOkOnShow();
        Optional<String> value = JIPipeDesktopDialogs.inputText(null, "t", "m", "abc");
        assertEquals(Optional.of("abc"), value, "submitting a value must return it");

        // The value is the field content at submit time (user edited the pre-fill).
        JIPipeDesktopDialogs.setPresenter(dialog -> {
            captured = dialog;
            findTextField(dialog).setText("edited");
            findButton(dialog, "OK").doClick();
            return dialog;
        });
        Optional<String> edited = JIPipeDesktopDialogs.inputText(null, "t", "m", "seed");
        assertEquals(Optional.of("edited"), edited, "the field content at submit time must be the value");

        // Enter inside the field submits the OK action (same wiring as the button).
        JIPipeDesktopDialogs.setPresenter(dialog -> {
            captured = dialog;
            findTextField(dialog).postActionEvent();
            return dialog;
        });
        Optional<String> entered = JIPipeDesktopDialogs.inputText(null, "t", "m", "typed");
        assertEquals(Optional.of("typed"), entered, "Enter inside the field must submit the OK action");
    }

    @Test
    void initialValueIsSet() {
        // No decision needed: the call presents the dialog (and, undecided,
        // maps to empty) - the captured dialog carries the pre-filled field.
        JIPipeDesktopDialogs.inputText(null, "t", "m", "seed");
        JTextField field = findTextField(captured);
        assertNotNull(field, "inputText must present a single-line text field");
        assertEquals("seed", field.getText(), "the initial value must pre-fill the field");

        // A null initial value pre-fills an empty field (spec §8 null-handling).
        JIPipeDesktopDialogs.inputText(null, "t", "m", null);
        assertEquals("", findTextField(captured).getText(), "a null initial value must pre-fill an empty field");
    }

    /**
     * Finds the first {@link JTextField} in the component tree, or null if
     * there is none. (Component class, not layout.)
     */
    private static JTextField findTextField(Component component) {
        if (component instanceof JTextField field) {
            return field;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JTextField found = findTextField(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Finds the {@link JButton} with the given label among the dialog's
     * action buttons, or fails. (Component class + content, not layout.)
     */
    private static JButton findButton(JIPipeDesktopDialog dialog, String label) {
        for (JButton button : dialog.getActionButtons()) {
            if (label.equals(button.getText())) {
                return button;
            }
        }
        throw new AssertionError("no '" + label + "' button in the dialog");
    }
}
