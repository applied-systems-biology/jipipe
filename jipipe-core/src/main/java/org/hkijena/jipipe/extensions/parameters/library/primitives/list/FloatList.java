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

package org.hkijena.jipipe.extensions.parameters.library.primitives.list;

import org.hkijena.jipipe.extensions.parameters.api.collections.ListParameter;

/**
 * A list of {@link Float}
 */
public class FloatList extends ListParameter<Float> {
    /**
     * Creates a new empty list
     */
    public FloatList() {
        super(Float.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public FloatList(FloatList other) {
        super(Float.class);
        addAll(other);
    }

    @Override
    public Float addNewInstance() {
        add(0.0f);
        return 0.0f;
    }
}
