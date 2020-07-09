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

import org.hkijena.jipipe.api.data.JIPipeDataDeclaration;
import org.hkijena.jipipe.ui.registries.JIPipeUIDatatypeRegistry;

import javax.swing.*;
import java.awt.*;

/**
 * Renders {@link JIPipeDataDeclaration}
 */
public class JIPipeDataDeclarationListCellRenderer extends JLabel implements ListCellRenderer<JIPipeDataDeclaration> {

    /**
     * Creates a new renderer
     */
    public JIPipeDataDeclarationListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends JIPipeDataDeclaration> list, JIPipeDataDeclaration value, int index, boolean isSelected, boolean cellHasFocus) {
        if (list.getFont() != null) {
            setFont(list.getFont());
        }
        if (value != null) {
            setText(value.getName());
            setIcon(JIPipeUIDatatypeRegistry.getInstance().getIconFor(value.getDataClass()));
        } else {
            setText("<No data type>");
            setIcon(null);
        }

        if (isSelected) {
            setBackground(new Color(184, 207, 229));
        } else {
            setBackground(new Color(255, 255, 255));
        }
        return this;
    }
}
