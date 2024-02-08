/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.expressions;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;

import java.util.Collections;
import java.util.Set;

/**
 * {@link ExpressionParameterVariablesInfo} that contains exactly one variable.
 * Please note that this class cannot be used directly within {@link JIPipeExpressionParameterSettings}, as there is not default constructor available.
 */
public class SingleExpressionParameterVariablesInfo implements ExpressionParameterVariablesInfo {
    private final JIPipeExpressionParameterVariableInfo variable;

    public SingleExpressionParameterVariablesInfo(String name, String description, String key) {
        this.variable = new JIPipeExpressionParameterVariableInfo(key, name, description);
    }

    public SingleExpressionParameterVariablesInfo(JIPipeExpressionParameterVariableInfo variable) {
        this.variable = variable;
    }

    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        return Collections.singleton(variable);
    }
}
