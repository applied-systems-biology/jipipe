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

package org.hkijena.jipipe.plugins.parameters.library.pairs;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;
import org.hkijena.jipipe.plugins.parameters.api.pairs.JIPipePairParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.ranges.IntegerRange;

/**
 * A parameter that renames an integer into another integer
 */
public class IntRangeAndIntegerPairParameter extends JIPipePairParameter<IntegerRange, Integer> {

    /**
     * Creates a new instance
     */
    public IntRangeAndIntegerPairParameter() {
        super(IntegerRange.class, Integer.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public IntRangeAndIntegerPairParameter(IntRangeAndIntegerPairParameter other) {
        super(other);
    }

}
