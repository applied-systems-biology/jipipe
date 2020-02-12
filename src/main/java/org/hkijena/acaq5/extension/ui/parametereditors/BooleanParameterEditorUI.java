package org.hkijena.acaq5.extension.ui.parametereditors;

import org.hkijena.acaq5.api.ACAQAlgorithm;
import org.hkijena.acaq5.api.ACAQParameterAccess;
import org.hkijena.acaq5.ui.grapheditor.ACAQParameterEditorUI;

import javax.swing.*;
import java.awt.*;

public class BooleanParameterEditorUI extends ACAQParameterEditorUI {

    private ACAQParameterAccess.Instance<Boolean> booleanAccess;

    public BooleanParameterEditorUI(ACAQAlgorithm algorithm, ACAQParameterAccess parameterAccess) {
        super(algorithm, parameterAccess);
        booleanAccess = parameterAccess.instantiate(algorithm);
        initialize();
    }

    @Override
    public boolean isUILabelEnabled() {
        return false;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JCheckBox checkBox = new JCheckBox(getParameterAccess().getName(), booleanAccess.get());
        add(checkBox, BorderLayout.CENTER);
        checkBox.addActionListener(e -> booleanAccess.set(checkBox.isSelected()));
    }
}
