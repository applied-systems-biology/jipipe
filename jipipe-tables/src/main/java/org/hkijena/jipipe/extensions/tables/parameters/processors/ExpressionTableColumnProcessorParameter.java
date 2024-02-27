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

package org.hkijena.jipipe.extensions.tables.parameters.processors;

import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.parameters.api.functions.FunctionParameter;

/**
 * A parameter that models processing an input column via an conversion function
 * and generating an output column
 */
public class ExpressionTableColumnProcessorParameter extends FunctionParameter<StringQueryExpression, JIPipeExpressionParameter, String> {

    public ExpressionTableColumnProcessorParameter() {
        super(StringQueryExpression.class, JIPipeExpressionParameter.class, String.class);
        setInput(new StringQueryExpression());
        setParameter(new JIPipeExpressionParameter("values"));
        setOutput("Output column");
    }

    public ExpressionTableColumnProcessorParameter(ExpressionTableColumnProcessorParameter other) {
        super(other);
    }

    @Override
    public String renderInputName() {
        return "Input column";
    }

    @Override
    public String renderParameterName() {
        return "Expression";
    }

    @Override
    public String renderOutputName() {
        return "Output column";
    }
}
