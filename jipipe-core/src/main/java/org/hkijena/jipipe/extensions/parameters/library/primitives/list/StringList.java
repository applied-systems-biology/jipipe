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

import java.util.Arrays;
import java.util.Collection;

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
     * Creates a new list from the provided items
     * @param items the items to add
     */
    public StringList(String... items) {
        super(String.class);
        this.addAll(Arrays.asList(items));
    }

    /**
     * Creates a new list from the provided items
     * @param items the items to add
     */
    public StringList(Collection<String> items) {
        super(String.class);
        addAll(items);
    }

    @Override
    public String addNewInstance() {
        add("");
        return "";
    }
}
