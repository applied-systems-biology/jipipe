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

import org.hkijena.jipipe.extensions.parameters.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.parameters.pairs.PairParameter;
import org.hkijena.jipipe.extensions.parameters.pairs.PairParameterSettings;

/**
 * Processor-like parameter that maps a column generator to a string
 */
@PairParameterSettings(singleRow = false, keyLabel = "Generator", valueLabel = "Column name")
public class ExpressionTableColumnGeneratorProcessor extends PairParameter<DefaultExpressionParameter, String> {
    /**
     * Creates a new instance
     */
    public ExpressionTableColumnGeneratorProcessor() {
        super(DefaultExpressionParameter.class, String.class);
        setKey(new DefaultExpressionParameter("column + \": row \" + row"));
        setValue("");
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ExpressionTableColumnGeneratorProcessor(ExpressionTableColumnGeneratorProcessor other) {
        super(other);
    }
}
