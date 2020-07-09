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

package org.hkijena.pipelinej.api.grouping.parameters;

import org.hkijena.pipelinej.api.parameters.ACAQParameterAccess;
import org.hkijena.pipelinej.ui.ACAQWorkbench;
import org.hkijena.pipelinej.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.pipelinej.utils.UIUtils;

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
        GraphNodeParametersUI panel = new GraphNodeParametersUI(getWorkbench(), parameters);
        JDialog editorDialog = new JDialog();
        editorDialog.setTitle("Edit parameters");
        editorDialog.setContentPane(panel);
        editorDialog.setModal(false);
        editorDialog.pack();
        editorDialog.setSize(480, 640);
        editorDialog.setLocationRelativeTo(getParent());
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
