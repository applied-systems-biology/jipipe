/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.acaq5.extensions.tables.parameters.collections;

import org.hkijena.acaq5.extensions.parameters.collections.ListParameter;
import org.hkijena.acaq5.extensions.tables.parameters.processors.ConvertingTableColumnProcessorParameter;

public class ConvertingTableColumnProcessorParameterList extends ListParameter<ConvertingTableColumnProcessorParameter> {

    /**
     * Creates a  new instance
     */
    public ConvertingTableColumnProcessorParameterList() {
        super(ConvertingTableColumnProcessorParameter.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ConvertingTableColumnProcessorParameterList(ConvertingTableColumnProcessorParameterList other) {
        super(ConvertingTableColumnProcessorParameter.class);
        for (ConvertingTableColumnProcessorParameter parameter : other) {
            add(new ConvertingTableColumnProcessorParameter(parameter));
        }
    }
}
