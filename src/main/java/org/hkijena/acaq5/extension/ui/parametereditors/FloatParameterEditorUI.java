package org.hkijena.acaq5.extension.ui.parametereditors;

import org.hkijena.acaq5.api.ACAQAlgorithm;
import org.hkijena.acaq5.api.ACAQParameterAccess;
import org.hkijena.acaq5.ui.grapheditor.ACAQParameterEditorUI;

import javax.swing.*;
import java.awt.*;

public class FloatParameterEditorUI extends ACAQParameterEditorUI {

    public FloatParameterEditorUI(ACAQParameterAccess parameterAccess) {
        super(parameterAccess);
        initialize();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        float min = -Float.MAX_VALUE;
        float max = Float.MAX_VALUE;
        Float initialValue = getParameterAccess().get();
        SpinnerNumberModel model = new SpinnerNumberModel(initialValue.doubleValue(), min, max, 1);
        JSpinner spinner = new JSpinner(model);
        spinner.addChangeListener(e -> getParameterAccess().set(model.getNumber().floatValue()));
        spinner.setPreferredSize(new Dimension(100, spinner.getPreferredSize().height));
        add(spinner, BorderLayout.CENTER);
    }
}
