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

package org.hkijena.jipipe.extensions.tables.parameters.processors;

import org.hkijena.jipipe.extensions.parameters.api.pairs.PairParameter;
import org.hkijena.jipipe.extensions.tables.parameters.enums.TableColumnGeneratorParameter;

/**
 * Processor-like parameter that maps a column generator to a string
 */
public class TableColumnGeneratorProcessor extends PairParameter<TableColumnGeneratorParameter, String> {
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
