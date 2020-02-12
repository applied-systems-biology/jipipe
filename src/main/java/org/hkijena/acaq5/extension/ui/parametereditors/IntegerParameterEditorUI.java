package org.hkijena.acaq5.extension.ui.parametereditors;

import org.hkijena.acaq5.api.ACAQAlgorithm;
import org.hkijena.acaq5.api.ACAQParameterAccess;
import org.hkijena.acaq5.ui.components.FileSelection;
import org.hkijena.acaq5.ui.grapheditor.ACAQParameterEditorUI;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

public class IntegerParameterEditorUI extends ACAQParameterEditorUI {

    private ACAQParameterAccess.Instance<Integer> numberAccess;

    public IntegerParameterEditorUI(ACAQAlgorithm algorithm, ACAQParameterAccess parameterAccess) {
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
        double min = Integer.MIN_VALUE;
        double max = Integer.MAX_VALUE;
        SpinnerNumberModel model = new SpinnerNumberModel((double)numberAccess.get(), min, max, 1);
        JSpinner spinner = new JSpinner(model);
        spinner.addChangeListener(e -> numberAccess.set(model.getNumber().intValue()));
        spinner.setPreferredSize(new Dimension(100, spinner.getPreferredSize().height));
        add(spinner, BorderLayout.CENTER);
    }
}
