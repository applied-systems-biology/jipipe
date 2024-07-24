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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi;

import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.MeasurementExpressionParameterVariablesInfo;

import java.util.HashSet;
import java.util.Set;

public class RoiOverlapStatisticsVariablesInfo implements JIPipeExpressionVariablesInfo {

    public static final Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("annotations", "Annotations map", "A map of annotations that are attached to the data batch"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("ROI1.z", "ROI 1 Z", "The Z location of the first ROI (first index is 1, zero indicates no Z constraint)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("ROI1.c", "ROI 1 C", "The channel (C) location of the first ROI (first index is 1, zero indicates no C constraint)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("ROI1.t", "ROI 1 T", "The frame (T) location of the first ROI (first index is 1, zero indicates no T constraint)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("ROI1.name", "ROI 1 Name", "The name of the first ROI (empty string if not set)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("ROI2.z", "ROI 2 Z", "The Z location of the second ROI (first index is 1, zero indicates no Z constraint)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("ROI2.c", "ROI 2 C", "The channel (C) location of the second ROI (first index is 1, zero indicates no C constraint)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("ROI2.t", "ROI 2 T", "The frame (T) location of the second ROI (first index is 1, zero indicates no T constraint)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("ROI2.name", "ROI 2 Name", "The name of the second ROI (empty string if not set)"));
        for (JIPipeExpressionParameterVariableInfo variable : MeasurementExpressionParameterVariablesInfo.VARIABLES) {
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("ROI1." + variable.getKey(), "ROI 1 " + variable.getName(), "ROI1. " + variable.getDescription()));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("ROI2." + variable.getKey(), "ROI 2 " + variable.getName(), "ROI2. " + variable.getDescription()));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("Overlap." + variable.getKey(), "ROI Overlap " + variable.getName(), "Overlap of first and second ROI. " + variable.getDescription()));
        }
    }

    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}
