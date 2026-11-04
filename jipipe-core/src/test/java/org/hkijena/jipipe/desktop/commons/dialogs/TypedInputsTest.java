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
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The typed input contracts of the {@link JIPipeDesktopDialogs} facade
 * (modern dialog API, #1341): the spinner carries the requested bounds and
 * step, a null initial value starts at {@code min}, reversed bounds fail fast
 * before any dialog is built, and OK reads the spinner's/area's current
 * value.
 * <p>
 * Headless-safe: a capturing {@link DialogPresenter} records the constructed
 * dialog instead of showing it, and the user's decision (OK click) is
 * simulated inside the presenter so that the facade's return value is
 * observed in the real order: decide, then return. AWT is required to
 * construct the dialog, so the suite skips in headless environments
 * (mirroring {@code DismissMappingTest}).
 */
class TypedInputsTest {

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
    void integerBoundsEnforced() {
        // The spinner must carry the requested bounds (min 0, max 10, step 1)
        // and start at the given initial value 5; OK returns that value.
        clickOkOnShow();
        Optional<Integer> value = JIPipeDesktopDialogs.inputInteger(null, "t", "m", 5, 0, 10);
        JSpinner spinner = findSpinner(captured);
        assertNotNull(spinner, "inputInteger must present a spinner");
        assertSpinnerModel(spinner, 0, 10, 1);
        assertEquals(5, spinner.getValue(), "the initial value must be the spinner's value");
        assertEquals(Optional.of(5), value, "clicking OK must return the spinner's value");

        // OK reads the model's value at submit time (the test moves the spinner to 7).
        JIPipeDesktopDialogs.setPresenter(dialog -> {
            captured = dialog;
            findSpinner(dialog).setValue(7);
            findButton(dialog, "OK").doClick();
            return dialog;
        });
        Optional<Integer> moved = JIPipeDesktopDialogs.inputInteger(null, "t", "m", 5, 0, 10);
        assertEquals(Optional.of(7), moved, "OK must read the spinner's current value");
    }

    @Test
    void integerNullInitialStartsAtMin() {
        // No decision needed: inspect the captured dialog's spinner.
        JIPipeDesktopDialogs.inputInteger(null, "t", "m", null, 0, 10);
        JSpinner spinner = findSpinner(captured);
        assertNotNull(spinner, "inputInteger must present a spinner");
        assertEquals(0, spinner.getValue(), "a null initial value must start the spinner at min");
    }

    @Test
    void integerMinMaxReversedThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> JIPipeDesktopDialogs.inputInteger(null, "t", "m", 5, 10, 0),
                "min > max must fail fast");
        assertNull(captured, "the exception must be thrown before any dialog is built");

        // The same fail-fast rule for inputDouble.
        assertThrows(IllegalArgumentException.class,
                () -> JIPipeDesktopDialogs.inputDouble(null, "t", "m", 5.0, 10.0, 0.0),
                "min > max must fail fast");
        assertNull(captured, "the exception must be thrown before any dialog is built (inputDouble)");
    }

    @Test
    void doubleValueRoundTrips() {
        // A null initial value starts at min, in a model with the requested
        // bounds and step 1.
        JIPipeDesktopDialogs.inputDouble(null, "t", "m", null, 0, 10);
        JSpinner spinner = findSpinner(captured);
        assertNotNull(spinner, "inputDouble must present a spinner");
        assertSpinnerModel(spinner, 0.0, 10.0, 1.0);
        assertEquals(0.0, spinner.getValue(), "a null initial value must start the spinner at min");

        // The user sets the spinner to 3.5 → OK → of(3.5).
        JIPipeDesktopDialogs.setPresenter(dialog -> {
            captured = dialog;
            findSpinner(dialog).setValue(3.5);
            findButton(dialog, "OK").doClick();
            return dialog;
        });
        Optional<Double> value = JIPipeDesktopDialogs.inputDouble(null, "t", "m", null, 0, 10);
        assertEquals(Optional.of(3.5), value, "OK must read the spinner's value");
    }

    @Test
    void multilineSubmitsText() {
        // The user types multiple lines → OK → the full text is returned.
        JIPipeDesktopDialogs.setPresenter(dialog -> {
            captured = dialog;
            findTextArea(dialog).setText("line1\nline2");
            findButton(dialog, "OK").doClick();
            return dialog;
        });
        Optional<String> value = JIPipeDesktopDialogs.inputMultiLine(null, "t", "m", null);
        assertEquals(Optional.of("line1\nline2"), value, "OK must return the area's text");

        // Same empty-submit rule as inputText: whitespace-only → dismiss.
        JIPipeDesktopDialogs.setPresenter(dialog -> {
            captured = dialog;
            findTextArea(dialog).setText("   ");
            findButton(dialog, "OK").doClick();
            return dialog;
        });
        Optional<String> blank = JIPipeDesktopDialogs.inputMultiLine(null, "t", "m", null);
        assertTrue(blank.isEmpty(), "submitting whitespace-only text must count as dismiss");
    }

    /**
     * Asserts that the spinner's model is a {@link SpinnerNumberModel} with
     * the requested bounds and step.
     */
    private static void assertSpinnerModel(JSpinner spinner, Number min, Number max, Number step) {
        assertTrue(spinner.getModel() instanceof SpinnerNumberModel,
                "the spinner must use a SpinnerNumberModel");
        SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();
        assertEquals(min, model.getMinimum(), "the model's minimum must match the requested bound");
        assertEquals(max, model.getMaximum(), "the model's maximum must match the requested bound");
        assertEquals(step, model.getStepSize(), "the model's step size must match the requested step");
    }

    /**
     * Finds the first {@link JSpinner} in the component tree, or null if
     * there is none. (Component class, not layout.)
     */
    private static JSpinner findSpinner(Component component) {
        if (component instanceof JSpinner spinner) {
            return spinner;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JSpinner found = findSpinner(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Finds the first {@link JTextArea} in the component tree, or null if
     * there is none. (Component class, not layout.)
     */
    private static JTextArea findTextArea(Component component) {
        if (component instanceof JTextArea area) {
            return area;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JTextArea found = findTextArea(child);
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
