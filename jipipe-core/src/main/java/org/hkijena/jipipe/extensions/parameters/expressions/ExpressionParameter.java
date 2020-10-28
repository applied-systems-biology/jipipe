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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * A parameter that contains an expression.
 * This allows users to set up filters etc. from within the UI.
 * Use {@link ExpressionParameterSettings} to control the behavior of this parameter.
 */
public class ExpressionParameter {
    private String expression = "";

    public ExpressionParameter() {
    }

    public ExpressionParameter(ExpressionParameter other) {
        this.expression = other.expression;
    }

    @JsonGetter("expression")
    public String getExpression() {
        return expression;
    }

    @JsonSetter("expression")
    public void setExpression(String expression) {
        this.expression = expression;
    }
}
