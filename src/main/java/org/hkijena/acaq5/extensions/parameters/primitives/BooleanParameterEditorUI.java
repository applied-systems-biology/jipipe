package org.hkijena.acaq5.extensions.parameters.primitives;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.parameters.ACAQParameterEditorUI;

import javax.swing.*;
import java.awt.*;

/**
 * Parameter editor for boolean data
 */
public class BooleanParameterEditorUI extends ACAQParameterEditorUI {

    private JCheckBox checkBox;
    private boolean skipNextReload = false;
    private boolean isReloading = false;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public BooleanParameterEditorUI(ACAQWorkbench workbench, ACAQParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
    }

    @Override
    public boolean isUILabelEnabled() {
        return false;
    }

    @Override
    public void reload() {
        if (skipNextReload) {
            skipNextReload = false;
            return;
        }
        isReloading = true;
        Object value = getParameterAccess().get(Object.class);
        boolean booleanValue = false;
        if (value != null)
            booleanValue = (boolean) value;
        checkBox.setSelected(booleanValue);
        isReloading = false;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        Object value = getParameterAccess().get(Object.class);
        boolean booleanValue = false;
        if (value != null)
            booleanValue = (boolean) value;
        checkBox = new JCheckBox(getParameterAccess().getName());
        checkBox.setSelected(booleanValue);
        add(checkBox, BorderLayout.CENTER);
        checkBox.addActionListener(e -> {
            if (!isReloading) {
                skipNextReload = true;
                getParameterAccess().set(checkBox.isSelected());
            }
        });
    }
}
