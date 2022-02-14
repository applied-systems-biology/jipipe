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

package org.hkijena.jipipe.extensions.parameters.library.primitives;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.parameters.DefaultJIPipeParameterGenerator;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Generator that creates instances of {@link Number}.
 * This cannot be used directly in {@link org.hkijena.jipipe.api.registries.JIPipeParameterTypeRegistry}, as the constructor does
 * not match. You have to inherit from this type and define the number type.
 */
public class NumberRangeParameterGenerator extends DefaultJIPipeParameterGenerator {

    private double minNumber = 0;
    private double maxNumber = 10;
    private double stepSize = 1;

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        if (minNumber <= maxNumber) {
            if (stepSize <= 0) {
                report.reportIsInvalid("Invalid step size!",
                        "The step size cannot be zero or negative.",
                        "Please ensure that the step size is greater than zero.",
                        this);
            }
        } else {
            if (stepSize >= 0) {
                report.reportIsInvalid("Invalid step size!",
                        "The step size cannot be zero or negative.",
                        "Please ensure that the step size is greater than zero.",
                        this);
            }
        }
    }

    @Override
    public <T> List<T> generateAfterDialog(JIPipeWorkbench workbench, Component parent, Class<T> klass) {
        List<T> result = new ArrayList<>();
        boolean outsideRange = false;
        double rangeMin = Double.NEGATIVE_INFINITY;
        double rangeMax = Double.POSITIVE_INFINITY;

        if (klass == byte.class || klass == Byte.class) {
            rangeMin = Byte.MIN_VALUE;
            rangeMax = Byte.MAX_VALUE;
        } else if (klass == short.class || klass == Short.class) {
            rangeMin = Short.MIN_VALUE;
            rangeMax = Short.MAX_VALUE;
        } else if (klass == int.class || klass == Integer.class) {
            rangeMin = Integer.MIN_VALUE;
            rangeMax = Integer.MAX_VALUE;
        } else if (klass == long.class || klass == Long.class) {
            rangeMin = Long.MIN_VALUE;
            rangeMax = Long.MAX_VALUE;
        }

        double current = minNumber;
        while (current < maxNumber) {
            if (current >= rangeMin && current <= rangeMax) {
                if (klass == byte.class || klass == Byte.class) {
                    result.add((T) ((Object) (byte) current));
                } else if (klass == short.class || klass == Short.class) {
                    result.add((T) ((Object) (short) current));
                } else if (klass == int.class || klass == Integer.class) {
                    result.add((T) ((Object) (int) current));
                } else if (klass == long.class || klass == Long.class) {
                    result.add((T) ((Object) (long) current));
                } else if (klass == float.class || klass == Float.class) {
                    result.add((T) ((Object) (float) current));
                } else {
                    result.add((T) ((Object) current));
                }
            } else {
                outsideRange = true;
            }
            current += stepSize;
        }

        if (outsideRange) {
            JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(parent),
                    "Numbers outside the supported range [" + rangeMin + ", " + rangeMax + "] have been ignored.",
                    getName(),
                    JOptionPane.WARNING_MESSAGE);
        }
        return result;
    }

    @JIPipeDocumentation(name = "Min number", description = "The minimum value in the range (inclusive)")
    @JIPipeParameter(value = "min-number", uiOrder = -50)
    public double getMinNumber() {
        return minNumber;
    }

    @JIPipeParameter("min-number")
    public void setMinNumber(double minNumber) {
        this.minNumber = minNumber;
    }

    @JIPipeDocumentation(name = "Max number", description = "The maximum value in the range (exclusive)")
    @JIPipeParameter(value = "max-number", uiOrder = -40)
    public double getMaxNumber() {
        return maxNumber;
    }

    @JIPipeParameter("max-number")
    public void setMaxNumber(double maxNumber) {
        this.maxNumber = maxNumber;
    }

    @JIPipeDocumentation(name = "Step size", description = "The difference between two following numbers in the range")
    @JIPipeParameter(value = "step-size", uiOrder = -30)
    public double getStepSize() {
        return stepSize;
    }

    @JIPipeParameter("step-size")
    public void setStepSize(double stepSize) {
        this.stepSize = stepSize;
    }

    @Override
    public String getName() {
        return "Number range";
    }

    @Override
    public String getDescription() {
        return "Generates a range of numbers";
    }
}
