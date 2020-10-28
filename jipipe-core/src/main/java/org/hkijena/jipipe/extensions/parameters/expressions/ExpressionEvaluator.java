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

import com.fathzer.soft.javaluator.AbstractEvaluator;
import com.fathzer.soft.javaluator.Parameters;

/**
 * The base class for an evaluator
 */
public abstract class ExpressionEvaluator extends AbstractEvaluator<Object> {
    /**
     * Constructor.
     *
     * @param parameters The evaluator parameters.
     *                   <br>Please note that there's no side effect between the evaluator and the parameters.
     *                   So, changes made to the parameters after the call to this constructor are ignored by the instance.
     */
    protected ExpressionEvaluator(Parameters parameters) {
        super(parameters);
    }
}
