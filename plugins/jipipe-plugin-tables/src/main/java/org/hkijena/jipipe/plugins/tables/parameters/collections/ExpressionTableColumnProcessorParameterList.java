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

package org.hkijena.jipipe.plugins.tables.parameters.collections;

import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.plugins.tables.parameters.processors.ExpressionTableColumnProcessorParameter;

public class ExpressionTableColumnProcessorParameterList extends ListParameter<ExpressionTableColumnProcessorParameter> {

    /**
     * Creates a  new instance
     */
    public ExpressionTableColumnProcessorParameterList() {
        super(ExpressionTableColumnProcessorParameter.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ExpressionTableColumnProcessorParameterList(ExpressionTableColumnProcessorParameterList other) {
        super(ExpressionTableColumnProcessorParameter.class);
        for (ExpressionTableColumnProcessorParameter parameter : other) {
            add(new ExpressionTableColumnProcessorParameter(parameter));
        }
    }
}
