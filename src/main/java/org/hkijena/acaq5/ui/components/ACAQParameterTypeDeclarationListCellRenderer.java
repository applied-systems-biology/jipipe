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

import org.hkijena.acaq5.api.parameters.ACAQParameterTypeDeclaration;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;

public class ACAQParameterTypeDeclarationListCellRenderer extends JLabel implements ListCellRenderer<ACAQParameterTypeDeclaration> {

    public ACAQParameterTypeDeclarationListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        setIcon(UIUtils.getIconFromResources("parameters.png"));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends ACAQParameterTypeDeclaration> list, ACAQParameterTypeDeclaration value, int index, boolean isSelected, boolean cellHasFocus) {
        if(value != null) {
            setText(value.getName());
        }
        else {
            setText("<Null>");
        }

        if (isSelected) {
            setBackground(new Color(184, 207, 229));
        } else {
            setBackground(new Color(255, 255, 255));
        }
        return this;
    }
}
