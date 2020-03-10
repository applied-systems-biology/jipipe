package org.hkijena.acaq5.extension.ui.parametereditors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQParameterEditorUI;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;

public class IntegerParameterEditorUI extends ACAQParameterEditorUI {

    public IntegerParameterEditorUI(ACAQWorkbenchUI workbenchUI, ACAQParameterAccess parameterAccess) {
        super(workbenchUI, parameterAccess);
        initialize();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        double min = Integer.MIN_VALUE;
        double max = Integer.MAX_VALUE;
        Integer initialValue = getParameterAccess().get();
        SpinnerNumberModel model = new SpinnerNumberModel(initialValue.doubleValue(), min, max, 1);
        JSpinner spinner = new JSpinner(model);
        spinner.addChangeListener(e -> {
            if (!getParameterAccess().set(model.getNumber().intValue())) {
                spinner.setValue(getParameterAccess().get());
            }
        });
        spinner.setPreferredSize(new Dimension(100, spinner.getPreferredSize().height));
        add(spinner, BorderLayout.CENTER);
    }
}
