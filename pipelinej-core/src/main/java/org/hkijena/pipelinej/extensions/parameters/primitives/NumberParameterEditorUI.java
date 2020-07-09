/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.pipelinej.extensions.parameters.primitives;

import org.hkijena.pipelinej.api.parameters.ACAQParameterAccess;
import org.hkijena.pipelinej.ui.ACAQWorkbench;
import org.hkijena.pipelinej.ui.parameters.ACAQParameterEditorUI;

import javax.swing.*;
import java.awt.*;

/**
 * Editor for a any numeric parameter
 */
public class NumberParameterEditorUI extends ACAQParameterEditorUI {
    private JSpinner spinner;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public NumberParameterEditorUI(ACAQWorkbench workbench, ACAQParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
        reload();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        spinner.setValue(getCurrentValue());
    }

    private Number getCurrentValue() {
        Object value = getParameterAccess().get(Object.class);
        if (getParameterAccess().getFieldClass() == byte.class || getParameterAccess().getFieldClass() == Byte.class) {
            return value != null ? (Byte) value : (byte) 0;
        } else if (getParameterAccess().getFieldClass() == short.class || getParameterAccess().getFieldClass() == Short.class) {
            return value != null ? (Short) value : (short) 0;
        } else if (getParameterAccess().getFieldClass() == int.class || getParameterAccess().getFieldClass() == Integer.class) {
            return value != null ? (Integer) value : 0;
        } else if (getParameterAccess().getFieldClass() == long.class || getParameterAccess().getFieldClass() == Long.class) {
            return value != null ? (Long) value : 0L;
        } else if (getParameterAccess().getFieldClass() == float.class || getParameterAccess().getFieldClass() == Float.class) {
            return value != null ? (Float) value : 0f;
        } else if (getParameterAccess().getFieldClass() == double.class || getParameterAccess().getFieldClass() == Double.class) {
            return value != null ? (Double) value : 0d;
        } else {
            throw new IllegalArgumentException("Unsupported numeric type: " + value);
        }
    }

    private Comparable<?> getMinimumValue() {
        NumberParameterSettings settings = getParameterAccess().getAnnotationOfType(NumberParameterSettings.class);
        if (settings != null) {
            return settings.min();
        }

        if (getParameterAccess().getFieldClass() == byte.class || getParameterAccess().getFieldClass() == Byte.class) {
            return Byte.MIN_VALUE;
        } else if (getParameterAccess().getFieldClass() == short.class || getParameterAccess().getFieldClass() == Short.class) {
            return Short.MIN_VALUE;
        } else if (getParameterAccess().getFieldClass() == int.class || getParameterAccess().getFieldClass() == Integer.class) {
            return Integer.MIN_VALUE;
        } else if (getParameterAccess().getFieldClass() == long.class || getParameterAccess().getFieldClass() == Long.class) {
            return Long.MIN_VALUE;
        } else if (getParameterAccess().getFieldClass() == float.class || getParameterAccess().getFieldClass() == Float.class) {
            return Float.NEGATIVE_INFINITY;
        } else if (getParameterAccess().getFieldClass() == double.class || getParameterAccess().getFieldClass() == Double.class) {
            return Double.NEGATIVE_INFINITY;
        } else {
            throw new IllegalArgumentException("Unsupported numeric type: " + getParameterAccess().getFieldClass());
        }
    }

    private Comparable<?> getMaximumValue() {
        NumberParameterSettings settings = getParameterAccess().getAnnotationOfType(NumberParameterSettings.class);
        if (settings != null) {
            return settings.max();
        }

        if (getParameterAccess().getFieldClass() == byte.class || getParameterAccess().getFieldClass() == Byte.class) {
            return Byte.MAX_VALUE;
        } else if (getParameterAccess().getFieldClass() == short.class || getParameterAccess().getFieldClass() == Short.class) {
            return Short.MAX_VALUE;
        } else if (getParameterAccess().getFieldClass() == int.class || getParameterAccess().getFieldClass() == Integer.class) {
            return Integer.MAX_VALUE;
        } else if (getParameterAccess().getFieldClass() == long.class || getParameterAccess().getFieldClass() == Long.class) {
            return Long.MAX_VALUE;
        } else if (getParameterAccess().getFieldClass() == float.class || getParameterAccess().getFieldClass() == Float.class) {
            return Float.POSITIVE_INFINITY;
        } else if (getParameterAccess().getFieldClass() == double.class || getParameterAccess().getFieldClass() == Double.class) {
            return Double.POSITIVE_INFINITY;
        } else {
            throw new IllegalArgumentException("Unsupported numeric type: " + getParameterAccess().getFieldClass());
        }
    }

    private double getStep() {
        NumberParameterSettings settings = getParameterAccess().getAnnotationOfType(NumberParameterSettings.class);
        if (settings != null) {
            return settings.step();
        }
        return 1;
    }

    private boolean setCurrentValue(Number number) {
        if (getParameterAccess().getFieldClass() == byte.class || getParameterAccess().getFieldClass() == Byte.class) {
            return setParameter(number.byteValue(), false);
        } else if (getParameterAccess().getFieldClass() == short.class || getParameterAccess().getFieldClass() == Short.class) {
            return setParameter(number.shortValue(), false);
        } else if (getParameterAccess().getFieldClass() == int.class || getParameterAccess().getFieldClass() == Integer.class) {
            return setParameter(number.intValue(), false);
        } else if (getParameterAccess().getFieldClass() == long.class || getParameterAccess().getFieldClass() == Long.class) {
            return setParameter(number.longValue(), false);
        } else if (getParameterAccess().getFieldClass() == float.class || getParameterAccess().getFieldClass() == Float.class) {
            return setParameter(number.floatValue(), false);
        } else if (getParameterAccess().getFieldClass() == double.class || getParameterAccess().getFieldClass() == Double.class) {
            return setParameter(number.doubleValue(), false);
        } else {
            throw new IllegalArgumentException("Unsupported numeric type: " + getParameterAccess().getFieldClass());
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());
        SpinnerNumberModel model = new SpinnerNumberModel(getCurrentValue(), getMinimumValue(), getMaximumValue(), getStep());
        spinner = new JSpinner(model);
        spinner.addChangeListener(e -> setCurrentValue(model.getNumber()));
        spinner.setPreferredSize(new Dimension(100, spinner.getPreferredSize().height));
        add(spinner, BorderLayout.CENTER);
    }
}
