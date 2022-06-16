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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.MeasurementExpressionParameterVariableSource;

import java.util.HashSet;
import java.util.Set;

public class RoiOverlapStatisticsVariableSource implements ExpressionParameterVariableSource {

    public static final Set<ExpressionParameterVariable> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        VARIABLES.add(new ExpressionParameterVariable("Annotations map", "A map of annotations that are attached to the data batch", "annotations"));
        VARIABLES.add(new ExpressionParameterVariable("ROI 1 Z", "The Z location of the first ROI (first index is 1, zero indicates no Z constraint)", "ROI1.z"));
        VARIABLES.add(new ExpressionParameterVariable("ROI 1 C", "The channel (C) location of the first ROI (first index is 1, zero indicates no C constraint)", "ROI1.c"));
        VARIABLES.add(new ExpressionParameterVariable("ROI 1 T", "The frame (T) location of the first ROI (first index is 1, zero indicates no T constraint)", "ROI1.t"));
        VARIABLES.add(new ExpressionParameterVariable("ROI 1 Name", "The name of the first ROI (empty string if not set)", "ROI1.name"));
        VARIABLES.add(new ExpressionParameterVariable("ROI 2 Z", "The Z location of the second ROI (first index is 1, zero indicates no Z constraint)", "ROI2.z"));
        VARIABLES.add(new ExpressionParameterVariable("ROI 2 C", "The channel (C) location of the second ROI (first index is 1, zero indicates no C constraint)", "ROI2.c"));
        VARIABLES.add(new ExpressionParameterVariable("ROI 2 T", "The frame (T) location of the second ROI (first index is 1, zero indicates no T constraint)", "ROI2.t"));
        VARIABLES.add(new ExpressionParameterVariable("ROI 2 Name", "The name of the second ROI (empty string if not set)", "ROI2.name"));
        for (ExpressionParameterVariable variable : MeasurementExpressionParameterVariableSource.VARIABLES) {
            VARIABLES.add(new ExpressionParameterVariable("ROI 1 " + variable.getName(), "ROI1. " + variable.getDescription(), "ROI1." + variable.getKey()));
            VARIABLES.add(new ExpressionParameterVariable("ROI 2 " + variable.getName(), "ROI2. " + variable.getDescription(), "ROI2." + variable.getKey()));
            VARIABLES.add(new ExpressionParameterVariable("ROI Overlap " + variable.getName(), "Overlap of first and second ROI. " + variable.getDescription(), "Overlap." + variable.getKey()));
        }
    }

    @Override
    public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}
