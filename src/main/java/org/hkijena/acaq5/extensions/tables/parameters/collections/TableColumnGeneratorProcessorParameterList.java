package org.hkijena.acaq5.extensions.tables.parameters.collections;

import org.hkijena.acaq5.extensions.parameters.collections.ListParameter;
import org.hkijena.acaq5.extensions.tables.parameters.processors.TableColumnGeneratorProcessorParameter;

public class TableColumnGeneratorProcessorParameterList extends ListParameter<TableColumnGeneratorProcessorParameter> {

    /**
     * Creates a  new instance
     */
    public TableColumnGeneratorProcessorParameterList() {
        super(TableColumnGeneratorProcessorParameter.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public TableColumnGeneratorProcessorParameterList(TableColumnGeneratorProcessorParameterList other) {
        super(TableColumnGeneratorProcessorParameter.class);
        for (TableColumnGeneratorProcessorParameter parameter : other) {
            add(new TableColumnGeneratorProcessorParameter(parameter));
        }
    }
}
