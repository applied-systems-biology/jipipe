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

import java.util.Set;

/**
 * Provides the list of available variables.
 */
public interface ExpressionParameterVariableSource {
    /**
     * Returns the list of known variables for the user interface.
     *
     * @param parameterAccess the parameter access that holds the {@link AbstractExpressionParameter} instance.
     * @return the list of variables
     */
    Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess);
}
