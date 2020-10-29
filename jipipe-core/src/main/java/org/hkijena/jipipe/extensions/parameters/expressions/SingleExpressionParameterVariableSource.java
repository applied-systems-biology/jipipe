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

package org.hkijena.jipipe.extensions.parameters.expressions;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;

import java.util.Collections;
import java.util.Set;

/**
 * {@link ExpressionParameterVariableSource} that contains exactly one variable.
 * Please note that this class cannot be used directly within {@link ExpressionParameterSettings}, as there is not default constructor available.
 */
public class SingleExpressionParameterVariableSource implements ExpressionParameterVariableSource {
    private final ExpressionParameterVariable variable;

    public SingleExpressionParameterVariableSource(String name, String description, String key) {
        this.variable = new ExpressionParameterVariable(name, description, key);
    }

    public SingleExpressionParameterVariableSource(ExpressionParameterVariable variable) {
        this.variable = variable;
    }

    @Override
    public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
        return Collections.singleton(variable);
    }
}
