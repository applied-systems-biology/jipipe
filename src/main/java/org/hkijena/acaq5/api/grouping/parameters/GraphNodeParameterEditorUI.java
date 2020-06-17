package org.hkijena.acaq5.api.grouping.parameters;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Editor for {@link GraphNodeParameters}
 */
public class GraphNodeParameterEditorUI extends ACAQParameterEditorUI {

    /**
     * Creates new instance
     *
     * @param workbench       the workbench
     * @param parameterAccess Parameter
     */
    public GraphNodeParameterEditorUI(ACAQWorkbench workbench, ACAQParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JButton editGraphButton = new JButton("Edit parameters", UIUtils.getIconFromResources("edit.png"));
        editGraphButton.addActionListener(e -> editParameters());
        add(editGraphButton, BorderLayout.CENTER);
    }

    private void editParameters() {
        GraphNodeParameters parameters = getParameter(GraphNodeParameters.class);
        GraphNodeParametersEditorPanel panel = new GraphNodeParametersEditorPanel(getWorkbench(), parameters);
        JDialog editorDialog = new JDialog();
        editorDialog.setTitle("Edit parameters");
        editorDialog.setContentPane(panel);
        editorDialog.setModal(false);
        editorDialog.setSize(480, 640);
        editorDialog.setLocationRelativeTo(this);
        editorDialog.pack();
        editorDialog.setVisible(true);
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {

    }
}
