package org.hkijena.acaq5.extensions.standardparametereditors.ui.parametereditors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQParameterEditorUI;
import org.scijava.Context;

import javax.swing.*;
import java.awt.*;

public class FloatParameterEditorUI extends ACAQParameterEditorUI {

    private JSpinner spinner;
    private boolean skipNextReload = false;
    private boolean isReloading = false;

    public FloatParameterEditorUI(Context context, ACAQParameterAccess parameterAccess) {
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
        float floatValue = 0;
        if (value != null) {
            floatValue = (float) value;
        }
        spinner.setValue(floatValue);
        isReloading = false;
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
        spinner = new JSpinner(model);
        spinner.addChangeListener(e -> {
            if (!isReloading) {
                skipNextReload = true;
                if (!getParameterAccess().set(model.getNumber().floatValue())) {
                    skipNextReload = false;
                    reload();
                }
            }
        });
        spinner.setPreferredSize(new Dimension(100, spinner.getPreferredSize().height));
        add(spinner, BorderLayout.CENTER);
    }
}
