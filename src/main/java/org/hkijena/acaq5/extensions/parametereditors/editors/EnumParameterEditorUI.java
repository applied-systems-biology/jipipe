package org.hkijena.acaq5.extensions.parametereditors.editors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;

/**
 * A parameter editor UI that works for all enumerations
 */
public class EnumParameterEditorUI extends ACAQParameterEditorUI {

    private boolean skipNextReload = false;
    private boolean isReloading = false;
    private JComboBox<Object> comboBox;

    /**
     * @param context         SciJava context
     * @param parameterAccess the parameter
     */
    public EnumParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
        super(context, parameterAccess);
        initialize();
        reload();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        if (skipNextReload) {
            skipNextReload = false;
            return;
        }
        isReloading = true;
        comboBox.setSelectedItem(getParameterAccess().get());
        isReloading = false;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        Object[] values = getParameterAccess().getFieldClass().getEnumConstants();
        comboBox = new JComboBox<>(values);
        comboBox.setSelectedItem(getParameterAccess().get());
        comboBox.addActionListener(e -> {
            if (!isReloading) {
                skipNextReload = true;
                if (!getParameterAccess().set(comboBox.getSelectedItem())) {
                    skipNextReload = false;
                    reload();
                }
            }
        });
        add(comboBox, BorderLayout.CENTER);
    }
}
