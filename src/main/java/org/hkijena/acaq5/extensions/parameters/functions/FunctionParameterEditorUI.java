package org.hkijena.acaq5.extensions.parameters.functions;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.acaq5.ui.registries.ACAQUIParameterTypeRegistry;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;

/**
 * Editor for {@link org.hkijena.acaq5.extensions.parameters.functions.FunctionParameter}
 */
public class FunctionParameterEditorUI extends ACAQParameterEditorUI {

    private FormPanel formPanel;

    /**
     * Creates new instance
     *
     * @param context         SciJava context
     * @param parameterAccess Parameter
     */
    public FunctionParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
        super(context, parameterAccess);
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

        formPanel.addToForm(ACAQUIParameterTypeRegistry.getInstance().createEditorFor(getContext(), inputAccess), new JLabel(functionParameter.renderInputName()), null);
        formPanel.addToForm(ACAQUIParameterTypeRegistry.getInstance().createEditorFor(getContext(), parameterAccess), new JLabel(functionParameter.renderParameterName()), null);
        formPanel.addToForm(ACAQUIParameterTypeRegistry.getInstance().createEditorFor(getContext(), outputAccess), new JLabel(functionParameter.renderOutputName()), null);
    }
}
