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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.labels;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.MeasurementExpressionParameterVariableSource;

import java.util.HashSet;
import java.util.Set;

public class LabelOverlapStatisticsVariableSource implements ExpressionParameterVariableSource {

    public static final Set<ExpressionParameterVariable> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        VARIABLES.add(new ExpressionParameterVariable("Annotations map", "A map of annotations that are attached to the data batch", "annotations"));
        VARIABLES.add(new ExpressionParameterVariable("Z", "The Z location of the label slice (the first index is zero)", "z"));
        VARIABLES.add(new ExpressionParameterVariable("Channel", "The channel location of the label slice (the first index is zero)", "c"));
        VARIABLES.add(new ExpressionParameterVariable("Frame", "The frame location of the label slice (the first index is zero)", "t"));
        VARIABLES.add(new ExpressionParameterVariable("Label 1 ID", "The ID of the first label", "Label1.label_id"));
        VARIABLES.add(new ExpressionParameterVariable("Label 2 ID", "The ID of the second label", "Label2.label_id"));
        for (ExpressionParameterVariable variable : MeasurementExpressionParameterVariableSource.VARIABLES) {
            VARIABLES.add(new ExpressionParameterVariable("Label 1 " + variable.getName(), "Label1. " + variable.getDescription(), "Label1." + variable.getKey()));
            VARIABLES.add(new ExpressionParameterVariable("Label 2 " + variable.getName(), "Label2. " + variable.getDescription(), "Label2." + variable.getKey()));
            VARIABLES.add(new ExpressionParameterVariable("Label Overlap " + variable.getName(), "Overlap of first and second ROI. " + variable.getDescription(), "Overlap." + variable.getKey()));
        }
    }

    @Override
    public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}
