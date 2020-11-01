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

package org.hkijena.jipipe.ui.grapheditor.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;

/**
 * Renders a {@link JIPipeDataSlot}
 */
public class JIPipeDataSlotListCellRenderer extends JLabel implements ListCellRenderer<JIPipeDataSlot> {

    /**
     * Creates a new renderer
     */
    public JIPipeDataSlotListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends JIPipeDataSlot> list, JIPipeDataSlot slot, int index, boolean selected, boolean cellHasFocus) {
        if (list.getFont() != null) {
            setFont(list.getFont());
        }

        if (slot != null) {
            String type = slot.isInput() ? "Input:" : "Output:";
            setText(type + " " + slot.getName());
            setIcon(JIPipe.getDataTypes().getIconFor(slot.getAcceptedDataType()));
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
