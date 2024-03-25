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

package org.hkijena.jipipe.plugins.imagejdatatypes.util;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.ExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariableInfo;

import java.util.HashSet;
import java.util.Set;

/**
 * Variable source that allows to address x, y
 */
public class PixelCoordinate2DExpressionParameterVariablesInfo implements ExpressionParameterVariablesInfo {

    private final static Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("width", "Image width", "The width of the image"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("height", "Image height", "The height of the image"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("x", "X coordinate", "The X coordinate within the image"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("y", "Y coordinate", "The Y coordinate within the image"));
    }

    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}
