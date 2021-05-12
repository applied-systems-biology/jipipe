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

import com.fathzer.soft.javaluator.Function;

import java.util.List;

/**
 * Function that can be utilized in {@link DefaultExpressionEvaluator}.
 */
public abstract class ExpressionFunction extends Function {
    public ExpressionFunction(String name, int argumentCount) {
        super(name, argumentCount);
    }

    public ExpressionFunction(String name, int minArgumentCount, int maxArgumentCount) {
        super(name, minArgumentCount, maxArgumentCount);
    }

    /**
     * Runs the function on given parameters
     *
     * @param parameters the parameters
     * @param variables  the set of current variables
     * @return the result
     */
    public abstract Object evaluate(List<Object> parameters, ExpressionParameters variables);

    /**
     * Returns info about the parameter at index
     *
     * @param index the parameter index
     * @return the info
     */
    public ParameterInfo getParameterInfo(int index) {
        return new ParameterInfo("x" + (index + 1), "");
    }

    /**
     * Returns a template function that can be used to get users started
     *
     * @return the template
     */
    public String getTemplate() {
        if (getMaximumArgumentCount() == Integer.MAX_VALUE)
            return getName() + "()";
        else
            return getSignature();
    }

    /**
     * Returns the signature of the function
     *
     * @return the signature
     */
    public String getSignature() {
        if (getMaximumArgumentCount() > 5) {
            return getName() + "(x1, x2, x3, x4, ...)";
        } else {
            StringBuilder result = new StringBuilder();
            result.append(getName()).append("(");
            for (int i = 0; i < getMaximumArgumentCount(); i++) {
                if (i != 0)
                    result.append(", ");
                result.append("x").append(i + 1);
            }
            result.append(")");
            return result.toString();
        }
    }
}
