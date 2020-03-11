package org.hkijena.acaq5.extension.ui.parametereditors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQParameterEditorUI;

import javax.swing.*;
import java.awt.BorderLayout;

public class BooleanParameterEditorUI extends ACAQParameterEditorUI {

    public BooleanParameterEditorUI(ACAQWorkbenchUI workbenchUI, ACAQParameterAccess parameterAccess) {
        super(workbenchUI, parameterAccess);
        initialize();
    }

    @Override
    public boolean isUILabelEnabled() {
        return false;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        Object value = getParameterAccess().get();
        boolean booleanValue = false;
        if (value != null)
            booleanValue = (boolean) value;
        JCheckBox checkBox = new JCheckBox(getParameterAccess().getName());
        checkBox.setSelected(booleanValue);
        add(checkBox, BorderLayout.CENTER);
        checkBox.addActionListener(e -> getParameterAccess().set(checkBox.isSelected()));
    }
}
