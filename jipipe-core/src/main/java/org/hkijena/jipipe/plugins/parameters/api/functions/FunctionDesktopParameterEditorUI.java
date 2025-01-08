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

package org.hkijena.jipipe.plugins.parameters.api.functions;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Editor for {@link FunctionParameter}
 */
public class FunctionDesktopParameterEditorUI extends JIPipeDesktopParameterEditorUI {

    private final JIPipeDesktopFormPanel formPanel;

    public FunctionDesktopParameterEditorUI(InitializationParameters parameters) {
       super(parameters);
        setLayout(new BorderLayout());
        setBorder(UIUtils.createControlBorder());
//        JToolBar toolBar = new JToolBar();
//        toolBar.setFloatable(false);
//        toolBar.add(new JLabel(getParameterAccess().getName()));
        formPanel = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.NONE);
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

        formPanel.addToForm(JIPipe.getParameterTypes().createEditorInstance(inputAccess, getDesktopWorkbench(), getParameterTree(), null), new JLabel(functionParameter.renderInputName()), null);
        formPanel.addToForm(JIPipe.getParameterTypes().createEditorInstance(parameterAccess, getDesktopWorkbench(), getParameterTree(), null), new JLabel(functionParameter.renderParameterName()), null);
        formPanel.addToForm(JIPipe.getParameterTypes().createEditorInstance(outputAccess, getDesktopWorkbench(), getParameterTree(), null), new JLabel(functionParameter.renderOutputName()), null);
    }
}
