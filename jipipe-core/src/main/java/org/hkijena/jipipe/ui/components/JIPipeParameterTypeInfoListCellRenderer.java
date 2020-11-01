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

import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;

public class JIPipeParameterTypeInfoListCellRenderer extends JLabel implements ListCellRenderer<JIPipeParameterTypeInfo> {

    public JIPipeParameterTypeInfoListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        setIcon(UIUtils.getIconFromResources("data-types/parameters.png"));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends JIPipeParameterTypeInfo> list, JIPipeParameterTypeInfo value, int index, boolean isSelected, boolean cellHasFocus) {
        if (value != null) {
            setText(value.getName());
        } else {
            setText("<Null>");
        }

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }
        return this;
    }
}
