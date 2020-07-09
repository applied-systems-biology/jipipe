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

package org.hkijena.acaq5.extensions.parameters.primitives;

import org.hkijena.acaq5.extensions.parameters.collections.ListParameter;

/**
 * A list of {@link String}
 */
public class StringList extends ListParameter<String> {
    /**
     * Creates a new empty list
     */
    public StringList() {
        super(String.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringList(StringList other) {
        super(String.class);
        addAll(other);
    }

    @Override
    public String addNewInstance() {
        add("");
        return "";
    }
}
