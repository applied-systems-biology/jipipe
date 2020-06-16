package org.hkijena.acaq5.api.grouping.parameters;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.grouping.ACAQNodeGroupUI;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.Context;

import javax.swing.*;
import java.awt.BorderLayout;

/**
 * Editor for {@link NodeGroupContents}
 */
public class NodeGroupContentsParameterEditorUI extends ACAQParameterEditorUI {

    /**
     * Creates new instance
     *
     * @param workbench        workbench
     * @param parameterAccess Parameter
     */
    public NodeGroupContentsParameterEditorUI(ACAQWorkbench workbench, ACAQParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JButton editGraphButton = new JButton("Edit graph", UIUtils.getIconFromResources("edit.png"));
        editGraphButton.addActionListener(e -> editGraph());
        add(editGraphButton, BorderLayout.CENTER);
    }

    private void editGraph() {
        NodeGroupContents contents = getParameter(NodeGroupContents.class);
        SwingUtilities.invokeLater(() -> ACAQNodeGroupUI.openGroupNodeGraph(getWorkbench(), contents.getParent(), true));
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {

    }
}
