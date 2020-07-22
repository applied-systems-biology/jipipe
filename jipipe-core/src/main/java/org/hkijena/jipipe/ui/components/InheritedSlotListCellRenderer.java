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

package org.hkijena.jipipe.ui.components;

import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.ui.registries.JIPipeUIDatatypeRegistry;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Renders an available entry for inherited slot in {@link JIPipeDataSlotInfo}
 */
public class InheritedSlotListCellRenderer extends JLabel implements ListCellRenderer<String> {

    private JIPipeGraphNode algorithm;

    /**
     * @param algorithm the algorithm that contains the input slots
     */
    public InheritedSlotListCellRenderer(JIPipeGraphNode algorithm) {
        this.algorithm = algorithm;
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }


    @Override
    public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {

        if (value == null || value.isEmpty()) {
            setIcon(UIUtils.getIconFromResources("actions/cancel.png"));
            setText("<No inheritance>");
        } else if ("*".equals(value)) {
            if (!algorithm.getInputSlotOrder().isEmpty()) {
                setIcon(JIPipeUIDatatypeRegistry.getInstance().getIconFor(algorithm.getFirstInputSlot().getAcceptedDataType()));
            } else {
                setIcon(UIUtils.getIconFromResources("actions/cancel.png"));
            }
            setText("<First data slot>");
        } else {
            JIPipeDataSlot slotInstance = algorithm.getInputSlotMap().getOrDefault(value, null);
            if (slotInstance != null && slotInstance.isInput()) {
                setIcon(JIPipeUIDatatypeRegistry.getInstance().getIconFor(slotInstance.getAcceptedDataType()));
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
