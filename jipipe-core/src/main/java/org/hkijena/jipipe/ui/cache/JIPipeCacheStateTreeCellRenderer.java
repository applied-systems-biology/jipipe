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

package org.hkijena.jipipe.ui.cache;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProjectCache;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeResultAlgorithmTree;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.Color;
import java.awt.Component;

/**
 * Renders the tree in {@link JIPipeResultAlgorithmTree}
 */
public class JIPipeCacheStateTreeCellRenderer extends JLabel implements TreeCellRenderer {
    private Icon compartmentIcon = UIUtils.getIconFromResources("data-types/graph-compartment.png");
    private Icon rootIcon = UIUtils.getIconFromResources("actions/database.png");

    /**
     * Creates new renderer
     */
    public JIPipeCacheStateTreeCellRenderer() {
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
            } else if (userObject instanceof JIPipeGraphNode) {
                JIPipeGraphNode algorithm = (JIPipeGraphNode) userObject;
                setIcon(JIPipe.getNodes().getIconFor(algorithm.getInfo()));
                setText(algorithm.getName());
            } else if (userObject instanceof JIPipeDataSlot) {
                JIPipeDataSlot slot = (JIPipeDataSlot) userObject;
                setIcon(JIPipe.getDataTypes().getIconFor(slot.getAcceptedDataType()));
                setText(slot.getName());
            } else if (userObject instanceof JIPipeProjectCache.State) {
                JIPipeProjectCache.State state = (JIPipeProjectCache.State) userObject;
                setIcon(UIUtils.getIconFromResources("actions/camera.png"));
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
