package org.hkijena.acaq5.extension.ui.parametereditors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQParameterEditorUI;

import javax.swing.*;
import java.awt.*;

public class IntegerParameterEditorUI extends ACAQParameterEditorUI {

    private JSpinner spinner;
    private boolean skipNextReload = false;
    private boolean isReloading = false;

    public IntegerParameterEditorUI(ACAQWorkbenchUI workbenchUI, ACAQParameterAccess parameterAccess) {
        super(workbenchUI, parameterAccess);
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
        Object value = getParameterAccess().get();
        int intValue = 0;
        if (value != null) {
            intValue = (int) value;
        }
        spinner.setValue(intValue);
        isReloading = false;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        int min = Integer.MIN_VALUE;
        int max = Integer.MAX_VALUE;
        Object value = getParameterAccess().get();
        int intValue = 0;
        if (value != null) {
            intValue = (int) value;
        }
        SpinnerNumberModel model = new SpinnerNumberModel(intValue, min, max, 1);
        spinner = new JSpinner(model);
        spinner.addChangeListener(e -> {
            if (!isReloading) {
                skipNextReload = true;
                if (!getParameterAccess().set(model.getNumber().intValue())) {
                    skipNextReload = false;
                    reload();
                }
            }
        });
        spinner.setPreferredSize(new Dimension(100, spinner.getPreferredSize().height));
        add(spinner, BorderLayout.CENTER);
    }
}
