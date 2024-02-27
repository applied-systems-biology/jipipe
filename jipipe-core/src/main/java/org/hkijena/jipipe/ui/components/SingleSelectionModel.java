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

package org.hkijena.jipipe.ui.components;

import javax.swing.*;

/**
 * Selection model for single selections
 */
public class SingleSelectionModel extends DefaultListSelectionModel {
    @Override
    public void setSelectionInterval(int startIndex, int endIndex) {
        if (startIndex == endIndex) {
            if (multipleItemsAreCurrentlySelected()) {
                clearSelection();
            }
            if (isSelectedIndex(startIndex)) {
                clearSelection();
            } else {
                super.setSelectionInterval(startIndex, endIndex);
            }
        }
        // User selected multiple items
        else {
            super.setSelectionInterval(startIndex, endIndex);
        }
    }

    private boolean multipleItemsAreCurrentlySelected() {
        return getMinSelectionIndex() != getMaxSelectionIndex();
    }
}
