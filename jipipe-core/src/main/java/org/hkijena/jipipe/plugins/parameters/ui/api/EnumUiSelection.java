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

package org.hkijena.jipipe.plugins.parameters.ui.api;

/**
 * Decides which UI an enum-like parameter editor renders for a given option
 * set (#1339).
 * <p>
 * Rule:
 * <ul>
 *   <li>editable dynamic enums keep the editable combo (free-text input) in
 *       all cases;</li>
 *   <li>option sets with {@link #COMBO_MAX_VALUES} or fewer values use a plain
 *       combo;</li>
 *   <li>larger sets use the rich selection box (searchable popup).</li>
 * </ul>
 */
public final class EnumUiSelection {

    /**
     * Option sets of at most this size use a plain combo box.
     */
    public static final int COMBO_MAX_VALUES = 5;

    private EnumUiSelection() {
    }

    /**
     * @param optionCount the number of currently available option values
     * @param dynamic     whether the options are dynamic (may change at runtime)
     * @param editable    whether the parameter allows free-text entry
     * @return the UI mode to render
     */
    public static EnumUiMode mode(int optionCount, boolean dynamic, boolean editable) {
        if (dynamic && editable) {
            return EnumUiMode.COMBO;
        }
        return optionCount <= COMBO_MAX_VALUES ? EnumUiMode.COMBO : EnumUiMode.BOX;
    }
}
