package org.hkijena.jipipe.plugins.parameters.library.graph;

import com.google.common.collect.Sets;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.commons.components.pickers.JIPipeDesktopPickNodeDialog;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.UUID;

public class GraphNodeReferenceParameterEditorUI extends JIPipeDesktopParameterEditorUI {

    private final JButton selectButton = new JButton();

    public GraphNodeReferenceParameterEditorUI(InitializationParameters initializationParameters) {
        super(initializationParameters);
        initialize();
        reload();
    }

    private void initialize() {
        selectButton.addActionListener(e -> selectNode());

        setLayout(new BorderLayout());
        add(selectButton, BorderLayout.CENTER);
        add(UIUtils.createButton("", UIUtils.getIconFromResources("actions/edit.png"), this::selectNode), BorderLayout.EAST);
    }

    private void selectNode() {
        JIPipeProject project = getWorkbench().getProject();
        if (project != null) {
            JIPipeGraphNode node = JIPipeDesktopPickNodeDialog.showDialog(getDesktopWorkbench().getWindow(),
                    Sets.union(project.getGraph().getGraphNodes(), project.getCompartments().values()),
                    null,
                    "Select node");
            if (node != null) {
                GraphNodeReferenceParameter parameter = new GraphNodeReferenceParameter();
                parameter.setNodeUUID(node.getUUIDInParentGraph().toString());
                setParameter(parameter, true);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Unable to find project", "Edit node reference", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        GraphNodeReferenceParameter parameter = getParameter(GraphNodeReferenceParameter.class);
        if (parameter != null && !StringUtils.isNullOrEmpty(parameter.getNodeUUID())) {
            JIPipeProject project = getWorkbench().getProject();
            if (project != null) {
                JIPipeGraphNode node = project.getGraph().getNodeByUUID(UUID.fromString(parameter.getNodeUUID()));
                if (node == null) {
                    // Might be a compartment?
                    node = project.getCompartments().get(UUID.fromString(parameter.getNodeUUID()));
                }
                if (node != null) {
                    selectButton.setEnabled(true);
                    selectButton.setText(node.getDisplayName());
                    selectButton.setIcon(JIPipe.getNodes().getIconFor(node.getInfo()));
                } else {
                    selectButton.setEnabled(true);
                    selectButton.setText("<Not found>");
                    selectButton.setIcon(UIUtils.getIconFromResources("actions/rectangle-xmark.png"));
                }
            } else {
                selectButton.setEnabled(false);
                selectButton.setText("<Unsupported>");
                selectButton.setIcon(UIUtils.getIconFromResources("actions/rectangle-xmark.png"));
            }
        } else {
            selectButton.setEnabled(true);
            selectButton.setText("None");
            selectButton.setIcon(UIUtils.getIconFromResources("actions/rectangle-xmark.png"));
        }
    }
}
