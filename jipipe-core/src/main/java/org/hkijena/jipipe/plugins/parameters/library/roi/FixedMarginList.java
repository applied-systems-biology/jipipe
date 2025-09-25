package org.hkijena.jipipe.plugins.parameters.library.roi;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

/**
 * List parameter of {@link FixedMargin}
 */
public class FixedMarginList extends JIPipeListParameter<FixedMargin> {
    /**
     * Creates a new instance
     */
    public FixedMarginList() {
        super(FixedMargin.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public FixedMarginList(FixedMarginList other) {
        super(FixedMargin.class);
        for (FixedMargin rectangle : other) {
            add(new FixedMargin(rectangle));
        }
    }
}
