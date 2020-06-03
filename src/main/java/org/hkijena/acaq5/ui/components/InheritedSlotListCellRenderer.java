package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Renders an available entry for inherited slot in {@link org.hkijena.acaq5.api.data.ACAQSlotDefinition}
 */
public class InheritedSlotListCellRenderer extends JLabel implements ListCellRenderer<String> {

    private ACAQAlgorithm algorithm;

    /**
     * @param algorithm the algorithm that contains the input slots
     */
    public InheritedSlotListCellRenderer(ACAQAlgorithm algorithm) {
        this.algorithm = algorithm;
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }


    @Override
    public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {

        if (value == null || value.isEmpty()) {
            setIcon(UIUtils.getIconFromResources("remove.png"));
            setText("<No inheritance>");
        } else if ("*".equals(value)) {
            if (!algorithm.getInputSlotOrder().isEmpty()) {
                setIcon(ACAQUIDatatypeRegistry.getInstance().getIconFor(algorithm.getFirstInputSlot().getAcceptedDataType()));
            } else {
                setIcon(UIUtils.getIconFromResources("remove.png"));
            }
            setText("<First data slot>");
        } else {
            ACAQDataSlot slotInstance = algorithm.getSlots().getOrDefault(value, null);
            if (slotInstance != null && slotInstance.isInput()) {
                setIcon(ACAQUIDatatypeRegistry.getInstance().getIconFor(slotInstance.getAcceptedDataType()));
            }
            setText(value);
        }

        if (isSelected) {
            setBackground(new Color(184, 207, 229));
        } else {
            setBackground(new Color(255, 255, 255));
        }
        return this;
    }
}
