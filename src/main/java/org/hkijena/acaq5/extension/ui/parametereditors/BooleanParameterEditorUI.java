package org.hkijena.acaq5.extension.ui.parametereditors;

import org.hkijena.acaq5.api.ACAQAlgorithm;
import org.hkijena.acaq5.api.ACAQParameterAccess;
import org.hkijena.acaq5.ui.grapheditor.ACAQParameterEditorUI;

import javax.swing.*;
import java.awt.*;

public class BooleanParameterEditorUI extends ACAQParameterEditorUI {

    public BooleanParameterEditorUI(ACAQParameterAccess parameterAccess) {
        super(parameterAccess);
        initialize();
    }

    @Override
    public boolean isUILabelEnabled() {
        return false;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JCheckBox checkBox = new JCheckBox(getParameterAccess().getName());
        checkBox.setSelected(getParameterAccess().get());
        add(checkBox, BorderLayout.CENTER);
        checkBox.addActionListener(e -> getParameterAccess().set(checkBox.isSelected()));
    }
}
