package org.hkijena.acaq5.extensions.tables.parameters.processors;

import org.hkijena.acaq5.extensions.parameters.pairs.Pair;
import org.hkijena.acaq5.extensions.tables.parameters.enums.TableColumnGeneratorParameter;

/**
 * Processor-like parameter that maps a column generator to a string
 */
public class TableColumnGeneratorProcessor extends Pair<TableColumnGeneratorParameter, String> {
    /**
     * Creates a new instance
     */
    public TableColumnGeneratorProcessor() {
        super(TableColumnGeneratorParameter.class, String.class);
        setValue("Output column name");
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public TableColumnGeneratorProcessor(TableColumnGeneratorProcessor other) {
        super(other);
    }
}
