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

package org.hkijena.jipipe.api.nodes;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariablesInfo;

import java.util.HashSet;
import java.util.Set;

public class JIPipeCustomAnnotationMatchingExpressionVariables implements ExpressionParameterVariablesInfo {

    public final static Set<JIPipeExpressionParameterVariableInfo> EXPRESSION_PARAMETER_VARIABLES;

    static {
        EXPRESSION_PARAMETER_VARIABLES = new HashSet<>();
        EXPRESSION_PARAMETER_VARIABLES.add(new JIPipeExpressionParameterVariableInfo("annotations", "Current annotations", "Annotations of the current data item (string-string dictionary)"));
        EXPRESSION_PARAMETER_VARIABLES.add(new JIPipeExpressionParameterVariableInfo("other_annotations", "Other annotations", "Annotations of the other data item (string-string dictionary)"));
        EXPRESSION_PARAMETER_VARIABLES.add(new JIPipeExpressionParameterVariableInfo("exact_match_results", "Exact match result", "Result that would be obtained if thew 'Exact match' method is used (boolean)"));
    }

    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        return EXPRESSION_PARAMETER_VARIABLES;
    }
}
