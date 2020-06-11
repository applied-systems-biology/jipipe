package org.hkijena.acaq5.extensions.tables.parameters.processors;

import org.hkijena.acaq5.extensions.parameters.predicates.StringPredicate;
import org.hkijena.acaq5.extensions.parameters.functions.FunctionParameter;
import org.hkijena.acaq5.extensions.tables.parameters.enums.TableColumnConversionParameter;

/**
 * A parameter that models processing an input column via an conversion function
 * and generating an output column
 */
public class ConvertingTableColumnProcessorParameter extends FunctionParameter<StringPredicate, TableColumnConversionParameter, String> {

    public ConvertingTableColumnProcessorParameter() {
        super(StringPredicate.class, TableColumnConversionParameter.class, String.class);
        setInput(new StringPredicate());
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
