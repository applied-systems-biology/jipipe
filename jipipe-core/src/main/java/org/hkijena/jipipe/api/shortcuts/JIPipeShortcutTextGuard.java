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

import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Container;

/**
 * Decides whether a component currently holds a text input.
 * <p>
 * The shortcut dispatcher consults this guard before firing an action: an
 * action that is not {@link JIPipeShortcutAction#isTextInputSafe() text input
 * safe} must not be triggered while the focus owner is typing. The predicate
 * is true exactly when the focus owner is (or hosts, at most two levels
 * deep) an <em>editable</em> {@link JTextComponent}: plain text fields, text
 * areas, text panes, and the editor fields of spinners and combo boxes.
 * Spinner and combo editors are panels that wrap their text field one or two
 * levels down, so the guard looks one level past the immediate children.
 * Disabled or read-only components are not text inputs: typing is not
 * happening there, so shortcut bindings may still apply.
 * <p>
 * The two-level bound is a deliberate trade-off: it covers the standard
 * editor containers (spinner editors, combo editors, viewports) without
 * walking the whole tree, and it is deterministic for a given component
 * structure.
 * <p>
 * Pure predicate with no event handling: null-safe and headless-testable.
 */
public final class JIPipeShortcutTextGuard {
    /**
     * Maximum descent depth below the probed component when looking for an
     * editable text component. Two levels cover spinner/combo editors (the
     * editor panel one down, the field two down).
     */
    private static final int MAX_DESCENT = 2;

    private JIPipeShortcutTextGuard() {
    }

    /**
     * @param c the focus owner or any component to probe
     * @return true when {@code c} is (or hosts, up to {@value #MAX_DESCENT}
     * levels deep) an editable {@link JTextComponent}; false for
     * {@code null}, non-text components, and read-only text components
     */
    public static boolean isTextInput(Component c) {
        if (c == null) {
            return false;
        }
        return containsEditableText(c, MAX_DESCENT);
    }

    /**
     * Recursively checks the component and its descendants up to the given
     * depth for an editable {@link JTextComponent}.
     */
    private static boolean containsEditableText(Component c, int depth) {
        if (c == null || depth < 0) {
            return false;
        }
        if (isEditableTextComponent(c)) {
            return true;
        }
        if (c instanceof Container container) {
            for (Component child : container.getComponents()) {
                if (containsEditableText(child, depth - 1)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isEditableTextComponent(Component c) {
        return c instanceof JTextComponent t && t.isEditable();
    }
}
