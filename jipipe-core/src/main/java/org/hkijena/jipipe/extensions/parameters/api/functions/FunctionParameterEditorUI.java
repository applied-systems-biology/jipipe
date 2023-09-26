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

package org.hkijena.jipipe.extensions.parameters.api.functions;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;

import javax.swing.*;
import java.awt.*;

/**
 * Editor for {@link FunctionParameter}
 */
public class FunctionParameterEditorUI extends JIPipeParameterEditorUI {

    private final FormPanel formPanel;

    /**
     * Creates new instance
     *
     * @param workbench       workbench
     * @param parameterAccess Parameter
     */
    public FunctionParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterTree, parameterAccess);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEtchedBorder());
//        JToolBar toolBar = new JToolBar();
//        toolBar.setFloatable(false);
//        toolBar.add(new JLabel(getParameterAccess().getName()));
        formPanel = new FormPanel(null, FormPanel.NONE);
        add(formPanel, BorderLayout.CENTER);
        reload();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        formPanel.clear();

        FunctionParameter<?, ?, ?> functionParameter = getParameter(FunctionParameter.class);
        FunctionParameterInputAccess<Object, Object, Object> inputAccess = new FunctionParameterInputAccess<>(getParameterAccess());
        FunctionParameterParameterAccess<Object, Object, Object> parameterAccess = new FunctionParameterParameterAccess<>(getParameterAccess());
        FunctionParameterOutputAccess<Object, Object, Object> outputAccess = new FunctionParameterOutputAccess<>(getParameterAccess());

        formPanel.addToForm(JIPipe.getParameterTypes().createEditorFor(getWorkbench(), getParameterTree(), inputAccess), new JLabel(functionParameter.renderInputName()), null);
        formPanel.addToForm(JIPipe.getParameterTypes().createEditorFor(getWorkbench(), getParameterTree(), parameterAccess), new JLabel(functionParameter.renderParameterName()), null);
        formPanel.addToForm(JIPipe.getParameterTypes().createEditorFor(getWorkbench(), getParameterTree(), outputAccess), new JLabel(functionParameter.renderOutputName()), null);
    }
}
