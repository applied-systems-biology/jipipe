package org.hkijena.acaq5.extension.ui.parametereditors;

import org.hkijena.acaq5.api.ACAQAlgorithm;
import org.hkijena.acaq5.api.ACAQParameterAccess;
import org.hkijena.acaq5.ui.grapheditor.ACAQParameterEditorUI;

import javax.swing.*;
import java.awt.*;

/**
 * A parameter editor UI that works for all enumerations
 */
public class EnumParameterEditorUI extends ACAQParameterEditorUI {

    private ACAQParameterAccess.Instance<Enum<?>> enumAccess;

    public EnumParameterEditorUI(ACAQAlgorithm algorithm, ACAQParameterAccess parameterAccess) {
        super(algorithm, parameterAccess);
        enumAccess = parameterAccess.instantiate(algorithm);
        initialize();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        Object[] values = getParameterAccess().getFieldClass().getEnumConstants();
        JComboBox<Object> comboBox = new JComboBox<>(values);
        comboBox.setSelectedItem(enumAccess.get());
        comboBox.addActionListener(e -> enumAccess.set((Enum<?>) comboBox.getSelectedItem()));
        add(comboBox, BorderLayout.CENTER);
    }
}
