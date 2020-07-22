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
