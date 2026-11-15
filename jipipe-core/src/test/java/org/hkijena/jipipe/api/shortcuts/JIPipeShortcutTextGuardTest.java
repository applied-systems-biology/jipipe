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

package org.hkijena.jipipe.api.shortcuts;

import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerListModel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link JIPipeShortcutTextGuard#isTextInput(java.awt.Component)}.
 * <p>
 * Headless-safe: plain Swing component construction is enough, and none of the
 * assertions depend on layout or painting.
 */
class JIPipeShortcutTextGuardTest {
    @Test void textFieldIsTextInput() {
        JTextField f = new JTextField();
        f.setEditable(true);
        assertTrue(JIPipeShortcutTextGuard.isTextInput(f));
    }

    @Test void disabledTextFieldIsNot() {
        JTextField f = new JTextField();
        f.setEditable(false);
        assertFalse(JIPipeShortcutTextGuard.isTextInput(f));
    }

    @Test void labelIsNot() {
        assertFalse(JIPipeShortcutTextGuard.isTextInput(new JLabel("x")));
    }

    @Test void nullIsNot() {
        assertFalse(JIPipeShortcutTextGuard.isTextInput(null));
    }

    @Test void spinnerWithTextFieldEditorIsTextInput() {
        // A JSpinner with a string model hosts an editable field inside its
        // editor panel (two levels down): the guard must find it, even when
        // only the spinner itself is passed.
        JSpinner spinner = new JSpinner(new SpinnerListModel(List.of("abc")));
        assertTrue(JIPipeShortcutTextGuard.isTextInput(spinner));
    }

    @Test void spinnerWithReadOnlyTextFieldEditorIsNot() {
        JSpinner spinner = new JSpinner(new SpinnerListModel(List.of("abc")));
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
        editor.getTextField().setEditable(false);
        assertFalse(JIPipeShortcutTextGuard.isTextInput(spinner));
    }
}
