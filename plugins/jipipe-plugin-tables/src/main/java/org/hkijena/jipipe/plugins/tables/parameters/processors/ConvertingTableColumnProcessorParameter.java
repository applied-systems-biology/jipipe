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

package org.hkijena.jipipe.plugins.tables.parameters.processors;

import org.hkijena.jipipe.plugins.expressions.StringQueryExpression;
import org.hkijena.jipipe.plugins.parameters.api.functions.FunctionParameter;
import org.hkijena.jipipe.plugins.tables.parameters.enums.TableColumnConversionParameter;

/**
 * A parameter that models processing an input column via an conversion function
 * and generating an output column
 */
public class ConvertingTableColumnProcessorParameter extends FunctionParameter<StringQueryExpression, TableColumnConversionParameter, String> {

    public ConvertingTableColumnProcessorParameter() {
        super(StringQueryExpression.class, TableColumnConversionParameter.class, String.class);
        setInput(new StringQueryExpression());
        setParameter(new TableColumnConversionParameter());
        setOutput("Output column");
    }

    public ConvertingTableColumnProcessorParameter(ConvertingTableColumnProcessorParameter other) {
        super(other);
    }

    @Override
    public String renderInputName() {
        return "Input column";
    }

    @Override
    public String renderParameterName() {
        return "Function";
    }

    @Override
    public String renderOutputName() {
        return "Output column";
    }
}
