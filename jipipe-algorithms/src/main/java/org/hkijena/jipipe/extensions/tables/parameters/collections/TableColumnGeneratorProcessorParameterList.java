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
import org.hkijena.jipipe.extensions.tables.parameters.processors.TableColumnGeneratorProcessor;

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
