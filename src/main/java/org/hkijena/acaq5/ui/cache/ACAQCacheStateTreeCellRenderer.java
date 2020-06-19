package org.hkijena.acaq5.ui.cache;

import org.hkijena.acaq5.api.ACAQProjectCache;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.ui.registries.ACAQUIAlgorithmRegistry;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.ui.resultanalysis.ACAQResultAlgorithmTree;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.Color;
import java.awt.Component;

/**
 * Renders the tree in {@link ACAQResultAlgorithmTree}
 */
public class ACAQCacheStateTreeCellRenderer extends JLabel implements TreeCellRenderer {
    private Icon compartmentIcon = UIUtils.getIconFromResources("graph-compartment.png");
    private Icon rootIcon = UIUtils.getIconFromResources("database.png");

    /**
     * Creates new renderer
     */
    public ACAQCacheStateTreeCellRenderer() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

        if (value instanceof DefaultMutableTreeNode) {
            Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
            if (userObject instanceof String) {
                setIcon(compartmentIcon);
                setText("" + userObject);
            } else if (userObject instanceof ACAQGraphNode) {
                ACAQGraphNode algorithm = (ACAQGraphNode) userObject;
                setIcon(ACAQUIAlgorithmRegistry.getInstance().getIconFor(algorithm.getDeclaration()));
                setText(algorithm.getName());
            } else if (userObject instanceof ACAQDataSlot) {
                ACAQDataSlot slot = (ACAQDataSlot) userObject;
                setIcon(ACAQUIDatatypeRegistry.getInstance().getIconFor(slot.getAcceptedDataType()));
                setText(slot.getName());
            } else if (userObject instanceof ACAQProjectCache.State) {
                ACAQProjectCache.State state = (ACAQProjectCache.State) userObject;
                setIcon(UIUtils.getIconFromResources("camera.png"));
                setText(state.renderGenerationTime());
            } else {
                setIcon(rootIcon);
                setText("Cache");
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
