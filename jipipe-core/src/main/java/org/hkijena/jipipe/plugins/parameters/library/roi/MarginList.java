package org.hkijena.jipipe.plugins.parameters.library.roi;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

/**
 * List parameter of {@link Margin}
 */
public class MarginList extends JIPipeListParameter<Margin> {
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
