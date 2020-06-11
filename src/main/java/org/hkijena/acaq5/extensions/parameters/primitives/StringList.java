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
