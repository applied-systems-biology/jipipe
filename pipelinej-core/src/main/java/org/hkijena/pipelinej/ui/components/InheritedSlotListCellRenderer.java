/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.pipelinej.ui.components;

import org.hkijena.pipelinej.api.algorithm.ACAQGraphNode;
import org.hkijena.pipelinej.api.data.ACAQDataSlot;
import org.hkijena.pipelinej.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.pipelinej.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Renders an available entry for inherited slot in {@link org.hkijena.pipelinej.api.data.ACAQSlotDefinition}
 */
public class InheritedSlotListCellRenderer extends JLabel implements ListCellRenderer<String> {

    private ACAQGraphNode algorithm;

    /**
     * @param algorithm the algorithm that contains the input slots
     */
    public InheritedSlotListCellRenderer(ACAQGraphNode algorithm) {
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
            ACAQDataSlot slotInstance = algorithm.getInputSlotMap().getOrDefault(value, null);
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
