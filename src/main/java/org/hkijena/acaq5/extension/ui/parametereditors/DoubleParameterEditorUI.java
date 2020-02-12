package org.hkijena.acaq5.extension.ui.parametereditors;

import org.hkijena.acaq5.api.ACAQAlgorithm;
import org.hkijena.acaq5.api.ACAQParameterAccess;
import org.hkijena.acaq5.ui.grapheditor.ACAQParameterEditorUI;

import javax.swing.*;
import java.awt.*;

public class DoubleParameterEditorUI extends ACAQParameterEditorUI {

    private ACAQParameterAccess.Instance<Double> numberAccess;

    public DoubleParameterEditorUI(ACAQAlgorithm algorithm, ACAQParameterAccess parameterAccess) {
        super(algorithm, parameterAccess);
        numberAccess = parameterAccess.instantiate(algorithm);
        initialize();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        double min = -Double.MAX_VALUE;
        double max = Double.MAX_VALUE;
        SpinnerNumberModel model = new SpinnerNumberModel((double)numberAccess.get(), min, max, 1);
        JSpinner spinner = new JSpinner(model);
        spinner.addChangeListener(e -> numberAccess.set(model.getNumber().doubleValue()));
        spinner.setPreferredSize(new Dimension(100, spinner.getPreferredSize().height));
        add(spinner, BorderLayout.CENTER);
    }
}
