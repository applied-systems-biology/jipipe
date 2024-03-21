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

package org.hkijena.jipipe.extensions.tables.parameters.collections;

import org.hkijena.jipipe.extensions.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.extensions.tables.parameters.processors.IntegratingTableColumnProcessorParameter;

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
