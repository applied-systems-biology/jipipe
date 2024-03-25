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

package org.hkijena.jipipe.plugins.parameters.library.primitives.list;

import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;

/**
 * A list of {@link Long}
 */
public class LongList extends ListParameter<Long> {
    /**
     * Creates a new empty list
     */
    public LongList() {
        super(Long.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public LongList(LongList other) {
        super(Long.class);
        addAll(other);
    }

    @Override
    public Long addNewInstance() {
        add(0L);
        return 0L;
    }
}
