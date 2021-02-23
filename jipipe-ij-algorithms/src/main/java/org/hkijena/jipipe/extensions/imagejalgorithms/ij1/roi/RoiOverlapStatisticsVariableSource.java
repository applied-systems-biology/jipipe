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
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.MeasurementExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameterVariableSource;

import java.util.HashSet;
import java.util.Set;

public class RoiOverlapStatisticsVariableSource implements ExpressionParameterVariableSource {

    public static final Set<ExpressionParameterVariable> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        VARIABLES.add(new ExpressionParameterVariable("First ROI Z", "The Z location of the first ROI (first index is 1, zero indicates no Z constraint)", "First.z"));
        VARIABLES.add(new ExpressionParameterVariable("First ROI C", "The channel (C) location of the first ROI (first index is 1, zero indicates no C constraint)", "First.c"));
        VARIABLES.add(new ExpressionParameterVariable("First ROI T", "The frame (T) location of the first ROI (first index is 1, zero indicates no T constraint)", "First.t"));
        VARIABLES.add(new ExpressionParameterVariable("First ROI Name", "The name of the first ROI (empty string if not set)", "First.name"));
        VARIABLES.add(new ExpressionParameterVariable("Second ROI Z", "The Z location of the second ROI (first index is 1, zero indicates no Z constraint)", "Second.z"));
        VARIABLES.add(new ExpressionParameterVariable("Second ROI C", "The channel (C) location of the second ROI (first index is 1, zero indicates no C constraint)", "Second.c"));
        VARIABLES.add(new ExpressionParameterVariable("Second ROI T", "The frame (T) location of the second ROI (first index is 1, zero indicates no T constraint)", "Second.t"));
        VARIABLES.add(new ExpressionParameterVariable("Second ROI Name", "The name of the second ROI (empty string if not set)", "Second.name"));
        for (ExpressionParameterVariable variable : MeasurementExpressionParameterVariableSource.VARIABLES) {
            VARIABLES.add(new ExpressionParameterVariable("First ROI " + variable.getName(), "First ROI. " + variable.getDescription(), "First." + variable.getKey()));
            VARIABLES.add(new ExpressionParameterVariable("Second ROI " + variable.getName(), "Second ROI. " + variable.getDescription(), "Second." + variable.getKey()));
            VARIABLES.add(new ExpressionParameterVariable("ROI Overlap " + variable.getName(), "Overlap of first and second ROI. " + variable.getDescription(), "Overlap." + variable.getKey()));
        }
    }

    @Override
    public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}
