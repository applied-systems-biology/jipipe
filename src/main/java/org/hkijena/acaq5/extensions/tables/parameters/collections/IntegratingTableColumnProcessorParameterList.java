package org.hkijena.acaq5.extensions.tables.parameters.collections;

import org.hkijena.acaq5.extensions.parameters.collections.ListParameter;
import org.hkijena.acaq5.extensions.tables.parameters.processors.IntegratingTableColumnProcessorParameter;

public class IntegratingTableColumnProcessorParameterList extends ListParameter<IntegratingTableColumnProcessorParameter> {

    /**
     * Creates a  new instance
     */
    public IntegratingTableColumnProcessorParameterList() {
        super(IntegratingTableColumnProcessorParameter.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public IntegratingTableColumnProcessorParameterList(IntegratingTableColumnProcessorParameterList other) {
        super(IntegratingTableColumnProcessorParameter.class);
        for (IntegratingTableColumnProcessorParameter parameter : other) {
            add(new IntegratingTableColumnProcessorParameter(parameter));
        }
    }
}
