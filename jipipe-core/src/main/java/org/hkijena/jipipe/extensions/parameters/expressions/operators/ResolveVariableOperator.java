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

package org.hkijena.jipipe.extensions.parameters.expressions.operators;

import com.fathzer.soft.javaluator.StaticVariableSet;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionOperator;
import org.hkijena.jipipe.extensions.parameters.expressions.ParameterInfo;

import java.util.Iterator;

@JIPipeDocumentation(name = "Resolve variable", description = "Resolves the right operand as variable.")
public class ResolveVariableOperator extends ExpressionOperator {
    public ResolveVariableOperator() {
        super("$", 1, Associativity.RIGHT, 10);
    }

    @Override
    public Object evaluate(Iterator<Object> operands, Object evaluationContext) {
        String right = operands.next() + "";
        return ((StaticVariableSet<Object>)evaluationContext).get(right);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("name", "The variable name", String.class);
        }
        return null;
    }
}
