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

package org.hkijena.jipipe.extensions.parameters.library.primitives.list;

import org.hkijena.jipipe.extensions.parameters.api.collections.ListParameter;

/**
 * A list of {@link Integer}
 */
public class IntegerList extends ListParameter<Integer> {
    /**
     * Creates a new empty list
     */
    public IntegerList() {
        super(Integer.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public IntegerList(IntegerList other) {
        super(Integer.class);
        addAll(other);
    }

    @Override
    public Integer addNewInstance() {
        add(0);
        return 0;
    }
}