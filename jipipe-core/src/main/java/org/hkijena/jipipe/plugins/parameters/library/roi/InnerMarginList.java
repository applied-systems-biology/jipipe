package org.hkijena.jipipe.plugins.parameters.library.roi;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

/**
 * List parameter of {@link InnerMargin}
 */
public class InnerMarginList extends JIPipeListParameter<InnerMargin> {
    /**
     * Creates a new instance
     */
    public InnerMarginList() {
        super(InnerMargin.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public InnerMarginList(InnerMarginList other) {
        super(InnerMargin.class);
        for (InnerMargin rectangle : other) {
            add(new InnerMargin(rectangle));
        }
    }
}
