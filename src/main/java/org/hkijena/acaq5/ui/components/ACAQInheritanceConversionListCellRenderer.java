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

package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.api.data.ACAQDataDeclaration;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.utils.ResourceUtils;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;
import java.util.Map;

/**
 * Renders inheritance conversion entries
 */
public class ACAQInheritanceConversionListCellRenderer extends JLabel implements ListCellRenderer<Map.Entry<ACAQDataDeclaration, ACAQDataDeclaration>> {

    /**
     * Creates a new renderer
     */
    public ACAQInheritanceConversionListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Map.Entry<ACAQDataDeclaration, ACAQDataDeclaration>> list,
                                                  Map.Entry<ACAQDataDeclaration, ACAQDataDeclaration> value, int index, boolean isSelected, boolean cellHasFocus) {
        if (value == null) {
            setText("<Null>");
        } else {
            ACAQDataDeclaration from = value.getKey();
            ACAQDataDeclaration to = value.getValue();
            String stringBuilder = "<html>" +
                    "<table><tr>" +
                    "<td><img src=\"" + ACAQUIDatatypeRegistry.getInstance().getIconURLFor(from.getDataClass()) + "\" /></td>" +
                    "<td>" + from.getName() + "</td>" +
                    "<td><img src=\"" + ResourceUtils.getPluginResource("icons/chevron-right.png") + "\" /></td>" +
                    "<td><img src=\"" + ACAQUIDatatypeRegistry.getInstance().getIconURLFor(to.getDataClass()) + "\" /></td>" +
                    "<td>" + to.getName() + "</td>" +
                    "</tr></table>" +
                    "</html>";
            setText(stringBuilder);
        }

        if (isSelected) {
            setBackground(new Color(184, 207, 229));
        } else {
            setBackground(new Color(255, 255, 255));
        }

        return this;
    }
}
