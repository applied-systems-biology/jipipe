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

package org.hkijena.jipipe.ui.extensions;

import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.JIPipeJsonExtension;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.Component;

/**
 * Renders an {@link JIPipeDependency}
 */
public class JIPipeDependencyListCellRenderer extends JLabel implements ListCellRenderer<JIPipeDependency> {

    /**
     * Creates a new renderer
     */
    public JIPipeDependencyListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends JIPipeDependency> list, JIPipeDependency value, int index, boolean isSelected, boolean cellHasFocus) {

        if (value == null) {
            setText("<Null>");
            setIcon(null);
        } else {
            setText("<html><strong>" + value.getMetadata().getName() + "</strong><br/>Version " + value.getDependencyVersion() + "</html>");
            if (value instanceof JIPipeJsonExtension)
                setIcon(UIUtils.getIcon32FromResources("module-json.png"));
            else
                setIcon(UIUtils.getIcon32FromResources("module-java.png"));
        }

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }

        return this;
    }
}
