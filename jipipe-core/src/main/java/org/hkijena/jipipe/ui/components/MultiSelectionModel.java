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
 * Selection model for multiple selections
 */
public class MultiSelectionModel extends DefaultListSelectionModel {
    @Override
    public void setSelectionInterval(int startIndex, int endIndex) {
        if (startIndex == endIndex) {
            if (isSelectedIndex(startIndex)) {
                removeSelectionInterval(startIndex, endIndex);
            } else {
                super.addSelectionInterval(startIndex, endIndex);
            }
        } else {
            super.addSelectionInterval(startIndex, endIndex);
        }
    }

    private boolean multipleItemsAreCurrentlySelected() {
        return getMinSelectionIndex() != getMaxSelectionIndex();
    }
}
