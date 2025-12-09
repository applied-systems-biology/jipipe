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

package org.hkijena.jipipe.plugins.ij3d.nodes.roi3d.filter;

import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesInfo;
import org.hkijena.jipipe.plugins.ij3d.utils.Roi3dMeasurementColumn;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.ImageJMeasurementsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.ij3d.utils.Roi3dRelationMeasurementColumn;

import java.util.HashSet;
import java.util.Set;

public class Roi3dOverlapStatisticsVariablesInfo implements JIPipeExpressionVariablesInfo {

    public static final Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("annotations", "Annotations map", "A map of annotations that are attached to the iteration step"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("Candidate.z", "Candidate Z", "The Z location of the candidate ROI (first index is 1, zero indicates no Z constraint)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("Candidate.c", "Candidate C", "The channel (C) location of the candidate ROI (first index is 1, zero indicates no C constraint)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("Candidate.name", "Candidate Name", "The name of the candidate ROI (empty string if not set)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("Filter.z", "Filter Z", "The Z location of the filter ROI (first index is 1, zero indicates no Z constraint)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("Filter.c", "Filter C", "The channel (C) location of the filter ROI (first index is 1, zero indicates no C constraint)"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("Filter.name", "Filter Name", "The name of the filter ROI (empty string if not set)"));
        
        // Add 3D-specific measurements
        for (Roi3dMeasurementColumn column : Roi3dMeasurementColumn.values()) {
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("Candidate." + column.getColumnName(), "Candidate " + column.getName(), "Candidate ROI measurement. " + column.getDescription()));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("Filter." +  column.getColumnName(), "Filter " + column.getName(), "Filter ROI measurement. " + column.getDescription()));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("Overlap." +  column.getColumnName(), "Overlap " + column.getName(), "Overlap of candidate and filter ROI measurement. " + column.getDescription()));
        }
    }

    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}