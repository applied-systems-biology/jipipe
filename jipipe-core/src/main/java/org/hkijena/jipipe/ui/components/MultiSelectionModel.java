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
