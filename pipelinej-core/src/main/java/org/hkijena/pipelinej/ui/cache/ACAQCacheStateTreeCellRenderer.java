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

package org.hkijena.pipelinej.ui.cache;

import org.hkijena.pipelinej.api.ACAQProjectCache;
import org.hkijena.pipelinej.api.algorithm.ACAQGraphNode;
import org.hkijena.pipelinej.api.data.ACAQDataSlot;
import org.hkijena.pipelinej.ui.registries.ACAQUIAlgorithmRegistry;
import org.hkijena.pipelinej.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.pipelinej.ui.resultanalysis.ACAQResultAlgorithmTree;
import org.hkijena.pipelinej.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

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
