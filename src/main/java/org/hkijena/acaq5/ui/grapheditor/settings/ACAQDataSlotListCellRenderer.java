package org.hkijena.acaq5.ui.grapheditor.settings;

import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.data.ACAQDataSlot;

import javax.swing.*;
import java.awt.*;

public class ACAQDataSlotListCellRenderer extends JLabel implements ListCellRenderer<ACAQDataSlot<?>> {

    public ACAQDataSlotListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends ACAQDataSlot<?>> list, ACAQDataSlot<?> slot, int index, boolean selected, boolean cellHasFocus) {
        if(list.getFont() != null) {
            setFont(list.getFont());
        }

        if(slot != null) {
            String type = slot.isInput() ? "Input:" : "Output:";
            setText(type + " " + slot.getName());
            setIcon(ACAQRegistryService.getInstance().getUIDatatypeRegistry().getIconFor(slot.getAcceptedDataType()));
        }
        else {
            setText("<No data slot selected>");
            setIcon(null);
        }

        // Update status
        // Update status
        if(selected) {
            setBackground(new Color(184, 207, 229));
        }
        else {
            setBackground(new Color(255,255,255));
        }
        return this;
    }
}
