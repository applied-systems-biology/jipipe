package org.hkijena.acaq5.extensions.tables.parameters.collections;

import org.hkijena.acaq5.extensions.parameters.collections.ListParameter;
import org.hkijena.acaq5.extensions.tables.parameters.processors.ConvertingTableColumnProcessorParameter;
import org.hkijena.acaq5.extensions.tables.parameters.processors.IntegratingTableColumnProcessorParameter;

public class ConvertingTableColumnProcessorParameterList extends ListParameter<ConvertingTableColumnProcessorParameter> {

    /**
     * Creates a  new instance
     */
    public ConvertingTableColumnProcessorParameterList() {
        super(ConvertingTableColumnProcessorParameter.class);
    }

    /**
     * Creates a copy
     * @param other the original
     */
    public ConvertingTableColumnProcessorParameterList(ConvertingTableColumnProcessorParameterList other) {
        super(ConvertingTableColumnProcessorParameter.class);
        for (ConvertingTableColumnProcessorParameter parameter : other) {
            add(new ConvertingTableColumnProcessorParameter(parameter));
        }
    }
}
