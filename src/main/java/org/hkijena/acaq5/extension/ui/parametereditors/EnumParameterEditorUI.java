package org.hkijena.acaq5.extension.ui.parametereditors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQParameterEditorUI;

import javax.swing.*;
import java.awt.*;

/**
 * A parameter editor UI that works for all enumerations
 */
public class EnumParameterEditorUI extends ACAQParameterEditorUI {

    public EnumParameterEditorUI(ACAQParameterAccess parameterAccess) {
        super(parameterAccess);
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
        comboBox.setSelectedItem(getParameterAccess().get());
        comboBox.addActionListener(e -> {
            if(!getParameterAccess().set(comboBox.getSelectedItem())) {
                comboBox.setSelectedItem(getParameterAccess().get());
            }
        });
        add(comboBox, BorderLayout.CENTER);
    }
}
