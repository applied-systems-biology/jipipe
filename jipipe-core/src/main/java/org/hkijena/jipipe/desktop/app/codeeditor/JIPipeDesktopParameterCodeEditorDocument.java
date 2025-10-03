package org.hkijena.jipipe.desktop.app.codeeditor;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.plugins.parameters.api.scripts.JIPipeScriptParameter;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopParameterCodeEditorDocument implements JIPipeDesktopCodeEditorDocument {
    private final JIPipeParameterAccess parameterAccess;

    public JIPipeDesktopParameterCodeEditorDocument(JIPipeParameterAccess parameterAccess) {
        this.parameterAccess = parameterAccess;
    }

    public JIPipeParameterAccess getParameterAccess() {
        return parameterAccess;
    }

    public JIPipeGraphNode getGraphNode() {
        if (parameterAccess.getSource() instanceof JIPipeGraphNode) {
            return (JIPipeGraphNode) parameterAccess.getSource();
        }
        return null;
    }

    @Override
    public void createActionsMenu(JIPipeDesktopCodeEditorUI editorUI, JPopupMenu popupMenu) {
        if (parameterAccess.getSource() instanceof JIPipeGraphNode node) {
            popupMenu.add(UIUtils.createMenuItem("Go to node",
                    "Selects the node that with the currently edited parameter",
                    JIPipe.RESOURCES.getIcon16("actions/go-jump.png"),
                    () -> {
                        Container container = SwingUtilities.getAncestorOfClass(JIPipeDesktopGraphEditorUI.class, editorUI);
                        if(container instanceof JIPipeDesktopGraphEditorUI graphEditorUI) {
                            JIPipeDesktopGraphNodeUI nodeUI = graphEditorUI.getCanvasUI().getNodeUIs().get(node);
                            if(nodeUI != null) {
                                graphEditorUI.getSelectionManager().selectOnly(nodeUI);
                            }
                        }
                    }));
        }
    }

    @Override
    public String getTitle() {
        JIPipeGraphNode graphNode = getGraphNode();
        if (graphNode != null) {
            return graphNode.getDisplayName() + "/" + StringUtils.orElse(getParameterAccess().getName(), getParameterAccess().getKey());
        }
        return StringUtils.orElse(getParameterAccess().getName(), getParameterAccess().getKey());
    }

    @Override
    public Icon getIcon() {
        JIPipeGraphNode graphNode = getGraphNode();
        if (graphNode != null) {
            return graphNode.getInfo().getIcon();
        }
        return JIPipe.RESOURCES.getIcon16("data-types/parameters.png");
    }

    @Override
    public JIPipeScriptParameter pull() {
        return parameterAccess.get(JIPipeScriptParameter.class);
    }

    @Override
    public void push(JIPipeScriptParameter param) {
        parameterAccess.set(param);
    }
}
