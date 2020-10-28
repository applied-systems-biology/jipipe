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
     * @param parameters the parameters
     * @return the result
     */
    public abstract Object evaluate(List<Object> parameters);
}
