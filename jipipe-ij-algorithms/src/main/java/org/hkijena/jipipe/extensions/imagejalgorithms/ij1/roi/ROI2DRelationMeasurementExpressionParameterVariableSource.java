package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.roi;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariableSource;

import java.util.HashSet;
import java.util.Set;

/**
 * A variable source that contains the ImageJ measurements.
 */
public class ROI2DRelationMeasurementExpressionParameterVariableSource implements ExpressionParameterVariableSource {

    public static final Set<ExpressionParameterVariable> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        for (ROI2DRelationMeasurementColumn column : ROI2DRelationMeasurementColumn.values()) {
            VARIABLES.add(new ExpressionParameterVariable(column.getName(), column.getDescription(), column.getColumnName()));
        }
    }

    @Override
    public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}
