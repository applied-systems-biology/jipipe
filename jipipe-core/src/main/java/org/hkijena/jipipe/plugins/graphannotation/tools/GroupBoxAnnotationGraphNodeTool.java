/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.graphannotation.tools;

import org.hkijena.jipipe.api.nodes.annotation.JIPipeAnnotationGraphNodeTool;
import org.hkijena.jipipe.plugins.graphannotation.nodes.GroupBoxAnnotationGraphNode;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class GroupBoxAnnotationGraphNodeTool extends JIPipeAnnotationGraphNodeTool<GroupBoxAnnotationGraphNode> {
    public GroupBoxAnnotationGraphNodeTool() {
        super(GroupBoxAnnotationGraphNode.class);
    }

    @Override
    protected GroupBoxAnnotationGraphNode createAndConfigureNode(Point firstPoint, Point secondPoint) {
        GroupBoxAnnotationGraphNode node = super.createAndConfigureNode(firstPoint, secondPoint);

        String title = JOptionPane.showInputDialog(getDesktopWorkbench().getWindow(), "Please input the title:", "Create group box", JOptionPane.PLAIN_MESSAGE);
        if (!StringUtils.isNullOrEmpty(title)) {
            node.setTextTitle(title);
        }

        return node;
    }

    @Override
    public KeyStroke getKeyBinding() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0);
    }

    @Override
    public int getPriority() {
        return -4900;
    }
}
