package org.hkijena.acaq5.extensions.tables.parameters.collections;

import org.hkijena.acaq5.extensions.parameters.collections.ListParameter;
import org.hkijena.acaq5.extensions.tables.parameters.processors.TableColumnGeneratorProcessor;

public class TableColumnGeneratorProcessorParameterList extends ListParameter<TableColumnGeneratorProcessor> {

    /**
     * Creates a  new instance
     */
    public TableColumnGeneratorProcessorParameterList() {
        super(TableColumnGeneratorProcessor.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public TableColumnGeneratorProcessorParameterList(TableColumnGeneratorProcessorParameterList other) {
        super(TableColumnGeneratorProcessor.class);
        for (TableColumnGeneratorProcessor parameter : other) {
            add(new TableColumnGeneratorProcessor(parameter));
        }
    }
}
