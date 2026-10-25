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

package org.hkijena.jipipe.plugins.parameters.ui;

import org.hkijena.jipipe.plugins.parameters.ui.api.EnumUiMode;
import org.hkijena.jipipe.plugins.parameters.ui.api.EnumUiSelection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The enum editor UI selection rule (#1339): editable dynamic enums always keep
 * the editable combo; small option sets (<= 5) use a plain combo; larger sets
 * use the rich selection box.
 */
class EnumUiSelectionTest {

    @Test
    void editableDynamicAlwaysCombo() {
        assertEquals(EnumUiMode.COMBO, EnumUiSelection.mode(50, true, true));
        assertEquals(EnumUiMode.COMBO, EnumUiSelection.mode(1, true, true));
    }

    @Test
    void fiveOrFewerCombo() {
        assertEquals(EnumUiMode.COMBO, EnumUiSelection.mode(1, false, false));
        assertEquals(EnumUiMode.COMBO, EnumUiSelection.mode(5, false, false));
        assertEquals(EnumUiMode.COMBO, EnumUiSelection.mode(5, true, false));
        assertEquals(EnumUiMode.COMBO, EnumUiSelection.mode(0, true, false));
    }

    @Test
    void moreThanFiveBox() {
        assertEquals(EnumUiMode.BOX, EnumUiSelection.mode(6, false, false));
        assertEquals(EnumUiMode.BOX, EnumUiSelection.mode(42, true, false));
    }
}
