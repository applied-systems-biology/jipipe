package org.hkijena.jipipe.extensions.graphannotation.tools;

import org.hkijena.jipipe.api.nodes.JIPipeAnnotationGraphNodeTool;
import org.hkijena.jipipe.extensions.graphannotation.nodes.GroupBoxAnnotationGraphNode;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.jsoup.helper.StringUtil;

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

        String title = JOptionPane.showInputDialog(getWorkbench().getWindow(), "Please input the title:", "Create group box", JOptionPane.PLAIN_MESSAGE);
        if(!StringUtils.isNullOrEmpty(title)) {
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
