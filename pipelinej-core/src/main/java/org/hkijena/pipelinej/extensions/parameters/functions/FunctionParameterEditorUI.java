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

package org.hkijena.pipelinej.extensions.parameters.functions;

import org.hkijena.pipelinej.api.parameters.ACAQParameterAccess;
import org.hkijena.pipelinej.ui.ACAQWorkbench;
import org.hkijena.pipelinej.ui.components.FormPanel;
import org.hkijena.pipelinej.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.pipelinej.ui.registries.ACAQUIParameterTypeRegistry;

import javax.swing.*;
import java.awt.*;

/**
 * Editor for {@link FunctionParameter}
 */
public class FunctionParameterEditorUI extends ACAQParameterEditorUI {

    private FormPanel formPanel;

    /**
     * Creates new instance
     *
     * @param workbench       workbench
     * @param parameterAccess Parameter
     */
    public FunctionParameterEditorUI(ACAQWorkbench workbench, ACAQParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEtchedBorder());
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(new JLabel(getParameterAccess().getName()));
        formPanel = new FormPanel(null, FormPanel.NONE);
        add(formPanel, BorderLayout.CENTER);
        reload();
    }

    @Override
    public boolean isUILabelEnabled() {
        return false;
    }

    @Override
    public void reload() {
        formPanel.clear();

        FunctionParameter<?, ?, ?> functionParameter = getParameter(FunctionParameter.class);
        FunctionParameterInputAccess<Object, Object, Object> inputAccess = new FunctionParameterInputAccess<>(getParameterAccess());
        FunctionParameterParameterAccess<Object, Object, Object> parameterAccess = new FunctionParameterParameterAccess<>(getParameterAccess());
        FunctionParameterOutputAccess<Object, Object, Object> outputAccess = new FunctionParameterOutputAccess<>(getParameterAccess());

        formPanel.addToForm(ACAQUIParameterTypeRegistry.getInstance().createEditorFor(getWorkbench(), inputAccess), new JLabel(functionParameter.renderInputName()), null);
        formPanel.addToForm(ACAQUIParameterTypeRegistry.getInstance().createEditorFor(getWorkbench(), parameterAccess), new JLabel(functionParameter.renderParameterName()), null);
        formPanel.addToForm(ACAQUIParameterTypeRegistry.getInstance().createEditorFor(getWorkbench(), outputAccess), new JLabel(functionParameter.renderOutputName()), null);
    }
}
