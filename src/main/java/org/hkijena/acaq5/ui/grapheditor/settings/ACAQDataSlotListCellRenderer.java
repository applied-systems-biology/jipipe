package org.hkijena.acaq5.ui.grapheditor.settings;

import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;

/**
 * Renders a {@link ACAQDataSlot}
 */
public class ACAQDataSlotListCellRenderer extends JLabel implements ListCellRenderer<ACAQDataSlot> {

    /**
     * Creates a new renderer
     */
    public ACAQDataSlotListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends ACAQDataSlot> list, ACAQDataSlot slot, int index, boolean selected, boolean cellHasFocus) {
        if (list.getFont() != null) {
            setFont(list.getFont());
        }

        if (slot != null) {
            String type = slot.isInput() ? "Input:" : "Output:";
            setText(type + " " + slot.getName());
            setIcon(ACAQUIDatatypeRegistry.getInstance().getIconFor(slot.getAcceptedDataType()));
        } else {
            setText("<No data slot selected>");
            setIcon(null);
        }

        // Update status
        // Update status
        if (selected) {
            setBackground(new Color(184, 207, 229));
        } else {
            setBackground(new Color(255, 255, 255));
        }
        return this;
    }
}
