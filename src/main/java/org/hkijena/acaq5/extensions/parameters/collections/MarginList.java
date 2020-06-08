package org.hkijena.acaq5.extensions.parameters.collections;

import org.hkijena.acaq5.extensions.parameters.roi.Margin;

/**
 * List parameter of {@link Margin}
 */
public class MarginList extends ListParameter<Margin> {
    /**
     * Creates a new instance
     */
    public MarginList() {
        super(Margin.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public MarginList(MarginList other) {
        super(Margin.class);
        for (Margin rectangle : other) {
            add(new Margin(rectangle));
        }
    }
}
