package org.hkijena.acaq5.extension.ui.parametereditors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQParameterEditorUI;

import javax.swing.*;
import java.awt.*;

public class FloatParameterEditorUI extends ACAQParameterEditorUI {

    public FloatParameterEditorUI(ACAQWorkbenchUI workbenchUI, ACAQParameterAccess parameterAccess) {
        super(workbenchUI, parameterAccess);
        initialize();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        float min = Float.NEGATIVE_INFINITY;
        float max = Float.POSITIVE_INFINITY;
        Object value = getParameterAccess().get();
        float floatValue = 0;
        if (value != null) {
            floatValue = (float) value;
        }
        SpinnerNumberModel model = new SpinnerNumberModel(floatValue, min, max, 1);
        JSpinner spinner = new JSpinner(model);
        spinner.addChangeListener(e -> {
            if (!getParameterAccess().set(model.getNumber().floatValue())) {
                spinner.setValue(getParameterAccess().get());
            }
        });
        spinner.setPreferredSize(new Dimension(100, spinner.getPreferredSize().height));
        add(spinner, BorderLayout.CENTER);
    }
}
