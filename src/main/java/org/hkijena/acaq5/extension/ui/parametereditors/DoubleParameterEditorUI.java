package org.hkijena.acaq5.extension.ui.parametereditors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQParameterEditorUI;

import javax.swing.*;
import java.awt.*;

public class DoubleParameterEditorUI extends ACAQParameterEditorUI {

    public DoubleParameterEditorUI(ACAQWorkbenchUI workbenchUI, ACAQParameterAccess parameterAccess) {
        super(workbenchUI, parameterAccess);
        initialize();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        double min = Double.NEGATIVE_INFINITY;
        double max = Double.POSITIVE_INFINITY;
        Object value = getParameterAccess().get();
        double doubleValue = 0;
        if (value != null) {
            doubleValue = (double) value;
        }
        SpinnerNumberModel model = new SpinnerNumberModel(doubleValue, min, max, 1);
        JSpinner spinner = new JSpinner(model);
        spinner.addChangeListener(e -> {
            if (!getParameterAccess().set(model.getNumber().doubleValue())) {
                spinner.setValue(getParameterAccess().get());
            }
        });
        spinner.setPreferredSize(new Dimension(100, spinner.getPreferredSize().height));
        add(spinner, BorderLayout.CENTER);
    }
}
