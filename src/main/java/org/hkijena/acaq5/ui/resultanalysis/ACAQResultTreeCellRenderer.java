package org.hkijena.acaq5.ui.resultanalysis;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.utils.UIUtils;

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
            } else if (userObject instanceof ACAQAlgorithm) {
                ACAQAlgorithm algorithm = (ACAQAlgorithm) userObject;
                setIcon(UIUtils.getIconFromColor(UIUtils.getFillColorFor(algorithm.getDeclaration())));
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
