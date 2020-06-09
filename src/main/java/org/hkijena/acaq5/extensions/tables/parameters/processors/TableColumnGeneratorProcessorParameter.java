package org.hkijena.acaq5.extensions.tables.parameters.processors;

import org.hkijena.acaq5.extensions.parameters.collections.KeyValuePairParameter;
import org.hkijena.acaq5.extensions.tables.parameters.enums.TableColumnGeneratorParameter;

/**
 * Processor-like parameter that maps a column generator to a string
 */
public class TableColumnGeneratorProcessorParameter extends KeyValuePairParameter<TableColumnGeneratorParameter, String> {
    /**
     * Creates a new instance
     */
    public TableColumnGeneratorProcessorParameter() {
        super(TableColumnGeneratorParameter.class, String.class);
        setValue("Output column name");
    }

    /**
     * Creates a copy
     * @param other the original
     */
    public TableColumnGeneratorProcessorParameter(TableColumnGeneratorProcessorParameter other) {
        super(other);
    }
}
