package org.hkijena.acaq5.extensions.standardparametereditors.ui.parametereditors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.ACAQProjectUI;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQParameterEditorUI;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;

public class DoubleParameterEditorUI extends ACAQParameterEditorUI {
    private JSpinner spinner;
    private boolean skipNextReload = false;
    private boolean isReloading = false;

    public DoubleParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
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
        Object value = getParameterAccess().get();
        double doubleValue = 0;
        if (value != null) {
            doubleValue = (double) value;
        }
        spinner.setValue(doubleValue);
        isReloading = false;
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
        spinner = new JSpinner(model);
        spinner.addChangeListener(e -> {
            if (!isReloading) {
                skipNextReload = true;
                if (!getParameterAccess().set(model.getNumber().doubleValue())) {
                    skipNextReload = false;
                    reload();
                }
            }
        });
        spinner.setPreferredSize(new Dimension(100, spinner.getPreferredSize().height));
        add(spinner, BorderLayout.CENTER);
    }
}
