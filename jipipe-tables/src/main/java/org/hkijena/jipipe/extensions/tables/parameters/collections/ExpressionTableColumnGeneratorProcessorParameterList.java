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

package org.hkijena.jipipe.extensions.tables.parameters.collections;

import org.hkijena.jipipe.extensions.parameters.collections.ListParameter;
import org.hkijena.jipipe.extensions.tables.parameters.processors.ExpressionTableColumnGeneratorProcessor;

public class ExpressionTableColumnGeneratorProcessorParameterList extends ListParameter<ExpressionTableColumnGeneratorProcessor> {

    /**
     * Creates a  new instance
     */
    public ExpressionTableColumnGeneratorProcessorParameterList() {
        super(ExpressionTableColumnGeneratorProcessor.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ExpressionTableColumnGeneratorProcessorParameterList(ExpressionTableColumnGeneratorProcessorParameterList other) {
        super(ExpressionTableColumnGeneratorProcessor.class);
        for (ExpressionTableColumnGeneratorProcessor parameter : other) {
            add(new ExpressionTableColumnGeneratorProcessor(parameter));
        }
    }
}
