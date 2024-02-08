package org.hkijena.jipipe.extensions.ij3d.utils;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariablesInfo;

import java.util.HashSet;
import java.util.Set;

/**
 * A variable source that contains the ImageJ measurements.
 */
public class AllROI3DRelationMeasurementExpressionParameterVariablesInfo implements ExpressionParameterVariablesInfo {


    public static final Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        for (ROI3DRelationMeasurementColumn column : ROI3DRelationMeasurementColumn.values()) {
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("all." + column.getColumnName(), column.getName() + " (All values)", column.getDescription() + ". This variable contains an array of all measurements."));
        }
    }

    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}
