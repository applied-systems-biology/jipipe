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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi.filter;

import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.ImageJMeasurementsExpressionParameterVariablesInfo;

import java.util.HashSet;
import java.util.Set;

public class RoiOverlapMatchStatisticsVariablesInfo implements JIPipeExpressionVariablesInfo {

    public static final Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("annotations", "Annotations map", "A map of annotations that are attached to the iteration step"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("Candidate.z", "Candidate Z", "The Z location of the candidate ROI (first index is 1, zero indicates no Z constraint)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("Candidate.c", "Candidate C", "The channel (C) location of the candidate ROI (first index is 1, zero indicates no C constraint)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("Candidate.t", "Candidate T", "The frame (T) location of the candidate ROI (first index is 1, zero indicates no T constraint)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("Candidate.name", "Candidate Name", "The name of the candidate ROI (empty string if not set)"));
        for (JIPipeExpressionParameterVariableInfo variable : ImageJMeasurementsExpressionParameterVariablesInfo.VARIABLES) {
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("Candidate." + variable.getKey(), "Candidate " + variable.getName(), "Candidate ROI measurement. " + variable.getDescription()));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("all.Filter." + variable.getKey(), "Filter " + variable.getName() + " (all)", "Array of measurements from all matching filter ROIs. " + variable.getDescription()));
        }
    }

    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}
