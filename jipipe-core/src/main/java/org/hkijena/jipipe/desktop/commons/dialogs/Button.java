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

import javax.swing.Icon;

/**
 * Immutable descriptor for a dialog button in the modern dialog API (#1341).
 * <p>
 * Predefined constants {@link #OK}, {@link #CANCEL}, {@link #YES} and {@link #NO}
 * carry JIPipe's standard 16px action icons. Use {@link #custom(String)} or
 * {@link #custom(String, Icon)} for arbitrary labels. {@link #asDefault()}
 * returns a copy flagged as the default button, which dialogs render as the
 * primary/trigger action.
 * <p>
 * Instances use identity equality: two descriptors with the same label are
 * distinct values, and flags/identity are never shared between copies.
 */
public final class Button {

    /**
     * Standard "OK" action button.
     */
    public static final Button OK = new Button("OK", JIPipe.RESOURCES.getIcon16("actions/ok.png"), false);
    /**
     * Standard "Cancel" action button.
     */
    public static final Button CANCEL = new Button("Cancel", JIPipe.RESOURCES.getIcon16("actions/cancel.png"), false);
    /**
     * Standard "Yes" action button.
     */
    public static final Button YES = new Button("Yes", JIPipe.RESOURCES.getIcon16("actions/checkmark.png"), false);
    /**
     * Standard "No" action button.
     */
    public static final Button NO = new Button("No", JIPipe.RESOURCES.getIcon16("actions/message-close.png"), false);

    private final String label;
    private final Icon icon;
    private final boolean isDefault;

    private Button(String label, Icon icon, boolean isDefault) {
        this.label = label;
        this.icon = icon;
        this.isDefault = isDefault;
    }

    /**
     * Creates a custom button with the given label and no icon.
     *
     * @param label the button label
     * @return a new, non-default button instance
     */
    public static Button custom(String label) {
        return new Button(label, null, false);
    }

    /**
     * Creates a custom button with the given label and icon.
     *
     * @param label the button label
     * @param icon  the button icon, or null
     * @return a new, non-default button instance
     */
    public static Button custom(String label, Icon icon) {
        return new Button(label, icon, false);
    }

    /**
     * Returns a copy of this button flagged as the default (primary) button.
     * This button is not modified.
     *
     * @return a new instance with the same label and icon and {@code isDefault() == true}
     */
    public Button asDefault() {
        return new Button(label, icon, true);
    }

    /**
     * @return the button label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @return the button icon, or null if this button has no icon
     */
    public Icon getIcon() {
        return icon;
    }

    /**
     * @return whether this button is the default (primary) button of its dialog
     */
    public boolean isDefault() {
        return isDefault;
    }

    @Override
    public String toString() {
        // the combo-box rendering of choose()'s list variant uses this
        return label;
    }
}
