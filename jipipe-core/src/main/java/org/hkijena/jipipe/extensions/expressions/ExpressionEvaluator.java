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

import com.fathzer.soft.javaluator.AbstractEvaluator;
import com.fathzer.soft.javaluator.Parameters;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;

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

    /**
     * Returns the boolean evaluated expression. If the expression does not return a boolean, an exception is thrown.
     *
     * @param expression the expression
     * @param variables  set of variables to use
     */
    public boolean test(String expression, ExpressionVariables variables) {
        Object result = evaluate(expression, variables);
        if (result instanceof Boolean) {
            return (boolean) result;
        } else {
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, new UnspecifiedValidationReportContext(),
                    "Expression does not return a boolean value: " + expression,
                    "Expression does not return a boolean!",
                    "You tried to evaluate the expression '" + expression + "', which did not return a boolean value (TRUE or FALSE).",
                    "Please check if you apply testing like for equality or if a value matches another value."));
        }
    }
}
