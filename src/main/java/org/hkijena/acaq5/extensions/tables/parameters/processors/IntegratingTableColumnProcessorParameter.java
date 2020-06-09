package org.hkijena.acaq5.extensions.tables.parameters.processors;

import org.hkijena.acaq5.extensions.parameters.filters.StringFilter;
import org.hkijena.acaq5.extensions.parameters.functions.FunctionParameter;
import org.hkijena.acaq5.extensions.tables.parameters.enums.TableColumnIntegrationParameter;

/**
 * A parameter that models processing an input column via an integration function
 * and generating an output column
 */
public class IntegratingTableColumnProcessorParameter extends FunctionParameter<StringFilter, TableColumnIntegrationParameter, String> {

    public IntegratingTableColumnProcessorParameter() {
        super(StringFilter.class, TableColumnIntegrationParameter.class, String.class);
        setInput(new StringFilter());
        setParameter(new TableColumnIntegrationParameter());
        setOutput("Output column");
    }

    public IntegratingTableColumnProcessorParameter(IntegratingTableColumnProcessorParameter other) {
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
