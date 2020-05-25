package org.hkijena.acaq5.extensions.parameters.collections;

/**
 * A list of {@link Double}
 */
public class DoubleListParameter extends ListParameter<Double> {
    /**
     * Creates a new empty list
     */
    public DoubleListParameter() {
        super(Double.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public DoubleListParameter(DoubleListParameter other) {
        super(Double.class);
        addAll(other);
    }

    @Override
    public Double addNewInstance() {
        add(0.0);
        return 0.0;
    }
}
