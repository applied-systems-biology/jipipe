package org.hkijena.acaq5.extensions.standardparametereditors.ui.parametereditors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.ACAQProjectUI;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQParameterEditorUI;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;

public class BooleanParameterEditorUI extends ACAQParameterEditorUI {

    private JCheckBox checkBox;
    private boolean skipNextReload = false;
    private boolean isReloading = false;

    public BooleanParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
        super(context, parameterAccess);
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
        Object value = getParameterAccess().get();
        boolean booleanValue = false;
        if (value != null)
            booleanValue = (boolean) value;
        checkBox.setSelected(booleanValue);
        isReloading = false;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        Object value = getParameterAccess().get();
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
