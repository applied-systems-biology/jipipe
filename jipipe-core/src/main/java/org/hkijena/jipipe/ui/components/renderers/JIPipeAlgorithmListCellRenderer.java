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

package org.hkijena.jipipe.ui.components.renderers;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;

import javax.swing.*;
import java.awt.Component;

/**
 * Renders {@link JIPipeGraphNode}
 */
public class JIPipeAlgorithmListCellRenderer extends JLabel implements ListCellRenderer<JIPipeGraphNode> {

    /**
     * Creates a new renderer
     */
    public JIPipeAlgorithmListCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends JIPipeGraphNode> list, JIPipeGraphNode value, int index, boolean isSelected, boolean cellHasFocus) {
        if (list.getFont() != null) {
            setFont(list.getFont());
        }
        if (value != null) {
            setText(value.getName());
            setIcon(JIPipe.getNodes().getIconFor(value.getInfo()));
        } else {
            setText("<No data type>");
            setIcon(null);
        }

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }
        return this;
    }
}
