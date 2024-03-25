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

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.ExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariableInfo;

import java.util.HashSet;
import java.util.Set;

/**
 * A variable source that contains the ImageJ measurements.
 */
public class ROI2DRelationMeasurementExpressionParameterVariablesInfo implements ExpressionParameterVariablesInfo {

    public static final Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        for (ROI2DRelationMeasurementColumn column : ROI2DRelationMeasurementColumn.values()) {
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo(column.getColumnName(), column.getName(), column.getDescription()));
        }
    }

    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}
