/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.labels;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.MeasurementExpressionParameterVariablesInfo;

import java.util.HashSet;
import java.util.Set;

public class LabelOverlapStatisticsVariablesInfo implements ExpressionParameterVariablesInfo {

    public static final Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("annotations", "Annotations map", "A map of annotations that are attached to the data batch"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("z", "Z", "The Z location of the label slice (the first index is zero)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("c", "Channel", "The channel location of the label slice (the first index is zero)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("t", "Frame", "The frame location of the label slice (the first index is zero)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("Label1.label_id", "Label 1 ID", "The ID of the first label"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("Label2.label_id", "Label 2 ID", "The ID of the second label"));
        for (JIPipeExpressionParameterVariableInfo variable : MeasurementExpressionParameterVariablesInfo.VARIABLES) {
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("Label1." + variable.getKey(), "Label 1 " + variable.getName(), "Label1. " + variable.getDescription()));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("Label2." + variable.getKey(), "Label 2 " + variable.getName(), "Label2. " + variable.getDescription()));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("Overlap." + variable.getKey(), "Label Overlap " + variable.getName(), "Overlap of first and second ROI. " + variable.getDescription()));
        }
    }

    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}
