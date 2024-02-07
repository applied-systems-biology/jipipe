package org.hkijena.jipipe.extensions.imagejdatatypes.util.measure;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariablesInfo;

import java.util.HashSet;
import java.util.Set;

/**
 * A variable source that contains the ImageJ measurements.
 */
public class AllMeasurementExpressionParameterVariablesInfo implements ExpressionParameterVariablesInfo {

    public static final Set<ExpressionParameterVariable> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        for (MeasurementColumn column : MeasurementColumn.values()) {
            VARIABLES.add(new ExpressionParameterVariable(column.getName() + " (All values)", column.getDescription() + ". This variable contains an array of all measurements.", "all." + column.getColumnName()));
        }
    }

    @Override
    public Set<ExpressionParameterVariable> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}
