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

package org.hkijena.pipelinej.extensions.parameters.primitives;

import org.hkijena.pipelinej.extensions.parameters.collections.ListParameter;

/**
 * A list of {@link Double}
 */
public class DoubleList extends ListParameter<Double> {
    /**
     * Creates a new empty list
     */
    public DoubleList() {
        super(Double.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public DoubleList(DoubleList other) {
        super(Double.class);
        addAll(other);
    }

    @Override
    public Double addNewInstance() {
        add(0.0);
        return 0.0;
    }
}
