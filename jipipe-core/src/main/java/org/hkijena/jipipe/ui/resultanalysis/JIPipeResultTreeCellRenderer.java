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

package org.hkijena.jipipe.ui.resultanalysis;

import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.*;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.ui.registries.JIPipeUINodeRegistry;
import org.hkijena.jipipe.ui.registries.JIPipeUIDatatypeRegistry;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

/**
 * Renders the tree in {@link JIPipeResultAlgorithmTree}
 */
public class JIPipeResultTreeCellRenderer extends JLabel implements TreeCellRenderer {
    private Icon compartmentIcon = UIUtils.getIconFromResources("data-types/graph-compartment.png");
    private Icon rootIcon = UIUtils.getIconFromResources("actions/run-build.png");

    /**
     * Creates new renderer
     */
    public JIPipeResultTreeCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

        if (value instanceof DefaultMutableTreeNode) {
            Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
            if (userObject instanceof JIPipeProjectCompartment) {
                JIPipeProjectCompartment compartment = (JIPipeProjectCompartment) userObject;
                setIcon(compartmentIcon);
                setText(compartment.getName());
            } else if (userObject instanceof JIPipeGraphNode) {
                JIPipeGraphNode algorithm = (JIPipeGraphNode) userObject;
                setIcon(JIPipeUINodeRegistry.getInstance().getIconFor(algorithm.getInfo()));
                setText(algorithm.getName());
            } else if (userObject instanceof JIPipeDataSlot) {
                JIPipeDataSlot slot = (JIPipeDataSlot) userObject;
                setIcon(JIPipeUIDatatypeRegistry.getInstance().getIconFor(slot.getAcceptedDataType()));
                setText(slot.getName());
            } else {
                setIcon(rootIcon);
                setText("Results");
            }
        } else {
            setIcon(null);
            setText("");
        }

        if (selected) {
            setBackground(new Color(184, 207, 229));
        } else {
            setBackground(new Color(255, 255, 255));
        }

        return this;
    }
}
