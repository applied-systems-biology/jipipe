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

package org.hkijena.pipelinej.ui.resultanalysis;

import org.hkijena.pipelinej.api.algorithm.ACAQGraphNode;
import org.hkijena.pipelinej.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.pipelinej.api.data.ACAQDataSlot;
import org.hkijena.pipelinej.ui.registries.ACAQUIAlgorithmRegistry;
import org.hkijena.pipelinej.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.pipelinej.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

/**
 * Renders the tree in {@link ACAQResultAlgorithmTree}
 */
public class ACAQResultTreeCellRenderer extends JLabel implements TreeCellRenderer {
    private Icon compartmentIcon = UIUtils.getIconFromResources("graph-compartment.png");
    private Icon rootIcon = UIUtils.getIconFromResources("run.png");

    /**
     * Creates new renderer
     */
    public ACAQResultTreeCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

        if (value instanceof DefaultMutableTreeNode) {
            Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
            if (userObject instanceof ACAQProjectCompartment) {
                ACAQProjectCompartment compartment = (ACAQProjectCompartment) userObject;
                setIcon(compartmentIcon);
                setText(compartment.getName());
            } else if (userObject instanceof ACAQGraphNode) {
                ACAQGraphNode algorithm = (ACAQGraphNode) userObject;
                setIcon(ACAQUIAlgorithmRegistry.getInstance().getIconFor(algorithm.getDeclaration()));
                setText(algorithm.getName());
            } else if (userObject instanceof ACAQDataSlot) {
                ACAQDataSlot slot = (ACAQDataSlot) userObject;
                setIcon(ACAQUIDatatypeRegistry.getInstance().getIconFor(slot.getAcceptedDataType()));
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
