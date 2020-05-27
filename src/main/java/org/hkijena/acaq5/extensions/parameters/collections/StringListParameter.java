package org.hkijena.acaq5.extensions.parameters.collections;

/**
 * A list of {@link String}
 */
public class StringListParameter extends ListParameter<String> {
    /**
     * Creates a new empty list
     */
    public StringListParameter() {
        super(String.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringListParameter(StringListParameter other) {
        super(String.class);
        addAll(other);
    }

    @Override
    public String addNewInstance() {
        add("");
        return "";
    }
}
